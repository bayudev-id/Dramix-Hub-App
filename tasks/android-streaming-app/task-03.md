# Task 03: Local-First Room Database & DAOs

**Source:** docs/prd/android-streaming-app.md — Technical Decisions & docs/schema/android-streaming-app.md

**What to build:** Mengimplementasikan database SQLite lokal Android (Room DB) untuk persistensi data *local-first* tanpa akun server. Mencakup tabel dan DAO untuk: riwayat tontonan per episode dengan agregasi drama terbaru (`WatchHistoryDao`), penanda favorit (`BookmarkDao`), dan pelacak rekaman unduhan offline (`DownloadRecordDao`).

## Acceptance criteria

- [ ] Entitas `WatchHistoryEntity` memiliki composite unique index `(drama_id, provider_id, episode_number)` dan index `updated_at DESC`.
- [ ] Entitas `BookmarkEntity` memiliki composite unique index `(drama_id, provider_id)`.
- [ ] Entitas `DownloadRecordEntity` memiliki unique index pada `media_id` dan index `status`.
- [ ] `WatchHistoryDao` memiliki metode upsert `insertOrUpdateWatchHistory()` dan query reaktif `getLatestWatchedDramas(): Flow<List<WatchHistoryEntity>>` (1 baris per drama untuk Beranda) serta `getDramaHistory(dramaId, providerId): Flow<List<WatchHistoryEntity>>` (semua episode dengan garis progres).
- [ ] `BookmarkDao` mendukung penambahan, penghapusan, dan query `isBookmarked(dramaId, providerId): Flow<Boolean>`.
- [ ] In-memory Room DB unit test memverifikasi operasi CRUD dan constraint unique.

## Verification

- [ ] Tests pass: `./gradlew testDebugUnitTest --tests "com.dramix.app.core.database.*"`
- [ ] Build succeeds: `./gradlew assembleDebug`
- [ ] Output matches: Room schema json ter-generate otomatis di direktori schemas/ (jika exportSchema aktif).
- [ ] No regressions: Tidak ada main-thread database query (semua akses dibungkus Coroutine Dispatchers.IO / Flow).
- [ ] Manual check: Menonton episode 1 lalu episode 2 menghasilkan 2 baris riwayat per episode, namun query `getLatestWatchedDramas()` hanya mengembalikan episode 2.

## Blocked by

Task 01

## Files likely touched

- `app/src/main/java/com/dramix/app/core/database/entity/WatchHistoryEntity.kt`
- `app/src/main/java/com/dramix/app/core/database/entity/BookmarkEntity.kt`
- `app/src/main/java/com/dramix/app/core/database/entity/DownloadRecordEntity.kt`
- `app/src/main/java/com/dramix/app/core/database/dao/WatchHistoryDao.kt`
- `app/src/main/java/com/dramix/app/core/database/dao/BookmarkDao.kt`
- `app/src/main/java/com/dramix/app/core/database/dao/DownloadRecordDao.kt`
- `app/src/main/java/com/dramix/app/core/database/AppDatabase.kt`
- `app/src/test/java/com/dramix/app/core/database/DatabaseTest.kt`

## Estimated scope

M (4–6 files)

## Rollback

Hapus paket `com/dramix/app/core/database/` dan bersihkan dependency Room di `app/build.gradle.kts`.

## Notes

Gunakan Room versi 2.6+ dengan KSP untuk performa kompilasi stabil.
