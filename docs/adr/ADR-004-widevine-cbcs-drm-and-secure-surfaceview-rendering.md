# ADR-004: Widevine CBCS DRM Decryption and Secure SurfaceView Rendering

## Status
Accepted

## Date
2026-10-11

## Context
Dramix Android stream player relies on AndroidX Media3 (ExoPlayer) to play videos across 24 third-party content providers. When integrating Youku video streams:
1. Stream media chunks are encrypted using Common Encryption AES-CBCS (`sample-aes` / `cbcs`) protected by Google Widevine Modular DRM (UUID: `edef8ba9-79d6-4ace-a3c8-27dcd51d21ed`).
2. The Youku DRM license server (`https://drm-license.youku.tv/ups/drm.json`) uses a non-standard protocol: `application/x-www-form-urlencoded` POST containing session tokens and a Base64-encoded `licenseRequest`, returning JSON (`{"data": "<base64_key>", "states": 0, "msg": "wvpl license gen succ"}`).
3. Sending license POST requests to URLs with residual query strings triggers server-side parameter conflicts (`states: 202`, `drm type error`).
4. Video and audio tracks are segmented separately by Youku's CDN. The default stream points to a video-only sub-playlist; full playback requires selecting the `master_url`.
5. Media3 player view was configured with `app:surface_type="texture_view"`. Widevine L1 decoders (`c2.mtk.avc.decoder.secure`) route decrypted frames directly into hardware TEE buffers. `TextureView` requires GPU OpenGL ES composition, which Android hardware security blocks (`GPUAUX: skip, cannot convert protect / secure buffer`), causing audio-only playback with black screen.
6. Android 9+ blocks cleartext HTTP by default, preventing media chunk downloads from Youku CDN endpoints (`*.cibntv.net`).

## Decision
1. **SurfaceView as Default Renderer**: Change `app:surface_type` in `item_player_view.xml` from `texture_view` to `surface_view`. SurfaceView uses direct hardware overlay plane composition through Android `BLASTBufferQueue`, granting direct access for secure hardware video decoders.
2. **Custom WidevineDrmCallback**: Implement `MediaDrmCallback` in `com.dramix.app.player.drm.WidevineDrmCallback` that:
   - Strips URL query strings prior to POST (`targetUrl.substringBefore("?")`).
   - Translates binary ExoPlayer challenge into `application/x-www-form-urlencoded` with `drmType=widevine` and Base64 `licenseRequest`.
   - Decodes Base64 JSON response payload into raw binary DRM key response.
   - Delegates `executeProvisionRequest` to AndroidX `HttpMediaDrmCallback` for Google device certificate provisioning.
3. **Multi-Session DRM Manager in Controller**: Initialize `DefaultDrmSessionManager` in `DramixPlayerController` with `multiSession = true` (handling separate audio and video encryption keys) and `playClearSamplesWithoutKeys = true`.
4. **MIME Type Inference**: Automatically map URLs matching `/m3u8` or `.m3u8` to `MimeTypes.APPLICATION_M3U8`.
5. **Gateway Master Playlist Selection**: Update Gateway hook `source.pb.js` to select `master_url` for Youku streams and normalize `drmType=widevine`.
6. **Cleartext CDN Configuration**: Add `cibntv.net`, `youku.com`, and `youku.tv` to `network_security_config.xml`.

## Consequences
- Widevine CBCS encrypted streams play smoothly with synchronized video and audio.
- Hardware secure video decoder operates without GPU texture composition rejection.
- Standard clear streams (HLS, MP4, DASH) continue operating without regression.
- SurfaceView cannot use Jetpack Compose `graphicsLayer` opacity or non-hardware-accelerated rotations; standard video zoom and fit-to-screen are handled via `PlayerView.resizeMode`.
- Cleartext HTTP traffic is strictly scoped to identified CDN domains in `network_security_config.xml`.
