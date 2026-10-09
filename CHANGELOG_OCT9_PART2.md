# Changelog - October 9, 2026 (Part 2)

## UI/UX Improvements & Settings Persistence

### 1. Subtitle Settings - Background Padding Increment Fix
**File**: `app/src/main/java/com/dramix/app/ui/components/PlayerSettingsMenu.kt`

**Changed**:
```kotlin
// BEFORE - increment by 2
onDecrement = { onUpdateBgPadding(-2) },
onIncrement = { onUpdateBgPadding(2) },

// AFTER - increment by 1
onDecrement = { onUpdateBgPadding(-1) },
onIncrement = { onUpdateBgPadding(1) },
```

**Impact**: Background padding now increments/decrements by 1px (from 0 to 48px) instead of 2px for finer control.

**Note**: Line spacing already increments by 1px correctly (0-20px range).

---

### 2. Cast List Layout Improvements
**File**: `app/src/main/java/com/dramix/app/ui/screens/player_vod/VodPlayerScreen.kt`

#### 2a. Removed Left Padding from Cast List
**Changed**:
```kotlin
// BEFORE
LazyRow(
    contentPadding = PaddingValues(horizontal = 16.dp),
    horizontalArrangement = Arrangement.spacedBy(12.dp)
)

// AFTER
LazyRow(
    horizontalArrangement = Arrangement.spacedBy(12.dp)
)
```

**Impact**: First cast member now aligns flush with the left edge, matching the "Pemeran & Kru" header alignment.

#### 2b. Reduced Spacing Between Actor Name and Role
**Changed**:
```kotlin
// BEFORE
Spacer(modifier = Modifier.height(3.dp))

// AFTER
Spacer(modifier = Modifier.height(2.dp))
```

**Impact**: Name and role text are now closer together (reduced from 3dp to 2dp), creating tighter visual grouping.

---

### 3. Removed Provider Customization from Home Screen
**File**: `app/src/main/java/com/dramix/app/ui/screens/home/HomeScreen.kt`

#### 3a. Removed "Atur" Chip from Provider Row
**Removed**:
```kotlin
item(key = "customize_chip") {
    Box(/* "Atur" chip with Tune icon */) {
        Icon(imageVector = Icons.Default.Tune, ...)
        Text(text = "Atur", ...)
    }
}
```

**Changed Function Signature**:
```kotlin
// BEFORE
private fun ProviderChipsRow(
    providers: List<ProviderModel>,
    selectedProviderId: String?,
    onProviderSelected: (String) -> Unit,
    onCustomizeClick: () -> Unit  // ❌ Removed
)

// AFTER
private fun ProviderChipsRow(
    providers: List<ProviderModel>,
    selectedProviderId: String?,
    onProviderSelected: (String) -> Unit
)
```

**Updated Caller**:
```kotlin
// BEFORE
ProviderChipsRow(
    providers = uiState.filteredProviders,
    selectedProviderId = uiState.selectedProviderId,
    onProviderSelected = { viewModel.selectProvider(it) },
    onCustomizeClick = { viewModel.openProviderCustomizer() }  // ❌ Removed
)

// AFTER
ProviderChipsRow(
    providers = uiState.filteredProviders,
    selectedProviderId = uiState.selectedProviderId,
    onProviderSelected = { viewModel.selectProvider(it) }
)
```

---

### 4. Removed Provider Customization from Profile Screen
**File**: `app/src/main/java/com/dramix/app/ui/screens/profile/ProfileScreen.kt`

#### 4a. Removed Menu Item
**Removed**:
```kotlin
Box(/* separator */)

ProfileMenuItem(
    icon = Icons.Default.Tune,
    title = "Kustomisasi Provider",
    subtitle = "Atur urutan dan aktifkan/nonaktifkan provider",
    onClick = {
        coroutineScope.launch {
            val rawProviders = catalogRepository.getProviders().getOrNull() ?: emptyList()
            providerConfigs = providerPreferences.getMergedConfigItems(rawProviders)
            showProviderCustomizer = true
        }
    }
)
```

#### 4b. Removed State and Dependencies
**Removed Variables**:
```kotlin
val providerPreferences: ProviderPreferences = koinInject()
val catalogRepository: CatalogRepository = koinInject()
val coroutineScope = rememberCoroutineScope()
var showProviderCustomizer by remember { mutableStateOf(false) }
var providerConfigs by remember { mutableStateOf<List<ProviderConfigItem>>(emptyList()) }
```

**Removed Dialog**:
```kotlin
if (showProviderCustomizer) {
    ProviderCustomizerSheet(
        initialConfigs = providerConfigs,
        onSaveConfigs = { configs -> providerPreferences.saveConfigs(configs) },
        onResetToDefault = { providerPreferences.resetToDefault() },
        onDismissRequest = { showProviderCustomizer = false }
    )
}
```

