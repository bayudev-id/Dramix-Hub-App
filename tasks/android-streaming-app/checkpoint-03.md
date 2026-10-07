# Checkpoint 03: Final End-to-End Verification (after Tasks 07–14)

Verifikasi menyeluruh terhadap kriteria sukses PRD (`docs/prd/android-streaming-app.md`):

- [ ] `./gradlew assembleRelease` atau `assembleDebug` menghasilkan APK fungsional.
- [ ] Pengguna dapat membuka katalog 24 provider secara instan tanpa proses login/registrasi.
- [ ] Direct-play Long Drama: membuka player di atas + metadata dan episode adaptif (kartu thumbnail vs grid angka) di bawah.
- [ ] Direct-play Shorts: membuka layar penuh 9:16 dengan snap-swipe vertikal tanpa lag + modal bottom sheet info detail.
- [ ] Direct-play Live TV: membuka player siaran di atas + daftar filter kategori dan channel di bawah.
- [ ] Episode 1–3 berputar gratis; Episode 4+ terkunci dialog lisensi.
- [ ] Memasukkan lisensi yang valid membuka seluruh episode.
- [ ] Riwayat durasi tontonan per episode tersimpan di Room DB dan resume di detik yang tepat.
- [ ] Download offline berjalan di background dan video dapat diputar saat Airplane Mode aktif.
- [ ] Trafik streaming mengalir langsung dari CDN provider ke HP (0 bytes konsumsi relay bandwidth pada server gateway MiniPC).
- [ ] Tidak ada memory leak pada pemutar Media3 saat berpindah-pindah layar (`adb logcat` bersih dari fatal crash).
