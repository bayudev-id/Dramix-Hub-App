# Task 06: Licensing & Entitlement Gate

**Source:** docs/prd/android-streaming-app.md — Monetisasi & Entitlement Service

**What to build:** Membangun modul manajemen lisensi berbasis *server-authoritative* dan *gatekeeper freemium*. Mengatur aktivasi kode lisensi (`LCN-SELLER-...`) melalui gateway PocketBase, penyimpanan sesi token di EncryptedDataStore, pemeriksaan status kadaluarsa lokal, serta *Freemium Policy Engine* yang mengizinkan pemutaran gratis untuk Episode 1–3 dan mewajibkan lisensi aktif untuk Episode 4 ke atas.

## Acceptance criteria

- [ ] `LicenseApiService` mendefinisikan `POST /api/license/activate` (mengirim `license_key` + `device_id`) dan `GET /api/license/status`.
- [ ] `LicensePreferences` menyimpan status lisensi (`isVip: Boolean`, `expiresAt: Long`, `sessionToken: String?`) secara terenkripsi menggunakan AndroidX Security Crypto (`EncryptedSharedPreferences`).
- [ ] `EntitlementManager` mengevaluasi izin pemutaran:
  - Episode 1–3: Mengembalikan status `AccessGranted(isFree = true)`.
  - Episode >= 4 tanpa lisensi aktif: Mengembalikan status `AccessDenied(reason = "REQUIRE_VIP_LICENSE")`.
  - Episode >= 4 dengan lisensi aktif valid: Mengembalikan status `AccessGranted(isFree = false)`.
- [ ] Auth header interceptor menyematkan token `Authorization: Bearer <sessionToken>` pada request pemutaran video jika lisensi aktif.
- [ ] Unit test memverifikasi skenario kadaluarsa lisensi dan penolakan akses pada episode 4.

## Verification

- [ ] Tests pass: `./gradlew testDebugUnitTest --tests "com.dramix.app.data.license.*"`
- [ ] Build succeeds: `./gradlew assembleDebug`
- [ ] Output matches: `EntitlementManager.canPlayEpisode(episode = 4)` mengembalikan `AccessDenied` saat token lisensi kosong.
- [ ] No regressions: Episode 1, 2, dan 3 tetap dapat diputar tanpa token lisensi dan tanpa crash.
- [ ] Manual check: Aktivasi lisensi dummy berhasil menyimpan token ke EncryptedDataStore dan mengubah status `isVip` menjadi `true`.

## Blocked by

Task 04, Task 05

## Files likely touched

- `app/src/main/java/com/dramix/app/data/source/remote/LicenseApiService.kt`
- `app/src/main/java/com/dramix/app/data/source/local/LicensePreferences.kt`
- `app/src/main/java/com/dramix/app/domain/manager/EntitlementManager.kt`
- `app/src/main/java/com/dramix/app/data/repository/LicenseRepositoryImpl.kt`
- `app/src/main/java/com/dramix/app/domain/repository/LicenseRepository.kt`
- `app/src/test/java/com/dramix/app/domain/manager/EntitlementManagerTest.kt`

## Estimated scope

M (4–5 files)

## Rollback

Kembalikan `EntitlementManager` ke mode stub yang selalu mengizinkan akses (untuk testing).

## Notes

Jangan menyimpan logika validasi rumus string lisensi di client; validasi 100% bergantung pada respons server gateway.
