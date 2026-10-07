# PRD: Dramix Streaming App

## Problem Statement

Penggemar drama Asia, drama pendek vertikal (micro drama), film, dan siaran Live TV di Indonesia sering kali harus berpindah-pindah aplikasi dengan model langganan mahal atau platform gratis yang dipenuhi iklan invasif. Di sisi lain, agregasi streaming skala besar sering membebani bandwidth server perantara (*bandwidth relay cost*) dan proses registrasi akun tradisional (email/password/OTP) menimbulkan hambatan konversi (*onboarding friction*) yang tinggi bagi calon pelanggan.

## Solution

Aplikasi mobile Android native (Kotlin, Jetpack Compose, AndroidX Media3) berkinerja tinggi yang mengonsumsi langsung 6 endpoint Dramix Gateway (:8090) dari 24 provider konten. Video diputar secara *direct stream* dari perangkat pengguna dengan injeksi header HTTP kustom (`Referer`, `User-Agent`) tanpa membebani bandwidth host MiniPC. Monetisasi mengusung model freemium berbasis lisensi digital (*server-authoritative license key* tanpa login akun), dengan persistensi data lokal (*local-first architecture* via Room DB) untuk bookmark dan riwayat tontonan.

## User Stories

1. Sebagai pengguna umum, saya ingin membuka aplikasi secara instan tanpa membuat akun atau login email, sehingga saya bisa langsung menikmati konten.
2. Sebagai penonton baru, saya ingin menonton Episode 1–3 secara gratis, sehingga saya dapat menilai kualitas cerita sebelum memutuskan membeli lisensi.
3. Sebagai penonton episode 4 ke atas, saya ingin memasukkan kode lisensi transaksi resmi (`LCN-SELLER-...`) di tab "Saya", sehingga seluruh episode terkunci dapat terbuka otomatis.
4. Sebagai penonton drama panjang/film, saya ingin poster yang saya klik langsung membuka layar pemutar video dengan metadata dan daftar episode adaptif di bawahnya, sehingga proses menonton lebih cepat tanpa melewati layar detail perantara.
5. Sebagai penonton, saya ingin daftar episode menampilkan kartu thumbnail jika penyedia menyediakan cover, atau tombol grid angka jika tidak ada cover, sehingga antarmuka tetap rapi dan informatif.
6. Sebagai penonton drama pendek, saya ingin konten langsung berputar di layar penuh 9:16 dengan navigasi snap-swipe vertikal dan deskripsi ringkas yang dapat diperluas lewat bottom sheet, sehingga navigasi terasa intuitif ala platform micro-drama modern.
7. Sebagai penonton Live TV, saya ingin memilih channel dan langsung menonton siaran langsung di bagian atas dengan pemilih kategori dan daftar channel di bagian bawah, sehingga saya bisa berpindah saluran dengan cepat.
8. Sebagai pengguna dengan kuota terbatas/bepergian, saya ingin mengunduh episode drama ke penyimpanan perangkat untuk ditonton secara offline, sehingga saya tetap bisa menonton tanpa koneksi internet.
9. Sebagai pengguna yang sering kembali menonton, saya ingin riwayat tontonan dan bookmark tersimpan rapi di perangkat saya secara lokal, sehingga saya bisa melanjutkan video tepat di detik terakhir saya berhenti.
10. Sebagai pengelola/pemilik aplikasi, saya ingin lisensi terikat secara kriptografis ke satu ID perangkat (*hardware device binding*) dan divalidasi ke gateway server, sehingga lisensi tidak dapat digenerate secara mandiri oleh pihak ketiga atau dibagikan ke banyak perangkat.
11. Sebagai pengelola sistem, saya ingin aplikasi kebal terhadap modifikasi runtime (Frida, root), bypass SSL (Burp Suite, mitmproxy), dan tampering signature, sehingga hak akses VIP dan URL stream terlindungi.

## Objective

