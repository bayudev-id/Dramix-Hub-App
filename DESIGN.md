# DESIGN.md - Dramix Android Design System & UI Specification

## 1. Overview & Visual Direction

Dramix Android mengadopsi estetika **Cinema Dark (OLED-First)** yang dirancang khusus untuk kenyamanan menonton jangka panjang pada layar mobile OLED/AMOLED, menghemat konsumsi daya baterai, serta memberikan kontras dramatis khas bioskop.

* **Core Aesthetic:** Pure OLED Black (`#000000`), Midnight Indigo (`#0F0F23`), dan Signature Play Crimson (`#E11D48`).
* **Filosofi Navigasi:** *Direct-Play First* — Memangkas friksi perpindahan layar dengan langsung memutar video saat item diklik, adaptif terhadap format konten (Long Drama/Movie, Shorts 9:16, Live TV).
* **Anti-AI-Slop Policy:**
  - Menghilangkan efek glow neon berlebih dan gradien ungu/violet generik AI.
  - Ikon murni menggunakan vektor geometris baku (Google Material Symbols / Lucide SVG), bukan emoji.
  - Shimmer skeleton loading mencerminkan dimensi persis kartu poster, bukan spinner lingkaran di tengah layar kosong.
  - Setiap target sentuh (*touch target*) memiliki ukuran minimal **48dp** dengan jarak antar-elemen minimal **8dp**.

---

## 2. Color System & Design Tokens

Struktur token 3-layer (Primitive ➔ Semantic ➔ Jetpack Compose M3 ColorScheme):

### Primitive Tokens
```text
--color-pure-black:       #000000
--color-midnight-base:    #0F0F23
--color-midnight-card:    #16162A
--color-midnight-border:  #25253E
--color-crimson-600:      #E11D48 (Signature Brand & Play Accent)
--color-crimson-700:      #BE123C (Pressed / Active State)
--color-gold-vip:         #F59E0B (Badge VIP & Lisensi Aktif)
--color-slate-50:         #F8FAFC (High Emphasis Text - 16.5:1 Contrast)
--color-slate-400:        #94A3B8 (Medium Emphasis Metadata Text)
--color-slate-600:        #475569 (Dividers & Inactive Icons)
--color-emerald-500:      #10B981 (Success & Download Complete)
--color-rose-500:         #F43F5E (Destructive / Error)
```

### Semantic Token Mapping (Jetpack Compose Material 3)
```kotlin
// Compose ColorScheme Mapping (Theme.kt)
val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFE11D48),            // Crimson Accent
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFBE123C),
    onPrimaryContainer = Color(0xFFFFFFFF),
    secondary = Color(0xFF1E1B4B),          // Deep Indigo
    onSecondary = Color(0xFFF8FAFC),
    background = Color(0xFF000000),         // Pure OLED Black
    onBackground = Color(0xFFF8FAFC),
    surface = Color(0xFF0F0F23),            // Midnight Surface
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFF16162A),     // Card & Drawer Container
    onSurfaceVariant = Color(0xFF94A3B8),   // Subtitle & Secondary Label
    outline = Color(0xFF25253E),            // Thin Card Borders
    error = Color(0xFFF43F5E),
    onError = Color(0xFF000000)
)
```

---

## 3. Typography & Type Scale

Menggunakan font sistem modern berkarakter teknis dan presisi (**Inter / Roboto**):

| Token Tipografi | Ukuran (sp) | Weight | Line Height | Penggunaan di Jetpack Compose |
|---|---|---|---|---|
| `displayLarge` | 32sp | Bold (700) | 40sp | Judul Hero Banner, Splash Welcome |
| `headlineMedium` | 20sp | SemiBold (600) | 26sp | Judul Drama di Player Screen, Section Header |
| `titleMedium` | 16sp | Medium (500) | 22sp | Judul Kartu Poster Katalog, Tab Bar Label |
| `bodyLarge` | 14sp | Normal (400) | 20sp | Deskripsi Sinopsis Lengkap (BottomSheet/Expanded) |
| `bodyMedium` | 13sp | Normal (400) | 18sp | Sinopsis Truncated pada Shorts Overlay |
| `labelLarge` | 13sp | Medium (500) | 18sp | Tombol Aksi ("Aktivasi Sekarang", "Download") |
| `labelSmall` | 11sp | Bold (700) | 14sp | Badge Tag ("VIP", "HD", "Eps 1", "LIVE") |

---

## 4. Layout, Spacing, & Touch Guidelines

### Spacing Scale
- `space-xxs`: **2dp** (Badge padding internal)
- `space-xs`: **4dp** (Jarak rating star ke angka)
- `space-sm`: **8dp** (Jarak antar elemen grid episode)
- `space-md`: **12dp** (Padding horizontal kartu katalog)
- `space-lg`: **16dp** (Padding standar margin layar kiri-kanan)
- `space-xl`: **24dp** (Pemisah antar-section katalog)

