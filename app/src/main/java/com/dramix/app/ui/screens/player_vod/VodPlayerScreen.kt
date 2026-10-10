package com.dramix.app.ui.screens.player_vod

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.dramix.app.player.model.PlaybackState
import com.dramix.app.ui.components.AdaptiveEpisodeList
import com.dramix.app.ui.components.LicenseGateDialog
import com.dramix.app.ui.components.RentalEpisodeGateDialog
import com.dramix.app.ui.components.VideoPlayerSurface
import coil.compose.AsyncImage
import com.dramix.app.domain.model.CastMember
import com.dramix.app.ui.components.SubtitleOverlay
import com.dramix.app.ui.components.VodPlayerOverlay
import com.dramix.app.ui.theme.CrimsonPlay
import com.dramix.app.ui.theme.MidnightBorder
import com.dramix.app.ui.theme.MidnightCard
import com.dramix.app.ui.theme.PureBlack
import com.dramix.app.ui.theme.Slate400
import com.dramix.app.ui.theme.Slate50
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VodPlayerScreen(
    viewModel: VodPlayerViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToProfile: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val playbackState by viewModel.playerController.playbackState.collectAsState()
    val currentSubtitle by viewModel.currentSubtitleText.collectAsState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val activity = context as? Activity

    var isFullscreen by remember { mutableStateOf(false) }
    var isSettingsMenuOpen by remember { mutableStateOf(false) }

    fun toggleFullscreen() {
        val target = !isFullscreen
        isFullscreen = target
        activity?.requestedOrientation = if (target) {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } else {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        activity?.window?.let { window ->
            val insetsController = WindowCompat.getInsetsController(window, window.decorView)
            insetsController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            if (target) {
                insetsController.hide(WindowInsetsCompat.Type.systemBars())
            } else {
                insetsController.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    BackHandler(enabled = isFullscreen) {
        toggleFullscreen()
    }

    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) {
        viewModel.pausePlayback()
    }

    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        viewModel.pausePlayback()
    }

    LaunchedEffect(isFullscreen) {
        activity?.window?.let { window ->
            val insetsController = WindowCompat.getInsetsController(window, window.decorView)
            if (isFullscreen) {
                insetsController.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                insetsController.hide(WindowInsetsCompat.Type.systemBars())
            } else {
                insetsController.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    // Autosave progress, restore orientation, restore system bars, and pause when disposing/navigating back
    DisposableEffect(Unit) {
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            activity?.window?.let { window ->
                val insetsController = WindowCompat.getInsetsController(window, window.decorView)
                insetsController.show(WindowInsetsCompat.Type.systemBars())
            }
            viewModel.pausePlayback()
            scope.launch {
                viewModel.saveCurrentProgress()
            }
        }
    }

    val isPlaying = when (val state = playbackState) {
        is PlaybackState.Ready -> state.isPlaying
        else -> viewModel.playerController.player.isPlaying
    }

    val currentPositionMs = when (val state = playbackState) {
        is PlaybackState.Ready -> state.currentPositionMs
        else -> {
            val pos = viewModel.playerController.player.currentPosition.coerceAtLeast(0L)
            if (pos > 0L) pos else uiState.initialPositionMs
        }
    }

    val durationMs = when (val state = playbackState) {
        is PlaybackState.Ready -> if (state.durationMs > 0L) state.durationMs else uiState.prefilledDurationMs
        else -> {
            val playerDuration = viewModel.playerController.player.duration.coerceAtLeast(0L)
            if (playerDuration > 0L) playerDuration else uiState.prefilledDurationMs
        }
    }

    val isBuffering = uiState.isLoadingPlayback || playbackState is PlaybackState.Buffering

    val dramaTitle = uiState.detail?.title ?: "Dramix"
    val episodeTitle = uiState.currentEpisode?.let { ep ->
        ep.title?.takeIf { it.isNotBlank() } ?: "Episode ${ep.number}"
    }

    val hasPreviousEpisode = remember(uiState.detail, uiState.currentEpisode, uiState.currentSeasonIndex) {
        viewModel.hasPreviousEpisode()
    }
    val hasNextEpisode = remember(uiState.detail, uiState.currentEpisode, uiState.currentSeasonIndex) {
        viewModel.hasNextEpisode()
    }

    val activeSubtitleStyle = if (isFullscreen) uiState.fullscreenSubtitleStyle else uiState.portraitSubtitleStyle
    val activeVideoZoom = if (isFullscreen) uiState.fullscreenVideoZoom else uiState.portraitVideoZoom

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PureBlack)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(if (!isFullscreen) Modifier.statusBarsPadding() else Modifier)
        ) {
            // Unified Player Viewport - never unmounted across fullscreen toggles
            Box(
                modifier = if (isFullscreen) {
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                } else {
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clipToBounds()
                        .background(Color.Black)
                }
            ) {
                VideoPlayerSurface(
                    player = viewModel.playerController.player,
                    modifier = Modifier.fillMaxSize(),
                    useController = false,
                    subtitleStyle = activeSubtitleStyle,
                    videoZoom = activeVideoZoom
                )

                if (!uiState.selectedSubtitleId.equals("off", ignoreCase = true) && currentSubtitle != null) {
                    SubtitleOverlay(
                        text = currentSubtitle!!.text,
                        style = activeSubtitleStyle,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                VodPlayerOverlay(
                    dramaTitle = dramaTitle,
                    episodeTitle = episodeTitle,
                    isPlaying = isPlaying,
                    isBuffering = isBuffering,
                    currentPositionMs = currentPositionMs,
                    durationMs = durationMs,
                    isFullscreen = isFullscreen,
                    onNavigateBack = {
                        if (isFullscreen) toggleFullscreen() else onNavigateBack()
                    },
                    onTogglePlayPause = { viewModel.playerController.togglePlayPause() },
                    onSeekTo = { pos -> viewModel.playerController.seekTo(pos) },
                    onSeekBy = { offset -> viewModel.playerController.seekBy(offset) },
                    onToggleFullscreen = { toggleFullscreen() },
                    isSettingsOpen = isSettingsMenuOpen,
                    onOpenSettings = { isSettingsMenuOpen = true },
                    onDismissSettings = { isSettingsMenuOpen = false },
                    qualities = uiState.availableQualities,
                    selectedQuality = uiState.selectedQuality,
                    onSelectQuality = { q -> viewModel.selectQuality(q) },
                    subtitles = uiState.availableSubtitles,
                    selectedSubtitleId = uiState.selectedSubtitleId,
                    onSelectSubtitle = { sId -> viewModel.selectSubtitle(sId) },
                    playbackSpeed = uiState.playbackSpeed,
                    onSelectSpeed = { speed -> viewModel.setPlaybackSpeed(speed) },
                    isAutoNext = uiState.isAutoNext,
                    onToggleAutoNext = { enabled -> viewModel.toggleAutoNext(enabled) },
                    hasPreviousEpisode = hasPreviousEpisode,
                    hasNextEpisode = hasNextEpisode,
                    onPlayPreviousEpisode = { viewModel.playPreviousEpisode() },
                    onPlayNextEpisode = { viewModel.playNextEpisode() },
                    videoZoom = activeVideoZoom,
                    onUpdateZoom = { delta -> viewModel.updateVideoZoom(delta, isFullscreen = isFullscreen) },
                    subtitleStyle = activeSubtitleStyle,
                    onSelectFontFamily = { f -> viewModel.selectSubtitleFontFamily(f, isFullscreen = isFullscreen) },
                    onSelectOutlineStyle = { o -> viewModel.selectSubtitleOutlineStyle(o, isFullscreen = isFullscreen) },
                    onUpdateFontSize = { delta -> viewModel.updateSubtitleFontSize(delta, isFullscreen = isFullscreen) },
                    onUpdatePosition = { delta -> viewModel.updateSubtitlePosition(delta, isFullscreen = isFullscreen) },
                    onUpdateBgOpacity = { delta -> viewModel.updateSubtitleBgOpacity(delta, isFullscreen = isFullscreen) },
                    onSetBgOpacity = { opacity -> viewModel.setSubtitleBgOpacity(opacity, isFullscreen = isFullscreen) },
                    onUpdateTextColor = { color -> viewModel.updateSubtitleTextColor(color, isFullscreen = isFullscreen) },
                    onUpdateBgColor = { color -> viewModel.updateSubtitleBgColor(color, isFullscreen = isFullscreen) },
                    onUpdateLineSpacing = { delta -> viewModel.updateSubtitleLineSpacing(delta, isFullscreen = isFullscreen) },
                    onUpdateBgPadding = { delta -> viewModel.updateSubtitleBackgroundPadding(delta, isFullscreen = isFullscreen) },
                    isCountdown = uiState.isCountdown
                )

                if (uiState.isCountdown) {
                    com.dramix.app.ui.components.VodPlayerCountdownOverlay(
                        dramaTitle = dramaTitle,
                        episodeTitle = episodeTitle,
                        countdownUrl = uiState.countdownUrl,
                        isFullscreen = isFullscreen,
                        isRefreshing = uiState.isRefreshing,
                        onNavigateBack = {
                            if (isFullscreen) toggleFullscreen() else onNavigateBack()
                        },
                        onToggleFullscreen = { toggleFullscreen() },
                        onRefresh = { viewModel.refreshCurrentEpisode() }
                    )
                }

                if (uiState.errorMessage != null && !uiState.isLoadingPlayback && uiState.rentalBlockedEpisode == null && !uiState.isCountdown) {
                    VodPlayerErrorOverlay(
                        errorMessage = uiState.errorMessage!!,
                        onRetry = { viewModel.refreshCurrentEpisode() }
                    )
                }
            }

            // Scrollable Content & Metadata Area - Only rendered in portrait mode
            if (!isFullscreen) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .navigationBarsPadding(),
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
                                            viewModel.downloadCurrentEpisode()
                                        },
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = if (uiState.isCurrentEpisodeDownloaded) Icons.Default.DownloadDone else Icons.Default.Download,
                                        contentDescription = "Unduh",
                                        tint = if (uiState.isCurrentEpisodeDownloaded) Color(0xFF10B981) else Slate50,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (uiState.isCurrentEpisodeDownloaded) "Tersimpan" else "Unduh",
                                        color = if (uiState.isCurrentEpisodeDownloaded) Color(0xFF10B981) else Slate50,
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

                            // Cast section (actors & directors)
                            if (detail.cast.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(20.dp))
                                CastSection(cast = detail.cast)
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
                        var isDubMenuExpanded by remember { mutableStateOf(false) }
                        val dubs = detail.dubs
                        val activeDub = dubs.find { it.id == uiState.selectedDubId }
                            ?: dubs.find { it.id == detail.id || detail.id.contains(it.id) || it.id.contains(detail.id) }
                            ?: dubs.firstOrNull { it.isOriginal }
                            ?: dubs.firstOrNull()

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Daftar Episode",
                                color = Slate50,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            if (dubs.isNotEmpty()) {
                                Box {
                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MidnightCard)
                                            .border(1.dp, MidnightBorder, RoundedCornerShape(8.dp))
                                            .clickable { isDubMenuExpanded = true }
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Language,
                                            contentDescription = "Dubbing",
                                            tint = CrimsonPlay,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = activeDub?.title ?: "Pilih Dub",
                                            color = Slate50,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.widthIn(max = 140.dp)
                                        )
                                        Icon(
                                            imageVector = Icons.Default.ArrowDropDown,
                                            contentDescription = null,
                                            tint = Slate400,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    DropdownMenu(
                                        expanded = isDubMenuExpanded,
                                        onDismissRequest = { isDubMenuExpanded = false },
                                        modifier = Modifier.background(MidnightCard)
                                    ) {
                                        dubs.forEach { dub ->
                                            val isSelected = dub.id == activeDub?.id
                                            DropdownMenuItem(
                                                text = {
                                                    Text(
                                                        text = dub.title,
                                                        color = if (isSelected) CrimsonPlay else Slate50,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                        fontSize = 13.sp
                                                    )
                                                },
                                                onClick = {
                                                    isDubMenuExpanded = false
                                                    if (!isSelected) {
                                                        viewModel.selectDub(dub)
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        val currentSeason = detail.seasons.find { it.index == uiState.currentSeasonIndex }
                            ?: detail.seasons.firstOrNull()
                        val episodes = currentSeason?.episodes ?: emptyList()

                        AdaptiveEpisodeList(
                            episodes = episodes,
                            activeEpisodeNumber = uiState.currentEpisode?.number ?: 1,
                            activeEpisodeId = uiState.currentEpisode?.id,
                            onEpisodeClick = { episode ->
                                viewModel.playEpisode(episode)
                            }
                        )
                    }
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

    // Rental Gate Dialog for Episode Sewa
    uiState.rentalBlockedEpisode?.let { blockedEp ->
        RentalEpisodeGateDialog(
            episodeTitle = blockedEp.title,
            episodeNumber = blockedEp.number,
            onDismiss = {
                viewModel.dismissRentalGate()
            }
        )
    }
}
}

@Composable
private fun CastSection(cast: List<CastMember>) {
    val hasDirector = cast.any { it.isDirector }
    Column(Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = if (hasDirector) "Pemeran & Kru" else "Pemeran",
                color = Slate50,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "(${cast.size})",
                color = Slate400,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(cast) { member ->
                CastMemberItem(member)
            }
        }
    }
}

@Composable
private fun CastMemberItem(member: CastMember) {
    Column(
        modifier = Modifier.width(76.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val initials = remember(member.name) {
            member.name.trim().split(Regex("\\s+"))
                .take(2)
                .mapNotNull { it.firstOrNull()?.toString() }
                .joinToString("")
                .uppercase()
        }
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(MidnightCard)
                .border(1.dp, MidnightBorder, CircleShape)
        ) {
            val cover = member.cover
            if (!cover.isNullOrBlank()) {
                AsyncImage(
                    model = cover,
                    contentDescription = member.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else if (initials.isNotEmpty()) {
                Text(
                    text = initials,
                    color = Slate400,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = member.name,
            color = Slate50,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            lineHeight = 15.sp,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = when {
                member.isDirector -> "Sutradara"
                !member.role.isNullOrBlank() -> member.role
                else -> "Pemeran"
            },
            color = if (member.isDirector) CrimsonPlay else Slate400,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun VodPlayerErrorOverlay(
    errorMessage: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.88f))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Error",
                tint = CrimsonPlay,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = errorMessage,
                color = Slate50,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = CrimsonPlay),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "Coba Lagi", color = Color.White, fontSize = 13.sp)
            }
        }
    }
}