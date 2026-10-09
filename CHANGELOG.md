# Changelog

Semua perubahan penting pada proyek **Dramix Hub App** didokumentasikan dalam file ini.
Format penulisan berpedoman pada [Keep a Changelog](https://keepachangelog.com/id/1.0.0/) dan mengikuti kaidah Semantic Versioning.

---

## [Unreleased]

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
- **Pembersihan State Pagination**: Menambahkan mekanisme reset `items`, `currentPage`, dan `hasMore` saat pengguna berpindah tab kategori di `HomeViewModel.kt`.

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
