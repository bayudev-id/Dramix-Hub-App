# Task 05: Gateway Retrofit Client & 6 Endpoint Mappings

**Source:** docs/prd/android-streaming-app.md — Skema Integrasi Dramix Gateway (:8090)

**What to build:** Membangun Retrofit API Service dan Data Transfer Objects (DTO) untuk mengonsumsi seluruh 6 endpoint standar Dramix Gateway (:8090) lintas 24 provider (`models`, `categories`, `videos`, `detail`, `source`, `search`). Menggunakan Kotlinx Serialization untuk parsing JSON deterministik dengan toleransi terhadap field dinamis.

## Acceptance criteria

- [ ] Definisi antarmuka `GatewayApiService` memetakan:
  1. `GET /api/modelles/models` ➔ `List<ProviderModelDto>`
  2. `GET /api/modelles/categories` ➔ `List<CategoryDto>`
  3. `GET /api/modelles/videos` ➔ `VideoFeedResponseDto` (mendukung parameter `model_id`, `category_id`, `page`)
  4. `GET /api/modelles/detail` ➔ `DramaDetailDto` (mendukung `model_id`, `book_id`/`drama_id`)
  5. `GET /api/modelles/source` ➔ `PlaybackSourceDto` (menghasilkan URL stream + map headers `Referer`/`User-Agent`)
  6. `POST & GET /api/modelles/search` ➔ `SearchResponseDto` (mendukung parameter `q`, `content_type`, `model_id`, `page`)
- [ ] DTO model memiliki serializer yang aman (`ignoreUnknownKeys = true`, `coerceInputValues = true`).
- [ ] Implementasi `CatalogRepository` memetakan DTO API ke entity model Domain yang bersih.
- [ ] Mock server unit test memverifikasi serialisasi/deserialisasi respons dari 24 provider yang berbeda (khususnya perbedaan format episode dan thumbnail).

## Verification

- [ ] Tests pass: `./gradlew testDebugUnitTest --tests "com.dramix.app.data.source.*"`
- [ ] Build succeeds: `./gradlew assembleDebug`
- [ ] Output matches: Unit test parsing JSON dari mock response `source` gateway berhasil mengekstrak URL m3u8 dan header `Referer`.
- [ ] No regressions: Tidak ada crash saat provider mengembalikan list kosong `items: []` (seperti upstream dramaboxbaru).
- [ ] Manual check: Uji coba request real ke gateway lokal `http://10.0.2.2:8090/api/modelles/models` berhasil mengembalikan 24 entri provider.

## Blocked by

Task 04

## Files likely touched

- `app/src/main/java/com/dramix/app/data/source/remote/GatewayApiService.kt`
- `app/src/main/java/com/dramix/app/data/source/remote/dto/CatalogDtos.kt`
- `app/src/main/java/com/dramix/app/data/source/remote/dto/PlaybackDtos.kt`
- `app/src/main/java/com/dramix/app/data/repository/CatalogRepositoryImpl.kt`
- `app/src/main/java/com/dramix/app/domain/repository/CatalogRepository.kt`
- `app/src/test/java/com/dramix/app/data/source/remote/GatewayApiServiceTest.kt`

## Estimated scope

M (4–6 files)

## Rollback

Kembalikan implementasi service ke mock repository interface.

## Notes

Gunakan Kotlinx Serialization bukan Gson/Moshi agar kompatibel penuh dengan Kotlin Multiplatform di masa depan jika diperlukan.