Membangun aplikasi Android APK native yang cepat, ringan, aman, dan tanpa friksi akun untuk konsumsi 24 platform konten Dramix Gateway. Tolok ukur keberhasilan:
- Waktu mulai putar video (*Time to First Frame / TTFF*) < 2.0 detik pada jaringan 4G stabil.
- 0 bytes konsumsi bandwidth pemutaran video pada server gateway MiniPC (100% direct client playback).
- Nol pembobolan validasi VIP di sisi client berkat arsitektur *server-authoritative entitlement*.

## Technical Decisions

### 1. Arsitektur Aplikasi
- **Pola:** Clean Architecture + MVI/MVVM (UI Layer, Domain Layer, Data Layer).
- **UI Framework:** Jetpack Compose (Material 3) dengan tema Cinema Dark / OLED (`#000000`) dan aksen Brand Red (`#E11D48`).
- **Dependency Injection:** Koin (ringan, cepat dikompilasi via CLI tanpa kapt/ksp overhead yang lambat) atau Hilt.
- **Asynchronous & Reactive:** Kotlin Coroutines + `StateFlow` + `SharedFlow`.

### 2. Media Player & Download Engine (AndroidX Media3)
- **Engine Pemutar:** `ExoPlayer` (versi Media3 1.3+).
- **Direct Playback Source:** `DefaultHttpDataSource.Factory` atau `OkHttpDataSource.Factory` yang secara dinamis menyuntikkan header `Referer` dan `User-Agent` yang diterima dari respons endpoint `source` gateway (:8090).
- **Shorts Pre-Buffering:** Instance `ExoPlayer` tunggal atau pool ganda teroptimasi yang didukung `SimpleCache` (Least-Recently-Used / LRU 200MB) untuk eliminasi jeda buffer saat swipe vertikal.
- **Background Download:** `Media3 DownloadService` terintegrasi dengan Android `WorkManager`, menyimpan segmen HLS/MP4 terenkripsi di private storage aplikasi (`context.getExternalFilesDir()`).

### 3. Persistensi Data (Pure Local-First)
- **Room Database:** Menyimpan data lokal:
  - `watch_history` (`drama_id`, `provider_id`, `episode_number`, `position_ms`, `duration_ms`, `updated_at`).
  - `bookmarks` (`drama_id`, `provider_id`, `title`, `poster_url`, `created_at`).
  - `download_records` (`download_id`, `drama_id`, `episode_title`, `local_path`, `progress`, `status`).
- **EncryptedSharedPreferences / Jetpack DataStore:** Menyimpan `device_id` unik dan token sesi lisensi VIP dari server.

### 4. Skema Integrasi Dramix Gateway (:8090)
Aplikasi mengonsumsi 6 endpoint standar REST:
1. `GET /api/modelles/models`: Daftar 24 provider aktif beserta tipe konten (`long_drama`, `short_drama`, `movie`, `live_tv`).
2. `GET /api/modelles/categories`: Kategori feed per provider.
3. `GET /api/modelles/videos`: Katalog video feed pagination.
4. `GET /api/modelles/detail`: Metadata lengkap, season, dan episode list.
5. `GET /api/modelles/source`: URL stream m3u8/mp4 dan custom headers bypass.
6. `POST & GET /api/modelles/search`: Pencarian lintas 24 provider.

Endpoint Tambahan Lisensi (PocketBase Extension/Hook):
- `POST /api/license/activate`: Validasi `license_key` + `device_id` ➔ menghasilkan token sesi VIP dan tanggal kedaluwarsa.
- `GET /api/license/status`: Pengecekan status lisensi aktif perangkat.

### 5. Keamanan & Anti-Tamper
- **Server-Authoritative:** Server menolak memberikan URL stream pada episode 4+ jika request tidak menyertakan header authorization token lisensi yang sah.
- **SSL Certificate Pinning:** Mengunci SHA-256 fingerprint sertifikat domain gateway pada `OkHttpClient` untuk mencegah inspeksi proxy (Burp Suite, mitmproxy).
- **Network Security Config:** Memblokir User/Custom CA certificate; hanya mempercayai System CA.
- **Anti-Frida & Anti-Root Check:** Deteksi runtime port Frida (`27042`), memory map library hooking, dan binary su/Magisk saat cold start.
- **App Signature Verification:** Pengecekan SHA-256 signature APK saat runtime untuk mencegah repackaging/modifikasi APK.