### Aturan Touch Target & Gesture
- **Ukuran Sentuh Minimum:** Setiap tombol, ikon navigasi, dan tombol episode wajib memiliki area klik minimal **48dp x 48dp** (`Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)`).
- **Safe Area Insets:** Kontrol video dan TopBar wajib menghitung `WindowInsets.statusBars` dan `WindowInsets.navigationBars` menggunakan Compose Accompanist/M3 WindowInsets.
- **Gesture Conflict Mitigation:** Gerakan swipe vertikal pada *Shorts* diprioritaskan di atas gesture sistem Android Back Navigation.

---

## 5. Bentuk & Sudut Sudut (Shape & Elevation)

- **Radius Kartu Poster:** `8dp` (`RoundedCornerShape(8.dp)`) — Memberikan kesan modern tanpa terkesan membulat kekanak-kanakan.
- **Radius Tombol Episode (Grid Angka):** `6dp`.
- **Radius BottomSheet & Dialog:** `16dp` hanya pada sudut atas kiri dan kanan (`RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)`).
- **Depth & Border:** Tanpa drop-shadow buram (karena latar OLED murni hitam). Kedalaman dicapai menggunakan **1dp stroke border** (`#25253E`) atau kontras warna container (`#16162A`).

---

## 6. Spesifikasi Layar & Pola Navigasi

### A. Screen: Home Catalog (`HomeScreen.kt`)
* **Top Bar:**
  - Kiri: Logo tegas **DRAMIX** (tinggi 28dp, typography bold dengan dot aksen Crimson `#E11D48`). Tidak ada avatar palsu.
  - Kanan: Ikon Search dan Ikon Filter Provider.
* **Provider Carousel Chips:** Tab horizontal chip provider (WeTV, Viu, DramaBox, KissKH, Live TV, dll.).
* **Hero Spotlight Banner:** Carousel 16:9 drama unggulan dengan tombol aksi utama "Tonton Sekarang" (`#E11D48`).
* **Section Feeds (Data Nyata Gateway):**
  - "Drama Populer" (Horizontal scroll poster rasio 2:3, data dari `GET /api/modelles/videos?model_id=wetv&category_id=popular` atau Viu/KissKH).
  - "Drama Pendek Trending" (Horizontal scroll poster rasio 9:16, data dari `GET /api/modelles/videos?model_id=freereels` atau DramaBox/ShortMax).
  - "Live Channel Pilihan" (Horizontal scroll thumbnail 16:9 + badge LIVE merah, data dari `GET /api/modelles/videos?model_id=CineTv&category_id=ID`).
* **Bottom Navigation Bar:**
  1. *Beranda* (Home icon)
  2. *Drama Pendek* (Reels/Fire icon)
  3. *Live TV* (Tv icon)
  4. *Saya* (User icon + indikator badge VIP)

---

### B. Screen: Direct-Play VOD / Long Drama (`VodPlayerScreen.kt`)
* **Navigasi Global:** **TopBar dan BottomNavigationBar otomatis disembunyikan total** saat masuk ke layar ini.
* **Bagian Atas (40% Viewport - Aspect Ratio 16:9):**
  - Pemutar video Media3 ExoPlayer edge-to-edge.
  - Overlay kontrol transparan (muncul saat disentuh, auto-hide 3 detik): Tombol Back (`←`), Judul Drama, Play/Pause, Slider Seekbar Crimson, Timer, Tombol Landscape/Rotate.
* **Bagian Bawah (60% Viewport - Scrollable):**
  - **Metadata Header:** Judul Drama, Rating (`★ 9.4`), Tahun, Provider Chip, Tag VIP.
  - **Sinopsis Ringkas:** Deskripsi teks dengan tombol "Selengkapnya".
  - **Aksi Cepat:** Tombol *Bookmark* (Favorit), Tombol *Download Semua*, Tombol *Share*.
  - **Season & Clip Selector:**
    * Tab Season 1, Season 2 (jika multi-season).
    * Tab **"Trailer & Klip"** (khusus provider seperti WeTV yang menyertakan klip cuplikan; seluruh trailer & klip gratis ditonton tanpa lisensi).
  - **Adaptive Episode Section:**
    * **Format A (Episode ada thumbnail):** Render `LazyRow` kartu horizontal (Thumbnail 16:9, Judul Episode, Durasi, Ikon Download).
    * **Format B (Episode tanpa thumbnail):** Render `LazyVerticalGrid` 5 kolom tombol kotak angka (1, 2, 3... 40).
    * Episode aktif ditandai warna Crimson (`#E11D48`), episode terkunci memiliki ikon gembok kecil di sudut kanan atas.
  - **Rekomendasi Konten ("Mungkin Anda Suka"):**
    * Diambil dari pemanggilan `GET /api/modelles/videos` dengan `model_id` yang sama atau dari array `recommendations` detail drama.

---

### C. Screen: Direct-Play Shorts 9:16 (`ShortsPlayerScreen.kt`)
* **Navigasi Global:** BottomBar tetap tampil di dasar layar (opsional translucent) atau auto-hide; TopBar disembunyikan.
* **Viewport:** Layar penuh 100% tinggi layar 9:16 vertikal.
* **Interaksi:** `VerticalPager` Jetpack Compose dengan gesture snap-to-page per episode.
* **Overlay Kiri Bawah:**
  - Judul Drama (Bold) + Episode `Ep. 12/80`.
  - Sinopsis ringkas terpotong maksimal 2 baris (`...`).
  - Klik pada area teks membuka **Detail Modal Bottom Sheet** (Deskripsi lengkap, info aktor, daftar 80 episode langsung).
