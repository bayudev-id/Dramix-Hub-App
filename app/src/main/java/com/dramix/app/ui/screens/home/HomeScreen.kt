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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
import com.dramix.app.ui.components.ShimmerPlaceholder
import com.dramix.app.ui.theme.CrimsonPlay
import com.dramix.app.ui.theme.MidnightBorder
import com.dramix.app.ui.theme.MidnightCard
import com.dramix.app.ui.theme.PureBlack
import com.dramix.app.ui.theme.Slate400
import com.dramix.app.ui.theme.Slate50
import com.dramix.app.ui.theme.TagBadgeShape

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

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PureBlack)
    ) {
        // TopBar (No fake avatar, DRAMIX logo on left, Search on right)
        HomeTopBar(onSearchClick = onNavigateToSearch)

        if (uiState.isLoading && uiState.providers.isEmpty()) {
            HomeShimmerLoading()
        } else if (uiState.errorMessage != null && uiState.popularVideos.isEmpty()) {
            HomeErrorState(
                message = uiState.errorMessage!!,
                onRetry = { viewModel.loadInitialData() }
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                // Provider Chips Carousel
                item {
                    ProviderChipsRow(
                        providers = uiState.providers,
                        selectedProviderId = uiState.selectedProviderId,
                        onProviderSelected = { viewModel.selectProvider(it) }
                    )
                }

                // Spotlight Hero Banner
                uiState.spotlightItem?.let { spotlight ->
                    item {
                        SpotlightBanner(
                            item = spotlight,
                            onPlayClick = {
                                val providerId = uiState.selectedProviderId ?: "wetv"
                                if (spotlight.type == "short_drama") {
                                    onNavigateToShorts(providerId, spotlight.id)
                                } else {
                                    onNavigateToVodPlayer(providerId, spotlight.id)
                                }
                            }
                        )
                    }
                }

                // Continue Watching (Lanjutkan Menonton)
                if (continueWatching.isNotEmpty()) {
                    item {
                        ContinueWatchingSection(
                            historyList = continueWatching,
                            onItemClick = { history ->
                                onNavigateToVodPlayer(history.providerId, history.dramaId)
                            }
                        )
                    }
                }

                // Popular Long Dramas Section (Poster 2:3)
                if (uiState.popularVideos.isNotEmpty()) {
                    item {
                        SectionHeader(title = "Drama Populer")
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(uiState.popularVideos, key = { it.id }) { video ->
                                PosterCard2x3(
                                    item = video,
                                    onClick = {
                                        val providerId = uiState.selectedProviderId ?: "wetv"
                                        onNavigateToVodPlayer(providerId, video.id)
                                    }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }

                // Trending Shorts Section (Poster 9:16)
                if (uiState.shortDramaVideos.isNotEmpty()) {
                    item {
                        SectionHeader(title = "Drama Pendek Trending")
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(uiState.shortDramaVideos, key = { it.id }) { video ->
                                PosterCard9x16(
                                    item = video,
                                    onClick = {
                                        val shortProvider = uiState.providers.firstOrNull { it.contentType == "short_drama" }?.id ?: "freereels"
                                        onNavigateToShorts(shortProvider, video.id)
                                    }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeTopBar(onSearchClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
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

        IconButton(
            onClick = onSearchClick,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Cari Drama",
                tint = Slate50,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun ProviderChipsRow(
    providers: List<ProviderModel>,
    selectedProviderId: String?,
    onProviderSelected: (String) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(providers, key = { it.id }) { provider ->
            val isSelected = provider.id == selectedProviderId
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isSelected) CrimsonPlay else MidnightCard)
                    .border(
                        width = 1.dp,
                        color = if (isSelected) CrimsonPlay else MidnightBorder,
                        shape = RoundedCornerShape(20.dp)
                    )
                    .clickable { onProviderSelected(provider.id) }
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = provider.name,
                    color = if (isSelected) Color.White else Slate400,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
private fun SpotlightBanner(
    item: VideoItem,
    onPlayClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onPlayClick() }
    ) {
        AsyncImage(
            model = item.cover,
            contentDescription = item.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Gradient overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f)),
                        startY = 100f
                    )
                )
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
        ) {
            Text(
                text = item.title,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            item.score?.let { score ->
                if (score.isNotBlank() && score != "0") {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "★ $score",
                        color = Color(0xFFF59E0B),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onPlayClick,
                colors = ButtonDefaults.buttonColors(containerColor = CrimsonPlay),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "Putar Sekarang", color = Color.White, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun ContinueWatchingSection(
    historyList: List<WatchHistoryEntity>,
    onItemClick: (WatchHistoryEntity) -> Unit
) {
    SectionHeader(title = "Lanjutkan Menonton")
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
    Spacer(modifier = Modifier.height(20.dp))
}

@Composable
private fun PosterCard2x3(
    item: VideoItem,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(120.dp)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(8.dp))
                .background(MidnightCard)
        ) {
            AsyncImage(
                model = item.cover,
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            if (item.isVip) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .clip(TagBadgeShape)
                        .background(Color(0xFFF59E0B))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "VIP",
                        color = Color.Black,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = item.title,
            color = Slate50,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun PosterCard9x16(
    item: VideoItem,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(100.dp)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(9f / 16f)
                .clip(RoundedCornerShape(8.dp))
                .background(MidnightCard)
        ) {
            AsyncImage(
                model = item.cover,
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = item.title,
            color = Slate50,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        color = Slate50,
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
    )
}

@Composable
private fun HomeShimmerLoading() {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        ShimmerPlaceholder(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f),
            cornerRadius = 12.dp
        )
        Spacer(modifier = Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            repeat(3) {
                ShimmerPlaceholder(
                    modifier = Modifier
                        .width(120.dp)
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
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = message, color = Slate400, style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = CrimsonPlay)
            ) {
                Text(text = "Coba Lagi", color = Color.White)
            }
        }
    }
}
