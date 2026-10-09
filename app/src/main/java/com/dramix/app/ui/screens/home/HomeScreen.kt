package com.dramix.app.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import coil.request.ImageRequest
import coil.size.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.dramix.app.core.database.entity.WatchHistoryEntity
import com.dramix.app.domain.model.ProviderModel
import com.dramix.app.domain.model.VideoItem
import com.dramix.app.ui.components.CategoryChipsRow
import com.dramix.app.ui.components.CategorySelectorSheet
import com.dramix.app.ui.components.ContentTypeChipsRow
import com.dramix.app.ui.components.ProviderCustomizerSheet
import com.dramix.app.ui.components.SewaBadge
import com.dramix.app.ui.components.ShimmerPlaceholder
import com.dramix.app.ui.components.VipBadge
import com.dramix.app.ui.components.animateToCentered
import com.dramix.app.ui.theme.CrimsonPlay
import com.dramix.app.ui.theme.MidnightBorder
import com.dramix.app.ui.theme.MidnightCard
import com.dramix.app.ui.theme.PureBlack
import com.dramix.app.ui.theme.Slate400
import com.dramix.app.ui.theme.Slate50
import com.dramix.app.ui.theme.TagBadgeShape

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToVodPlayer: (providerId: String, dramaId: String) -> Unit,
    onNavigateToShorts: (providerId: String, dramaId: String) -> Unit,
    onNavigateToSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val continueWatching by viewModel.continueWatchingList.collectAsState()
    val listState = rememberLazyListState()

    var showCategorySelectorSheet by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isLoading, uiState.isLoadingContent) {
        if (!uiState.isLoading && !uiState.isLoadingContent) {
            isRefreshing = false
        }
    }

    // Infinite scroll trigger
    LaunchedEffect(listState) {
        snapshotFlow {
            val lastVisibleIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val totalItems = listState.layoutInfo.totalItemsCount
            lastVisibleIndex to totalItems
        }.collect { (lastVisible, total) ->
            if (total > 0 && lastVisible >= total - 5 && uiState.hasMoreContent && !uiState.isLoadingMore && !uiState.isLoadingContent && !isRefreshing) {
                viewModel.loadMoreVideos()
            }
        }
    }

    LaunchedEffect(uiState.selectedContentType, uiState.selectedProviderId, uiState.selectedCategoryId) {
        listState.scrollToItem(0)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PureBlack)
    ) {
        // TopBar (DRAMIX logo on left, Customizer & Search on right)
        HomeTopBar(
            onSearchClick = onNavigateToSearch,
            onCustomizeProvidersClick = { viewModel.openProviderCustomizer() }
        )

        // Three-Level Filtering Hierarchy Block
        if (uiState.contentTypes.isNotEmpty() || uiState.filteredProviders.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(PureBlack)
                    .padding(vertical = 4.dp)
            ) {
                // LEVEL 1: Content Type Selector (e.g. Drama Pendek 10 >, Film & Serial 13 >)
                ContentTypeChipsRow(
                    contentTypes = uiState.contentTypes,
                    selectedContentType = uiState.selectedContentType,
                    onContentTypeSelected = { viewModel.selectContentType(it) }
                )

                Spacer(modifier = Modifier.height(4.dp))

                // LEVEL 2: List Provider Name (filtered by selected content type)
                ProviderChipsRow(
                    providers = uiState.filteredProviders,
                    selectedProviderId = uiState.selectedProviderId,
                    onProviderSelected = { viewModel.selectProvider(it) }
                )

                // LEVEL 3: Categories of Selected Provider
                if (uiState.categories.isNotEmpty() || uiState.isLoadingCategories) {
                    Spacer(modifier = Modifier.height(4.dp))
                    CategoryChipsRow(
                        categories = uiState.categories,
                        selectedCategoryId = uiState.selectedCategoryId,
                        isLoading = uiState.isLoadingCategories,
                        onCategorySelected = { viewModel.selectCategory(it.id) },
                        onOpenCategorySheet = { showCategorySelectorSheet = true }
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Subtle separator
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(MidnightBorder.copy(alpha = 0.5f))
                )
            }
        }

        if (uiState.isLoading && uiState.providers.isEmpty()) {
            HomeShimmerLoading()
        } else if (uiState.errorMessage != null && uiState.categoryVideos.isEmpty() && uiState.popularVideos.isEmpty()) {
            HomeErrorState(
                message = uiState.errorMessage!!,
                onRetry = { viewModel.loadInitialData() }
            )
        } else {
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = {
                    isRefreshing = true
                    viewModel.refreshCurrentCategory()
                },
                modifier = Modifier.fillMaxSize()
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    // Continue Watching (Lanjutkan Menonton)
                if (continueWatching.isNotEmpty()) {
                    item(key = "continue_watching_section") {
                        ContinueWatchingSection(
                            historyList = continueWatching,
                            onItemClick = { history ->
                                onNavigateToVodPlayer(history.providerId, history.dramaId)
                            }
                        )
                    }
                }

                // Feed Section Header
                item(key = "feed_section_header") {
                    val activeCategoryName = uiState.categories.firstOrNull { it.id == uiState.selectedCategoryId }?.name
                        ?: "Koleksi Tayangan"
                    val activeProviderName = uiState.providers.firstOrNull { it.id == uiState.selectedProviderId }?.name
                        ?: ""

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (activeProviderName.isNotBlank()) "$activeCategoryName • $activeProviderName" else activeCategoryName,
                            color = Slate50,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )

                        if (uiState.categoryVideos.isNotEmpty()) {
                            Text(
                                text = "${uiState.categoryVideos.size} Judul",
                                color = Slate400,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                if (uiState.isLoadingContent) {
                    item(key = "content_loading_shimmer") {
                        FeedShimmerGrid(isShorts = uiState.selectedContentType == "short_drama")
                    }
                } else if (uiState.categoryVideos.isEmpty()) {
                    item(key = "empty_category_state") {
                        EmptyCategoryState()
                    }
                } else {
                    // Adaptive Grid: 3 columns for shorts and movies/dramas, 2 columns for live tv
                    val isShorts = uiState.selectedContentType == "short_drama"
                    val isLiveTv = uiState.selectedContentType == "live_tv"
                    val columns = if (isLiveTv) 2 else 3
                    val chunkedVideos = uiState.categoryVideos.chunked(columns)

                    items(chunkedVideos, key = { row -> row.firstOrNull()?.id ?: row.hashCode().toString() }) { rowVideos ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            rowVideos.forEach { video ->
                                Box(modifier = Modifier.weight(1f)) {
                                    val currentProviderId = uiState.selectedProviderId ?: video.source ?: "freereels"
                                    if (isShorts) {
                                        PosterCard9x16(
                                            item = video,
                                            onClick = { onNavigateToShorts(currentProviderId, video.id) }
                                        )
                                    } else {
                                        PosterCard2x3(
                                            item = video,
                                            onClick = { onNavigateToVodPlayer(currentProviderId, video.id) }
                                        )
                                    }
                                }
                            }
                            // Pad remaining columns if last row is incomplete
                            repeat(columns - rowVideos.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }

                if (uiState.isLoadingMore) {
                    item(key = "loading_more_indicator") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            LinearProgressIndicator(
                                color = CrimsonPlay,
                                trackColor = MidnightBorder,
                                modifier = Modifier.fillMaxWidth(0.5f)
                            )
                        }
                    }
                }
            }
        }
    }

        // Category Selector Bottom Sheet (for providers with many categories)
        if (showCategorySelectorSheet) {
            CategorySelectorSheet(
                categories = uiState.categories,
                selectedCategoryId = uiState.selectedCategoryId ?: "",
                onCategorySelected = { category ->
                    viewModel.selectCategory(category.id)
                    showCategorySelectorSheet = false
                },
                onDismissRequest = {
                    showCategorySelectorSheet = false
                }
            )
        }

        // Provider Customizer Sheet
        if (uiState.isCustomizingProviders) {
            ProviderCustomizerSheet(
                initialConfigs = viewModel.getAllProviderConfigs(),
                onSaveConfigs = { configs ->
                    viewModel.updateProviderConfigs(configs)
                },
                onResetToDefault = {
                    viewModel.resetProviderConfigs()
                },
                onDismissRequest = {
                    viewModel.closeProviderCustomizer()
                }
            )
        }
    }
}

