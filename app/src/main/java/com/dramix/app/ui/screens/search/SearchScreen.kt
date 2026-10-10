package com.dramix.app.ui.screens.search

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.dramix.app.domain.model.ProviderModel
import com.dramix.app.domain.model.VideoItem
import com.dramix.app.ui.components.SearchBarComponent
import com.dramix.app.ui.components.SearchResultGrid
import com.dramix.app.ui.components.animateToCentered
import com.dramix.app.ui.theme.CrimsonPlay
import com.dramix.app.ui.theme.MidnightBorder
import com.dramix.app.ui.theme.MidnightCard
import com.dramix.app.ui.theme.PureBlack
import com.dramix.app.ui.theme.Slate400
import com.dramix.app.ui.theme.Slate50

@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToVodPlayer: (providerId: String, dramaId: String) -> Unit,
    onNavigateToShortsPlayer: (providerId: String, dramaId: String) -> Unit,
    onNavigateToLiveTvPlayer: (providerId: String, channelId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PureBlack)
    ) {
        // Top Search Bar
        SearchBarComponent(
            query = uiState.query,
            onQueryChange = { viewModel.onQueryChange(it) },
            onSearchAction = { viewModel.searchImmediately(it) },
            onNavigateBack = onNavigateBack
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Provider Selector Chips (HomeScreen style with icons & centered auto-scroll)
        if (uiState.availableProviders.isNotEmpty()) {
            SearchProviderChipsRow(
                providers = uiState.availableProviders,
                selectedProviderId = uiState.selectedProviderId,
                onProviderSelected = { viewModel.onProviderChange(it) }
            )
            Spacer(modifier = Modifier.height(4.dp))
        }

        // Search Results / History Area
        if (uiState.errorMessage != null && !uiState.isLoading && uiState.results.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Error",
                        tint = CrimsonPlay,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = uiState.errorMessage!!,
                        color = Slate50,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            SearchResultGrid(
                results = uiState.results,
                isLoading = uiState.isLoading,
                query = uiState.query,
                selectedContentType = uiState.selectedContentType,
                selectedProviderId = uiState.selectedProviderId,
                recentQueries = uiState.recentQueries,
                hasMoreResults = uiState.hasMoreResults,
                isLoadingMore = uiState.isLoadingMore,
                onLoadMore = { viewModel.loadMoreResults() },
                onSelectRecentQuery = { viewModel.selectRecentQuery(it) },
                onDeleteRecentQuery = { viewModel.deleteRecentQuery(it) },
                onClearAllRecentQueries = { viewModel.clearAllRecentQueries() },
                onItemClick = { item ->
                    handleItemClick(
                        item = item,
                        selectedContentType = uiState.selectedContentType,
                        selectedProviderId = uiState.selectedProviderId,
                        onNavigateToVodPlayer = onNavigateToVodPlayer,
                        onNavigateToShortsPlayer = onNavigateToShortsPlayer,
                        onNavigateToLiveTvPlayer = onNavigateToLiveTvPlayer
                    )
                }
            )
        }
    }
}

@Composable
private fun SearchProviderChipsRow(
    providers: List<ProviderModel>,
    selectedProviderId: String?,
    onProviderSelected: (String) -> Unit
) {
    val activeProviders = remember(providers) {
        providers.filter { it.isActive }
    }
    val listState = rememberLazyListState()

    // Auto-scroll to selected provider (centered)
    LaunchedEffect(selectedProviderId, activeProviders) {
        selectedProviderId?.let { selected ->
            val selectedIndex = activeProviders.indexOfFirst { it.id == selected }
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
        items(activeProviders, key = { it.id }) { provider ->
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

private fun handleItemClick(
    item: VideoItem,
    selectedContentType: String?,
    selectedProviderId: String,
    onNavigateToVodPlayer: (providerId: String, dramaId: String) -> Unit,
    onNavigateToShortsPlayer: (providerId: String, dramaId: String) -> Unit,
    onNavigateToLiveTvPlayer: (providerId: String, channelId: String) -> Unit
) {
    val effectiveProvider = item.source ?: selectedProviderId
    val isShorts = item.type == "short_drama" || selectedContentType == "short_drama"
    val isLiveTv = item.type == "live_tv" || selectedContentType == "live_tv"

    when {
        isShorts -> onNavigateToShortsPlayer(effectiveProvider, item.id)
        isLiveTv -> onNavigateToLiveTvPlayer(effectiveProvider, item.id)
        else -> onNavigateToVodPlayer(effectiveProvider, item.id)
    }
}
