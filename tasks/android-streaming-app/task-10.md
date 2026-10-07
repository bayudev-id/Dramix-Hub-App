# Task 10: Direct-Play Shorts 9:16 Screen

**Source:** docs/prd/android-streaming-app.md — Pola Navigasi Direct-Play & DESIGN.md (Screen C)

**What to build:** Membangun antarmuka pemutar drama pendek layar penuh vertikal 9:16 (`ShortsPlayerScreen`). Menggunakan `VerticalPager` Jetpack Compose dengan gesture snap-to-page per episode. Menampilkan overlay kiri bawah berisi judul drama dan sinopsis terpotong (`...`) yang dapat diklik untuk membuka `ModalBottomSheet` detail lengkap, action bar kanan vertikal (bookmark, episode drawer, download), serta pre-buffering episode berikutnya menggunakan `SimpleCache`.

## Acceptance criteria

- [ ] `ShortsPlayerScreen` menampilkan video layar penuh 9:16 tanpa batas pinggir hitam (*crop-to-fit* / *fit-aspect* vertikal).
- [ ] Menggunakan `VerticalPager` untuk navigasi antar-episode drama pendek via sapuan vertikal (*snap swipe*).
- [ ] Episode n+1 di-pre-buffer di background melalui ExoPlayer SimpleCache agar pemutaran terjadi instan saat di-swipe.
- [ ] Overlay kiri bawah menampilkan judul tebal, nomor episode (`Ep. 1/80`), dan deskripsi terpotong 2 baris.
- [ ] Mengklik teks judul/deskripsi membuka `ModalBottomSheet` detail (sinopsis lengkap, info pemeran, dan laci seluruh daftar 80 episode).
- [ ] Memilih episode tertentu dari BottomSheet langsung melompatkan `VerticalPager` ke index episode yang dipilih.
- [ ] Episode >= 4 pada pengguna non-VIP memunculkan bottom sheet peringatan aktivasi lisensi.

## Verification

- [ ] Tests pass: `./gradlew testDebugUnitTest --tests "com.dramix.app.ui.screens.player_shorts.*"`
- [ ] Build succeeds: `./gradlew assembleDebug`
- [ ] Output matches: Sapuan vertikal ke atas berpindah ke episode berikutnya dalam waktu <0.5 detik.
- [ ] No regressions: Audio video sebelumnya otomatis mati saat berpindah ke video baru.
- [ ] Manual check: Membuka BottomSheet detail tidak mematikan pemutaran video latar belakang.

## Blocked by

Task 07, Task 08

## Files likely touched

- `app/src/main/java/com/dramix/app/ui/screens/player_shorts/ShortsPlayerScreen.kt`
- `app/src/main/java/com/dramix/app/ui/screens/player_shorts/ShortsPlayerViewModel.kt`
- `app/src/main/java/com/dramix/app/ui/screens/player_shorts/ShortsDetailBottomSheet.kt`
- `app/src/main/java/com/dramix/app/ui/components/ShortsActionButtons.kt`

## Estimated scope

M (4 files)

## Rollback

Kembalikan implementasi ke pager standar tanpa bottom sheet overlay.

## Notes

Gunakan `derivedStateOf` pada `pagerState.currentPage` untuk menghindari recomposition yang tidak perlu saat scrolling sedang berlangsung.
