# Task 01: Project Scaffolding & Gradle CLI Setup

**Source:** docs/prd/android-streaming-app.md — Project Structure & Commands

**What to build:** Mempersiapkan pondasi proyek Android Kotlin Native yang dapat dikompilasi langsung lewat CLI tanpa Android Studio. Menghasilkan struktur folder standar, file Gradle wrapper, konfigurasi `build.gradle.kts` tingkat root dan modul app, serta mengintegrasikan dependensi inti (Jetpack Compose M3, AndroidX Media3 ExoPlayer, Room, Koin DI, Retrofit/OkHttp, dan Coroutines).

## Acceptance criteria

- [ ] Proyek memiliki `settings.gradle.kts`, `build.gradle.kts` root, dan `app/build.gradle.kts`.
- [ ] Gradle wrapper (`gradlew` & `gradlew.bat`) terkonfigurasi dengan Gradle versi 8.7+ dan kompatibel dengan JDK 21.
- [ ] `./gradlew assembleDebug` sukses mengompilasi APK debug minimal dengan `applicationId = "com.dramix.app"`, `minSdk = 24`, dan `targetSdk = 34`.
- [ ] `DramixApplication` terdaftar di `AndroidManifest.xml` dengan inisialisasi Koin DI kosong yang valid.

## Verification

- [ ] Tests pass: `./gradlew test`
- [ ] Build succeeds: `./gradlew assembleDebug`
- [ ] Output matches: File `app/build/outputs/apk/debug/app-debug.apk` terbentuk dan berukuran valid.
- [ ] No regressions: Tidak ada warning `compileSdk` usang atau error versi Java toolchain.
- [ ] Manual check: Eksekusi `aapt dump badging app/build/outputs/apk/debug/app-debug.apk` menampilkan package `com.dramix.app` dan sdkVersion `24`.

## Blocked by

None — can start immediately.

## Files likely touched

- `settings.gradle.kts`
- `build.gradle.kts`
- `gradle.properties`
- `app/build.gradle.kts`
- `app/src/main/AndroidManifest.xml`
- `app/src/main/java/com/dramix/app/DramixApplication.kt`

## Estimated scope

M (3–5 files)

## Rollback

Hapus file konfigurasi gradle dan direktori `app/` untuk kembali ke kondisi awal repo kosong.

## Notes

Gunakan Koin DI versi 3.5+ dan Media3 versi 1.3+ untuk menghindari konflik dependensi ExoPlayer versi lama.
