package com.dramix.app.ui.screens.player_shorts

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.dramix.app.ui.components.LicenseGateDialog
import com.dramix.app.ui.components.ShortsActionButtons
import com.dramix.app.ui.theme.CrimsonPlay
import com.dramix.app.ui.theme.PureBlack
import com.dramix.app.ui.theme.Slate400
import com.dramix.app.ui.theme.Slate50
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ShortsPlayerScreen(
    viewModel: ShortsPlayerViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToProfile: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    val episodes = uiState.episodes

    val pagerState = rememberPagerState(
        initialPage = uiState.currentEpisodeIndex,
        pageCount = { episodes.size.coerceAtLeast(1) }
    )

    // Sync pager scroll to viewModel
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { settledPage ->
            if (settledPage != uiState.currentEpisodeIndex && settledPage in episodes.indices) {
                viewModel.onEpisodeSelected(settledPage)
            }
        }
    }

    // Sync external episode selection to pager
    LaunchedEffect(uiState.currentEpisodeIndex) {
        if (pagerState.currentPage != uiState.currentEpisodeIndex && uiState.currentEpisodeIndex in episodes.indices) {
            pagerState.animateScrollToPage(uiState.currentEpisodeIndex)
        }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) {
        viewModel.pausePlayback()
    }

    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        viewModel.pausePlayback()
    }

    val context = LocalContext.current
    var isPausedByUser by remember { mutableStateOf(false) }

    val playerView = remember {
        PlayerView(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            keepScreenOn = true
            useController = false
            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            player = viewModel.playerController.player
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.pausePlayback()
            playerView.player = null
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
        if (uiState.isLoading && episodes.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = CrimsonPlay)
            }
        } else {
            VerticalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val episode = episodes.getOrNull(page)
                val isActivePage = page == uiState.currentEpisodeIndex

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            if (viewModel.playerController.player.isPlaying) {
                                viewModel.playerController.pause()
                                isPausedByUser = true
                            } else {
                                viewModel.playerController.play()
                                isPausedByUser = false
                            }
                        }
                ) {
                    if (isActivePage) {
                        AndroidView(
                            factory = { playerView },
                            modifier = Modifier.fillMaxSize()
                        )

                        // Paused icon overlay
                        if (isPausedByUser) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.5f))
                                    .align(Alignment.Center),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Jeda",
                                    tint = Color.White,
                                    modifier = Modifier.size(44.dp)
                                )
                            }
                        }
                    } else {
                        // Empty dark background for other pages during scroll
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(PureBlack)
                        )
                    }

                    // Bottom Gradient Shadow for readability
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .align(Alignment.BottomCenter)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                                )
                            )
                    )

                    // Left-Bottom Metadata Overlay
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = 16.dp, bottom = 24.dp, end = 90.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                viewModel.openDetailSheet()
                            }
                    ) {
                        Text(
                            text = uiState.detail?.title ?: "Drama Pendek",
                            color = Slate50,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Ep. ${episode?.number ?: (page + 1)}/${episodes.size}",
                                color = CrimsonPlay,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            uiState.detail?.score?.let { score ->
                                if (score.isNotBlank()) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "★ $score",
                                        color = Color(0xFFF59E0B),
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }

                        uiState.detail?.description?.let { desc ->
                            if (desc.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = desc,
                                    color = Slate400,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    // Right Vertical Action Column
                    ShortsActionButtons(
                        isBookmarked = uiState.isBookmarked,
                        onBookmarkClick = { viewModel.toggleBookmark() },
                        onEpisodesClick = { viewModel.openDetailSheet() },
                        onDownloadClick = { viewModel.downloadCurrentEpisode() },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 12.dp, bottom = 40.dp)
                    )
                }
            }

            // Top-Left Back Button Overlay
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier
                    .padding(top = 16.dp, start = 12.dp)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.45f))
                    .align(Alignment.TopStart)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Kembali",
                    tint = Color.White
                )
            }
        }

        // Modal Bottom Sheet Detail
        if (uiState.showDetailSheet && uiState.detail != null) {
            ShortsDetailBottomSheet(
                detail = uiState.detail!!,
                episodes = episodes,
                activeEpisodeNumber = episodes.getOrNull(uiState.currentEpisodeIndex)?.number ?: 1,
                onEpisodeSelected = { selectedEp ->
                    val index = episodes.indexOf(selectedEp)
                    if (index >= 0) {
                        viewModel.onEpisodeSelected(index)
                    }
                },
                onDismissRequest = { viewModel.closeDetailSheet() }
            )
        }

        // License Gate Dialog for Episode >= 4
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
