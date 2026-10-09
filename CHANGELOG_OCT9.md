# Changelog - October 9, 2026

## Bug Fixes & Feature Restoration

### Issue: Infinite Scroll & UI Rollback After Git Reset
After git reset to previous commit, several October 8-9 changes were lost and bugs reappeared.

---

## Backend Changes

### `Dramix_Gateway/pocketbase/pb_hooks/videos.pb.js`

**Fixed hasMore Logic for MovieBox**
```javascript
// BEFORE (incorrect fallback)
if (res.json && res.json.pager && typeof res.json.pager.has_more === "boolean") {
    hasMore = res.json.pager.has_more;
} else {
    hasMore = ((res.json && res.json.data) || []).length >= 18; // ❌ Wrong fallback
}

// AFTER (correct)
if (res.json && res.json.pager && typeof res.json.pager.has_more === "boolean") {
    hasMore = res.json.pager.has_more;
} else {
    hasMore = false; // ✅ Only use explicit flag from backend
}
```

**Root Cause**: Fallback logic `length >= 18` caused non-paginated categories to incorrectly show hasMore=true, triggering infinite scroll on all MovieBox categories instead of only "Rekomendasi".

---

## Android App Changes

### 1. Domain & Data Layer

**`domain/model/CatalogModels.kt`**
- Added `VideoFeedPage` data class:
```kotlin
data class VideoFeedPage(
    val items: List<VideoItem> = emptyList(),
    val hasMore: Boolean = false
)
```

**`data/source/remote/dto/CatalogDtos.kt`**
- Added `hasMore` field to `VideoFeedDataDto`:
```kotlin
@Json(name = "has_more") val hasMore: Boolean? = false
```

**`domain/repository/CatalogRepository.kt`**
- Added `getVideoFeed()` method with default implementation:
```kotlin
suspend fun getVideoFeed(modelId: String, categoryId: String, page: Int = 1): Result<VideoFeedPage> =
    getVideos(modelId, categoryId, page).map { VideoFeedPage(items = it, hasMore = it.size >= 10) }
```

**`data/repository/CatalogRepositoryImpl.kt`**
- Implemented `getVideoFeed()` using backend's hasMore flag:
```kotlin
override suspend fun getVideoFeed(
    modelId: String,
    categoryId: String,
    page: Int
): Result<VideoFeedPage> = runCatching {
    val response = apiService.getVideos(modelId, categoryId, page)
    val data = response.data
    val items = (data?.items ?: emptyList()).map { it.toDomain() }
    val hasMore = data?.hasMore ?: false
    VideoFeedPage(items = items, hasMore = hasMore)
}
```

---

### 2. HomeViewModel Changes

**`ui/screens/home/HomeViewModel.kt`**

**Updated `loadCategoryVideos()`** to use `getVideoFeed()`:
```kotlin
private suspend fun loadCategoryVideos(providerId: String, categoryId: String) {
    _uiState.value = _uiState.value.copy(isLoadingContent = true, errorMessage = null)
    val feedResult = catalogRepository.getVideoFeed(providerId, categoryId, 1)

    if (feedResult.isSuccess) {
        val feed = feedResult.getOrDefault(VideoFeedPage())
        val items = feed.items
        // ... process items ...
        
        _uiState.value = _uiState.value.copy(
            categoryVideos = items,
            currentPage = 1,
            hasMoreContent = feed.hasMore, // ✅ Use backend flag
            // ...
        )
    }
}
```

**Updated `loadMoreVideos()`** to use `getVideoFeed()`:
```kotlin
fun loadMoreVideos() {
    // ...
    val result = catalogRepository.getVideoFeed(providerId, categoryId, nextPage)
    if (result.isSuccess) {
        val feed = result.getOrDefault(VideoFeedPage())
        _uiState.value = _uiState.value.copy(
            categoryVideos = currentState.categoryVideos + feed.items,
            currentPage = nextPage,
            hasMoreContent = feed.hasMore, // ✅ Use backend flag
            isLoadingMore = false
        )
    }
}
```

---

### 3. HomeScreen UI Changes

**`ui/screens/home/HomeScreen.kt`**

**Removed Hero/Spotlight Section**:
- Deleted `SpotlightBanner` composable function
- Removed spotlight rendering from LazyColumn
- Layout now goes directly from category chips to video grid

**Updated Shimmer Loading**:
```kotlin
@Composable
private fun HomeShimmerLoading() {
    Column(modifier = Modifier.fillMaxSize().padding(vertical = 4.dp)) {
        // Content Type chips shimmer
        // Provider chips shimmer
        // Category chips shimmer
        // Video grid shimmer (removed hero placeholder)
    }
}
```

**Before**: Content Type → Provider → Category → **Hero Banner** → Video Grid  
**After**: Content Type → Provider → Category → Video Grid

---

### 4. Bottom Navigation Changes

**`ui/components/BottomNavigationBar.kt`**

**Updated Navigation Items**:
```kotlin
val BottomNavItems = listOf(
    BottomNavItem(
        title = "Beranda",
        icon = Icons.Default.Home,
        route = Screen.Home.route
    ),
    BottomNavItem(
        title = "Unduhan",
        icon = Icons.Default.Download,
        route = Screen.DownloadManager.route
    ),
    BottomNavItem(
        title = "Saya",
        icon = Icons.Default.Person,
        route = Screen.Profile.route,
        isProfile = true
    )
)
```

**Before**: Beranda, Drama Pendek, Live TV, Saya (4 items)  
**After**: Beranda, Unduhan, Saya (3 items)

**`ui/navigation/AppNavigation.kt`**

