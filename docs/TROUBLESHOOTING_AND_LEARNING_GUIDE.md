# Panduan Pemecahan Masalah & Pembelajaran Teknikal (Engineering Learning Guide)

Dokumen ini mendokumentasikan secara rinci analisis akar masalah (*root cause analysis*), metodologi diagnosis, solusi implementasi, serta langkah pencegahan (*prevention*) dari berbagai tantangan dan perbaikan yang dihadapi pada proyek **Dramix Hub App**. Dokumen ini dirancang sebagai referensi belajar dan *post-mortem* arsitektur untuk masa mendatang.

---

## Daftar Isi
1. [Kasus 1: Infinite Scroll Duplikasi & Query Loop (MovieBox Pagination)](#kasus-1-infinite-scroll-duplikasi--query-loop-moviebox-pagination)
2. [Kasus 2: Kontrol Subtitle Tidak Konsisten & Granularitas Stepper](#kasus-2-kontrol-subtitle-tidak-konsisten--granularitas-stepper)
3. [Kasus 3: Pergeseran Alignment & Double Padding pada Jetpack Compose LazyRow](#kasus-3-pergeseran-alignment--double-padding-pada-jetpack-compose-lazyrow)
4. [Kasus 4: Redundansi Entry Point & Bloated State Management (Provider Customizer)](#kasus-4-redundansi-entry-point--bloated-state-management-provider-customizer)
5. [Kasus 5: Inadvertent Git Reset & Pencegahan Kehilangan Kode](#kasus-5-inadvertent-git-reset--pencegahan-kehilangan-kode)
6. [Kasus 6: Visual Stutter & Lagging Transisi Fullscreen (Compose Unmount & Insets Race Condition)](#kasus-6-visual-stutter--lagging-transisi-fullscreen-compose-unmount--insets-race-condition)
7. [Kasus 7: Migrasi Versioning Subtitle Preferences & Boundary Stepper Controls](#kasus-7-migrasi-versioning-subtitle-preferences--boundary-stepper-controls)
8. [Kasus 8: Filter Provider Inaktif pada Homescreen & Layer Preferensi Klien (Database Inactive Status Whitelist)](#kasus-8-filter-provider-inaktif-pada-homescreen--layer-preferensi-klien-database-inactive-status-whitelist)
9. [Kasus 9: Shimmer Loading KissKH & Live TV (Grid Column Mismatch 3-Kolom Portrait vs 2-Kolom Landscape)](#kasus-9-shimmer-loading-kisskh--live-tv-grid-column-mismatch-3-kolom-portrait-vs-2-kolom-landscape)
10. [Kasus 10: Sinkronisasi Status Provider Dinamis pada Pull-to-Refresh Homescreen (Tanpa Restart Aplikasi)](#kasus-10-sinkronisasi-status-provider-dinamis-pada-pull-to-refresh-homescreen-tanpa-restart-aplikasi)
11. [Kasus 11: Independensi & Dekomposisi State Video Zoom Portrait vs Fullscreen (Isolated Preferences Storage)](#kasus-11-independensi--dekomposisi-state-video-zoom-portrait-vs-fullscreen-isolated-preferences-storage)
12. [Kasus 12: Urutan Episode Terbalik & Bug Episode Bernomor 0 pada KissKH (Falsy JavaScript Evaluation & Ascending Sort)](#kasus-12-urutan-episode-terbalik--bug-episode-bernomor-0-pada-kisskh-falsy-javascript-evaluation--ascending-sort)
13. [Kasus 13: Error ExoPlayer UnrecognizedInputFormatException pada Episode Ongoing KissKH (Countdown Timer Widget vs Video Stream)](#kasus-13-error-exoplayer-unrecognizedinputformatexception-pada-episode-ongoing-kisskh-countdown-timer-widget-vs-video-stream)
14. [Kasus 14: Kegagalan Pemutaran Youku — Widevine CBCS DRM Decryption, Master Playlist Track Merging, Cleartext HTTP CDN & TextureView vs SurfaceView Secure Decoders](#kasus-14-kegagalan-pemutaran-youku--widevine-cbcs-drm-decryption-master-playlist-track-merging-cleartext-http-cdn--textureview-vs-surfaceview-secure-decoders)

---

## Kasus 1: Infinite Scroll Duplikasi, Pull-to-Refresh & Query Loop (MovieBox Pagination)

### Gejala Masalah
1. Saat pengguna menggulir (*scrolling*) katalog MovieBox pada kategori non-trending (seperti Drama, Film, Anime selain "Rekomendasi"), aplikasi terus-menerus memicu request halaman lanjutan (`page + 1`) padahal kategori tersebut tidak memiliki pagination.
2. Ketika pengguna melakukan **Tarik ke Bawah (Pull-to-Refresh)** pada kategori non-trending tersebut, `hasMoreContent` kembali aktif dan memicu infinite scroll kembali, mengakibatkan konten yang sama ter-append berulang kali (duplikasi item dari awal).

### Akar Masalah (Root Cause)
1. **Ketidaksinkronan Upstream MovieBox**: Microservice upstream MovieBox (`/content?opId=...`) untuk kategori seksi operasional (non-trending) mengabaikan parameter `page` dan selalu mengembalikan daftar film statis yang sama.
2. **Gateway Tidak Memutus Page > 1**: Pada `videos.pb.js`, permintaan `pageNum > 1` untuk kategori non-trending tetap diteruskan ke upstream, sehingga upstream mengembalikan item yang identik dan Gateway mengemasnya ulang sebagai halaman baru.
3. **State Residu di ViewModel Saat Refresh**: Di `HomeViewModel.kt`, method `refreshCurrentCategory()` dan `selectCategory()` tidak mereset `currentPage`, `hasMoreContent`, dan `isLoadingMore` secara eksplisit sebelum memulai pemuatan baru.
4. **Race Condition di Jetpack Compose `snapshotFlow`**: Trigger infinite scroll di `HomeScreen.kt` memantau `lastVisibleIndex >= total - 5`. Ketika pull-to-refresh dijalankan, list dikosongkan lalu diisi ulang; observer mendeteksi perubahan index dan memanggil `loadMoreVideos()` di tengah proses refresh karena tidak memeriksa flag `isLoadingContent` dan `isRefreshing`.
5. **Ketiadaan Deduplikasi di Sisi Client**: Pada `loadMoreVideos()`, item baru langsung digabungkan (`categoryVideos + feed.items`) tanpa memeriksa apakah ID item sudah ada di dalam list sebelumnya.

### Cara Mendiagnosis
1. **Network Payload Inspection**:
   ```http
   GET http://127.0.0.1:8090/api/modelles/videos?model_id=moviebox&category_id=1856704839045055408&page=2
   ```
   Sebelum perbaikan, respons mengembalikan data yang sama persis dengan `page=1`.
2. **Trace StateFlow Android**:
   Pantau `HomeUiState.hasMoreContent` saat pull-to-refresh dieksekusi. Terlihat `loadMoreVideos()` terpanggil seketika saat `categoryVideos` terisi kembali, memicu fetch `page=2` yang isinya menduplikasi `page=1`.

### Solusi & Implementasi Multi-Layer

#### 1. Gateway Level (`pocketbase/pb_hooks/videos.pb.js`)
Bedakan kategori trending dengan kategori operasional. Jika bukan trending dan `pageNum > 1`, langsung hentikan dan kembalikan array kosong dengan `has_more: false`:
```javascript
const isTrending = (String(categoryId) === "3521493905000087296");
if (!isTrending && pageNum > 1) {
    // Kategori selain Rekomendasi tidak memiliki pagination; kembalikan kosong jika page > 1
    hasMore = false;
    rawItems = [];
} else {
    // Fetch upstream & strictly validate boolean pager.has_more
    ...
    if (isTrending && res.json && res.json.pager && typeof res.json.pager.has_more === "boolean") {
        hasMore = res.json.pager.has_more;
    } else {
        hasMore = false;
    }
}
```

#### 2. ViewModel Level (`HomeViewModel.kt`)
Reset state pagination secara atomik saat pergantian kategori maupun pull-to-refresh, serta proteksi deduplikasi ID:
```kotlin
fun refreshCurrentCategory() {
    _uiState.value = _uiState.value.copy(
        categoryVideos = emptyList(),
        currentPage = 1,
        hasMoreContent = false,
        isLoadingMore = false,
        isLoadingContent = true
    )
    viewModelScope.launch {
        loadCategoryVideos(providerId, categoryId)
    }
}

fun loadMoreVideos() {
    val currentState = _uiState.value
    // Guard terhadap kondisi loading konten utama
    if (currentState.isLoadingMore || !currentState.hasMoreContent || currentState.isLoadingContent) return

    viewModelScope.launch {
        ...
        val existingIds = currentState.categoryVideos.map { it.id }.toSet()
        val newUniqueItems = feed.items.filter { it.id !in existingIds }
        // Otomatis matikan hasMoreContent jika tidak ada item baru yang unik
        val actuallyHasMore = feed.hasMore && newUniqueItems.isNotEmpty()

        _uiState.value = _uiState.value.copy(
            categoryVideos = currentState.categoryVideos + newUniqueItems,
            currentPage = nextPage,
            hasMoreContent = actuallyHasMore,
            isLoadingMore = false
        )
    }
}
```

#### 3. UI Level (`HomeScreen.kt`)
Tambahkan guard pada `snapshotFlow` infinite scroll agar tidak terpicu saat pull-to-refresh berlangsung:
```kotlin
LaunchedEffect(listState) {
    snapshotFlow {
        val lastVisibleIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
        val totalItems = listState.layoutInfo.totalItemsCount
        lastVisibleIndex to totalItems
    }.collect { (lastVisible, total) ->
        if (total > 0 && lastVisible >= total - 5 && 
            uiState.hasMoreContent && !uiState.isLoadingMore && 
            !uiState.isLoadingContent && !isRefreshing) {
            viewModel.loadMoreVideos()
        }
    }
}
```

### Pelajaran Penting (Lessons Learned)
- **Multi-Layer Defensive Design**: Masalah pagination duplikat harus dilindungi di 3 lapisan: (1) Gateway memotong request halaman melebihi kapasitas provider, (2) ViewModel memfilter duplikat ID dan mereset state saat refresh/switch kategori, dan (3) UI mencegah trigger scroll event saat operasi pembaruan berlangsung.
- **Upstream Semantics**: Jangan mengasumsikan endpoint REST API pihak ketiga berperilaku seragam. Identifikasi secara jelas mana seksi yang mendukung pagination dinamis (seperti trending) dan mana yang berupa seksi kurasi statis (seperti banner atau kategori tetap).

---

## Kasus 2: Kontrol Subtitle Tidak Konsisten & Granularitas Stepper

### Gejala Masalah
Pengguna merasa pengaturan teks subtitle terlalu kasar. Nilai *background padding* melompat kelipatan 2px (contoh: 8, 10, 12), sedangkan pengguna menginginkan kontrol presisi kelipatan 1px mulai dari batas minimum. Selain itu, ada keraguan apakah pengaturan subtitle tersimpan permanen saat aplikasi ditutup.

### Akar Masalah (Root Cause)
1. Di `PlayerSettingsMenu.kt`, parameter `onUpdateBgPadding` mempassing delta `-2` dan `+2`:
   ```kotlin
   // Bug: Step terlalu besar
   onDecrement = { onUpdateBgPadding(-2) },
   onIncrement = { onUpdateBgPadding(2) }
   ```
2. Meskipun `PlayerPreferences` sudah mengimplementasikan penyimpanan SharedPreferences, nilai default tidak memiliki batas aman (*boundary constraint*) yang seragam antara UI dan ViewModel.

### Cara Mendiagnosis
1. Buka player VOD, masuk ke Menu Pengaturan > Tab Subtitle.
2. Tekan tombol `+` pada *Background Padding*, amati label angka berubah dari `8px` ke `10px` bukan `9px`.
3. Telusuri stack pemanggilan: `PlayerSettingsMenu` -> `VodPlayerScreen` -> `VodPlayerViewModel` -> `PlayerPreferences`.

### Solusi & Implementasi
1. **Ubah Delta Stepper ke 1px di UI**:
   ```kotlin
   StepperSettingRow(
       label = "Background Padding",
       valueText = "${subtitleStyle.backgroundPaddingPx}px",
       onDecrement = { onUpdateBgPadding(-1) },
       onIncrement = { onUpdateBgPadding(1) },
       canDecrement = subtitleStyle.backgroundPaddingPx > 8,
       canIncrement = subtitleStyle.backgroundPaddingPx < 48
   )
   ```
2. **Kunci Nilai Menggunakan `coerceIn` di ViewModel**:
   ```kotlin
   fun updateSubtitleBackgroundPadding(delta: Int, isFullscreen: Boolean = true) {
       val current = getActiveSubtitleStyle(isFullscreen)
       val newPadding = (current.backgroundPaddingPx + delta).coerceIn(8, 48)
       persistSubtitleStyle(current.copy(backgroundPaddingPx = newPadding), isFullscreen)
   }
   ```
3. **Pemisahan Mode Portrait & Fullscreen di Local Storage**:
   `PlayerPreferences` menyimpan key terpisah untuk mode portrait (`sub_portrait_*`) dan fullscreen (`sub_fs_*`) sehingga preferensi layar penuh tidak merusak tata letak layar tegak.

### Pelajaran Penting (Lessons Learned)
- Terapkan validasi ganda (*dual validation*): di UI cegah tombol diklik jika mencapai batas (`canDecrement = current > min`), dan di ViewModel lindungi dengan `coerceIn(min, max)` untuk menjaga konsistensi state.

---

## Kasus 3: Pergeseran Alignment & Double Padding pada Jetpack Compose LazyRow

### Gejala Masalah
Pada detail video VOD, baris daftar pemeran (aktor & kru) tampak menjorok terlalu jauh ke kanan dibandingkan judul bagian "Pemeran & Kru". Item pertama tidak sejajar dengan margin kiri kontainer utama. Selain itu, teks nama dan peran aktor berjarak terlalu renggang.

### Akar Masalah (Root Cause)
1. **Double Padding**: Komponen induk (`Column`) sudah memiliki padding horizontal dari layar atau parent (`16.dp`). Namun, `LazyRow` di dalamnya kembali diberi `contentPadding = PaddingValues(horizontal = 16.dp)`. Akibatnya, item pertama menerima margin total 32dp ke kiri.
2. **Spasi Teks Berlebih**: Komponen teks nama dan role dipisahkan oleh `Spacer(modifier = Modifier.height(3.dp))` yang terlalu besar untuk ukuran font 10–12sp.

### Cara Mendiagnosis
1. Aktifkan **Layout Inspector** di Android Studio atau periksa hirarki composable:
   ```
   DetailColumn (padding 16.dp)
     ├── Text("Pemeran & Kru") -> Berada di x = 16.dp
     └── LazyRow (contentPadding 16.dp)
           └── CastMemberItem[0] -> Berada di x = 32.dp (TIDAK SEJAJAR)
   ```

### Solusi & Implementasi
1. **Hapus Redundant Content Padding**:
   ```kotlin
   // SEBELUM
   LazyRow(
       contentPadding = PaddingValues(horizontal = 16.dp),
       horizontalArrangement = Arrangement.spacedBy(12.dp)
   )
   
   // SESUDAH
   LazyRow(
       horizontalArrangement = Arrangement.spacedBy(12.dp)
   )
   ```
2. **Rapatkan Jarak Nama dan Peran**:
   ```kotlin
   Text(text = member.name, ...)
   Spacer(modifier = Modifier.height(2.dp)) // Diturunkan dari 3.dp ke 2.dp
   Text(text = roleText, ...)
   ```

### Pelajaran Penting (Lessons Learned)
- Saat meletakkan `LazyRow` di dalam kontainer yang sudah memiliki padding lateral, gunakan `contentPadding` hanya jika Anda menginginkan efek *clip to padding* dengan scroll tembus batas layar. Jika ingin sejajar lurus dengan judul di atasnya, hilangkan `contentPadding` horizontal.

---

## Kasus 4: Redundansi Entry Point & Bloated State Management (Provider Customizer)

### Gejala Masalah
Fitur kustomisasi provider (urutan dan visibilitas provider) dapat diakses dari tiga tempat berbeda sekaligus:
1. Ikon di Top Navigation Bar (sebelah search).
2. Chip "Atur" di baris daftar provider HomeScreen.
3. Item menu "Kustomisasi Provider" di ProfileScreen.

Hal ini membebani antarmuka, membingungkan pengguna (*decision fatigue*), serta menduplikasi state flow dan injeksi Koin di beberapa ViewModel dan Composable screen.

### Akar Masalah (Root Cause)
Fitur ditambahkan secara bertahap tanpa konsolidasi arsitektur UI, menyebabkan kode duplikat:
- Di `ProfileScreen.kt`, terdapat pemanggilan `ProviderPreferences`, `CatalogRepository`, `showProviderCustomizer` modal state, dan coroutine scope lokal yang hanya dipakai untuk satu aksi ini.
- Di `HomeScreen.kt`, terdapat chip khusus di awal list yang memperpanjang scroll horizontal provider.

### Cara Mendiagnosis
Lakukan audit antarmuka (*UI navigation flow audit*): temukan semua pemanggilan `openProviderCustomizer` atau `ProviderCustomizerSheet`. Evaluasi titik akses mana yang paling kontekstual dan ergonomis bagi pengguna.

### Solusi & Implementasi
1. **Sentralisasi ke Top Navigation Bar**:
   Navbar atas adalah tempat permanen yang selalu terlihat saat menjelajah konten di beranda. Titik akses ini dipertahankan.
2. **Eliminasi di HomeScreen**:
   Hapus chip "Atur" dari `ProviderChipsRow` dan hapus parameter `onCustomizeClick` dari kontrak fungsi.
3. **Pembersihan Bersih (*Clean Refactor*) di ProfileScreen**:
   Hapus seluruh state berikut dari `ProfileScreen.kt`:
   ```kotlin
   // Dihapus sepenuhnya
   val providerPreferences: ProviderPreferences = koinInject()
   val catalogRepository: CatalogRepository = koinInject()
   var showProviderCustomizer by remember { mutableStateOf(false) }
   var providerConfigs by remember { mutableStateOf(...) }
   ```
   Hapus pula `ProviderCustomizerSheet` dialog rendering block dan 10 import yang tidak lagi terpakai.

### Pelajaran Penting (Lessons Learned)
- **Single Source of Access untuk Fitur Konfigurasi**: Fitur sekunder/konfigurasi tidak boleh disebar di banyak layar utama. Satu entry point yang jelas dan konsisten jauh lebih baik daripada tiga titik redundan.
- **Dead Code Hygiene**: Saat menghapus komponen antarmuka, hapus pula state, coroutine scope, dan dependensi DI yang mengikatnya agar memori aplikasi tetap hemat.

---

## Kasus 5: Inadvertent Git Reset & Pencegahan Kehilangan Kode

### Gejala Masalah
Sebelum perbaikan, sempat terjadi ketidaksengajaan `git reset` atau pergantian branch yang membatalkan sejumlah modifikasi fitur (seperti penghapusan hero banner, navigasi 3-tab, dan kartu VIP).

### Akar Masalah (Root Cause)
1. Mengembangkan beberapa fitur berbeda secara bersamaan di working tree tanpa melakukan commit bertahap (*atomic save points*).
2. Tidak adanya file changelog lokal atau remote backup yang mencatat state terakhir dari file yang telah diuji.

### Cara Menangani & Mencegah
1. **Pola Save Point Berbasis Commit**:
   Gunakan prinsip:
   $$\text{Implementasi Slice} \longrightarrow \text{Verifikasi/Test} \longrightarrow \text{Commit} \longrightarrow \text{Next Slice}$$
2. **Dokumentasi Changelog Real-Time**:
   Buat file `CHANGELOG.md` dan arsip tanggalan sebelum melakukan operasi git yang berisiko.
3. **Pemanfaatan Git Reflog**:
   Jika terjadi `git reset --hard` yang tidak disengaja, commit lama tidak langsung terhapus dari disk Git. Temukan SHA commit dengan perintah:
   ```powershell
   git reflog
   git checkout <SHA-terakhir-sebelum-reset>
   ```
4. **Push Teratur ke Remote**:
   Selalu sinkronkan branch utama ke remote repository (`git push origin <branch>`) setelah serangkaian pengujian perangkat fisik berhasil.

---

## Kasus 6: Visual Stutter & Lagging Transisi Fullscreen (Compose Unmount & Insets Race Condition)

### Gejala Masalah
Saat pengguna menekan tombol fullscreen pada pemutar video (`VodPlayerScreen`) untuk pertama kalinya:
1. Layar sempat mengalami *glitch* visual / stutter selama 200–400 milidetik.
2. Video tidak langsung memenuhi seluruh bentang layar secara instan, melainkan tertahan oleh padding atau blank frame sesaat sebelum akhirnya meluas penuh ke tepi display.
3. Transisi berikutnya terasa lebih cepat daripada transisi pertama.

### Akar Masalah (Root Cause)
1. **Unmount & Re-inflate AndroidView/PlayerView (Penyebab Utama)**:
   - Pada implementasi awal, `VideoPlayerSurface` diletakkan di dua cabang terpisah:
     ```kotlin
     if (isFullscreen) {
         Box(Modifier.fillMaxSize()) {
             VideoPlayerSurface(...)
         }
     } else {
         Column {
             Box(Modifier.aspectRatio(16f/9f)) {
                 VideoPlayerSurface(...)
             }
         }
     }
     ```
   - Ketika `isFullscreen` berubah dari `false` ke `true`, Jetpack Compose menafsirkan ini sebagai dua node yang berbeda. Node portrait di-*dispose* (memanggil pembersihan `player = null`), lalu node landscape baru di-*inflate* dari XML layout (`PlayerViewBinding.inflate`).
   - Akibatnya, Media3/ExoPlayer terpaksa melepaskan hardware `SurfaceView`, mengalokasikan surface baru di GPU buffer, dan memasang ulang decoder. Ini memakan waktu CPU/GPU yang signifikan pada cold start.
2. **Animasi Insets Global dari Scaffold (`innerPadding`)**:
   - Di `AppNavigation.kt`, `NavHost` dibungkus dengan `Modifier.padding(innerPadding)`.
   - `innerPadding` secara default mengonsumsi System Bars (Status Bar + Navigation Bar).
   - Saat masuk mode fullscreen dan System Bars mulai di-hide, OS menganimasikan penyusutan System Bars dari ~30dp ke 0dp secara asinkron.
   - Karena `NavHost` terikat pada `innerPadding`, seluruh layar VOD Player ikut tertahan dan tertekan selama animasi insets berlangsung.
3. **Display Cutout (Notch) & Asynchronous Insets Controller**:
   - Pemanggilan `insetsController.hide(WindowInsetsCompat.Type.systemBars())` awalnya ditaruh di dalam `LaunchedEffect(isFullscreen)`. `LaunchedEffect` berjalan setelah siklus komposisi pertama selesai, bukan instan saat klik tombol.
   - Window belum mengonfigurasi `layoutInDisplayCutoutMode = LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES`, sehingga WindowManager Android harus melakukan komputasi penataan ulang boundary notch saat rotasi ke landscape.

### Solusi & Implementasi Multi-Layer

#### 1. Unified Player Viewport (`VodPlayerScreen.kt`)
Satukan `VideoPlayerSurface`, `SubtitleOverlay`, dan `VodPlayerOverlay` ke dalam satu node yang persisten (tidak pernah di-unmount/dispose):
```kotlin
val activeSubtitleStyle = if (isFullscreen) uiState.fullscreenSubtitleStyle else uiState.portraitSubtitleStyle

Column(
    modifier = Modifier
        .fillMaxSize()
        .then(if (!isFullscreen) Modifier.statusBarsPadding() else Modifier)
) {
    Box(
        modifier = if (isFullscreen) {
            Modifier.fillMaxSize().background(Color.Black)
        } else {
            Modifier.fillMaxWidth().aspectRatio(16f / 9f).clipToBounds().background(Color.Black)
        }
    ) {
        // Node ini tidak pernah di-destroy saat toggle fullscreen
        VideoPlayerSurface(
            player = viewModel.playerController.player,
            modifier = Modifier.fillMaxSize(),
            subtitleStyle = activeSubtitleStyle,
            videoZoom = uiState.videoZoom
        )
        ...
    }
}
```

#### 2. Bypass `innerPadding` Scaffold untuk Route Player (`AppNavigation.kt`)
Cegah `Scaffold` memberikan padding dinamis yang menganimasikan ukuran `NavHost` pada rute VOD Player:
```kotlin
val isVodPlayer = currentRoute?.startsWith("vod_player") == true

NavHost(
    navController = navController,
    startDestination = Screen.Home.route,
    modifier = Modifier
        .fillMaxSize()
        .padding(if (isVodPlayer) PaddingValues(0.dp) else innerPadding)
        .background(PureBlack)
)
```

#### 3. Sinkronisasi Instan & Short Edges Cutout (`MainActivity.kt` & `VodPlayerScreen.kt`)
Izinkan window menembus area cutout/notch kamera dan sembunyikan System Bars seketika di callback klik:
```kotlin
// MainActivity.kt
if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
    window.attributes.layoutInDisplayCutoutMode =
        android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
}

// VodPlayerScreen.kt
fun toggleFullscreen() {
    val target = !isFullscreen
    isFullscreen = target
    activity?.requestedOrientation = if (target) {
        ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
    } else {
        ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }
    // Eksekusi langsung tanpa menunggu LaunchedEffect
    activity?.window?.let { window ->
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        if (target) {
            insetsController.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            insetsController.show(WindowInsetsCompat.Type.systemBars())
        }
    }
}
```

### Pencegahan ke Depan
- **Jangan pernah membuat dua instance `AndroidView` untuk player/surface yang sama dalam percabangan kondisi UI (`if-else`)**. Gunakan satu instance dan ubah modifier layout-nya secara deklaratif.
- **Waspadai `innerPadding` Scaffold pada layar Media/Player Edge-to-Edge**. Layar pemutar video harus mengelola insets secara mandiri.

---

## Kasus 7: Migrasi Versioning Subtitle Preferences & Boundary Stepper Controls

### Gejala Masalah
1. Pengaturan default subtitle baru (misal font Arial, outline medium, font size 20px / 14px, opacity 0%, line spacing 0px, bg padding 0px) tidak ter-apply pada perangkat yang sudah pernah menyimpan preferensi subtitle versi lama di SharedPreferences.
2. Tombol stepper (-) dan (+) untuk `Background Padding` dan `Line Spacing` di menu pengaturan player sempat tidak merespons perubahan atau memiliki batas bawah yang tidak sesuai (misal padding tertahan di 8px bukannya bisa 0px).

### Akar Masalah (Root Cause)
1. **Ketiadaan Skema Migrasi Preferences**: `PlayerPreferences` memuat data lama dari SharedPreferences menggunakan default fallback hanya jika key belum pernah ada. Jika key sudah tersimpan dengan default versi sebelumnya, nilai lama tersebut terus dipakai.
2. **Batasan Hardcoded pada Stepper**: Di `PlayerSettingsMenu.kt` dan `VodPlayerViewModel.kt`, nilai padding dibatasi dengan `coerceIn(8, 48)` bukannya rentang fleksibel `0` hingga `20`.
3. **Ketiadaan Binding Callback**: Callback `onUpdateLineSpacing` dan `onUpdateBgPadding` belum terhubung sepenuhnya dari menu popover ke ViewModel di kedua mode portrait dan fullscreen.

### Solusi & Implementasi
1. **Skema Versioning di `PlayerPreferences.kt`**:
   Tambahkan key `KEY_SUBTITLE_CONFIG_VERSION` dan konstanta `CURRENT_SUBTITLE_VERSION = 2`. Jika versi tersimpan lebih rendah, timpa preferensi lama dengan nilai default standar yang baru:
   ```kotlin
   private const val KEY_SUBTITLE_CONFIG_VERSION = "pref_subtitle_config_version"
   private const val CURRENT_SUBTITLE_VERSION = 2

   private fun migrateSubtitlePreferencesIfNeeded() {
       val storedVersion = prefs.getInt(KEY_SUBTITLE_CONFIG_VERSION, 1)
       if (storedVersion < CURRENT_SUBTITLE_VERSION) {
           // Simpan default baru untuk fullscreen dan portrait
           saveSubtitleConfig(SubtitleStyleConfig.DEFAULT_FULLSCREEN, isFullscreen = true)
           saveSubtitleConfig(SubtitleStyleConfig.DEFAULT_PORTRAIT, isFullscreen = false)
           prefs.edit().putInt(KEY_SUBTITLE_CONFIG_VERSION, CURRENT_SUBTITLE_VERSION).apply()
       }
   }
   ```
2. **Boundary Stepper 0–20px**:
   Perbarui clamping delta update di `VodPlayerViewModel.kt`:
   ```kotlin
   val newPadding = (currentStyle.backgroundPadding + delta).coerceIn(0, 20)
   val newLineSpacing = (currentStyle.lineSpacing + delta).coerceIn(0, 20)
   ```
3. **Penyambungan Callback**: Hubungkan callback `onUpdateLineSpacing` dan `onUpdateBgPadding` di `VodPlayerOverlay.kt` dan `VodPlayerScreen.kt` dengan parameter `isFullscreen`.

---

## Kasus 8: Filter Provider Inaktif pada Homescreen & Layer Preferensi Klien (Database Inactive Status Whitelist)

### Gejala Masalah
Saat status salah satu provider diubah menjadi `inactive` pada database (misalnya `dramaboxbaru` yang dinonaktifkan di panel admin PocketBase):
1. Provider tersebut masih berpotensi muncul sebagai chip di `HomeScreen` jika tersimpan di cache lokal atau dimuat dari daftar kustomisasi preferensi (`ProviderPreferences`).
2. Jika pengguna mengklik chip provider yang inaktif, aplikasi akan menampilkan error `ERR_CAT_001` atau memuat layar kosong karena endpoint upstream menolak melayani provider yang tidak aktif.

### Akar Masalah (Root Cause)
1. **Ketiadaan Validasi Helper `isActive` di Domain Model**: `ProviderModel` hanya menyimpan field `status: String = "active"`, namun tidak menyediakan pengecekan status terstandardisasi yang aman terhadap huruf besar/kecil (`case-insensitive`) atau default fallback.
2. **Bypass di Layer Preferensi**: Fungsi `applyToProviders()` dan `getMergedConfigItems()` di `ProviderPreferences.kt` memetakan provider yang tersimpan di SharedPreferences tanpa memvalidasi apakah provider tersebut masih berstatus aktif di database. Jika user pernah menyimpan preferensi provider sebelum dinonaktifkan, ID provider inaktif tersebut tetap masuk ke dalam list aktif.
3. **Komponen UI Chip Mengonsumsi List Tanpa Filter**: `ProviderChipsRow` di `HomeScreen.kt` dan `SearchProviderChipsRow` di `SearchScreen.kt` langsung mengiterasi parameter `providers` tanpa memfilter `it.isActive`.

### Solusi & Implementasi Multi-Layer

#### 1. Tambah Helper `isActive` pada `ProviderModel` (`CatalogModels.kt`)
```kotlin
data class ProviderModel(
    val id: String,
    val name: String,
    val iconUrl: String? = null,
    val description: String? = null,
    val contentType: String = "long_drama",
    val status: String = "active"
) {
    val isActive: Boolean
        get() = status.equals("active", ignoreCase = true)
}
```

#### 2. Sanitasi Ketat di `ProviderPreferences.kt`
Pastikan data raw selalu difilter sebelum dicocokkan dengan preferensi lokal:
```kotlin
fun applyToProviders(rawProviders: List<ProviderModel>): List<ProviderModel> {
    val activeProviders = rawProviders.filter { it.isActive }
    val saved = loadConfigsFromPrefs()
    if (saved.isEmpty()) return activeProviders

    val providerMap = activeProviders.associateBy { it.id }
    val result = mutableListOf<ProviderModel>()
    val seenIds = mutableSetOf<String>()

    for (cfg in saved) {
        val prov = providerMap[cfg.id]
        if (prov != null) {
            seenIds.add(prov.id)
            if (cfg.isEnabled) {
                result.add(prov)
            }
        }
    }

    for (prov in activeProviders) {
        if (prov.id !in seenIds) {
            result.add(prov)
        }
    }

    return if (result.isEmpty()) activeProviders else result
}
```

#### 3. Proteksi UI di `HomeScreen.kt` & `SearchScreen.kt`
Gunakan `remember(providers)` untuk memastikan hanya provider aktif yang dirender pada chip list:
```kotlin
private fun ProviderChipsRow(
    providers: List<ProviderModel>,
    selectedProviderId: String?,
    onProviderSelected: (String) -> Unit
) {
    val activeProviders = remember(providers) {
        providers.filter { it.isActive }
    }
    // Render LazyRow hanya menggunakan activeProviders
}
```

#### 4. Validasi Pemilihan di `HomeViewModel.kt`
Cegah pemilihan provider inaktif baik melalui event klik maupun saat pergantian `contentType`:
```kotlin
fun selectProvider(providerId: String) {
    if (_uiState.value.selectedProviderId == providerId) return
    val prov = _uiState.value.providers.firstOrNull { it.id == providerId && it.isActive }
    if (prov == null) return
    // lanjutkan muat kategori
}
```

### Pencegahan ke Depan
- **Prinsip Defense in Depth**: Jangan hanya mengandalkan filter di sisi server. Selalu terapkan filter status di model domain klien dan di level komponen UI perenderan.
- **Unit Test Komprehensif**: Sertakan skenario pengujian dengan data mock yang berisi provider `active` dan `inactive` untuk memastikan entitas inaktif tidak lolos ke state UI.

---

## Kasus 9: Shimmer Loading KissKH & Live TV (Grid Column Mismatch 3-Kolom Portrait vs 2-Kolom Landscape)

### Gejala Masalah
1. Saat pengguna berpindah ke provider **KissKH** atau tab berkategori konten landscape/Live TV, kartu skeleton shimmer yang muncul saat loading awal berformat **3 kolom portrait** (aspect ratio 2:3).
2. Ketika respons feed selesai dimuat dari backend, tampilan tiba-tiba berubah secara drastis (*layout shift / jarring jump*) menjadi **2 kolom landscape** (aspect ratio 16:9).
3. Transisi visual tersebut tampak kasar dan mengurangi kualitas polish antarmuka aplikasi.

### Akar Masalah (Root Cause)
1. **Komponen Shimmer Statis**: `FeedShimmerGrid` di `HomeScreen.kt` dikonfigurasi secara hardcoded menggunakan `GridCells.Fixed(3)` dan kartu item berskala portrait (`aspectRatio(2f / 3f)`).
2. **Ketiadaan Konteks Provider pada State Loading**: Saat kategori sedang dimuat (`isLoadingFeed = true`), `HomeScreen` tidak meneruskan ID provider atau flag tipe konten ke fungsi `FeedShimmerGrid`, sehingga komponen perender skeleton tidak dapat mengadaptasi tata letak kolomnya dengan bentuk konten aktual yang akan datang.

### Solusi & Implementasi
1. Teruskan parameter `providerId` dan `contentType` ke dalam `FeedShimmerGrid`.
2. Hitung kondisi tata letak landscape secara adaptif:
   ```kotlin
   val isKissKH = providerId?.equals("kisskh", ignoreCase = true) == true
   val isLiveTv = contentType.equals("live_tv", ignoreCase = true)
   val isLandscape = isKissKH || isLiveTv
   val columns = if (isLandscape) 2 else 3
   val cardAspectRatio = if (isLandscape) 16f / 9f else 2f / 3f
   ```
3. Gunakan `GridCells.Fixed(columns)` dan aplikasikan `cardAspectRatio` pada setiap kartu shimmer.

### Pencegahan ke Depan
- **Satu Desain Sesuai Realita Output**: Komponen loading skeleton/shimmer harus selalu mencerminkan struktur grid dan rasio aspek dari data aktual yang akan dirender agar layout shift tidak terjadi (*zero visual jumping*).

---

## Kasus 10: Sinkronisasi Status Provider Dinamis pada Pull-to-Refresh Homescreen (Tanpa Restart Aplikasi)

### Gejala Masalah
1. Ketika status suatu provider dinonaktifkan di panel admin/database PocketBase saat aplikasi sedang berjalan, pengguna masih melihat chip provider tersebut di HomeScreen.
2. Melakukan gesture **Tarik ke Bawah (Pull-to-Refresh)** hanya me-refresh item video kategori saat ini (`refreshCurrentCategory()`), namun tidak memperbarui daftar chip provider.
3. Pengguna terpaksa harus mematikan dan membuka ulang aplikasi (*force restart*) agar chip provider yang inaktif menghilang.

### Akar Masalah (Root Cause)
1. **Scope Refresh Terlalu Sempit**: Pull-to-refresh di `HomeScreen.kt` hanya memanggil `viewModel.refreshCurrentCategory()`.
2. **Fetch Provider Hanya di `init`**: `HomeViewModel` hanya memanggil `catalogRepository.getProviders()` sekali pada blok `init`. Tidak ada mekanisme untuk memvalidasi ulang daftar provider aktif saat lifecycle berjalan.

### Solusi & Implementasi
1. Tambahkan metode `refreshHome()` di `HomeViewModel.kt`:
   ```kotlin
   fun refreshHome() {
       viewModelScope.launch {
           _uiState.value = _uiState.value.copy(isRefreshing = true)
           try {
               val rawProviders = catalogRepository.getProviders()
               val activeProviders = providerPreferences.applyToProviders(rawProviders)
                   .filter { it.isActive }
               
               val currentSelected = _uiState.value.selectedProviderId
               val providerStillActive = activeProviders.any { it.id == currentSelected }
               val targetProvider = if (providerStillActive) {
                   currentSelected
               } else {
                   activeProviders.firstOrNull()?.id
               }

               _uiState.value = _uiState.value.copy(
                   providers = activeProviders,
                   selectedProviderId = targetProvider
               )

               if (targetProvider != null) {
                   loadCategoriesForProvider(targetProvider)
               }
           } catch (e: Exception) {
               // Fallback ke refresh kategori saat ini jika gagal fetch provider
               refreshCurrentCategory()
           } finally {
               _uiState.value = _uiState.value.copy(isRefreshing = false)
           }
       }
   }
   ```
2. Hubungkan `HomeScreen.kt` pull-to-refresh listener ke `viewModel.refreshHome()`.

### Pencegahan ke Depan
- **Refresh Komprehensif**: Tindakan pull-to-refresh pada layar root/katalog utama harus menyegarkan konfigurasi struktural (ketersediaan provider) selain hanya menyegarkan item feed anak.

---

## Kasus 11: Independensi & Dekomposisi State Video Zoom Portrait vs Fullscreen (Isolated Preferences Storage)

### Gejala Masalah
1. Pengaturan video zoom (misal 100%, 110%, 125%, dst.) yang diubah saat video diputar di mode portrait (rasio vertikal) ikut diterapkan ketika video diputar di mode landscape/fullscreen.
2. Pengguna sering kali menginginkan rasio pembesaran berbeda untuk kedua orientasi: di portrait biasanya 100% (fit) agar tidak terpotong, sedangkan di fullscreen landscape sering disetel ke 110%–125% untuk menghilangkan letterbox hitam pada layar rasio 20:9.
3. Menyimpan zoom di portrait menimpa pengaturan zoom fullscreen, dan sebaliknya.

### Akar Masalah (Root Cause)
1. **Shared Single Preference Key**: `PlayerPreferences.kt` hanya menyimpan zoom pada satu key umum `pref_video_zoom`.
2. **Monolitik UI State**: `VodPlayerUiState` hanya memiliki satu field `videoZoom: String = "100%"`.
3. **Ketiadaan Orientasi Target**: Fungsi `updateVideoZoom(delta)` dan `selectVideoZoom(zoom)` tidak mengetahui apakah aksi pembesaran dipicu dari controller portrait atau fullscreen.

### Solusi & Implementasi Multi-Layer
1. **Dekomposisi Key Preferences (`PlayerPreferences.kt`)**:
   ```kotlin
   private const val KEY_VIDEO_ZOOM_PORTRAIT = "pref_video_zoom_portrait"
   private const val KEY_VIDEO_ZOOM_FULLSCREEN = "pref_video_zoom_fullscreen"

   fun getVideoZoom(isFullscreen: Boolean = false): String {
       val key = if (isFullscreen) KEY_VIDEO_ZOOM_FULLSCREEN else KEY_VIDEO_ZOOM_PORTRAIT
       val saved = prefs.getString(key, null)
       if (saved != null) return saved
       return prefs.getString(KEY_VIDEO_ZOOM, "100%") ?: "100%"
   }

   fun saveVideoZoom(zoom: String, isFullscreen: Boolean = false) {
       val key = if (isFullscreen) KEY_VIDEO_ZOOM_FULLSCREEN else KEY_VIDEO_ZOOM_PORTRAIT
       prefs.edit().putString(key, zoom).apply()
   }
   ```
2. **Dekomposisi UI State (`VodPlayerViewModel.kt`)**:
   Pisahkan `portraitVideoZoom` dan `fullscreenVideoZoom` di `VodPlayerUiState`. Saat inisialisasi, baca masing-masing nilai dari preferences. Saat update, perbarui state dan preferences khusus untuk orientasi aktif (`isFullscreen`).
3. **Binding di UI Layar Pemutar (`VodPlayerScreen.kt`)**:
   Oper nilai zoom yang sesuai ke `VideoPlayerSurface`:
   ```kotlin
   val activeZoom = if (isFullscreen) uiState.fullscreenVideoZoom else uiState.portraitVideoZoom
   VideoPlayerSurface(
       ...
       videoZoom = activeZoom
   )
   ```

### Pencegahan ke Depan
- **Orientasi-Aware State**: Properti pemutar yang memiliki preferensi ergonomis berbeda antara orientasi vertikal dan horizontal (seperti ukuran font subtitle, margin subtitle, dan rasio zoom video) wajib dipisahkan baik di tingkat UI state maupun persistence layer.

---

## Kasus 12: Urutan Episode Terbalik & Bug Episode Bernomor 0 pada KissKH (Falsy JavaScript Evaluation & Ascending Sort)

### Gejala Masalah
1. Drama pada provider KissKH menyajikan daftar episode terbalik: episode terbaru/terakhir berada di posisi paling atas/depan, sementara Episode 1 berada di urutan paling belakang. Akibatnya, pemutar otomatis memilih episode terbaru yang mungkin belum rilis.
2. Pada drama yang memiliki episode prolog / Episode 0 (seperti *100 Days of Deception* yang memiliki episode 0, 1, dan 2), sistem salah membaca dan menampilkan Episode 0 sebagai Episode 3 (atau episode bernomor 1 ke atas).

### Akar Masalah (Root Cause)
1. **Upstream Descending Order**: API upstream KissKH (`/api/DramaList/Drama/{id}`) mengembalikan daftar episode dalam urutan descending (episode terbaru pertama).
2. **Evaluasi Falsy pada Nilai 0 di JavaScript (`detail.pb.js`)**:
   ```javascript
   // Kode lama yang bermasalah:
   number: parseInt(ep.number, 10) || (i + 1),
   title: "Episode " + (ep.number || (i + 1))
   ```
   Di JavaScript, angka `0` bernilai *falsy*. Ketika `ep.number` adalah `0`, ekspresi `0 || (i + 1)` mengabaikan nilai 0 dan mengevaluasi ke index fallback `(i + 1)`. Pada array berisi 3 item di mana episode 0 berada di index ke-2, hasilnya menjadi `2 + 1 = 3`. Episode 0 pun hilang dan berganti nama menjadi "Episode 3".

### Solusi & Implementasi Multi-Layer
1. **Sorting Ascending Terstandarisasi (`detail.pb.js` & `CatalogRepositoryImpl.kt`)**:
   Urutkan array episode berdasarkan nilai numerik `number` sebelum proses pemetaan:
   ```javascript
   rawEps.sort(function(a, b) {
       var numA = (a && a.number !== undefined && a.number !== null && !isNaN(Number(a.number)))
           ? Number(a.number) : 999999;
       var numB = (b && b.number !== undefined && b.number !== null && !isNaN(Number(b.number)))
           ? Number(b.number) : 999999;
       return numA - numB;
   });
   ```
2. **Validasi Numerik Eksplisit (Preservasi Episode 0)**:
   Gunakan pengecekan eksplisit `ep.number !== undefined && !isNaN(Number(ep.number))` agar angka 0 tidak dianggap falsy.
3. **Penyortiran Sisi Klien (`CatalogRepositoryImpl.kt`)**:
   Sebagai perlindungan berlapis, repository Android menyortir kembali daftar episode secara ascending:
   ```kotlin
   if (modelId.equals("kisskh", ignoreCase = true)) {
       val sortedSeasons = domain.seasons.map { season ->
           season.copy(
               episodes = season.episodes.sortedWith(
                   compareBy<Episode> { it.number }.thenBy { it.id }
               )
           )
       }
       domain.copy(seasons = sortedSeasons)
   }
   ```

### Pencegahan ke Depan
- **Hindari Logical OR (`||`) untuk Angka di JavaScript**: Jangan pernah gunakan operator `||` untuk menetapkan nilai default jika tipe data adalah angka, karena `0` akan tereliminasi secara keliru. Gunakan operator nullish coalescing (`??`) atau validasi eksplisit `!== undefined && !== null`.

---

## Kasus 13: Error ExoPlayer UnrecognizedInputFormatException pada Episode Ongoing KissKH (Countdown Timer Widget vs Video Stream)

### Gejala Masalah
1. Saat pengguna memutar episode drama KissKH yang masih berlangsung/ongoing dan belum dirilis resminya, pemutar video crash/error menampilkan pesan:
   `UnrecognizedInputFormatException: None of the available extractors (FlvExtractor, FlacExtractor, WavExtractor, ...) could read the stream`.
2. Pengguna tidak mendapatkan informasi kapan episode tersebut akan tayang, melainkan hanya layar hitam dengan dialog error pemutaran video.

### Akar Masalah (Root Cause)
1. **Penyatuan Stream URL Timer**: Pada API KissKH, episode yang belum dirilis mengembalikan data dengan `Type = 2` atau URL video yang mengarah ke widget countdown pihak ketiga (`https://www.tickcounter.com/widget/countdown/...`).
2. **ExoPlayer Membaca HTML Widget sebagai Video Media**: Gateway sebelumnya memasukkan URL widget ini ke dalam array `streams` dengan format `m3u8`. Akibatnya, ExoPlayer mencoba mengunduh halaman web HTML tersebut dan menganggapnya sebagai chunk kontainer media HLS, memicu kegagalan extractor.

### Solusi & Implementasi Multi-Layer
1. **Deteksi Countdown Stream di Backend Gateway (`streamController.js` & `source.pb.js`)**:
   Pisahkan URL timer dari list stream video playable:
   ```javascript
   const isTimer = streamData.Type === 2 || 
                   rawUrl.includes('tickcounter.com') || 
                   rawUrl.includes('countdown');
   if (isTimer) {
       countdownUrl = rawUrl;
       isCountdown = true;
       // JANGAN masukkan ke dalam array streams
   }
   ```
2. **Deteksi Sisi Klien di Repository Android (`CatalogRepositoryImpl.kt`)**:
   Tangkap URL timer jika lolos ke respons client dan tandai `isCountdown = true`.
3. **Komponen Khusus `VodPlayerCountdownOverlay.kt`**:
   Rancang overlay elegan di layar pemutar yang memuat WebView widget TickCounter secara langsung, menyembunyikan kontrol pemutaran video (karena tidak ada video yang diputar), dan menyertakan tombol "Cek Ketersediaan" (*Refresh*) agar pengguna dapat memeriksa rilis episode sewaktu-waktu.

### Pencegahan ke Depan
- **Validasi Format Stream Sebelum Dikirim ke Pemutar**: Pastikan URL stream media yang diteruskan ke ExoPlayer benar-benar merupakan manifest media (.m3u8, .mpd) atau file kontainer (.mp4), bukan URL halaman web atau widget embedding.

---

## Kasus 14: Kegagalan Pemutaran Youku — Widevine CBCS DRM Decryption, Master Playlist Track Merging, Cleartext HTTP CDN & TextureView vs SurfaceView Secure Decoders

### Gejala Masalah
1. Saat pengguna memutar drama atau episode dari provider Youku, layar pemutar hanya menampilkan layar hitam pekat (*black screen*), animasi buffering tidak kunjung selesai, dan tidak ada audio maupun gambar yang muncul.
2. Pada investigasi logcat awal, muncul error berturut-turut:
   - `UnrecognizedInputFormatException: None of the available extractors ... could read the stream` saat membaca playlist HLS Youku.
   - `Cleartext HTTP traffic not permitted` saat ExoPlayer mencoba mengunduh chunk segmen video dari domain CDN pihak ketiga Youku.
   - `IllegalStateException (DefaultDrmSession.requiresSecureDecoder)` akibat kegagalan request provisioning sertifikat perangkat.
   - Status 202 `drm type error` dari server lisensi DRM Youku saat Android client meminta decoding kunci.
3. Setelah alur DRM lisensi diperbaiki hingga berhasil (`states: 0` dan `"wvpl license gen succ"`), **audio terdengar berjalan lancar, namun video tetap black screen dan tampilan tampak freeze/kaku**. Logcat sistem menampilkan peringatan GPU berulang:
   `GPUAUX : [AUX]_AUXPrepareSrcImageInfo: skip, cannot convert protect / secure buffer`.

### Akar Masalah (Root Cause)
1. **Enkripsi DRM CBCS / Widevine Modular**:
   - Berbeda dari provider video umum yang menyajikan stream HLS tanpa enkripsi (*clear*), stream Youku diproteksi menggunakan enkripsi Common Encryption Pattern AES-CBCS (`sample-aes` / `cbcs`) dengan sistem perlindungan Google Widevine Modular (UUID: `edef8ba9-79d6-4ace-a3c8-27dcd51d21ed`).
   - Informasi enkripsi tidak diletakkan pada tag `#EXT-X-KEY` standar di playlist m3u8, melainkan di dalam init fragment MP4 (`_video_init.mp4`) pada box `pssh` (Protection System Specific Header). Tanpa DRM Session Manager terkonfigurasi, ExoPlayer tidak dapat menginisialisasi decoder.
2. **Protokol Lisensi Kustom Youku (Form-Encoded Base64)**:
   - Endpoint lisensi Youku (`https://drm-license.youku.tv/ups/drm.json`) tidak menggunakan payload binary OCTET-STREAM standar Android Widevine.
   - Server lisensi mewajibkan HTTP POST dengan format `application/x-www-form-urlencoded` yang membawa parameter sesi (`token`, `vid`, `utdid`, `psid`, `drmType=widevine`, dll.) dan Base64-encoded binary challenge pada parameter `licenseRequest`.
   - Respons server berupa JSON (`{"data": "<base64_key>", "states": 0, "msg": "wvpl license gen succ"}`).
   - Jika POST dikirimkan ke URL yang masih membawa query string lama (`?drmType=cbcs...`), server mengalami konflik parameter dan membalas dengan status 202 `drm type error`.
3. **Kegagalan Delegasi Provisioning Sertifikat Widevine**:
   - Perangkat Android tertentu membutuhkan provisioning sertifikat dari server Google (`https://www.googleapis.com/certificateprovisioning/...`) sebelum sesi DRM pertama dapat dibuka.
   - Implementasi kustom `MediaDrmCallback` yang mencoba menangani `executeProvisionRequest` secara manual dengan request POST kosong memicu kegagalan network, sehingga `DefaultDrmSession` gagal memperoleh secure crypto object dan memicu `IllegalStateException` saat memeriksa `requiresSecureDecoder`.
4. **Pemisahan Track Audio dan Video (Sub-Playlist vs Master Playlist)**:
   - Upstream API Youku menyajikan stream dalam bentuk sub-playlist terpisah: video m3u8 hanya berisi segmen video (`_video_00001.mp4`), sedangkan audio disajikan terpisah (`_audio_00001.mp4`).
   - URL default `s.url` pada upstream hanya menunjuk ke video sub-playlist (tanpa deklarasi `#EXT-X-MEDIA:TYPE=AUDIO`). Tanpa `master_url`, ExoPlayer memutar video tanpa track audio sama sekali.
5. **Inferensi Format Manifest `/playlist/m3u8`**:
   - URL manifest Youku berformat `https://pl-ali.youku.tv/playlist/m3u8?vid=...`. Karena path berakhir dengan `/m3u8` dan bukan ekstensi file `.m3u8`, ExoPlayer gagal mengenali container type secara otomatis dan menganggapnya sebagai file MP4 progresif, memicu `UnrecognizedInputFormatException`.
6. **Blokir Lalu Lintas HTTP Cleartext CDN**:
   - Server CDN Youku (`valipl10.cp31.ott.cibntv.net`) menyajikan potongan media melalui HTTP port 80 (bukan HTTPS). Android 9 (API 28)+ secara default memblokir semua lalu lintas HTTP cleartext, menyebabkan download segmen gagal seketika.
7. **Inkompatibilitas TextureView vs Hardware Secure Decoder (L1 Widevine)**:
   - Ini merupakan penyebab utama audio berputar normal tetapi video tetap black screen dan freeze.
   - Pada layout `item_player_view.xml`, komponen `PlayerView` dikonfigurasi dengan:
     ```xml
     app:surface_type="texture_view"
     ```
   - Ketika media terenkripsi Widevine diputar, ExoPlayer menginisialisasi hardware secure decoder (`c2.mtk.avc.decoder.secure` pada chipset MediaTek / Qualcomm). Hardware secure decoder mengalirkan frame video terdekripsi secara eksklusif ke dalam secure buffer memori TEE (*Trusted Execution Environment*).
   - `TextureView` beroperasi dengan mengekspos frame video sebagai OpenGL ES texture yang harus di-composite oleh GPU driver aplikasi. Karena secure buffer tidak boleh diakses oleh subsistem GPU non-secure demi proteksi hak cipta, GPU driver menolak memproses frame:
     `GPUAUX : skip, cannot convert protect / secure buffer`.
   - Akibatnya tidak ada satu pun frame video yang berhasil digambar ke layar, sedangkan decoder audio (`c2.android.aac.decoder`) beroperasi tanpa pembatasan secure surface sehingga audio tetap bersuara normal.

### Cara Mendiagnosis
1. **Pemeriksaan Header Playlist & Init Segment**:
   ```bash
   curl -s "https://valipl10.cp31.ott.cibntv.net/..._video_init.mp4" | grep -a "pssh"
   ```
   Ditemukan box PSSH dengan SystemID `edef8ba9-79d6-4ace-a3c8-27dcd51d21ed`, mengonfirmasi stream membutuhkan Widevine decryption.
2. **Inspeksi Respons Server Lisensi**:
   Mengirim POST dengan challenge biner Widevine ke `https://drm-license.youku.tv/ups/drm.json` memverifikasi struktur form-data yang dibutuhkan serta mendeteksi bahwa pengiriman parameter di query URL memicu `states: 202` (`drm type error`).
3. **Trace Logcat MediaCodec & GPU Buffer**:
   ```bash
   adb logcat | grep -E "GPUAUX|gralloc|c2.mtk|MediaCodec|DefaultDrmSession"
   ```
   Menemukan log `c2.mtk.avc.decoder.secure` berhasil terhubung ke surface dan DRM berstatus `wvpl license gen succ`, tetapi diikuti ratusan pesan `GPUAUX : skip, cannot convert protect / secure buffer`. Ini adalah bukti definitif bahwa tipe surface yang digunakan (`TextureView`) tidak dapat merender secure buffer hardware.

### Solusi & Implementasi Multi-Layer

#### 1. Transformasi SurfaceView Hardware Overlay (`item_player_view.xml`)
Ubah konfigurasi permukaan render `PlayerView` dari `texture_view` menjadi `surface_view`:
```xml
<!-- app/src/main/res/layout/item_player_view.xml -->
<androidx.media3.ui.PlayerView xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:id="@+id/player_view"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    app:surface_type="surface_view"
    app:use_controller="false"
    app:show_buffering="never" />
```
`SurfaceView` mengalokasikan hardware overlay terpisah yang terhubung langsung ke SurfaceFlinger (`BLASTBufferQueue`), memungkinkan secure decoder menuliskan frame langsung ke display plane tanpa perlu melalui GPU texture compositor.

#### 2. Implementasi Kustom Widevine DRM Callback (`WidevineDrmCallback.kt`)
Bangun bridge kustom yang menerjemahkan protokol biner ExoPlayer ke protokol Form-Data Youku:
```kotlin
@OptIn(UnstableApi::class)
class WidevineDrmCallback(
    private val defaultLicenseUrl: String?,
    private val licenseParams: Map<String, String>,
    private val headers: Map<String, String>,
    private val okHttpClient: OkHttpClient
) : MediaDrmCallback {

    private val defaultDrmCallback = HttpMediaDrmCallback(
        defaultLicenseUrl,
        DefaultHttpDataSource.Factory()
    )

    override fun executeProvisionRequest(
        uuid: UUID,
        request: ExoMediaDrm.ProvisionRequest
    ): ByteArray {
        // Delegasikan provisioning sertifikat perangkat ke handler bawaan ExoPlayer
        return defaultDrmCallback.executeProvisionRequest(uuid, request)
    }

    override fun executeKeyRequest(
        uuid: UUID,
        request: ExoMediaDrm.KeyRequest
    ): ByteArray {
        val rawTargetUrl = request.licenseServerUrl.takeIf { !it.isNullOrBlank() }
            ?: defaultLicenseUrl
            ?: "https://drm-license.youku.tv/ups/drm.json"
        
        // Bersihkan query string agar tidak konflik dengan form body
        val targetUrl = rawTargetUrl.substringBefore("?")
        val challengeBase64 = Base64.encodeToString(request.data, Base64.NO_WRAP)

        val formBodyBuilder = FormBody.Builder()
        var hasDrmType = false
        for ((key, value) in licenseParams) {
            if (key.equals("drmType", ignoreCase = true)) {
                formBodyBuilder.add("drmType", "widevine")
                hasDrmType = true
            } else if (!key.equals("licenseRequest", ignoreCase = true)) {
                formBodyBuilder.add(key, value)
            }
        }
        if (!hasDrmType) {
            formBodyBuilder.add("drmType", "widevine")
        }
        formBodyBuilder.add("licenseRequest", challengeBase64)

        val req = Request.Builder()
            .url(targetUrl)
            .post(formBodyBuilder.build())
            .build()

        okHttpClient.newCall(req).execute().use { response ->
            val respString = response.body?.string() ?: ""
            val json = JSONObject(respString)
            val states = json.optInt("states", -1)
            val dataB64 = json.optString("data", "")
            if (dataB64.isNotBlank() && (states == 0 || states == 1)) {
                return Base64.decode(dataB64, Base64.DEFAULT)
            } else {
                val msg = json.optString("msg", "states=$states")
                throw IOException("DRM license error: $msg ($states)")
            }
        }
    }
}
```

#### 3. Konfigurasi DefaultDrmSessionManager di Player Controller (`DramixPlayerController.kt`)
Aktifkan manajemen multi-session untuk track audio dan video yang terenkripsi terpisah, serta toleransi clear sample:
```kotlin
if (drmConfig != null && (!drmConfig.licenseUrl.isNullOrBlank() || drmConfig.licenseParams.isNotEmpty())) {
    val drmCallback = WidevineDrmCallback(
        defaultLicenseUrl = drmConfig.licenseUrl,
        licenseParams = drmConfig.licenseParams,
        headers = headers,
        okHttpClient = headerDataSourceFactory.getOkHttpClient()
    )
    val drmSessionManager = DefaultDrmSessionManager.Builder()
        .setUuidAndExoMediaDrmProvider(
            C.WIDEVINE_UUID,
            FrameworkMediaDrm.DEFAULT_PROVIDER
        )
        .setMultiSession(true)
        .setPlayClearSamplesWithoutKeys(true)
        .build(drmCallback)

    val mediaSourceFactory = DefaultMediaSourceFactory(headerDataSourceFactory)
        .setDrmSessionManagerProvider { drmSessionManager }

    val mediaSource = mediaSourceFactory.createMediaSource(mediaItem)
    player.setMediaSource(mediaSource, /* resetPosition = */ true)
}
```

#### 4. Inferensi Eksplisit MIME Type HLS (`DramixPlayerController.kt`)
Tangani URL path `/m3u8` agar tidak disalahartikan sebagai format kontainer lain:
```kotlin
val inferredMimeType = when {
    streamFormat?.equals("m3u8", ignoreCase = true) == true ||
        streamFormat?.equals("hls", ignoreCase = true) == true ||
        streamUrl.contains(".m3u8", ignoreCase = true) ||
        streamUrl.contains("/m3u8", ignoreCase = true) -> MimeTypes.APPLICATION_M3U8
    streamFormat?.equals("mpd", ignoreCase = true) == true ||
        streamFormat?.equals("dash", ignoreCase = true) == true ||
        streamUrl.contains(".mpd", ignoreCase = true) -> MimeTypes.APPLICATION_MPD
    streamFormat?.equals("mp4", ignoreCase = true) == true ||
        streamUrl.contains(".mp4", ignoreCase = true) -> MimeTypes.VIDEO_MP4
    else -> null
}
if (inferredMimeType != null) {
    mediaItemBuilder.setMimeType(inferredMimeType)
}
```

#### 5. Pemilihan Master Playlist & Normalisasi DRM di Gateway (`source.pb.js`)
Pilih `master_url` agar audio tracks (`cmfa1hd`, `cmfa1sd`) tersinkronisasi bersama video:
```javascript
let streamUrl = String(s.url || "");
if (canonicalId === "youku" && s.master_url) {
    streamUrl = String(s.master_url);
} else if (!streamUrl && s.master_url) {
    streamUrl = String(s.master_url);
}

let drmObj = s.drm || null;
if (drmObj && canonicalId === "youku") {
    if (drmObj.license_params) {
        drmObj.license_params.drmType = "widevine";
    }
    if (drmObj.license_url) {
        drmObj.license_url = drmObj.license_url.replace(/drmType=[^&]+/, "drmType=widevine");
    }
}
```

#### 6. Whitelist Domain Cleartext HTTP (`network_security_config.xml`)
Izinkan lalu lintas HTTP port 80 untuk CDN segmen video Youku:
```xml
<domain includeSubdomains="true">cibntv.net</domain>
<domain includeSubdomains="true">youku.com</domain>
<domain includeSubdomains="true">youku.tv</domain>
```

### Pencegahan ke Depan
- **Gunakan SurfaceView untuk Aplikasi Media Streaming Universal**:
  Jangan pernah menggunakan `TextureView` sebagai default renderer pada aplikasi yang berpotensi memutar konten terenkripsi DRM (Widevine, PlayReady). Meskipun `TextureView` memudahkan animasi alpha dan transformasi view di masa lalu, Android modern dengan Compose dan `SurfaceView` (khususnya melalui `BLASTBufferQueue`) jauh lebih efisien dalam konsumsi baterai, bebas alokasi GPU ganda, dan wajib digunakan untuk video terproteksi TEE hardware level.
- **Isolasi Query String pada Endpoint Lisensi Berbasis Form-Data**:
  Saat memanggil server DRM pihak ketiga yang menggunakan form-encoded POST, selalu buang query string dari target URL (`url.substringBefore("?")`) guna mencegah ambiguitas parameter parser di sisi gateway/server otentikasi.
- **Pertahankan Delegasi Provisioning Default**:
  Jangan pernah menulis ulang logika `executeProvisionRequest` secara manual kecuali benar-benar menggunakan server provisioning in-house privat. Selalu delegasikan ke `HttpMediaDrmCallback` bawaan sistem operasi.

