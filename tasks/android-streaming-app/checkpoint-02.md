# Checkpoint 02: Core Engine & Security Gate (after Tasks 04–06)

- [ ] Network Security Config memblokir user-installed CA certificates.
- [ ] Deteksi anti-Frida dan anti-Root terpicu saat diuji di environment testing.
- [ ] Pemetaan 6 endpoint Dramix Gateway (:8090) lolos integrasi unit test dengan mock responses (parsing deterministik JSON).
- [ ] Logika Freemium Gatekeeper terverifikasi: Episode 1–3 lolos tanpa lisensi, Episode 4+ melempar status `RequireLicenseException`.
- [ ] Token lisensi tersimpan dengan enkripsi aman di `EncryptedSharedPreferences` / `DataStore`.
- [ ] Human review: pastikan tidak ada kredensial admin atau token hardcoded di source code.

Jika ada item yang gagal, hentikan proses dan perbaiki sebelum memulai Phase 3 (Player & Screens).
