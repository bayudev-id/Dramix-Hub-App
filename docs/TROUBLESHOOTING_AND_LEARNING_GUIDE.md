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
