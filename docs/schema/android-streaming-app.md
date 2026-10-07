# Schema: Android Local Database (Room SQLite)

**Source:** docs/prd/android-streaming-app.md  
**Database:** SQLite / Android Jetpack Room - Mendukung arsitektur *local-first*, operasi reaktif berbasis Coroutines `Flow`, query instan tanpa latensi jaringan, serta kompatibel penuh dengan background worker AndroidX Media3 Download Tracker.

## Overview

Skema database lokal ini mengelola persistensi data di sisi perangkat pengguna tanpa mewajibkan akun server. Model data mendukung tiga pilar utama: pencatatan riwayat tontonan per episode dengan agregasi drama terbaru (*watch history*), daftar drama favorit yang disimpan (*bookmarks*), dan pelacakan status unduhan offline (*download records*) yang tersinkronisasi dengan file segmen video fisik.

## Entities

### watch_history

Serves: PRD User Story 9 ("Sebagai pengguna yang sering kembali menonton, saya ingin riwayat tontonan tersimpan rapi...").

| Column | Type | Nullable | Default | Notes |
|---|---|---|---|---|
| id | integer | no | generated | Primary key (auto-increment) |
| drama_id | text | no | - | ID unik drama dari gateway provider |
| provider_id | text | no | - | ID provider (contoh: 'wetv', 'dramabox', 'freereels') |
| drama_title | text | no | - | Judul drama untuk display offline |
| drama_poster | text | yes | null | URL cover poster |
| episode_number | integer | no | - | Nomor urut episode yang ditonton |
| episode_title | text | yes | null | Judul episode (jika ada dari metadata) |
| position_ms | integer | no | 0 | Posisi timestamp terakhir pemutaran dalam milidetik |
| duration_ms | integer | no | 0 | Total durasi episode dalam milidetik |
| is_completed | integer | no | 0 | Flag (0/1) jika video ditonton >= 95% dari durasi |
| updated_at | integer | no | generated | Timestamp Unix epoch (milidetik) saat record diperbarui |

### bookmark

Serves: PRD User Story 9 ("...dan bookmark tersimpan rapi di perangkat saya secara lokal").

| Column | Type | Nullable | Default | Notes |
|---|---|---|---|---|
| id | integer | no | generated | Primary key (auto-increment) |
| drama_id | text | no | - | ID unik drama dari gateway |
| provider_id | text | no | - | ID provider asal konten |
| title | text | no | - | Judul drama |
| poster_url | text | yes | null | Cover poster drama |
| content_type | text | no | 'long_drama' | Tipe konten: 'long_drama', 'short_drama', 'movie', 'live_tv' |
| rating | text | yes | null | Rating nilai drama (misal: '9.4') |
| total_episodes | integer | yes | null | Jumlah total episode jika diketahui |
| created_at | integer | no | generated | Timestamp Unix epoch saat ditambahkan ke favorit |

### download_record

Serves: PRD User Story 8 ("Sebagai pengguna dengan kuota terbatas... ingin mengunduh episode drama...").

| Column | Type | Nullable | Default | Notes |
|---|---|---|---|---|
| id | integer | no | generated | Primary key (auto-increment) |
| media_id | text | no | - | String unik pengidentifikasi download (Media3 MediaItem ID) |
| drama_id | text | no | - | ID drama induk |
| provider_id | text | no | - | ID provider |
| drama_title | text | no | - | Judul drama induk |
| episode_number | integer | no | - | Nomor episode yang diunduh |
| episode_title | text | yes | null | Judul episode |
| stream_url | text | no | - | URL stream sumber saat proses antrean dibuat |
| local_uri | text | yes | null | File URI absolut penyimpanan internal privat |
| bytes_downloaded | integer | no | 0 | Jumlah byte yang telah terunduh |
| total_bytes | integer | no | 0 | Estimasi total ukuran file dalam byte |
| progress_percentage | integer | no | 0 | Nilai persentase progres download (0–100) |
| status | text | no | 'QUEUED' | Status siklus hidup: 'QUEUED', 'DOWNLOADING', 'COMPLETED', 'FAILED', 'PAUSED' |
| error_message | text | yes | null | Catatan error jika status 'FAILED' |
| created_at | integer | no | generated | Waktu antrean unduhan dibuat |
| completed_at | integer | yes | null | Waktu unduhan selesai 100% |

## Relationships

| From | To | Cardinality | On delete | On update | Notes |
|---|---|---|---|---|---|
| watch_history | bookmark | many-to-one (logical) | no action | no action | Relasi logis via (drama_id, provider_id); menghapus bookmark tidak menghapus riwayat tontonan |
| download_record | watch_history | one-to-one (logical) | no action | no action | Menonton file unduhan tetap memperbarui position_ms di watch_history |

*Catatan:* Karena aplikasi bersifat local-first tanpa tabel induk master drama yang statis di database HP (katalog bersumber dinamis dari API Gateway), relasi antar tabel dimodelkan secara relasi logis (*compound keys*) tanpa foreign key fisik hard-lock antar entitas untuk mencegah kegagalan cascade saat cache dibersihkan.

## Indexes

| Table | Columns | Type | Rationale |
|---|---|---|---|
| watch_history | (drama_id, provider_id, episode_number) | unique | Memastikan hanya 1 baris per episode drama; tonton ulang akan me-replace record |
| watch_history | (updated_at desc) | btree | Mempercepat query pakan "Lanjutkan Menonton" di Beranda |
| watch_history | (drama_id, provider_id, updated_at desc) | btree | Mempercepat pengambilan episode terakhir yang aktif untuk satu judul drama tertentu |
| bookmark | (drama_id, provider_id) | unique | Mencegah duplikasi drama yang sama di daftar bookmark |
| bookmark | (created_at desc) | btree | Mengurutkan tampilan daftar favorit dari yang paling baru disimpan |
| download_record | (media_id) | unique | Mencegah antrean download ganda untuk episode yang sama |
| download_record | (status, created_at desc) | btree | Query aktif antrean downloader Media3 WorkManager |
| download_record | (drama_id, provider_id) | btree | Menampilkan daftar episode yang sudah diunduh di halaman player |

## Constraints

| Table | Constraint | Type | Definition |
|---|---|---|---|
| watch_history | composite key unique | unique | `(drama_id, provider_id, episode_number)` |
| watch_history | position_ms valid | check | `position_ms >= 0` |
| watch_history | duration_ms valid | check | `duration_ms >= 0` |
| bookmark | composite key unique | unique | `(drama_id, provider_id)` |
| bookmark | content_type valid | check | `content_type in ('long_drama', 'short_drama', 'movie', 'live_tv')` |
| download_record | media_id unique | unique | `media_id` |
| download_record | status valid enum | check | `status in ('QUEUED', 'DOWNLOADING', 'COMPLETED', 'FAILED', 'PAUSED')` |
| download_record | progress range | check | `progress_percentage between 0 and 100` |

## Migration Notes

Semua tabel merupakan skema baru (*Database Version 1*). Tidak diperlukan skrip migrasi atau backfill data lama. Migrasi masa depan akan dikelola melalui mekanisme `Room Migration(from, to)` standar dengan pengujian otomatis `MigrationTest`.

## Open Questions

Tidak ada. Seluruh entitas, constraint gabungan, dan optimasi query indeks telah diselaraskan dengan kebutuhan PRD dan navigasi Direct-Play.