@Composable
private fun HomeTopBar(
    onSearchClick: () -> Unit,
    onCustomizeProvidersClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "DRAMIX",
            color = CrimsonPlay,
            fontWeight = FontWeight.Black,
            fontSize = 24.sp,
            letterSpacing = (-0.5).sp
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconButton(
                onClick = onCustomizeProvidersClick,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = "Kustomisasi Provider",
                    tint = Slate50,
                    modifier = Modifier.size(22.dp)
                )
            }

            IconButton(
                onClick = onSearchClick,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Cari Drama",
                    tint = Slate50,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
private fun ProviderChipsRow(
    providers: List<ProviderModel>,
    selectedProviderId: String?,
    onProviderSelected: (String) -> Unit
) {
    val listState = rememberLazyListState()

    // Auto-scroll to selected provider (centered)
    LaunchedEffect(selectedProviderId) {
        selectedProviderId?.let { selected ->
            val selectedIndex = providers.indexOfFirst { it.id == selected }
            if (selectedIndex >= 0) {
                listState.animateToCentered(selectedIndex)
            }
        }
    }

    LazyRow(
        state = listState,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(providers, key = { it.id }) { provider ->
            val isSelected = provider.id == selectedProviderId
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (isSelected) CrimsonPlay.copy(alpha = 0.22f)
                        else MidnightCard
                    )
                    .border(
                        width = 1.dp,
                        color = if (isSelected) CrimsonPlay else MidnightBorder.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .clickable { onProviderSelected(provider.id) }
                    .padding(horizontal = 12.dp, vertical = 5.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (!provider.iconUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = provider.iconUrl,
                            contentDescription = provider.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                        )
                    }

                    Text(
                        text = provider.name,
                        color = if (isSelected) CrimsonPlay else Slate400,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun ContinueWatchingSection(
    historyList: List<WatchHistoryEntity>,
    onItemClick: (WatchHistoryEntity) -> Unit
) {
    Text(
        text = "Lanjutkan Menonton",
        color = Slate50,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )

    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(historyList, key = { it.id }) { history ->
            Box(
                modifier = Modifier
                    .width(180.dp)
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MidnightCard)
                    .clickable { onItemClick(history) }
            ) {
                AsyncImage(
                    model = history.dramaPoster,
                    contentDescription = history.dramaTitle,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                            )
                        )
                )

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp)
                ) {
                    Text(
                        text = history.dramaTitle,
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Ep. ${history.episodeNumber}",
                        color = Slate400,
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                // Progress Bar
                val progress = if (history.durationMs > 0L) {
                    (history.positionMs.toFloat() / history.durationMs.toFloat()).coerceIn(0f, 1f)
                } else 0f

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .align(Alignment.BottomCenter),
                    color = CrimsonPlay,
                    trackColor = Color.DarkGray
                )
            }
        }
    }
    Spacer(modifier = Modifier.height(16.dp))
}