## Project Structure

Struktur direktori proyek di `D:\03_Development_dan_Programming\02_Mobile_Development\Dramix_Android`:

```text
Dramix_Android/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/dramix/app/
│   │   │   │   ├── core/
│   │   │   │   │   ├── common/         -> Result wrapper, dispatchers, extensions
│   │   │   │   │   ├── network/        -> OkHttpClient, SSL Pinning, interceptors, Retrofit
│   │   │   │   │   ├── security/       -> DeviceId generator, AntiFrida, SignatureVerifier
│   │   │   │   │   └── database/       -> Room DB, Entities, DAOs (History, Bookmark, Download)
│   │   │   │   ├── data/
│   │   │   │   │   ├── repository/     -> Implementasi repository (Catalog, License, Player)
│   │   │   │   │   └── source/         -> GatewayApiService, LocalDataSource
│   │   │   │   ├── domain/
│   │   │   │   │   ├── model/          -> Domain entity model (Drama, Episode, StreamSource)
│   │   │   │   │   └── usecase/        -> Business logic use cases
│   │   │   │   ├── player/
│   │   │   │   │   ├── engine/         -> Media3 ExoPlayer wrapper, CacheManager, HeaderInjector
│   │   │   │   │   └── download/       -> Media3 DownloadService, DownloadTracker
│   │   │   │   ├── ui/
│   │   │   │   │   ├── theme/          -> Color, Type, Shape, Theme (Cinema Dark M3)
│   │   │   │   │   ├── components/     -> VideoPlayerBox, AdaptiveEpisodeList, BottomNav
│   │   │   │   │   ├── screens/
│   │   │   │   │   │   ├── splash/     -> Splash screen (License & Security check)
│   │   │   │   │   │   ├── home/       -> Catalog, Category tabs, Search bar
│   │   │   │   │   │   ├── player_vod/ -> Direct player long drama/movie + adaptive episodes
│   │   │   │   │   │   ├── player_shorts/ -> 9:16 Fullscreen vertical snap + bottom sheet
│   │   │   │   │   │   ├── player_tv/  -> Live TV player + channel switcher
│   │   │   │   │   │   ├── search/     -> Unified search screen
│   │   │   │   │   │   └── profile/    -> Tab "Saya", input lisensi, download manager, history
│   │   │   │   │   └── navigation/     -> AppNavigation, NavGraph, Route definitions
│   │   │   │   └── DramixApplication.kt
│   │   │   ├── res/
│   │   │   │   ├── xml/                -> network_security_config.xml
│   │   │   │   └── values/             -> strings, colors, splash themes
│   │   │   └── AndroidManifest.xml
│   │   └── test/                       -> Unit tests (Repositories, UseCases, ViewModels)
│   └── build.gradle.kts
├── gradle/
│   └── wrapper/                        -> Gradle wrapper (v8.7+)
├── docs/
│   ├── prd/
│   │   └── android-streaming-app.md    -> Dokumen PRD ini
│   ├── schema/                         -> Skema database Room
│   └── tasks/                          -> Task plan eksekusi
├── build.gradle.kts
└── settings.gradle.kts
```

## Commands

Executable build and test CLI commands:

- Build Debug APK:
  `./gradlew assembleDebug`
- Build Release APK:
  `./gradlew assembleRelease`
- Run Unit Tests:
  `./gradlew testDebugUnitTest`
- Install Debug APK ke Perangkat via ADB:
  `./gradlew installDebug` atau `adb install -r app/build/outputs/apk/debug/app-debug.apk`