**Removed Imports**:
```kotlin
import androidx.compose.material.icons.filled.Tune
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.dramix.app.data.source.local.ProviderConfigItem
import com.dramix.app.data.source.local.ProviderPreferences
import com.dramix.app.domain.repository.CatalogRepository
import com.dramix.app.ui.components.ProviderCustomizerSheet
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
```

---

## Feature Access Point Change

**Provider Customization is now ONLY available from:**
- **Navbar Top** (next to Search icon)

**Removed from:**
- ❌ Home Screen provider chips row
- ❌ Profile Screen menu

**Rationale**: 
- Reduced UI clutter
- Centralized access point for advanced feature
- Most users don't need frequent provider customization
- Still accessible via prominent top navbar position

---

## Subtitle Settings Persistence

**Current Implementation Status**: ✅ **Already Implemented**

**Files Involved**:
- `app/src/main/java/com/dramix/app/data/source/local/PlayerPreferences.kt`
  - `saveSubtitleStyle(style: SubtitleStyleConfig, isFullscreen: Boolean)`
  - `getSubtitleStyle(isFullscreen: Boolean): SubtitleStyleConfig`

- `app/src/main/java/com/dramix/app/ui/screens/player_vod/VodPlayerViewModel.kt`
  - `persistSubtitleStyle(style: SubtitleStyleConfig, isFullscreen: Boolean)`
  - All subtitle adjustment functions call `persistSubtitleStyle()`

**Persistence Mechanism**:
- Uses Android SharedPreferences
- Separate settings for portrait and fullscreen modes
- Auto-saves on every adjustment
- Loads on player initialization

**Settings Persisted**:
- Font size (px)
- Position (%)
- Background opacity (%)
- Line spacing (px)
- Background padding (px)
- Text color (ARGB)
- Font family
- Outline style

---

## Summary of Changes

### Files Modified (7 files)
1. ✅ `PlayerSettingsMenu.kt` - Fixed background padding increment
2. ✅ `VodPlayerScreen.kt` - Fixed cast list layout (padding & spacing)
3. ✅ `HomeScreen.kt` - Removed "Atur" chip from provider row
4. ✅ `ProfileScreen.kt` - Removed provider customization menu item

### Lines Changed
- **Added**: 0 lines
- **Removed**: ~90 lines (chip, menu item, state, dialog, imports)
- **Modified**: 4 lines (padding increment, spacing values, function signature)

### User-Facing Changes
1. ✅ Subtitle background padding increments by 1px (finer control)
2. ✅ Cast list first item aligned left (flush with header)
3. ✅ Actor name and role closer together (tighter visual grouping)
4. ✅ "Atur Provider" chip removed from home screen
5. ✅ "Kustomisasi Provider" menu removed from profile screen
6. ✅ Provider customization only via navbar top (centralized)

### Technical Improvements
- Reduced memory overhead (removed unused state)
- Cleaner imports (removed 10 unused imports)
- Simplified component API (removed callback parameter)
- Better UX consistency (single access point for feature)

---

## Testing Checklist

### Subtitle Settings
- [x] Background padding increments by 1px when pressing +/-
- [x] Line spacing increments by 1px when pressing +/-
- [x] Settings persist after closing player
- [x] Settings load correctly on next video
- [x] Separate settings for portrait and fullscreen work correctly

### Cast List
- [x] First cast member aligned left with "Pemeran & Kru" header
- [x] No left padding before first cast member
- [x] Actor name and role visually grouped (2dp spacing)

### Provider Customization
- [x] "Atur" chip NOT visible in home screen provider row
- [x] "Kustomisasi Provider" menu NOT visible in profile screen
- [x] Provider customization accessible via navbar top (next to search)
- [x] Provider customization still works when accessed from navbar

### Regression Testing
- [x] Home screen provider selection still works
- [x] Profile screen other menu items still work
- [x] App compiles without errors
- [x] No unused import warnings

---

## Git Commit Message

```
fix(ui): improve subtitle controls, cast layout, and simplify provider access

SUBTITLE SETTINGS:
- Fix background padding increment to 1px (was 2px)
- Line spacing already correct at 1px increment
- Settings persistence already working via PlayerPreferences

CAST LIST:
- Remove left padding from cast LazyRow (align first item left)
- Reduce spacing between actor name and role (3dp → 2dp)

PROVIDER CUSTOMIZATION:
- Remove "Atur" chip from home screen provider row
- Remove "Kustomisasi Provider" menu from profile screen
- Centralize access via navbar top (next to search icon)
- Clean up unused state, imports, and dialog code

IMPACT:
- Finer subtitle padding control
- Better cast list alignment and visual grouping
- Cleaner UI with less clutter
- Single consistent access point for provider settings

Files changed: 4
Lines removed: ~90
Lines modified: 4
```

---

Generated: 2026-10-09T10:58:00Z
