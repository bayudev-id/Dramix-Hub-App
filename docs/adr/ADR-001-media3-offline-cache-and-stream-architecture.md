# ADR-001: Media3 Offline Cache and Stream Playback Architecture

## Status
Accepted

## Date
2026-10-08

## Context
Dramix streaming application requires low-latency playback of HLS (`m3u8`) and progressive streams from 24 third-party content providers via the Dramix Gateway. Key constraints:
- No media relay on the gateway server; Android client connects directly to CDN origins.
- Offline download support for long drama, short drama, and anime episodes.
- Custom headers (`User-Agent`, `Referer`) required by specific CDN providers.
- Avoid Media3 HLS Type-2 playlist cache corruption.

## Decision
Use AndroidX Media3 (ExoPlayer 1.3.1) with:
1. `HeaderInjectingDataSourceFactory`: Wraps `OkHttpDataSource.Factory` injecting dynamic provider headers (`User-Agent: Dramix/1.0.0`, CDN-specific `Referer`).
2. Singleton `SimpleCache` with `NoOpCacheEvictor` and `StandaloneDatabaseProvider` for deterministic offline storage management.
3. Media3 `DownloadManager` paired with `DownloadService` (`DramixDownloadService`).
4. Room `DownloadDao` and `DownloadTracker` synchronizer tracking lifecycle states (`QUEUED`, `DOWNLOADING`, `COMPLETED`, `PAUSED`, `FAILED`).
5. Direct local playback in `VodPlayerViewModel` and `ShortsPlayerViewModel` checking Room `download_record` for completed downloads prior to CDN resolution.
6. Omit `setCustomCacheKey` in `DownloadRequest.Builder` to prevent Media3 HLS sub-playlist `IllegalArgumentException`.

## Consequences
- Offline video playback works in Airplane Mode with zero network calls.
- Downloads survive app process termination via foreground Android Service.
- SimpleCache directory is isolated in `context.filesDir/media3_cache`.
- Downloader requires `FOREGROUND_SERVICE_DATA_SYNC` permission on Android 14+ (SDK 34).