- Monitor Runtime Log:
  `adb logcat -s DramixApp:V ExoPlayer:D`

## Testing Strategy

- **Unit Testing (JUnit 5 + MockK + Turbine):**
  - Menguji parsing deterministik JSON dari 6 endpoint gateway.
  - Menguji logika pemotongan episode gratis (Freemium Gatekeeper: Episode <= 3 lolos tanpa lisensi, Episode > 3 memicu event `RequireLicense`).
  - Menguji Room DAO (penyimpanan riwayat tontonan dan bookmark).
- **Security Verification Test:**
  - Uji penolakan handshake SSL ketika proxy MITM aktif.
  - Uji deteksi integritas signature APK.
- **Media Player Smoke Test:**
  - Verifikasi injeksi header `Referer` dan `User-Agent` pada HTTP data source.
  - Verifikasi transisi seek dan resume playback dari riwayat posisi `position_ms`.

## Boundaries

- **Always:**
  - Gunakan `collectAsStateWithLifecycle()` pada Jetpack Compose untuk menghemat baterai saat layar di background.
  - Release instance player atau stop playback saat komponen composable keluar dari backstack (`DisposableEffect`).
  - Validasi parameter query API gateway sebelum dikirim.
  - Simpan token sensitif hanya di `EncryptedSharedPreferences`.
- **Ask First:**
  - Menambahkan library dependency third-party baru di luar stack resmi.
  - Mengubah batas episode gratis (misal dari 3 episode menjadi 1 episode).
  - Mengubah skema migrasi Room Database.
- **Never:**
  - Menyimpan URL stream atau kunci enkripsi hardcoded di dalam source code Kotlin.
  - Melakukan *relay* streaming video melalui server PocketBase.
  - Mengizinkan cleartext traffic HTTP tanpa konfigurasi pengecualian eksplisit untuk testing lokal.
  - Menyertakan kredensial admin PocketBase di dalam APK.

## Out of Scope

- Pembelian in-app billing Google Play (semua pembayaran lisensi via QRIS web reseller eksternal).
- Sistem akun pengguna dengan login email/kata sandi.
- Sinkronisasi riwayat tontonan antar-perangkat berbasis multi-cloud.
- Kompresi atau transcoding video di sisi perangkat klien.
- Dukungan untuk platform iOS (fokus saat ini 100% Android Native).

## Success Criteria

- [ ] Gradle build menghasilkan file APK debug tanpa error kompilasi menggunakan JDK 21 dan Android SDK 34+.
- [ ] Pengguna dapat membuka aplikasi dan langsung melihat katalog dari endpoint `:8090` tanpa proses login.
- [ ] Klik drama panjang langsung membuka player di atas dan daftar episode adaptif di bawahnya dengan pemutaran lancar.
- [ ] Klik drama pendek langsung membuka layar penuh 9:16 dengan navigasi snap-swipe vertikal tanpa lag.
- [ ] Klik Live TV langsung memutar siaran dan menampilkan daftar channel di bawahnya.
- [ ] Episode 1–3 dapat diputar tanpa lisensi; Episode 4+ otomatis terkunci dan meminta aktivasi lisensi.
- [ ] Memasukkan lisensi yang valid di tab "Saya" membuka seluruh episode yang terkunci.
- [ ] Riwayat durasi tontonan tersimpan lokal dan melanjutkan playback di posisi yang sama saat dibuka kembali.
- [ ] Fitur download offline berhasil mengunduh video dan dapat diputar saat perangkat dalam mode Airplane.
- [ ] Request streaming video mengalir langsung ke CDN provider tanpa melewatkan traffic data video ke server MiniPC.

## Open Questions

- *Q-Open-1:* Port default gateway saat testing di HP fisik melalui Wi-Fi lokal: apakah menggunakan IP LAN host MiniPC (misal: `http://192.168.1.x:8090`) dengan switch configurable di menu pengaturan developer aplikasi? (Rekomendasi: Ya, sediakan konfigurasi Base URL di menu debug).
