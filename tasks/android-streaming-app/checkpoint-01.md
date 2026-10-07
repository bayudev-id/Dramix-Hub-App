# Checkpoint 01: Foundation Gate (after Tasks 01–03)

- [ ] Gradle build `./gradlew assembleDebug` berhasil tanpa error kompilasi menggunakan JDK 21 dan Android SDK 34+.
- [ ] Theme Jetpack Compose (`Theme.kt`, `Color.kt`, `Type.kt`) terkonfigurasi sesuai token `DESIGN.md` (Cinema Dark OLED `#000000`).
- [ ] Entitas Room Database (`watch_history`, `bookmark`, `download_record`) lulus tes in-memory database test (`@RunWith(AndroidJUnit4::class)`).
- [ ] Dependency Injection Koin terinisialisasi bersih di `DramixApplication`.
- [ ] Human review: periksa `build.gradle.kts` dan pastikan tidak ada dependensi eksternal yang tidak diperlukan.

Jika ada yang gagal, perbaiki sebelum lanjut ke Phase 2 (Core Engine & Security).
