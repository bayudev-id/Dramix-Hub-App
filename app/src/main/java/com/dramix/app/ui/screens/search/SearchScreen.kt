package com.dramix.app.ui.screens.search

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dramix.app.domain.model.VideoItem
import com.dramix.app.ui.components.SearchBarComponent
import com.dramix.app.ui.components.SearchResultGrid
import com.dramix.app.ui.theme.CrimsonPlay
import com.dramix.app.ui.theme.MidnightBorder
import com.dramix.app.ui.theme.MidnightCard
import com.dramix.app.ui.theme.PureBlack
import com.dramix.app.ui.theme.Slate400
import com.dramix.app.ui.theme.Slate50

private data class ContentTypeFilter(
    val label: String,
    val value: String?
)

private val CONTENT_TYPE_FILTERS = listOf(
    ContentTypeFilter("Semua", null),
    ContentTypeFilter("Drama", "long_drama"),
    ContentTypeFilter("Shorts", "short_drama"),
    ContentTypeFilter("Film", "movie"),
    ContentTypeFilter("Live TV", "live_tv")
)

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

        // Content Type Filter Chips
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(CONTENT_TYPE_FILTERS) { filter ->
                val isSelected = uiState.selectedContentType == filter.value
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) CrimsonPlay else MidnightCard)
                        .border(
                            width = 1.dp,
                            color = if (isSelected) CrimsonPlay else MidnightBorder,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable { viewModel.onContentTypeChange(filter.value) }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = filter.label,
                        color = if (isSelected) Color.White else Slate400,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }

        // Provider Selector Chips (if > 1 provider available)
        if (uiState.availableProviders.size > 1) {
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.availableProviders, key = { it.id }) { provider ->
                    val isSelected = uiState.selectedProviderId == provider.id
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) Slate400.copy(alpha = 0.25f) else PureBlack)
                            .border(
                                width = 1.dp,
                                color = if (isSelected) Slate50 else MidnightBorder,
                                shape = RoundedCornerShape(6.dp)
                            )
                            .clickable { viewModel.onProviderChange(provider.id) }
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = provider.name,
                            color = if (isSelected) Slate50 else Slate400,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Search Results / History Area
        SearchResultGrid(
            results = uiState.results,
            isLoading = uiState.isLoading,
            query = uiState.query,
            selectedContentType = uiState.selectedContentType,
            recentQueries = uiState.recentQueries,
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