@Composable
private fun PosterCard2x3(
    item: VideoItem,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val cardWidthPx = with(density) { 360.dp.roundToPx() }
    val cardHeightPx = (cardWidthPx * 3f / 2f).toInt()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(8.dp))
                .background(MidnightCard)
        ) {
            val imageRequest = ImageRequest.Builder(context)
                .data(item.cover)
                .size(Size(cardWidthPx, cardHeightPx))
                .build()

            AsyncImage(
                model = imageRequest,
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Sewa Badge / VIP Badge (Kanan Atas)
            if (item.isSewa) {
                SewaBadge(modifier = Modifier.align(Alignment.TopEnd))
            } else if (item.isVip) {
                VipBadge(modifier = Modifier.align(Alignment.TopEnd))
            }

            item.score?.let { score ->
                if (score.isNotBlank() && score != "0") {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(4.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.Black.copy(alpha = 0.7f))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "★ $score",
                            color = Color(0xFFF59E0B),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = item.title,
            color = Slate50,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 16.sp
        )
    }
}

@Composable
private fun PosterCard9x16(
    item: VideoItem,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val cardWidthPx = with(density) { 360.dp.roundToPx() }
    val cardHeightPx = (cardWidthPx * 16f / 9f).toInt()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(9f / 16f)
                .clip(RoundedCornerShape(8.dp))
                .background(MidnightCard)
        ) {
            val imageRequest = ImageRequest.Builder(context)
                .data(item.cover)
                .size(Size(cardWidthPx, cardHeightPx))
                .build()

            AsyncImage(
                model = imageRequest,
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Sewa Badge / VIP Badge (Kanan Atas)
            if (item.isSewa) {
                SewaBadge(modifier = Modifier.align(Alignment.TopEnd))
            } else if (item.isVip) {
                VipBadge(modifier = Modifier.align(Alignment.TopEnd))
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = item.title,
            color = Slate50,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 16.sp
        )
    }
}

@Composable
private fun FeedShimmerGrid(isShorts: Boolean) {
    val aspectRatio = if (isShorts) 9f / 16f else 2f / 3f
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        repeat(3) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                repeat(3) {
                    ShimmerPlaceholder(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(aspectRatio),
                        cornerRadius = 8.dp
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyCategoryState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp, horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Movie,
                contentDescription = null,
                tint = Slate400.copy(alpha = 0.5f),
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Belum ada tayangan untuk kategori ini",
                color = Slate400,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun HomeShimmerLoading() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 4.dp)
    ) {
        // Content Type Chip Shimmer Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ShimmerPlaceholder(modifier = Modifier.size(width = 110.dp, height = 28.dp), cornerRadius = 16.dp)
            ShimmerPlaceholder(modifier = Modifier.size(width = 95.dp, height = 28.dp), cornerRadius = 16.dp)
            ShimmerPlaceholder(modifier = Modifier.size(width = 85.dp, height = 28.dp), cornerRadius = 16.dp)
        }

        // Provider Chip Shimmer Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ShimmerPlaceholder(modifier = Modifier.size(width = 90.dp, height = 28.dp), cornerRadius = 16.dp)
            ShimmerPlaceholder(modifier = Modifier.size(width = 105.dp, height = 28.dp), cornerRadius = 16.dp)
            ShimmerPlaceholder(modifier = Modifier.size(width = 80.dp, height = 28.dp), cornerRadius = 16.dp)
        }

        // Category Chip Shimmer Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ShimmerPlaceholder(modifier = Modifier.size(width = 75.dp, height = 28.dp), cornerRadius = 16.dp)
            ShimmerPlaceholder(modifier = Modifier.size(width = 85.dp, height = 28.dp), cornerRadius = 16.dp)
            ShimmerPlaceholder(modifier = Modifier.size(width = 65.dp, height = 28.dp), cornerRadius = 16.dp)
            ShimmerPlaceholder(modifier = Modifier.size(width = 70.dp, height = 28.dp), cornerRadius = 16.dp)
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            repeat(3) {
                ShimmerPlaceholder(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(2f / 3f),
                    cornerRadius = 8.dp
                )
            }
        }
    }
}

@Composable
private fun HomeErrorState(
    message: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Terjadi Kesalahan",
                color = Slate50,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                color = Slate400,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = CrimsonPlay),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(text = "Coba Lagi", color = Color.White)
            }
        }
    }
}
