# Implementation Plan: Dramix Android Streaming App

## Overview

Dokumen rencana implementasi ini memetakan urutan kerja modular berorientasi dependensi untuk membangun aplikasi streaming native Android (Kotlin, Jetpack Compose, AndroidX Media3) yang mengonsumsi Dramix Gateway (:8090) dari 24 provider. Disusun berdasarkan kontrak `docs/prd/android-streaming-app.md`, panduan visual `DESIGN.md`, dan skema database `docs/schema/android-streaming-app.md`.

## Architecture Decisions

- **Koin DI:** Dipilih dibanding Hilt/KSP untuk mengoptimalkan kecepatan build CLI `./gradlew assembleDebug` tanpa kompilasi code-generation kapt/ksp overhead yang lambat.
- **Direct-Play Unified Route:** Memangkas detail screen terpisah; navigasi langsung mengarahkan user ke player layar penuh/atas sesuai tipe konten (`long_drama`, `short_drama`, `live_tv`).
- **Media3 OkHttpDataSource Factory:** Menyuntikkan custom HTTP header (`Referer` dan `User-Agent`) secara dinamis langsung dari perangkat klien ke CDN stream untuk menghindari relay server MiniPC (0 bytes overhead gateway).
- **Local-First Room Storage:** Bookmark, history per episode, dan download offline dikelola murni lokal tanpa dependensi akun server.

## Phases

### Phase 1: Foundation (Tasks 01–03)
- [ ] Task 01: Project Scaffolding & Gradle CLI Setup
- [ ] Task 02: Cinema Dark Design Tokens & Theme Setup
- [ ] Task 03: Local-First Room Database & DAOs

**Checkpoint 01:** `./gradlew assembleDebug` berhasil, tema Compose ter-render, dan Room Database Migration unit test lolos.

### Phase 2: Core Engine & Security (Tasks 04–06)
- [ ] Task 04: Core Networking & Security Hardening
- [ ] Task 05: Gateway Retrofit Client & 6 Endpoint Mappings
- [ ] Task 06: Licensing & Entitlement Gate

**Checkpoint 02:** SSL Pinning lolos, semua 6 endpoint Dramix Gateway (:8090) terhubung sukses, dan validasi lisensi unit test lolos.

### Phase 3: Player Engines & Direct-Play Screens (Tasks 07–13)
- [ ] Task 07: Media3 ExoPlayer Engine & Header Injection
- [ ] Task 08: Catalog Home Screen & Navigation Graph
- [ ] Task 09: Direct-Play VOD Player Screen (Long Drama & Movie)
- [ ] Task 10: Direct-Play Shorts 9:16 Screen
- [ ] Task 11: Direct-Play Live TV Screen
- [ ] Task 12: Profile "Saya" Screen & UI Aktivasi Lisensi
- [ ] Task 13: Unified Search Screen

### Phase 4: Offline Engine & Integration (Task 14)
- [ ] Task 14: Background Offline Downloader (Media3 DownloadService + WorkManager)

**Final Checkpoint (Checkpoint 03):** Seluruh kriteria sukses di PRD terverifikasi, aplikasi berjalan mulus di perangkat Android fisik/emulator dengan 0 byte relay bandwidth pada host gateway.

## Parallelization

| Task | Category | Notes |
|---|---|---|
| 01 | Must be sequential | Mempersiapkan root build.gradle, settings.gradle, dan dependensi dasar |
| 02 | Must be sequential | Mengonfigurasi ColorScheme, Typography, dan Shape Jetpack Compose |
| 03 | Must be sequential | Mengonfigurasi Room Database dan DAO contracts |
| 04 | Must be sequential | Setup OkHttpClient, SSL Pinning, dan Anti-Tamper |
| 05 | Must be sequential | Pemetaan 6 endpoint REST Gateway (:8090) |
| 06 | Needs coordination | Menghubungkan lisensi dengan gateway dan EncryptedDataStore |
| 07 | Needs coordination | Menghubungkan Media3 data source dengan respons endpoint `source` |
| 08 | Safe to parallelize | Layout katalog beranda dan navigasi tab |
| 09 | Must be sequential | Menggabungkan player Media3 dengan episode list adaptif dan Room DB |
| 10 | Safe to parallelize | Player vertikal 9:16 snap swipe shorts |
| 11 | Safe to parallelize | Player Live TV dan channel switcher |
| 12 | Safe to parallelize | Halaman profil dan dialog input lisensi |
| 13 | Safe to parallelize | Pencarian multi-provider |
| 14 | Must be sequential | Download background terhubung dengan Room DB dan Media3 engine |

## Risks and Mitigations

| Risk | Impact | Mitigation |
|---|---|---|
| Stream diblokir karena missing header HTTP (403 Forbidden) | High | Implementasi OkHttpDataSource di Task 07 menyuntikkan header Referer/UA dinamis persis seperti instruksi endpoint `source`. |
| Memory leak instance ExoPlayer saat ganti screen | High | Gunakan DisposableEffect di Compose untuk memanggil `player.release()` atau detach surface saat composable di-destroy. |
| Pembajakan lisensi lewat modifikasi APK | High | Server-authoritative token validation di Gateway PocketBase (:8090) + device binding; APK tanpa token sah ditolak memutar video. |
| Recomposition berlebih pada list episode/shorts feed | Medium | Gunakan `@Stable` / `@Immutable` data model dan `key` stabil pada `LazyRow` / `LazyVerticalGrid` / `VerticalPager`. |

## Open Questions

- *Q-Open-1 (Blocks Task 05):* Base URL gateway saat testing: default menggunakan `http://10.0.2.2:8090` (emulator) atau `http://<IP_LAN>:8090` (device fisik)? ➔ Mitigasi: buat BuildConfig field dinamis atau menu switch debug.

## Task Index

| # | Task | Size | Blocked by | Category | File |
|---|---|---|---|---|---|
| 01 | Project Scaffolding & Gradle CLI Setup | M | — | sequential | `task-01.md` |
| 02 | Cinema Dark Design Tokens & Theme Setup | S | 01 | sequential | `task-02.md` |
| 03 | Local-First Room Database & DAOs | M | 01 | sequential | `task-03.md` |
| 04 | Core Networking & Security Hardening | M | 01 | sequential | `task-04.md` |
| 05 | Gateway Retrofit Client & 6 Endpoint Mappings | M | 04 | sequential | `task-05.md` |
| 06 | Licensing & Entitlement Gate | M | 04, 05 | needs coordination | `task-06.md` |
| 07 | Media3 ExoPlayer Engine & Header Injection | M | 05, 06 | needs coordination | `task-07.md` |
| 08 | Catalog Home Screen & Navigation Graph | M | 02, 05 | parallel | `task-08.md` |
| 09 | Direct-Play VOD Player Screen (Long Drama & Movie) | M | 03, 07, 08 | sequential | `task-09.md` |
| 10 | Direct-Play Shorts 9:16 Screen | M | 07, 08 | parallel | `task-10.md` |
| 11 | Direct-Play Live TV Screen | M | 07, 08 | parallel | `task-11.md` |
| 12 | Profile "Saya" Screen & UI Aktivasi Lisensi | S | 03, 06 | parallel | `task-12.md` |
| 13 | Unified Search Screen | S | 05, 08 | parallel | `task-13.md` |
| 14 | Background Offline Downloader | M | 03, 07, 12 | sequential | `task-14.md` |
