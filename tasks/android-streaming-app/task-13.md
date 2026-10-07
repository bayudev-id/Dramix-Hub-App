# Task 13: Unified Search Screen

**Source:** docs/prd/android-streaming-app.md — Skema Integrasi Dramix Gateway (:8090)

**What to build:** Membangun antarmuka pencarian terpadu (`SearchScreen`) yang memanfaatkan endpoint pencarian lintas 24 provider (`POST & GET /api/modelles/search`). Menyediakan input bar pencarian dengan debounce query (500ms), filter tipe konten (Semua, Drama, Movie, Shorts, Live TV), filter provider, riwayat pencarian lokal, serta grid hasil pencarian dengan empty state ramah penonton.

## Acceptance criteria

- [ ] Input search bar dengan tombol hapus teks instan dan debouncing 500ms sebelum request dikirim ke gateway.
- [ ] Mendukung filter tipe konten: `long_drama`, `short_drama`, `movie`, `live_tv`.
- [ ] Memanggil endpoint gateway `/api/modelles/search` dengan parameter `{ q, content_type, model_id, page }`.
- [ ] Menyimpan kata kunci pencarian terakhir ke penyimpanan lokal dan menampilkannya sebagai chip riwayat pencarian.
- [ ] Grid hasil pencarian menampilkan poster kartu 2:3 untuk drama/film dan 9:16 untuk drama pendek.
- [ ] Mengklik hasil pencarian langsung mengarahkan pengguna ke layar player yang sesuai (VOD Player atau Shorts Player).
- [ ] Empty state ramah penonton tampil saat hasil tidak ditemukan (*"Tidak ada judul yang cocok untuk '[query]'"*).

## Verification

- [ ] Tests pass: `./gradlew testDebugUnitTest --tests "com.dramix.app.ui.screens.search.*"`
- [ ] Build succeeds: `./gradlew assembleDebug`
- [ ] Output matches: Mengetik query "love" memicu pencarian dan merender daftar hasil kartu drama.
- [ ] No regressions: Tidak terjadi crash saat mengetik karakter spesial atau emoji di search bar.
- [ ] Manual check: Menekan riwayat kata kunci pencarian otomatis mengisi search bar dan memicu pencarian ulang.

## Blocked by

Task 05, Task 08

## Files likely touched

- `app/src/main/java/com/dramix/app/ui/screens/search/SearchScreen.kt`
- `app/src/main/java/com/dramix/app/ui/screens/search/SearchViewModel.kt`
- `app/src/main/java/com/dramix/app/ui/components/SearchBarComponent.kt`
- `app/src/main/java/com/dramix/app/ui/components/SearchResultGrid.kt`

## Estimated scope

S (3–4 files)

## Rollback

Kembalikan implementasi ke form pencarian statis.

## Notes

Gunakan Coroutines `Flow.debounce(500)` dan `distinctUntilChanged()` pada input query.
