# Task 07: Media3 ExoPlayer Engine & Header Injection

**Source:** docs/prd/android-streaming-app.md — Media Player & Download Engine (AndroidX Media3)

**What to build:** Membangun engine pemutar video berbasis AndroidX Media3 ExoPlayer yang mengeliminasi relay bandwidth server perantara. Menghasilkan wrapper `DramixPlayer` dengan injeksi dinamis header HTTP (`Referer`, `User-Agent`) melalui `OkHttpDataSource.Factory`, konfigurasi buffer rendah untuk pemutaran instan (<2 detik TTFF), integrasi `SimpleCache` (LRU 200MB) untuk pre-buffering drama pendek, dan penanganan lifecycle yang bersih untuk mencegah memory leak.

## Acceptance criteria

- [ ] `PlayerFactory` menyediakan instance `ExoPlayer` yang menggunakan `OkHttpDataSource.Factory` dengan header dinamis per `MediaItem`.
- [ ] Header `Referer` dan `User-Agent` yang diterima dari respons `source` gateway (:8090) disuntikkan secara otomatis ke setiap request segmen HLS/MP4.
- [ ] Konfigurasi `DefaultLoadControl` diatur dengan buffer minimum 1.500ms dan buffer maksimum 5.000ms untuk percepatan *time-to-first-frame*.
- [ ] `CacheDataSource.Factory` mengintegrasikan `SimpleCache` berkapasitas 200MB di direktori cache privat aplikasi.
- [ ] Wrapper `DramixPlayerController` menyediakan kontrol playback (play, pause, seek, setMediaSource) dan state reaktif (`PlaybackState: Idle, Buffering, Ready, Ended, Error`).
- [ ] Pengujian unit memverifikasi bahwa header yang disetel pada MediaItem terbawa ke request HTTP DataSource.

## Verification

- [ ] Tests pass: `./gradlew testDebugUnitTest --tests "com.dramix.app.player.*"`
- [ ] Build succeeds: `./gradlew assembleDebug`
- [ ] Output matches: Request video segmen .ts/.m3u8 mengalir langsung ke host asal CDN dengan header Referer yang tepat tanpa melewati port 8090.
- [ ] No regressions: Tidak ada `IllegalStateException` saat player di-release di background thread.
- [ ] Manual check: Memutar video uji HLS menghasilkan pemutaran audio/video yang sinkron dan lancar.

## Blocked by

Task 05, Task 06

## Files likely touched

- `app/src/main/java/com/dramix/app/player/engine/PlayerFactory.kt`
- `app/src/main/java/com/dramix/app/player/engine/HeaderInjectingDataSourceFactory.kt`
- `app/src/main/java/com/dramix/app/player/engine/CacheManager.kt`
- `app/src/main/java/com/dramix/app/player/controller/DramixPlayerController.kt`
- `app/src/main/java/com/dramix/app/player/model/PlaybackState.kt`

## Estimated scope

M (4–5 files)

## Rollback

Gunakan `DefaultHttpDataSource.Factory` standar tanpa custom cache layer.

## Notes

Gunakan `MediaItem.Builder().setRequestMetadata()` atau implementasikan delegating `HttpDataSource` untuk menyematkan custom header per-stream.
