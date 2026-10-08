# ADR-003: Cinema Dark Design Tokens and Immersive Player Navigation

## Status
Accepted

## Date
2026-10-08

## Context
Video streaming applications require distraction-free playback experiences with optimal contrast on OLED displays and compliant accessibility touch targets.

## Decision
1. Color System:
   - Primary OLED pitch black: `#0A0A0C` (background) and `#121216` (surface).
   - Accents: Electric Amber `#FFB800` (VIP/Actions), Neon Cyan `#00E5FF` (Live/Active indicators), Coral Rose `#FF3366` (Shorts/Likes).
   - Surface cards: Elevated `#1A1A22` with 1dp border `#2A2A38`.
2. Ergonomics and Touch Targets:
   - Minimum 48dp touch targets on all interactive elements.
   - 12dp rounded corners on thumbnails and cards.
3. Immersive Player Experience:
   - Dedicated player screens (`player_vod`, `player_shorts`, `player_tv`) hide `TopAppBar` and `BottomNavigationBar`.
   - Vertical paging for Shorts with gesture-based swipe and double-tap heart interactions.
   - 9:16 aspect ratio fitting with subtitle overlay.
4. Navigation Structure:
   - Jetpack Navigation Compose with sealed route hierarchies (`Home`, `Shorts`, `LiveTv`, `Profile`, `Search`, `PlayerVod`, `PlayerTv`).

## Consequences
- OLED battery savings during extended playback sessions.
- Full screen utilization during video viewing without navigation clutter.
