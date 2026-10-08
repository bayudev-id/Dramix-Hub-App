# Dramix Android Streaming App

Native Android streaming application for Dramix (`com.dramix.app`) built with Kotlin, Jetpack Compose, and AndroidX Media3 (ExoPlayer).

Consumes 6 Dramix Gateway endpoints across 24 content providers without host stream relay.

---

## Features

- **Multi-Format Streaming**: Long drama, short drama (vertical reels), anime, and live TV channels.
- **Direct CDN Playback**: Media3 HLS and progressive playback direct to origin with custom header injection.
- **Offline Downloader**: Background download service with persistent Room state tracking and zero-network airplane mode playback.
- **Freemium Gating**: Episodes 1–3 free; Episode 4+ requires hardware-bound license validated against PocketBase.
- **Cinema Dark OLED Theme**: Pitch black `#0A0A0C` background, high contrast tokens, 48dp touch targets, and distraction-free immersive players.
- **Hardened Security**: Root/Frida runtime detection, anonymous hardware device binding (`X-Device-Id`, `X-Timestamp`), strict `network_security_config.xml`, and ProGuard/R8 rules.

---

## Tech Stack & Architecture

- **Language**: Kotlin 2.1.0
- **UI**: Jetpack Compose (Material 3, Navigation Compose)
- **Audio/Video**: AndroidX Media3 1.3.1 (ExoPlayer, HLS, OkHttp DataSource, Cache)
- **Local Database**: Room 2.6.1 (SQLite, Coroutines Flow, Migrations)
- **Network**: Retrofit 2.11.0, OkHttp 4.12.0, Moshi 1.15.1
- **Dependency Injection**: Koin 3.5.6
- **Asynchrony**: Kotlin Coroutines & StateFlow
- **Architecture**: Clean Architecture (Domain, Data, UI) + MVVM / MVI Flow

---

## Prerequisites

- **JDK**: Java 17 or 21 (`JAVA_HOME` configured)
- **Android SDK**: Platform 36 (`compileSdk = 36`, `targetSdk = 34`, `minSdk = 24`)
- **Gradle**: 9.3.1 (wrapper included)
- **Dramix Gateway & PocketBase**: Running locally on port `8090`

---

## CLI Build & Test Commands

### Run Unit Tests
```powershell
.\gradlew.bat testDebugUnitTest
```

### Build Debug APK
```powershell
.\gradlew.bat assembleDebug
```
*Output: `app/build/outputs/apk/debug/app-debug.apk`*

### Build Release APK
```powershell
.\gradlew.bat assembleRelease
```
*Output: `app/build/outputs/apk/release/app-release.apk`*

---

## Physical Device & Wireless Debugging

When testing on a physical device connected via USB or Wireless ADB, forward port 8090 from the device to the local host machine:

```powershell
# 1. Verify device connection
adb devices

# 2. Reverse port forwarding (device 127.0.0.1:8090 -> host 127.0.0.1:8090)
adb reverse tcp:8090 tcp:8090

# 3. Install debug or release APK
adb install -r app/build/outputs/apk/release/app-release.apk

# 4. Launch app
adb shell am start -n com.dramix.app/com.dramix.app.MainActivity

# 5. Monitor logs
adb logcat -v time | findstr -i "dramix okhttp"
```

---

## Architecture Decision Records (ADRs)

- [ADR-001: Media3 Offline Cache and Stream Playback Architecture](docs/adr/ADR-001-media3-offline-cache-and-stream-architecture.md)
- [ADR-002: Gateway Anonymous Device Binding and Licensing](docs/adr/ADR-002-gateway-anonymous-device-binding-and-license.md)
- [ADR-003: Cinema Dark Design Tokens and Immersive Navigation](docs/adr/ADR-003-cinema-dark-and-navigation-patterns.md)
