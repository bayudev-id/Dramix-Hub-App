# Task 12: Profile "Saya" Screen & UI Aktivasi Lisensi

**Source:** docs/prd/android-streaming-app.md — Monetisasi & DESIGN.md (Screen E)

**What to build:** Membangun antarmuka tab "Saya" (`ProfileScreen`) sebagai pusat akun perangkat lokal dan gerbang aktivasi lisensi. Menampilkan kartu identitas perangkat, status VIP (Free User / VIP Member dengan tanggal kedaluwarsa), formulir aktivasi kode lisensi (`LCN-SELLER-...`) yang terhubung langsung ke Gateway PocketBase, serta menu navigasi lokal: Riwayat Menonton, Daftar Favorit, Manajer Download, dan Pembersihan Cache.

## Acceptance criteria

- [ ] Header menampilkan ID perangkat singkat dan badge status (`FREE USER` atau `VIP MEMBER s/d dd MMM yyyy`).
- [ ] Card aktivasi lisensi menampilkan input field `OutlinedTextField` untuk kode lisensi dan tombol primer "Aktivasi Sekarang" `#E11D48`.
- [ ] Menekan tombol aktivasi memanggil `LicenseViewModel.activate(licenseKey)` ke Gateway `:8090`.
- [ ] Menampilkan feedback visual snackbar/dialog saat aktivasi berhasil atau gagal (misal: "Lisensi sudah terikat ke HP lain" / "Kode salah").
- [ ] Menu navigasi lokal:
  - *Riwayat Menonton:* Membuka daftar `watch_history` lokal.
  - *Favorit:* Membuka daftar drama `bookmark`.
  - *Download:* Membuka manajer rekaman unduhan offline.
  - *Bersihkan Cache:* Menghitung ukuran cache ExoPlayer dan menghapusnya saat dikonfirmasi.

## Verification

- [ ] Tests pass: `./gradlew testDebugUnitTest --tests "com.dramix.app.ui.screens.profile.*"`
- [ ] Build succeeds: `./gradlew assembleDebug`
- [ ] Output matches: Memasukkan lisensi valid mengubah tampilan status dari Free User menjadi VIP Member seketika tanpa restart aplikasi.
- [ ] No regressions: Tombol "Bersihkan Cache" tidak menghapus database Room (hanya menghapus file temporary cache ExoPlayer).
- [ ] Manual check: Klik "Riwayat Menonton" menampilkan episode yang sebelumnya ditonton di Task 09.

## Blocked by

Task 03, Task 06

## Files likely touched

- `app/src/main/java/com/dramix/app/ui/screens/profile/ProfileScreen.kt`
- `app/src/main/java/com/dramix/app/ui/screens/profile/ProfileViewModel.kt`
- `app/src/main/java/com/dramix/app/ui/screens/profile/LicenseActivationCard.kt`
- `app/src/main/java/com/dramix/app/ui/screens/profile/LocalLibraryScreens.kt`

## Estimated scope

S (3–4 files)

## Rollback

Kembalikan ke tampilan profil statis tanpa form aktivasi.

## Notes

Gunakan keyboard action `ImeAction.Done` pada field lisensi agar tombol langsung memicu submit tanpa klik manual.
