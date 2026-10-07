package com.dramix.app.ui.screens.player_vod

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dramix.app.ui.components.AdaptiveEpisodeList
import com.dramix.app.ui.components.LicenseGateDialog
import com.dramix.app.ui.components.VideoPlayerSurface
import com.dramix.app.ui.theme.CrimsonPlay
import com.dramix.app.ui.theme.MidnightBorder
import com.dramix.app.ui.theme.MidnightCard
import com.dramix.app.ui.theme.PureBlack
import com.dramix.app.ui.theme.Slate400
import com.dramix.app.ui.theme.Slate50
import kotlinx.coroutines.launch

@Composable
fun VodPlayerScreen(
    viewModel: VodPlayerViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToProfile: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()

    // Autosave progress when disposing/navigating back
    DisposableEffect(Unit) {
        onDispose {
            scope.launch {
                viewModel.saveCurrentProgress()
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PureBlack)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Player Area (16:9 aspect ratio at the top)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
            ) {
                VideoPlayerSurface(
                    player = viewModel.playerController.player,
                    modifier = Modifier.fillMaxSize()
                )

                // Top Back Button Overlay
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .padding(8.dp)
                        .size(40.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.Black.copy(alpha = 0.5f))
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Kembali",
                        tint = Color.White
                    )
                }

                // Playback Loading Overlay
                if (uiState.isLoadingPlayback) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.6f)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = CrimsonPlay,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            }

            // Scrollable Content & Metadata Area
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                uiState.detail?.let { detail ->
                    item {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // Drama Title
                            Text(
                                text = detail.title,
                                color = Slate50,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            // Metadata Info Row (Rating, Score, Views)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                detail.score?.let { score ->
                                    if (score.isNotBlank() && score != "0") {
                                        Text(
                                            text = "★ $score",
                                            color = Color(0xFFF59E0B),
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.sp
                                        )
                                    }
                                }

                                detail.releaseDate?.let { date ->
                                    Text(
                                        text = date,
                                        color = Slate400,
                                        fontSize = 12.sp
                                    )
                                }

                                Text(
                                    text = "${detail.totalEpisodes} Episode",
                                    color = Slate400,
                                    fontSize = 12.sp
                                )
                            }

                            // Tags Row
                            if (detail.tags.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    detail.tags.take(3).forEach { tag ->
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(MidnightCard)
                                                .border(1.dp, MidnightBorder, RoundedCornerShape(4.dp))
                                                .padding(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = tag,
                                                color = Slate400,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Action Buttons: Bookmark & Download
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Bookmark Button
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (uiState.isBookmarked) CrimsonPlay.copy(alpha = 0.2f) else MidnightCard)
                                        .border(
                                            1.dp,
                                            if (uiState.isBookmarked) CrimsonPlay else MidnightBorder,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable { viewModel.toggleBookmark() },
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = if (uiState.isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                        contentDescription = "Bookmark",
                                        tint = if (uiState.isBookmarked) CrimsonPlay else Slate50,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (uiState.isBookmarked) "Tersimpan" else "Favorit",
                                        color = if (uiState.isBookmarked) CrimsonPlay else Slate50,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 14.sp
                                    )
                                }

                                // Download Action
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MidnightCard)
                                        .border(1.dp, MidnightBorder, RoundedCornerShape(8.dp))
                                        .clickable {
                                            // Handled by offline download manager in Task 11
                                        },
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Download,
                                        contentDescription = "Unduh",
                                        tint = Slate50,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Unduh",
                                        color = Slate50,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 14.sp
                                    )
                                }
                            }

                            // Description Synopsis
                            detail.description?.let { desc ->
                                if (desc.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Text(
                                        text = desc,
                                        color = Slate400,
                                        style = MaterialTheme.typography.bodyMedium,
                                        lineHeight = 20.sp
                                    )
                                }
                            }
                        }
                    }

                    // Season Selector (if more than 1 season exists)
                    if (detail.seasons.size > 1) {
                        item {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(detail.seasons) { season ->
                                    val isSelected = season.index == uiState.currentSeasonIndex
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isSelected) CrimsonPlay else MidnightCard)
                                            .clickable { viewModel.selectSeason(season.index) }
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = season.name ?: "Season ${season.index}",
                                            color = if (isSelected) Color.White else Slate400,
                                            fontSize = 13.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }

                    // Episode List Section
                    item {
                        Text(
                            text = "Daftar Episode",
                            color = Slate50,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                        )

                        val currentSeason = detail.seasons.find { it.index == uiState.currentSeasonIndex }
                            ?: detail.seasons.firstOrNull()
                        val episodes = currentSeason?.episodes ?: emptyList()

                        AdaptiveEpisodeList(
                            episodes = episodes,
                            activeEpisodeNumber = uiState.currentEpisode?.number ?: 1,
                            onEpisodeClick = { episode ->
                                viewModel.playEpisode(episode)
                            }
                        )
                    }
                }
            }
        }

        // License Gate Dialog
        if (uiState.showLicenseGate) {
            LicenseGateDialog(
                episodeNumber = uiState.lockedEpisodeNumber,
                onActivateClick = {
                    viewModel.dismissLicenseGate()
                    onNavigateToProfile()
                },
                onDismiss = {
                    viewModel.dismissLicenseGate()
                }
            )
        }
    }
}
