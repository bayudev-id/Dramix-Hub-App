# Task 14: Background Offline Downloader

**Source:** docs/prd/android-streaming-app.md — Offline Background Download (Media3 + WorkManager)

**What to build:** Membangun engine pengunduhan video latar belakang menggunakan AndroidX Media3 `DownloadService` yang terintegrasi dengan `WorkManager` dan entitas `DownloadRecordDao`. Memungkinkan pengguna mengunduh episode video (HLS segmen / MP4) ke penyimpanan internal privat, memantau persentase progres unduhan di notification bar dan menu Manajer Download, serta memutar video secara offline tanpa koneksi internet (Airplane Mode).

## Acceptance criteria

- [ ] Implementasi `DramixDownloadService` turunan dari `androidx.media3.exoplayer.offline.DownloadService`.
- [ ] Terintegrasi dengan `DownloadNotificationHelper` untuk menampilkan foreground notification lengkap dengan progress bar unduhan.
- [ ] Pengunduhan menyuntikkan header `Referer` dan `User-Agent` yang valid saat mengambil file segmen HLS.
- [ ] Siklus unduhan memperbarui baris `download_record` di Room DB:
  - Saat antrean masuk: status `QUEUED`.
  - Saat progres berjalan: status `DOWNLOADING` dan update `progress_percentage`.
  - Saat rampung: status `COMPLETED` dan menyimpan `local_uri`.
  - Saat error: status `FAILED` beserta `error_message`.
- [ ] Layar Manajer Download menampilkan daftar file unduhan dengan tombol pause, resume, hapus, dan play.
- [ ] Pemutar `VodPlayerScreen` mendeteksi jika episode sudah tersimpan offline dan memutar file lokal tanpa melakukan request HTTP ke CDN.

## Verification

- [ ] Tests pass: `./gradlew testDebugUnitTest --tests "com.dramix.app.player.download.*"`
- [ ] Build succeeds: `./gradlew assembleDebug`
- [ ] Output matches: File video terunduh tersimpan di direktori privat aplikasi (`context.getExternalFilesDir()`) dan tercatat `COMPLETED` di database Room.
- [ ] No regressions: Mematikan koneksi internet (Airplane Mode) tetap memungkinkan pemutaran episode yang telah selesai diunduh.
- [ ] Manual check: Menghapus unduhan dari UI membersihkan file fisik di storage dan baris database.

## Blocked by

Task 03, Task 07, Task 12

## Files likely touched

- `app/src/main/java/com/dramix/app/player/download/DramixDownloadService.kt`
- `app/src/main/java/com/dramix/app/player/download/DownloadTracker.kt`
- `app/src/main/java/com/dramix/app/player/download/DownloadManagerHelper.kt`
- `app/src/main/java/com/dramix/app/ui/screens/profile/DownloadManagerScreen.kt`

## Estimated scope

M (4–5 files)

## Rollback

Nonaktifkan tombol download pada UI dan hapus pendaftaran DownloadService di AndroidManifest.xml.

## Notes

Gunakan `Media3 DownloadRequest.Builder` dengan kustomisasi data untuk menyimpan metadata drama (judul, episode, provider).
