# ADR-002: Gateway Anonymous Device Binding and Freemium Licensing

## Status
Accepted

## Date
2026-10-08

## Context
Dramix requires anonymous user identification and freemium entitlement enforcement:
- No user login or registration form.
- Episodes 1–3 free for all users across drama providers.
- Episode 4+ gated behind server-authoritative license key validation via PocketBase (`:8090`).
- Prevent MITM interception, rooting bypasses, and unauthorized proxy relay.

## Decision
1. Anonymous Hardware Binding:
   - Compute SHA-256 hash of `Settings.Secure.ANDROID_ID`.
   - Fallback to cryptographically random UUID stored in EncryptedSharedPreferences if `ANDROID_ID` unavailable.
   - Inject `X-Device-Id` and `X-Timestamp` on all network calls via `DeviceIdentifierInterceptor`.
2. Dynamic Environment Host Resolution:
   - Emulator connects to `http://10.0.2.2:8090/`.
   - Physical device connects to `http://127.0.0.1:8090/` forwarded over ADB reverse tunnel (`adb reverse tcp:8090 tcp:8090`).
3. License Validation Flow:
   - Client calls `POST /api/collections/licenses/records` with `license_key` and `device_id`.
   - PocketBase binds license to device; client persists token in `LicensePreferences`.
   - `EntitlementManager` evaluates access: allows episodes 1–3 unconditionally; verifies valid license for episode >= 4.
4. Security Hardening:
   - Cleartext traffic restricted strictly to localhost/emulator in `network_security_config.xml`.
   - Disabled user CA certificates in production builds.
   - Cold-start root and Frida detection via `SecurityManager` displaying Cinema Dark alert dialog.

## Consequences
- Single license key cannot be shared across multiple devices simultaneously.
- Gating works offline for previously downloaded episodes if entitlement was cached.
- Tampered devices with Frida or root binaries are warned on startup without crash loops.
