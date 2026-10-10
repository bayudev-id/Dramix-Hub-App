# Changelog

Semua perubahan penting pada proyek **Dramix Hub App** didokumentasikan dalam file ini.
Format penulisan berpedoman pada [Keep a Changelog](https://keepachangelog.com/id/1.0.0/) dan mengikuti kaidah Semantic Versioning.

---

## [Unreleased]

### Added
- **Dukungan Widevine CBCS DRM & Custom Form License Flow (`WidevineDrmCallback.kt`, `DramixPlayerController.kt`)**:
  - Menghadirkan callback DRM kustom `WidevineDrmCallback` yang menerjemahkan permintaan kunci biner ExoPlayer menjadi HTTP POST `application/x-www-form-urlencoded` dengan Base64 challenge dan parameter sesi Youku (`token`, `vid`, `utdid`, `psid`, `drmType=widevine`).
  - Mendekode respons JSON lisensi (`states: 0`, `"wvpl license gen succ"`) menjadi binary key response untuk ExoPlayer.
  - Mendelegasikan eksekusi provisioning sertifikat perangkat ke `HttpMediaDrmCallback` default Google Play Services.
  - Mengonfigurasi `DefaultDrmSessionManager` dengan `multiSession = true` dan `playClearSamplesWithoutKeys = true` untuk dekripsi terpisah track audio dan video.
- **Penerusan Metadata DRM Multi-Layer (`PlaybackDtos.kt`, `CatalogModels.kt`, `CatalogRepositoryImpl.kt`, `VodPlayerViewModel.kt`, `LiveTvPlayerViewModel.kt`, `ShortsPlayerViewModel.kt`)**:
  - Menambahkan struktur data `DrmDto` dan `DrmConfig` (`licenseUrl`, `licenseMethod`, `licenseParams`, `type`, `systems`) pada DTO, model domain, dan ViewModel pemutar.
- **Whitelist Domain Cleartext HTTP untuk CDN Media (`network_security_config.xml`)**:
  - Mengizinkan lalu lintas HTTP port 80 untuk domain CDN Youku (`cibntv.net`, `youku.com`, `youku.tv`) yang menyajikan segmen fMP4.
- **Inferensi Format Manifest `/playlist/m3u8` (`DramixPlayerController.kt`)**:
  - Menambahkan deteksi MIME type otomatis (`MimeTypes.APPLICATION_M3U8`) untuk URL path `/m3u8` tanpa ekstensi file.
- **Widget Countdown Timer untuk Episode Ongoing/Belum Rilis (`VodPlayerCountdownOverlay.kt`, `VodPlayerScreen.kt`)**:
  - Menghadirkan overlay pemutar khusus saat episode drama masih berstatus ongoing dan upstream menyediakan widget hitung mundur (seperti TickCounter).
  - Menyematkan `WebView` terisolasi dengan latar belakang transparan/gelap untuk merender countdown timer secara langsung di dalam area pemutar, dilengkapi tombol "Cek Ketersediaan" (refresh).
  - Mengintegrasikan flag `isCountdown` dan URL `countdownUrl` pada `PlaybackSourceDataDto`, `PlaybackSource`, dan `VodPlayerUiState`.
  - Mengadaptasi `VodPlayerOverlay` agar menyembunyikan kontrol pemutaran tengah dan seekbar scrub saat mode countdown aktif, sementara navigasi atas (kembali, judul) dan toggle fullscreen tetap dapat diakses.
- **Pemisahan Preferensi & State Video Zoom Portrait vs Fullscreen (`PlayerPreferences.kt`, `VodPlayerViewModel.kt`, `VodPlayerScreen.kt`)**:
  - Memisahkan persistensi zoom video ke dalam key terisolasi (`pref_video_zoom_portrait` dan `pref_video_zoom_fullscreen`) dengan fallback kompatibel ke key legacy `pref_video_zoom`.
  - Memisahkan state UI pemutar di `VodPlayerUiState` (`portraitVideoZoom` dan `fullscreenVideoZoom`) sehingga pengaturan pembesaran layar di mode portrait tidak menimpa pengaturan di mode fullscreen landscape.
- **Dukungan Dynamic Pull-to-Refresh Provider (`HomeViewModel.kt`, `HomeScreen.kt`)**:
  - Menambahkan method `refreshHome()` yang memicu fetch ulang daftar provider dari server/database saat pengguna melakukan gesture pull-to-refresh di Homescreen.
  - Memfilter ulang provider aktif dan memperbarui list chip secara dinamis, otomatis mendepak provider yang baru dinonaktifkan dan memilih provider pengganti jika provider terpilih menjadi inaktif tanpa perlu me-restart aplikasi.
- **Validasi Status Provider Inaktif (`ProviderModel.isActive`)**:
  - Menambahkan property helper `isActive` pada model domain `ProviderModel` (`status.equals("active", ignoreCase = true)`).
  - Melakukan filter ketat pada `HomeScreen` (`ProviderChipsRow`), `HomeViewModel`, `ProviderPreferences` (`applyToProviders` & `getMergedConfigItems`), `SearchScreen` (`SearchProviderChipsRow`), `SearchViewModel`, `ShortsPlayerViewModel`, dan `LiveTvPlayerViewModel`.
  - Provider yang berstatus `inactive` pada database/gateway tidak akan pernah dirender pada deretan chip HomeScreen, sheet kustomisasi provider, maupun deretan chip Search.
- **Fallback Placeholder Gambar Pencarian (`SearchResultGrid.kt`)**: Menambahkan listener error handling pada Coil `ImageRequest` dan merender kartu cadangan dengan ikon `Icons.Default.Movie` berwarna slate ketika URL cover null, kosong, atau gagal dimuat oleh CDN.
- **Skema Migrasi Subtitle Preferences (`PlayerPreferences.kt`)**: Menambahkan `KEY_SUBTITLE_CONFIG_VERSION` dengan mekanisme auto-migrasi (`CURRENT_SUBTITLE_VERSION = 2`) untuk menimpa preferensi usang dengan nilai default terstandarisasi.
- **Short Edges Display Cutout (`MainActivity.kt`)**: Mengonfigurasi `layoutInDisplayCutoutMode = LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES` pada level window untuk memastikan konten video membentang langsung melintasi area kamera depan/notch tanpa komputasi tunda.
- **Search Infinite Scroll Pagination**: Menambahkan dukungan infinite scrolling di `SearchScreen` yang setara dengan `HomeScreen`. Mengonsumsi field `has_more` dari response gateway, memicu auto-load page berikutnya saat mencapai 5 item dari batas bawah grid, melakukan deduplikasi ID secara otomatis, dan me-reset state pagination saat kata kunci atau provider berubah.
- **Provider Priority Reordering**: Mengatur urutan prioritas resmi provider pada Search & Home:
  1. WeTV (`wetv`)
  2. MovieBox (`moviebox`)
  3. VIU (`viu`)
  4. KissKH (`kisskh`)
  5. iQIYI (`iqiyi`)
  6. Youku (`youku`)
  7. FreeReels (`freereels`)
  8. Provider lainnya secara alfabetis.
- **Icon Support pada Search Provider Chips**: Menampilkan ikon asli masing-masing provider (`AsyncImage` 14dp circular) dengan animasi auto-scroll ke posisi tengah saat terpilih.
- **Search Portrait Covers**: Mengaktifkan portrait cover display di search hasil. Aspect ratio 2:3 untuk drama/film, 9:16 untuk shorts. 15/24 provider dikonfirmasi ada cover data lengkap.

### Changed
- **Standarisasi Default Subtitle Styling**:
  - Fullscreen: Arial, Medium outline, Font Size 20px, Position 10%, Background Opacity 0%, Line Spacing 0px, Background Padding 0px, Text Color White (`0xFFFFFFFF`).
  - Portrait: Arial, Medium outline, Font Size 14px, Position 10%, Background Opacity 0%, Line Spacing 0px, Background Padding 0px, Text Color White (`0xFFFFFFFF`).
- **Granularitas Stepper Padding & Line Spacing Subtitle**: Menyesuaikan batas perubahan delta background padding dan line spacing menjadi 0px – 20px (step 1px) di `VodPlayerViewModel.kt` dan `PlayerSettingsMenu.kt`.
- **Tuning Klien Gambar OkHttp (`OkHttpProvider.kt`)**: Menyesuaikan connect timeout menjadi 15s dan read timeout menjadi 25s, serta membatasi `maxRequestsPerHost = 16` guna mencegah stalling koneksi HTTP/2 pada domain cover pihak ketiga.
- **Pembersihan Tab Search**: Menghapus tab filter tipe konten yang redundan ("Semua", "Drama", "Short", "Film", "Live TV") pada `SearchScreen.kt`.
- **Standarisasi Visual Chip Provider**: Mengadopsi styling chip dari HomeScreen (`RoundedCornerShape(16.dp)`, aksen `CrimsonPlay`, background `MidnightCard`, border `MidnightBorder`).
- **Integrasi Preferences pada Search**: Menghubungkan `ProviderPreferences` ke dalam `SearchViewModel` melalui DI Koin (`AppModule.kt`).
- **Category Validation Fallback**: Auto-fallback ke kategori pertama jika `lastCategoryId` tidak valid/tidak ditemukan di provider baru (fix untuk HTTP 502 saat ganti provider).
- **Migrasi Render Surface ke SurfaceView (`item_player_view.xml`)**:
  - Mengubah `app:surface_type="texture_view"` menjadi `app:surface_type="surface_view"` pada layout PlayerView VOD dan Live TV.
  - Memungkinkan hardware secure video decoder (`c2.mtk.avc.decoder.secure`) merender secure frame buffer langsung ke hardware overlay plane tanpa penolakan dari GPU OpenGL compositor (`GPUAUX: skip, cannot convert protect / secure buffer`).

### Fixed
- **Black Screen & Freeze pada Konten Terenkripsi DRM Youku (`item_player_view.xml`, `WidevineDrmCallback.kt`, `DramixPlayerController.kt`)**:
  - Menyelesaikan masalah black screen dan UI freeze yang terjadi karena ketidakcocokan antara hardware secure decoder TEE dan `TextureView`.
  - Memperbaiki kegagalan perolehan kunci lisensi Youku (status 202 `drm type error`) dengan membersihkan parameter URL query string dan mengirimkan parameter form body murni.
  - Memperbaiki crash `IllegalStateException` pada pemeriksaan `requiresSecureDecoder` dengan mendelegasikan alur provisioning sertifikat perangkat ke `HttpMediaDrmCallback` default.
  - Memperbaiki video tanpa suara dengan memilih `master_url` yang menggabungkan audio dan video sub-playlist.
- **Urutan Episode KissKH Ascending & Dimulai dari Episode Awal (0 atau 1) (`detail.pb.js`, `CatalogRepositoryImpl.kt`)**:
  - Menyortir urutan episode KissKH secara ascending berdasarkan nomor episode sehingga Episode 1 (atau Episode 0) selalu berada di depan dan diputar pertama kali, alih-alih terbalik dengan episode terbaru/belum rilis di awal.
  - Memperbaiki bug konversi JavaScript di `detail.pb.js` di mana `0 || (i + 1)` menganggap episode bernomor 0 sebagai falsy dan mengubahnya menjadi episode terakhir ("Episode 3"). Kini nilai 0 dipertahankan dengan validasi numerik eksplisit.
- **Pencegahan Error Parsing Media Container pada Episode Countdown (`streamController.js`, `source.pb.js`, `CatalogRepositoryImpl.kt`)**:
  - Mengeliminasi error ExoPlayer `UnrecognizedInputFormatException` yang terjadi akibat pemutar mencoba mem-parsing URL widget countdown (TickCounter) sebagai video stream m3u8.
  - Mendeteksi tipe stream timer dan mengisolasinya ke field `countdown_url` / `is_countdown` baik di level gateway maupun fallback client repository.
- **Koreksi Tanda Kunci VIP pada Daftar Episode (`AdaptiveEpisodeList.kt`)**:
  - Menghapus pengecekan hardcoded `episode.number >= 4` yang sebelumnya menyebabkan episode reguler gratis berurutan 4 ke atas pada KissKH menampilkan ikon gembok VIP padahal `isVip` bernilai false.
- **Shimmer Layout Shift KissKH & Live TV (`HomeScreen.kt`)**: Menyesuaikan `FeedShimmerGrid` agar secara adaptif mengenali provider `kisskh` atau tipe konten `live_tv` dan merender grid shimmer 2-kolom dengan aspect ratio landscape 16:9 alih-alih 3-kolom portrait 2:3, mengeliminasi kedipan pergeseran layout (layout shift) saat feed selesai dimuat.
- **Visual Stutter & Jeda Transisi Fullscreen Pertama Kali (`VodPlayerScreen.kt`)**: Menghilangkan unmount/re-inflate `VideoPlayerSurface` dengan menyatukan viewport pemutar ke dalam satu node pohon Compose persisten, mengubah modifier aspect ratio secara langsung tanpa memutus decoder hardware SurfaceView ExoPlayer.
- **Constraint Padding `Scaffold` pada Rute Pemutar (`AppNavigation.kt`)**: Mem-bypass `innerPadding` bawaan Scaffold (di-set `PaddingValues(0.dp)`) ketika pengguna berada pada rute `vod_player`, mengeliminasi jeda animasi penyusutan System Bars saat rotasi landscape.
- **Sinkronisasi Langsung Sembunyikan System Bars**: Mengeksekusi `insetsController.hide(systemBars())` secara sinkron langsung di dalam fungsi `toggleFullscreen()` alih-alih menunggunya di dalam `LaunchedEffect`.

---

## [1.2.0] - 2026-10-09

### Added
- **VipActiveCard di Profile**: Kartu status keanggotaan VIP aktif dengan gradient tema gelap elegan (`#1F2937` → `#374151`), badge terverifikasi, tanggal kedaluwarsa, dan info kuota download.
- **Dukungan Pagination Eksplisit (`has_more`)**: Model domain `VideoFeedPage` dan metode `CatalogRepository.getVideoFeed()` untuk konsumsi status paging akurat dari Gateway.
- **Penyimpanan Lokal Subtitle**: Persistensi pengaturan subtitle lengkap (ukuran font, line spacing, background opacity, background padding, font family, outline) ke SharedPreferences melalui `PlayerPreferences` dengan penyimpanan terpisah untuk mode portrait dan fullscreen.
- **Panduan Pembelajaran & Pemecahan Masalah**: Penambahan panduan teknikal komprehensif di [`docs/TROUBLESHOOTING_AND_LEARNING_GUIDE.md`](docs/TROUBLESHOOTING_AND_LEARNING_GUIDE.md) yang membahas analisis akar masalah, metode diagnosis, dan pencegahan bug di masa mendatang.

### Changed
- **Penyederhanaan Bottom Navigation Bar**: Dipadatkan menjadi 3 menu utama berfokus pengguna:
  1. **Beranda** (`Screen.Home.route`)
  2. **Unduhan** (`Screen.DownloadManager.route`)
  3. **Saya** (`Screen.Profile.route`)
  - Menghapus tab redundan Drama Pendek dan Live TV dari navigasi bawah utama.
- **Granularitas Background Padding Subtitle**: Mengubah stepping penambahan dan pengurangan dari 2px menjadi 1px (rentang 8px–48px) pada `PlayerSettingsMenu.kt`.
- **Tata Letak Baris Pemeran (Cast List)**:
  - Menghilangkan `contentPadding` horizontal pada `LazyRow` pemeran agar item pertama sejajar rata kiri dengan teks judul "Pemeran & Kru".
  - Merapatkan jarak vertikal antara teks nama pemeran dan peran dari `3.dp` menjadi `2.dp`.
- **Kondisi Tampilan Profile**: Form aktivasi lisensi disembunyikan secara otomatis ketika status VIP pengguna terdeteksi aktif.

### Fixed
- **Infinite Scroll Loop & Duplikasi Konten (MovieBox)**: Memperbaiki kesalahan gateway `videos.pb.js` yang sebelumnya menganggap kategori non-paginasi memiliki halaman lanjutan. Kini `hasMore` default ke `false` kecuali gateway mengembalikan boolean `true` eksplisit.
- **Pembersihan State Pagination & Pull-to-Refresh Guard**: Menambahkan mekanisme reset atomik `categoryVideos`, `currentPage`, `hasMoreContent`, dan `isLoadingMore` saat pengguna berpindah tab kategori maupun melakukan gesture tarik ke bawah (*pull-to-refresh*) di `HomeViewModel.kt`.
- **Deduplikasi ID Item Video Feed**: Pada `HomeViewModel.loadMoreVideos()`, item baru kini difilter berdasarkan ID unik dan `hasMoreContent` otomatis dinonaktifkan jika response halaman berikutnya tidak menghasilkan item baru.
- **Race Condition Infinite Scroll Trigger**: Di `HomeScreen.kt`, `snapshotFlow` kini dipagari dengan proteksi `!uiState.isLoadingContent && !isRefreshing` untuk mencegah pemanggilan prematur `loadMoreVideos()` di tengah siklus refresh.

### Removed
- **Hero Banner (Spotlight)**: Menghapus `SpotlightBanner` dan placeholder shimmer-nya dari `HomeScreen.kt` untuk memaksimalkan ruang tampilan katalog konten.
- **Chip Redundan "Atur Provider"**: Menghapus chip tombol "Atur" dari baris chip provider di `HomeScreen.kt`.
- **Menu Kustomisasi Provider di Profile**: Menghapus item menu "Kustomisasi Provider" dari `ProfileScreen.kt`, beserta dependency injection dan sheet modal terkait. Akses fitur kini disentralisasi secara eksklusif pada Top Navbar di sebelah tombol pencarian.

### Dokumentasi Terkait
- [Catatan Rinci Perbaikan Bagian 1](CHANGELOG_OCT9.md)
- [Catatan Rinci Perbaikan Bagian 2](CHANGELOG_OCT9_PART2.md)
- [Ringkasan Keseluruhan Perubahan](SUMMARY_OCT9.md)

---

## [1.1.0] - 2026-10-08

### Added
- **Gate Lisensi & Sewa Episode**: Penerapan dialog modal formal untuk episode bertanda sewa dan episode berbayar (> episode 3) terikat ID perangkat perangkat keras.
- **Hirarki Filter Tiga Tingkat di Beranda**: Navigasi konten berdasarkan Tipe Konten (`Drama Pendek`, `Film & Serial`) → Provider → Kategori.
- **Dukungan Live TV**: Pemutaran streaming kanal langsung berbasis HLS, DASH, dan RTSP dengan kartu pertandingan interaktif.

### Changed
- Penyesuaian tema Cinema Dark OLED (`#0A0A0C`) di seluruh komponen antarmuka.

---

## [1.0.0] - 2026-10-01

### Added
- Rilis perdana Dramix Android Streaming App.
- Integrasi AndroidX Media3 (ExoPlayer) dengan streaming langsung CDN dan injeksi custom header.
- Layanan download offline latar belakang dengan Room Database cache.
- Dukungan autentikasi anonim berbasis hardware device binding.
