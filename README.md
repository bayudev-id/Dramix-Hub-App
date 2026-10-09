# Dramix Hub App (Android Native Streaming)

[![Kotlin](https://img.shields.io/badge/Kotlin-2.1.0-purple.svg?logo=kotlin)](https://kotlinlang.org)
[![Android](https://img.shields.io/badge/Platform-Android_14_%28API_34--36%29-green.svg?logo=android)](https://developer.android.com)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack_Compose-blue.svg?logo=jetpackcompose)](https://developer.android.com/jetpack/compose)
[![Media3](https://img.shields.io/badge/Playback-AndroidX_Media3_1.3.1-red.svg)](https://developer.android.com/media/media3)
[![Room](https://img.shields.io/badge/Database-Room_2.6.1-orange.svg)](https://developer.android.com/training/data-storage/room)
[![Koin](https://img.shields.io/badge/DI-Koin_3.5.6-gold.svg)](https://insert-koin.io/)
[![Repository](https://img.shields.io/badge/GitHub-bayudev--id%2FDramix--Hub--App-black.svg?logo=github)](https://github.com/bayudev-id/Dramix-Hub-App)

**Dramix Hub App** (`com.dramix.app`) adalah aplikasi streaming video native untuk sistem operasi Android yang dirancang dengan performa tinggi, efisiensi konsumsi memori, dan antarmuka modern berbasis Jetpack Compose. 

Aplikasi ini mengonsumsi API dari **Dramix Gateway** yang mengintegrasikan puluhan penyedia konten video (drama panjang, drama pendek vertikal/reels, film, serial, hingga Live TV) secara langsung ke Content Delivery Network (CDN) tanpa perantara server relay berat.

---

## Daftar Isi
- [Fitur Utama](#fitur-utama)
- [Arsitektur & Tech Stack](#arsitektur--tech-stack)
- [Struktur Direktori Proyek](#struktur-direktori-proyek)
- [Panduan Pembelajaran & Pemecahan Masalah (Engineering Guide)](#panduan-pembelajaran--pemecahan-masalah-engineering-guide)
- [Prasyarat & Persiapan Lingkungan](#prasyarat--persiapan-lingkungan)
- [Kompilasi & Pengujian CLI](#kompilasi--pengujian-cli)
- [Pengujian Perangkat Fisik & Wireless ADB](#pengujian-perangkat-fisik--wireless-adb)
- [Protokol Pembaruan & Changelog](#protokol-pembaruan--changelog)
- [Dokumen Arsitektur Terkait (ADRs)](#dokumen-arsitektur-terkait-adrs)

---

## Fitur Utama

### 1. Multi-Format Streaming Engine
- **VOD (Video-on-Demand)**: Pemutaran drama dan film resolusi adaptif (HLS/m3u8, MP4, DASH) dengan dukungan pergantian resolusi, rasio aspek dinamis, dan kontrol gestur.
- **Drama Pendek (Vertical Shorts)**: Pemutaran video vertikal gaya reels dengan fitur auto-swipe, prefetching segmen, dan kontrol episode cepat.
- **Live TV Streaming**: Dukungan pemutaran kanal siaran langsung berbasis HLS, DASH, dan RTSP dengan kartu pertandingan interaktif.

### 2. Kustomisasi & Persistensi Gaya Subtitle
- Konfigurasi presisi tinggi: ukuran teks (px), posisi vertikal (%), opasitas background (%), line spacing (kelipatan 1px), background padding (kelipatan 1px, 8–48px), jenis font, dan gaya outline.
- **Penyimpanan Lokal Terpisah**: Preferensi tersimpan otomatis di SharedPreferences secara terpisah antara mode portrait dan fullscreen.

### 3. Pemutar Offline Mandiri (Offline Downloader)
- Background download service berbasis AndroidX Media3 DownloadManager.
- Penyimpanan persisten status unduhan dengan Room Database.
- Mendukung pemutaran tanpa jaringan (airplane mode) langsung dari cache media terenkripsi.

### 4. Navigasi Hirarki Konten Terstruktur
- **Filter Tiga Tingkat**: Tipe Konten (`Drama Pendek`, `Film & Serial`) $\rightarrow$ Provider $\rightarrow$ Kategori.
- **Sentralisasi Kustomisasi Provider**: Pengaturan urutan dan visibilitas provider terpusat eksklusif di Top Navigation Bar (sebelah kolom pencarian).
- **Bottom Navigation Ramping (3-Tab)**: Navigasi intuitif terdiri dari: **Beranda**, **Unduhan**, dan **Saya**.

### 5. Kartu Anggota VIP & Proteksi Lisensi
- Tampilan `VipActiveCard` dengan aksen tema gelap premium untuk anggota aktif.
- Proteksi episode sewa dan gating lisensi perangkat keras (hardware-bound license ID) untuk episode berbayar.

---

## Arsitektur & Tech Stack

Aplikasi dibangun mengikuti prinsip **Clean Architecture** yang dipadukan dengan pola **MVI/MVVM (Unidirectional Data Flow)**:

```
┌────────────────────────────────────────────────────────┐
│                      UI Layer                          │
│  Jetpack Compose Screen • ViewModel (StateFlow) • UI   │
└───────────────────────────▲────────────────────────────┘
                            │ Domain Models & Use Cases
┌───────────────────────────┴────────────────────────────┐
│                    Domain Layer                        │
│   Repository Interfaces • Domain Models • Business     │
└───────────────────────────▲────────────────────────────┘
                            │ Data Operations
┌───────────────────────────┴────────────────────────────┐
│                     Data Layer                         │
│  Retrofit/Moshi API • Room Database • SharedPreferences │
└────────────────────────────────────────────────────────┘
```

### Tabel Komponen Teknologi

| Komponen | Library / Tool | Versi | Tujuan |
|---|---|---|---|
| **Bahasa** | Kotlin | 2.1.0 | Bahasa utama aplikasi |
| **UI Framework** | Jetpack Compose + Material 3 | BOM 2024.09.00 | Antarmuka deklaratif modern |
| **Media Engine** | AndroidX Media3 (ExoPlayer) | 1.3.1 | Core pemutaran HLS/DASH/MP4 & cache |
| **Injeksi Dependensi** | Koin | 3.5.6 | Dependency Injection ringan |
| **Database Lokal** | Room Database | 2.6.1 | Riwayat tontonan & pelacak download |
| **Jaringan & Parsing**| Retrofit, OkHttp, Moshi | 2.11.0 / 4.12.0 | HTTP client dan JSON parsing |
| **Asinkron & Reaktif**| Coroutines & StateFlow | 1.8.1 | Penanganan thread dan state UI |

---

## Struktur Direktori Proyek

```
app/src/main/java/com/dramix/app/
├── core/
│   ├── database/       # Room database, entitas, dan DAO (WatchHistory, Download)
│   ├── network/        # Interceptor OkHttp, header binding, SSL config
│   └── util/           # Helper format durasi, ukuran file, tanggal
├── data/
│   ├── repository/     # Implementasi repository (CatalogRepositoryImpl, dll.)
│   └── source/
│       ├── local/      # PlayerPreferences, ProviderPreferences
│       └── remote/     # Retrofit endpoints, Moshi DTOs
├── domain/
│   ├── model/          # Pure domain models (VideoItem, VideoFeedPage, dll.)
│   └── repository/     # Kontrak antarmuka repository
├── player/
│   ├── controller/     # DramixPlayerController (wrapper ExoPlayer)
│   └── download/       # DownloadTracker & Media3 DownloadService
└── ui/
    ├── components/     # Reusable composables (TopBar, BottomNav, PlayerSettings)
    ├── navigation/     # NavHost, AppNavigation, dan rute layar
    ├── screens/
    │   ├── home/       # HomeScreen & HomeViewModel
    │   ├── player_vod/ # VodPlayerScreen & VodPlayerViewModel
    │   ├── profile/    # ProfileScreen & ProfileViewModel
    │   └── search/     # SearchScreen & SearchViewModel
    └── theme/          # Cinema Dark tokens, Color.kt, Typography.kt
```

---

## Panduan Pembelajaran & Pemecahan Masalah (Engineering Guide)

Bagian ini merangkum metodologi penyelesaian masalah nyata yang dihadapi pada proyek ini agar dapat menjadi bahan evaluasi dan pembelajaran teknikal di masa depan:

### 1. Masalah: Infinite Scroll Berulang & Data Terduplikasi
- **Penyebab**: Fallback nilai `has_more` pada gateway default ke `true` untuk feed non-paginasi.
- **Cara Menganalisis**: Pantau payload JSON respons gateway pada OkHttp logcat dan amati apakah `pager.has_more` bertipe boolean atau hilang.
- **Solusi**: Terapkan *safe default* (`hasMore = false` kecuali secara eksplisit menerima boolean `true`). Selalu bersihkan (*reset*) state list dan pagination di ViewModel ketika parameter filter berubah.

### 2. Masalah: Kontrol Stepper UI Melompat Terlalu Jauh
- **Penyebab**: Delta nilai pada pemanggilan event handler diset ke angka kelipatan 2 (`onIncrement = { delta(2) }`), bukan 1.
- **Cara Menganalisis**: Periksa binding lambda pada Composable row stepper dan verifikasi parameter delta yang dikirim ke ViewModel.
- **Solusi**: Gunakan langkah 1px untuk pengaturan presisi tinggi, kombinasikan dengan pembatas `coerceIn(min, max)` di sisi ViewModel.

### 3. Masalah: Perataan Kiri Composable Tidak Rata (Double Padding)
- **Penyebab**: Memberikan `contentPadding = PaddingValues(horizontal = 16.dp)` pada `LazyRow` yang sudah berada di dalam `Column` dengan padding 16dp.
- **Cara Menganalisis**: Gunakan Android Layout Inspector untuk mengukur margin aktual item pertama terhadap batas tepi layar.
- **Solusi**: Hapus `contentPadding` horizontal pada child jika komponen induk sudah menerapkan batas tepi kontainer.

### 4. Masalah: Redundansi Aksi Navigasi & Beban Memori
- **Penyebab**: Membuka satu fitur yang sama (Kustomisasi Provider) dari 3 layar berbeda, membawa serta state dan dependency injection yang tidak efisien.
- **Cara Menganalisis**: Lakukan audit alur navigasi dan identifikasi duplikasi modal sheet.
- **Solusi**: Pusatkan satu fitur konfigurasi pada satu entry point utama (TopBar). Hapus seluruh kode state, sheet, dan inject dependensi yang tidak terpakai (*dead code cleanup*).

> 📘 **Baca Analisis Lengkap**: Penjelasan teknis mendalam beserta potongan kode tersedia di [`docs/TROUBLESHOOTING_AND_LEARNING_GUIDE.md`](docs/TROUBLESHOOTING_AND_LEARNING_GUIDE.md).

---

## Prasyarat & Persiapan Lingkungan

1. **Java Development Kit (JDK)**: JDK 17 atau 21 terpasang dan disetel pada environment variable `JAVA_HOME`.
2. **Android SDK**:
   - `compileSdk = 36`
   - `targetSdk = 34`
   - `minSdk = 24` (Android 7.0 Nougat ke atas)
3. **Gradle Wrapper**: Gradle 8.x / 9.x (sudah terintegrasi melalui `gradlew.bat`).
4. **Dramix Gateway & PocketBase**: Berjalan aktif di mesin lokal host pada port `8090`.

---

## Kompilasi & Pengujian CLI

Jalankan perintah berikut melalui terminal PowerShell di root repositori:

### 1. Menjalankan Unit Tests
```powershell
.\gradlew.bat testDebugUnitTest
```

### 2. Mengompilasi Debug APK
```powershell
.\gradlew.bat assembleDebug
```
*Hasil berkas APK*: `app/build/outputs/apk/debug/app-debug.apk`

### 3. Mengompilasi Release APK
```powershell
.\gradlew.bat assembleRelease
```
*Hasil berkas APK*: `app/build/outputs/apk/release/app-release.apk`

---

## Pengujian Perangkat Fisik & Wireless ADB

Untuk menjalankan dan memvalidasi aplikasi langsung di perangkat uji fisik:

```powershell
# 1. Periksa daftar perangkat yang terhubung (USB atau Wi-Fi ADB)
adb devices

# 2. Reverse port forwarding (meneruskan port 8090 perangkat ke host laptop/PC)
adb reverse tcp:8090 tcp:8090

# 3. Instal APK debug ke perangkat tertentu
adb -s <DEVICE_ID_ATAU_IP:PORT> install -r app/build/outputs/apk/debug/app-debug.apk

# 4. Jalankan aplikasi langsung dari terminal
adb -s <DEVICE_ID_ATAU_IP:PORT> shell am start -n com.dramix.app/com.dramix.app.MainActivity

# 5. Monitoring logcat secara real-time
adb -s <DEVICE_ID_ATAU_IP:PORT> logcat -v time | findstr -i "Dramix OkHttp Media3"
```

---

## Protokol Pembaruan & Changelog

Setiap penambahan fitur atau perbaikan bug **wajib** mendokumentasikan:
1. Riwayat perubahan di [`CHANGELOG.md`](CHANGELOG.md) mengikuti pedoman [Keep a Changelog](https://keepachangelog.com/).
2. Analisis teknis dan alasan desain pada panduan pemecahan masalah jika terdapat *architectural trade-off*.
3. Commit Git atomik dengan format pesan deskriptif: `<tipe>(<lingkup>): <deskripsi singkat>`.

---

## Dokumen Arsitektur Terkait (ADRs)

- [ADR-001: Media3 Offline Cache and Stream Playback Architecture](docs/adr/ADR-001-media3-offline-cache-and-stream-architecture.md)
- [ADR-002: Gateway Anonymous Device Binding and Licensing](docs/adr/ADR-002-gateway-anonymous-device-binding-and-license.md)
- [ADR-003: Cinema Dark Design Tokens and Immersive Navigation](docs/adr/ADR-003-cinema-dark-and-navigation-patterns.md)
- [PRD: Android Streaming App Specifications](docs/prd/android-streaming-app.md)
- [Database Schema Document](docs/schema/android-streaming-app.md)

---

## Repositori & Kontribusi

- **Repository**: [https://github.com/bayudev-id/Dramix-Hub-App](https://github.com/bayudev-id/Dramix-Hub-App)
- **Maintainer**: Bayu Dev (`com.dramix.app`)
