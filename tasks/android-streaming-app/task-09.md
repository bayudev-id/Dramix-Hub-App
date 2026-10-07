# Task 09: Direct-Play VOD Player Screen (Long Drama & Movie)

**Source:** docs/prd/android-streaming-app.md — Pola Navigasi Direct-Play & DESIGN.md (Screen B)

**What to build:** Membangun layar pemutar langsung untuk drama panjang dan film (`VodPlayerScreen`). Pengguna yang mengklik poster di beranda langsung masuk ke layar ini: 40% area atas menampilkan pemutar Media3 yang langsung memutar episode 1 (atau episode terakhir yang ada di riwayat), sedangkan 60% area bawah menampilkan metadata lengkap, tombol aksi bookmark/download, selektor season, serta daftar episode adaptif (kartu thumbnail jika ada cover, atau grid angka jika tanpa cover).

## Acceptance criteria

- [ ] `VodPlayerScreen` terbagi dua area secara vertikal: Player view 16:9 di bagian atas, dan scrollable metadata di bagian bawah.
- [ ] Layar memuat detail drama dari `GET /api/modelles/detail` dan secara bersamaan memuat source video episode target dari `GET /api/modelles/source`.
- [ ] Jika posisi tontonan tersimpan di `WatchHistoryDao`, pemutar otomatis melanjutkan playback (*resume*) di detik `position_ms` terakhir.
- [ ] Daftar episode merender secara adaptif:
  - Jika item episode memiliki URL thumbnail ➔ Tampilkan `LazyRow` kartu horizontal dengan thumbnail 16:9.
  - Jika item episode tidak memiliki thumbnail ➔ Tampilkan `LazyVerticalGrid` tombol angka (5 kolom).
- [ ] Episode aktif ditandai dengan warna Crimson `#E11D48`.
- [ ] Tombol Bookmark mengubah state favorit secara instan dan menyimpannya ke `BookmarkDao`.
- [ ] Episode >= 4 pada pengguna non-VIP otomatis terhenti dan menampilkan modal/dialog peringatan lisensi.
- [ ] Progres pemutaran disimpan ke `WatchHistoryDao` setiap interval 5 detik atau saat pengguna meninggalkan layar (`DisposableEffect`).

## Verification

- [ ] Tests pass: `./gradlew testDebugUnitTest --tests "com.dramix.app.ui.screens.player_vod.*"`
- [ ] Build succeeds: `./gradlew assembleDebug`
- [ ] Output matches: Memilih episode 2 mengganti stream video tanpa me-reload seluruh halaman.
- [ ] No regressions: Tidak terjadi crash atau memory leak saat menekan tombol Back di HP (ExoPlayer di-release).
- [ ] Manual check: Menonton drama sampai menit 05:00, keluar dari app, masuk kembali ➔ video melanjutkan dari menit 05:00.

## Blocked by

Task 03, Task 07, Task 08

## Files likely touched

- `app/src/main/java/com/dramix/app/ui/screens/player_vod/VodPlayerScreen.kt`
- `app/src/main/java/com/dramix/app/ui/screens/player_vod/VodPlayerViewModel.kt`
- `app/src/main/java/com/dramix/app/ui/components/VideoPlayerSurface.kt`
- `app/src/main/java/com/dramix/app/ui/components/AdaptiveEpisodeList.kt`
- `app/src/main/java/com/dramix/app/ui/components/LicenseGateDialog.kt`

## Estimated scope

M (4–5 files)

## Rollback

Kembalikan ke halaman player statis tanpa integrasi episode list.

## Notes

Gunakan `AndroidView` untuk me-render `PlayerView` Media3 di dalam Jetpack Compose dengan `keepScreenOn = true`.