* **Overlay Kanan Bawah (Floating Vertical Action Bar):**
  - Ikon *Bookmark* (Hati / Bintang).
  - Ikon *Episode Drawer* (Daftar episode instan).
  - Ikon *Download Episode Ini*.
* **Progress Bar:** Garis tipis 2dp di dasar layar bergerak dinamis menunjukkan durasi per video.

---

### D. Screen: Direct-Play Live TV (`LiveTvPlayerScreen.kt`)
* **Bagian Atas (16:9 Viewport):**
  - Pemutar siaran langsung HLS.
  - Badge kedip merah `● LIVE` di sudut kiri atas.
* **Bagian Bawah:**
  - **Filter Kategori Chips:** Horisontal chip dinamis dari respons `GET /api/modelles/categories?model_id=CineTv` (contoh: `ID` untuk Indonesia Nasional, `SP` untuk Olahraga, `US`, `GB`). Default aktif: `ID`.
  - **Channel List:** Daftar saluran vertikal dengan logo stasiun TV, nama channel, dan program yang sedang tayang (EPG program title).
  - Mengklik channel baru langsung mengganti stream video di player atas tanpa reloading layar.

---

### E. Screen: Tab "Saya" & License Gate (`ProfileScreen.kt`)
* **Header Kartu Identitas Perangkat:**
  - Avatar default profile minimalis (tanpa nama input buatan).
  - Nama Tampilan Otomatis: **"Pengguna Tamu"** atau **"Dramix User #[4-digit DeviceId]"**.
  - Subtitle: `ID Perangkat: DRM-XXXX-XX` (hardware Android ID yang terikat saat aktivasi).
  - Badge Status: `FREE (Ep. 1-3 Gratis)` jika belum aktivasi, atau `VIP MEMBER (Aktif s/d [Tanggal])` setelah aktivasi.
* **Formulir Aktivasi Lisensi:**
  - Card berlatar `#16162A` dengan border `#25253E`.
  - Input field OutlinedTextField: `Masukkan Kode Lisensi (LCN-SELLER-XXXX)`.
  - Tombol Primer `#E11D48`: `Aktivasi Sekarang`.
  - Teks Bantuan: *"Belum punya lisensi? Dapatkan lisensi resmi via reseller atau admin."*
* **Menu Pengaturan & Data Lokal:**
  - *Riwayat Menonton* (Buka daftar riwayat lokal Room DB).
  - *Favorit / Tersimpan* (Daftar drama bookmark).
  - *Manajer Download* (Daftar file video tersimpan offline).
  - *Pembersihan Cache* (Menampilkan ukuran cache MB dan tombol bersihkan).

---

## 7. State Handling (Loading, Error, Empty, & Gate)

| State | Komposisi Visual |
|---|---|
| **Loading Katalog** | Animasi Shimmer monokrom (`#16162A` ke `#25253E`) dengan bentuk kartu identik dengan poster asli. |
| **Buffering Video** | Indikator lingkaran Crimson minimalis di tengah player transparan tanpa menutup frame video sebelumnya. |
| **Error Koneksi / Gateway** | Ikon sinyal terputus + Pesan bersahabat (*"Gagal terhubung ke Gateway :8090"*) + Tombol *"Coba Lagi"*. |
| **Freemium Gate (Ep. 4+)** | Video otomatis terhenti di akhir detik Ep 3 ➔ Muncul Overlay Dialog transparan di atas player: *"Episode 4 Terkunci - Masukkan Lisensi VIP untuk Melanjutkan"* + Tombol pintasan langsung ke form aktivasi. |
| **Empty State (Download/Riwayat)** | Ilustrasi minimalis abu-abu + Teks *"Belum ada video diunduh"* + Tombol *"Jelajahi Drama"*. |

---

## 8. Anti-AI-Slop & Design Consistency Rules

* **DO:**
  - Pertahankan latar belakang murni hitam `#000000` pada semua layar pemutar dan katalog.
  - Berikan feedback sentuh visual (`ripple effect`) pada setiap interaksi tombol.
  - Batasi jumlah warna aksen: hanya gunakan Crimson `#E11D48` untuk aksi pemutaran dan Gold `#F59E0B` untuk status VIP.
  - Tampilkan judul episode yang informatif dan terpotong secara proporsional.
* **DON'T:**
  - Jangan gunakan gradien warna pelangi/ungu ala template AI generik.
  - Jangan gunakan icon emoji teks (misal: 🎬, 📺, ⭐) di dalam UI; gunakan vektor Icon resmi Jetpack Compose (`Icons.Rounded.*`).
  - Jangan buat dialog popup modal berukuran layar penuh yang menutupi navigasi secara kasar.
  - Jangan sembunyikan progress download; selalu sertakan persentase progres yang jelas.