**Updated Main Tabs**:
```kotlin
val mainTabs = listOf(
    Screen.Home.route,
    Screen.DownloadManager.route,
    Screen.Profile.route
)
val shouldShowBottomBar = currentRoute in mainTabs
```

---

### 5. ProfileScreen Changes

**`ui/screens/profile/ProfileScreen.kt`**

**Added VipActiveCard Composable**:
```kotlin
@Composable
private fun VipActiveCard(
    planName: String,
    expiresAtFormatted: String?
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(Color(0xFF1F2937), Color(0xFF374151))
                )
            )
            .border(1.5.dp, Color(0xFFF59E0B).copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Column {
            // VIP icon + plan name
            // "Akses Premium Penuh" text
            // Expiry date badge
        }
    }
}
```

**Conditional License Form Rendering**:
```kotlin
// License Card: Show Active Status or Activation Form
if (uiState.isVip) {
    item {
        VipActiveCard(
            planName = uiState.planName ?: "VIP Member",
            expiresAtFormatted = uiState.expiresAtFormatted
        )
    }
} else {
    item {
        LicenseActivationCard(
            licenseKey = uiState.licenseKeyInput,
            // ...
        )
    }
}
```

**Before**: License activation form always visible  
**After**: VIP members see active status card, non-VIP see activation form

---

### 6. Rental Badge Implementation (Previous Work - Verified)

**Components Updated**:
- `ui/screens/home/HomeScreen.kt` - Sewa badge on catalog cards (top-left)
- `ui/components/SearchResultGrid.kt` - Sewa badge on search results
- `ui/components/AdaptiveEpisodeList.kt` - Sewa badge on episode thumbnails
- `ui/screens/player_shorts/ShortsPlayerScreen.kt` - Sewa indicator in player overlay
- `ui/screens/player_shorts/ShortsPlayerViewModel.kt` - Rental episode blocking logic
- `ui/screens/player_vod/VodPlayerViewModel.kt` - Rental episode blocking logic
- `ui/components/RentalEpisodeGateDialog.kt` - NEW: Formal rental access dialog

**Badge Behavior**:
- Home screen: Show Sewa badge OR VIP badge (not both)
- Detail screen: Show both if applicable
- Episode lists: Show Sewa badge on rental episodes

**Rental Blocking Logic**:
```kotlin
// In ShortsPlayerViewModel & VodPlayerViewModel
if (stream == null) {
    if (targetEpisode.isSewa) {
        playerController.pause()
        _uiState.value = _uiState.value.copy(
            rentalBlockedEpisode = targetEpisode,
            errorMessage = null
        )
    } else {
        _uiState.value = _uiState.value.copy(
            errorMessage = "Tidak ada stream video yang tersedia untuk episode ini"
        )
    }
}
```

---

## Test Updates

**`test/.../HomeViewModelTest.kt`**

**Added VideoFeedPage Support**:
```kotlin
override suspend fun getVideoFeed(modelId: String, categoryId: String, page: Int): Result<VideoFeedPage> = 
    Result.success(
        VideoFeedPage(
            items = listOf(
                VideoItem(id = "v-1", title = "Spotlight Drama", score = "9.5"),
                VideoItem(id = "v-2", title = "Popular Drama 2", score = "8.8")
            ),
            hasMore = false
        )
    )
```

**Updated Test Assertions**:
```kotlin
@Test
fun homeViewModel_initializes_and_loads_default_feed() = runTest {
    // ...
    assertEquals(2, state.categoryVideos.size)
    assertFalse(state.hasMoreContent) // ✅ Check hasMore flag
}
```

---

## Summary of Changes

### Fixed Issues
1. ✅ Infinite scroll only triggers when backend sets `hasMore=true`
2. ✅ MovieBox categories without pagination no longer show false "load more"
3. ✅ Category switching resets pagination correctly
4. ✅ Hero section removed to simplify UI
5. ✅ Bottom navbar updated to 3 items (Beranda, Unduhan, Saya)
6. ✅ VIP members see active status card instead of activation form
7. ✅ Rental episode blocking with formal dialog

### Verification Steps
1. Test MovieBox "Rekomendasi" category - should paginate
2. Test MovieBox other categories - should NOT show infinite scroll
3. Switch between categories - list should reset properly
4. Check bottom navbar shows 3 items
5. Login as VIP - should see VipActiveCard
6. Try playing WeTV rental episode without purchase - should show dialog

---

## Files Modified

### Backend
- `Dramix_Gateway/pocketbase/pb_hooks/videos.pb.js`

### Android Domain
- `domain/model/CatalogModels.kt`
- `domain/repository/CatalogRepository.kt`

### Android Data
- `data/source/remote/dto/CatalogDtos.kt`
- `data/repository/CatalogRepositoryImpl.kt`

### Android UI
- `ui/screens/home/HomeViewModel.kt`
- `ui/screens/home/HomeScreen.kt`
- `ui/components/BottomNavigationBar.kt`
- `ui/navigation/AppNavigation.kt`
- `ui/screens/profile/ProfileScreen.kt`
- `ui/components/RentalEpisodeGateDialog.kt`

### Android Tests
- `test/.../HomeViewModelTest.kt`

---

## Technical Notes

**hasMore Flag Priority**:
1. Backend explicit flag (`has_more` in JSON)
2. Default to `false` if not present
3. Never use fallback heuristics like `length >= 18`

**State Management**:
- `hasMoreContent` in `HomeUiState` now reflects backend truth
- No client-side guessing of pagination availability
- Category switching resets page to 1 and clears items

**UI/UX Improvements**:
- Cleaner home screen without hero section
- Faster load time (one less section to render)
- Better navigation focus with 3-item navbar
- VIP status more prominent with dedicated card

---

Generated: 2026-10-09T10:40:09Z
