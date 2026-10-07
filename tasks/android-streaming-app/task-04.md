# Task 04: Core Networking & Security Hardening

**Source:** docs/prd/android-streaming-app.md — Security & Anti-Tamper Strategy

**What to build:** Membangun lapisan jaringan dasar (`OkHttpClient`) yang diperkeras untuk mencegah penyadapan proxy (Burp Suite, mitmproxy) dan manipulasi runtime (Frida, Root). Menyediakan Network Security Config yang mematikan User CA, SSL Certificate Pinning, utilitas deteksi integritas APK (Signature Check), serta generator Device ID unik terenkripsi untuk pengikatan lisensi hardware.

## Acceptance criteria

- [ ] File `res/xml/network_security_config.xml` mematikan `cleartextTrafficPermitted` dan hanya mempercayai System CA (menolak User CA Burp/mitmproxy).
- [ ] `OkHttpClient` mengonfigurasi CertificatePinner dengan pin SHA-256 untuk domain produksi Dramix Gateway.
- [ ] Modul `SecurityManager` menyediakan fungsi `isDeviceCompromised()` yang memeriksa keberadaan binary `su`, build test-keys, dan port aktif runtime Frida (`27042`).
- [ ] Modul `DeviceIdentifier` menghasilkan hardware fingerprint stabil berbasis `Settings.Secure.ANDROID_ID` + SHA-256 yang di-cache di EncryptedSharedPreferences.
- [ ] Request interceptor menyematkan header `X-Device-Id` dan timestamp untuk penanda validasi request.

## Verification

- [ ] Tests pass: `./gradlew testDebugUnitTest --tests "com.dramix.app.core.security.*"`
- [ ] Build succeeds: `./gradlew assembleDebug`
- [ ] Output matches: Request HTTP ke domain selain gateway atau via proxy tidak resmi menghasilkan `SSLPeerUnverifiedException`.
- [ ] No regressions: Koneksi debug lokal (localhost / IP LAN dev) tetap dapat diuji melalui flavor atau build flag khusus.
- [ ] Manual check: Logcat mencetak konfirmasi inisialisasi SecurityManager tanpa crash pada perangkat Android resmi.

## Blocked by

Task 01

## Files likely touched

- `app/src/main/res/xml/network_security_config.xml`
- `app/src/main/java/com/dramix/app/core/network/OkHttpProvider.kt`
- `app/src/main/java/com/dramix/app/core/security/SecurityManager.kt`
- `app/src/main/java/com/dramix/app/core/security/DeviceIdentifier.kt`
- `app/src/main/java/com/dramix/app/core/network/SecurityHeadersInterceptor.kt`

## Estimated scope

M (3–5 files)

## Rollback

Nonaktifkan CertificatePinner pada OkHttpClient dan kembalikan Network Security Config ke mode default.

## Notes

Sediakan toggle `DEBUG_MODE` di BuildConfig agar developer dapat melakukan debugging API lokal tanpa terblokir SSL pinning.
