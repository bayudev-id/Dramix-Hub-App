# Task 02: Cinema Dark Design Tokens & Theme Setup

**Source:** docs/prd/android-streaming-app.md — Technical Decisions & DESIGN.md

**What to build:** Membangun sistem desain visual Jetpack Compose Material 3 yang mengimplementasikan spesifikasi `DESIGN.md`. Menyediakan tema Cinema Dark OLED (`#000000`), aksen Crimson Play (`#E11D48`), badge Gold (`#F59E0B`), tipografi modern berbasis type scale terukur, radius sudut baku, dan modifier pembantu touch target 48dp.

## Acceptance criteria

- [ ] File `Color.kt` mendefinisikan seluruh primitive dan semantic colors (Pure OLED Black, Midnight Surface, Crimson Play, Gold VIP, Text High/Medium Emphasis).
- [ ] File `Theme.kt` mengekspor fungsi composable `DramixTheme` yang menerapkan `DarkColorScheme` secara konsisten pada Material 3.
- [ ] File `Type.kt` mengonfigurasi `Typography` Material 3 (Display, Headline, Title, Body, Label) dengan ukuran font dan line-height sesuai `DESIGN.md`.
- [ ] File `Shape.kt` mengonfigurasi corner radius kartu (8dp), tombol episode (6dp), dan bottom sheet (16dp).
- [ ] Menyediakan ekstensi `Modifier.touchTargetMin()` untuk menjamin area sentuh minimum 48dp x 48dp.

## Verification

- [ ] Tests pass: `./gradlew testDebugUnitTest --tests "com.dramix.app.ui.theme.*"`
- [ ] Build succeeds: `./gradlew assembleDebug`
- [ ] Output matches: Pratinjau atau dummy screen Composable berhasil dirender dengan latar belakang `#000000` dan tombol aksen `#E11D48`.
- [ ] No regressions: Tidak ada warna default Material 3 (ungu M3 standar) yang bocor ke komponen.
- [ ] Manual check: Status bar dan navigation bar perangkat Android terwarnai hitam menyatu dengan tema (*edge-to-edge*).

## Blocked by

Task 01

## Files likely touched

- `app/src/main/java/com/dramix/app/ui/theme/Color.kt`
- `app/src/main/java/com/dramix/app/ui/theme/Type.kt`
- `app/src/main/java/com/dramix/app/ui/theme/Shape.kt`
- `app/src/main/java/com/dramix/app/ui/theme/Theme.kt`
- `app/src/main/java/com/dramix/app/ui/components/TouchExtensions.kt`

## Estimated scope

S (1–2 core theme files + helpers)

## Rollback

Revert commit file `app/src/main/java/com/dramix/app/ui/theme/` ke tema template default.

## Notes

Strict anti-AI-slop: Hindari penambahan gradien pelangi atau efek glow yang tidak terdaftar di `DESIGN.md`.
