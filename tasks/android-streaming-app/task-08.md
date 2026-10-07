# Task 08: Catalog Home Screen & Navigation Graph

**Source:** docs/prd/android-streaming-app.md — Project Structure & DESIGN.md (Screen A)

**What to build:** Membangun layar utama aplikasi (`HomeScreen`), navigasi bottom bar (Beranda, Drama Pendek, Live TV, Saya), dan struktur Navigation Graph Jetpack Compose (`AppNavigation`). Menampilkan TopBar dengan branding Dramix & tombol pencarian, horizontal carousel chips untuk pemilihan provider, spotlight hero banner 16:9, dan deretan section katalog ("Drama Populer", "Drama Pendek Trending", "Live Channel Pilihan") dengan loading shimmer skeleton.

## Acceptance criteria

- [ ] `AppNavigation` mengonfigurasi `NavHost` dengan 4 rute utama: `Home`, `ShortsFeed`, `LiveTvFeed`, dan `Profile`, serta rute pemutar langsung: `VodPlayer/{providerId}/{dramaId}`, `LiveTvPlayer/{channelId}`.
- [ ] `BottomNavigationBar` menampilkan 4 tab dengan touch target minimal 48dp dan indikator aktif warna Crimson `#E11D48`.
- [ ] `HomeViewModel` memuat daftar provider dari `/api/modelles/models` dan katalog video rekomendasi dari `/api/modelles/videos`.
- [ ] Komposisi `HomeScreen` menampilkan TopBar, Provider Chips Carousel, Hero Spotlight Banner, dan feed deretan poster film/drama rasio 2:3.
- [ ] Shimmer skeleton placeholder ter-render saat data katalog masih dalam status loading.
- [ ] Mengklik kartu drama panjang mengarahkan navigasi langsung ke `VodPlayer/{providerId}/{dramaId}`.
- [ ] Mengklik kartu drama pendek mengarahkan navigasi langsung ke tab Shorts.

## Verification

- [ ] Tests pass: `./gradlew testDebugUnitTest --tests "com.dramix.app.ui.screens.home.*"`
- [ ] Build succeeds: `./gradlew assembleDebug`
- [ ] Output matches: Compose preview `HomeScreenPreview` berhasil dirender di lingkungan dev.
- [ ] No regressions: Tidak ada recomposition berulang (*recomposition loop*) pada horizontal scroll list.
- [ ] Manual check: Navigasi antar-tab di bottom bar berjalan mulus tanpa kehilangan scroll state beranda.

## Blocked by

Task 02, Task 05

## Files likely touched

- `app/src/main/java/com/dramix/app/ui/navigation/AppNavigation.kt`
- `app/src/main/java/com/dramix/app/ui/navigation/Screen.kt`
- `app/src/main/java/com/dramix/app/ui/components/BottomNavigationBar.kt`
- `app/src/main/java/com/dramix/app/ui/screens/home/HomeScreen.kt`
- `app/src/main/java/com/dramix/app/ui/screens/home/HomeViewModel.kt`
- `app/src/main/java/com/dramix/app/ui/components/ShimmerPlaceholder.kt`

## Estimated scope

M (4–6 files)

## Rollback

Kembalikan `AppNavigation` ke layar placeholder teks sederhana.

## Notes

Gunakan `collectAsStateWithLifecycle()` di dalam composable untuk observasi StateFlow ViewModel.
