# Task 11: Direct-Play Live TV Screen

**Source:** docs/prd/android-streaming-app.md — Pola Navigasi Direct-Play & DESIGN.md (Screen D)

**What to build:** Membangun antarmuka siaran langsung Live TV (`LiveTvPlayerScreen`). Bagian atas menampilkan pemutar video siaran langsung low-latency HLS dengan badge kedip merah `● LIVE`. Bagian bawah menampilkan filter chips kategori channel (Semua, Nasional, Olahraga, Berita, dll.) dan daftar vertikal saluran TV tanpa sinopsis panjang. Mengklik saluran baru langsung menukar stream pemutar atas tanpa me-reload layar.

## Acceptance criteria

- [ ] `LiveTvPlayerScreen` terbagi dua area: Player siaran 16:9 di atas, dan daftar saluran di bawah.
- [ ] Bagian pemutar memiliki indikator status siaran langsung (`● LIVE`) di sudut kiri atas.
- [ ] Bagian bawah memiliki deretan chips filter kategori channel (`LazyRow`).
- [ ] Daftar saluran (`LazyColumn`) menampilkan logo stasiun TV, nama channel, dan program yang sedang tayang.
- [ ] Mengklik saluran lain langsung memanggil `player.setMediaItem()` dengan URL stream channel baru secara mulus (*seamless stream switch*).
- [ ] Mendukung pemutaran low-latency HLS untuk siaran olahraga/berita langsung.

## Verification

- [ ] Tests pass: `./gradlew testDebugUnitTest --tests "com.dramix.app.ui.screens.player_tv.*"`
- [ ] Build succeeds: `./gradlew assembleDebug`
- [ ] Output matches: Memilih channel "RCTI" lalu "TVRI" langsung menukar siaran dalam waktu <1.5 detik.
- [ ] No regressions: Tidak ada crash saat stream siaran langsung offline atau putus sinyal (menampilkan retry overlay).
- [ ] Manual check: Memfilter chip "Olahraga" hanya menampilkan channel kategori olahraga.

## Blocked by

Task 07, Task 08

## Files likely touched

- `app/src/main/java/com/dramix/app/ui/screens/player_tv/LiveTvPlayerScreen.kt`
- `app/src/main/java/com/dramix/app/ui/screens/player_tv/LiveTvPlayerViewModel.kt`
- `app/src/main/java/com/dramix/app/ui/components/LiveChannelItem.kt`
- `app/src/main/java/com/dramix/app/ui/components/CategoryChipsRow.kt`

## Estimated scope

M (4 files)

## Rollback

Kembalikan ke list TV sederhana tanpa integrasi inline player.

## Notes

Setel `exoPlayer.videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT` agar rasio siaran 4:3 atau 16:9 tidak terdistorsi secara paksa.
