package com.dramix.app.ui.components

import android.app.Activity
import android.content.Context
import android.media.AudioManager
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dramix.app.ui.screens.player_vod.SubtitleStyleConfig
import com.dramix.app.ui.screens.player_vod.SubtitleUiModel
import com.dramix.app.ui.theme.CrimsonPlay
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

private enum class GestureType {
    BRIGHTNESS,
    VOLUME
}

@Composable
fun VodPlayerOverlay(
    dramaTitle: String,
    episodeTitle: String?,
    isPlaying: Boolean,
    isBuffering: Boolean,
    currentPositionMs: Long,
    durationMs: Long,
    isFullscreen: Boolean,
    onNavigateBack: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSeekBy: (Long) -> Unit,
    onToggleFullscreen: () -> Unit,
    isSettingsOpen: Boolean = false,
    onOpenSettings: () -> Unit = {},
    onDismissSettings: () -> Unit = {},
    qualities: List<String> = emptyList(),
    selectedQuality: String? = null,
    onSelectQuality: (String) -> Unit = {},
    subtitles: List<SubtitleUiModel> = emptyList(),
    selectedSubtitleId: String = "off",
    onSelectSubtitle: (String) -> Unit = {},
    playbackSpeed: Float = 1.0f,
    onSelectSpeed: (Float) -> Unit = {},
    isAutoNext: Boolean = false,
    onToggleAutoNext: (Boolean) -> Unit = {},
    hasPreviousEpisode: Boolean = false,
    hasNextEpisode: Boolean = false,
    onPlayPreviousEpisode: () -> Unit = {},
    onPlayNextEpisode: () -> Unit = {},
    videoZoom: String = "100%",
    onUpdateZoom: (Int) -> Unit = {},
    subtitleStyle: SubtitleStyleConfig = SubtitleStyleConfig(),
    onSelectFontFamily: (String) -> Unit = {},
    onSelectOutlineStyle: (String) -> Unit = {},
    onUpdateFontSize: (Int) -> Unit = {},
    onUpdatePosition: (Int) -> Unit = {},
    onUpdateBgOpacity: (Int) -> Unit = {},
    onSetBgOpacity: (Int) -> Unit = {},
    onUpdateLineSpacing: (Int) -> Unit = {},
    onUpdateBgPadding: (Int) -> Unit = {},
    onUpdateTextColor: (Long) -> Unit = {},
    onUpdateBgColor: (Long) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager }

    var isControlsVisible by remember { mutableStateOf(true) }
    var isScrubbing by remember { mutableStateOf(false) }
    var scrubPositionMs by remember { mutableLongStateOf(0L) }
    var userInteractionCounter by remember { mutableLongStateOf(0L) }

    // Gesture control states (Volume: kanan, Brightness: kiri)
    var isDraggingGesture by remember { mutableStateOf(false) }
    var gestureType by remember { mutableStateOf<GestureType?>(null) }
    var lastActiveGestureType by remember { mutableStateOf<GestureType?>(null) }
    var gestureValue by remember { mutableFloatStateOf(0f) }
    var showGestureHud by remember { mutableStateOf(false) }
    var overlayWidth by remember { mutableFloatStateOf(1f) }
    var overlayHeight by remember { mutableFloatStateOf(1f) }

    var optimisticSeekPositionMs by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(isDraggingGesture) {
        if (!isDraggingGesture && showGestureHud) {
            delay(800L)
            showGestureHud = false
            gestureType = null
        }
    }

    LaunchedEffect(currentPositionMs) {
        optimisticSeekPositionMs?.let { opt ->
            if (kotlin.math.abs(currentPositionMs - opt) <= 1200L) {
                optimisticSeekPositionMs = null
            }
        }
    }

    LaunchedEffect(optimisticSeekPositionMs) {
        if (optimisticSeekPositionMs != null) {
            delay(1500L)
            optimisticSeekPositionMs = null
        }
    }

    fun registerInteraction() {
        userInteractionCounter++
        isControlsVisible = true
    }

    LaunchedEffect(isSettingsOpen) {
        if (isSettingsOpen) {
            isControlsVisible = true
        }
    }

    // Auto-hide controls after 4 seconds of inactivity when playing
    LaunchedEffect(isControlsVisible, isPlaying, isScrubbing, isSettingsOpen, userInteractionCounter) {
        if (isControlsVisible && isPlaying && !isScrubbing && !isSettingsOpen) {
            delay(4000L)
            isControlsVisible = false
        }
    }

    val displayedPosition = when {
        isScrubbing -> scrubPositionMs
        optimisticSeekPositionMs != null -> optimisticSeekPositionMs!!
        else -> currentPositionMs
    }

    fun applySeekDelta(deltaMs: Long) {
        val base = optimisticSeekPositionMs ?: (if (isScrubbing) scrubPositionMs else currentPositionMs)
        val target = if (durationMs > 0L) {
            (base + deltaMs).coerceIn(0L, durationMs)
        } else {
            (base + deltaMs).coerceAtLeast(0L)
        }
        optimisticSeekPositionMs = target
        onSeekTo(target)
        registerInteraction()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { size ->
                overlayWidth = size.width.toFloat().coerceAtLeast(1f)
                overlayHeight = size.height.toFloat().coerceAtLeast(1f)
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        if (!showGestureHud) {
                            isControlsVisible = !isControlsVisible
                            if (isControlsVisible) {
                                registerInteraction()
                            }
                        }
                    }
                )
            }
            .pointerInput(isFullscreen) {
                detectVerticalDragGestures(
                    onDragStart = { offset ->
                        // Hanya aktif ketika fullscreen untuk mencegah konflik gesture saat portrait
                        if (!isFullscreen) {
                            isDraggingGesture = false
                            gestureType = null
                            return@detectVerticalDragGestures
                        }

                        // Berikan ruang margin aman dari tepi layar (status bar atas, nav bar bawah, dan system back gesture kiri/kanan)
                        val topMargin = overlayHeight * 0.18f
                        val bottomMargin = overlayHeight * 0.18f
                        val sideMargin = overlayWidth * 0.12f

                        if (offset.y < topMargin || offset.y > (overlayHeight - bottomMargin) ||
                            offset.x < sideMargin || offset.x > (overlayWidth - sideMargin)
                        ) {
                            isDraggingGesture = false
                            gestureType = null
                            return@detectVerticalDragGestures
                        }

                        isDraggingGesture = true
                        val isLeft = offset.x < overlayWidth / 2f
                        if (isLeft) {
                            gestureType = GestureType.BRIGHTNESS
                            lastActiveGestureType = GestureType.BRIGHTNESS
                            val winBrightness = activity?.window?.attributes?.screenBrightness ?: -1f
                            val currentBrightness = if (winBrightness < 0f) {
                                try {
                                    Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS) / 255f
                                } catch (_: Exception) {
                                    0.5f
                                }
                            } else {
                                winBrightness
                            }
                            gestureValue = currentBrightness.coerceIn(0.01f, 1f)
                        } else {
                            gestureType = GestureType.VOLUME
                            lastActiveGestureType = GestureType.VOLUME
                            val maxVol = audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 15
                            val currVol = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 7
                            gestureValue = (currVol.toFloat() / maxVol.toFloat()).coerceIn(0f, 1f)
                        }
                        showGestureHud = true
                    },
                    onDragEnd = {
                        isDraggingGesture = false
                    },
                    onDragCancel = {
                        isDraggingGesture = false
                    },
                    onVerticalDrag = { change, dragAmount ->
                        if (!isDraggingGesture || gestureType == null) {
                            return@detectVerticalDragGestures
                        }
                        change.consume()
                        val delta = -dragAmount / (overlayHeight * 0.75f)
                        val newVal = (gestureValue + delta).coerceIn(0f, 1f)
                        gestureValue = newVal
                        showGestureHud = true

                        if (gestureType == GestureType.BRIGHTNESS) {
                            activity?.let { act ->
                                val lp = act.window.attributes
                                lp.screenBrightness = newVal.coerceIn(0.01f, 1f)
                                act.window.attributes = lp
                            }
                        } else if (gestureType == GestureType.VOLUME) {
                            audioManager?.let { am ->
                                val maxVol = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                                val targetVol = (newVal * maxVol).roundToInt().coerceIn(0, maxVol)
                                am.setStreamVolume(AudioManager.STREAM_MUSIC, targetVol, 0)
                            }
                        }
                    }
                )
            }
    ) {
        // Buffering / Loading Indicator in the Center only when controls are HIDDEN
        if (isBuffering && !isControlsVisible) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = CrimsonPlay,
                    modifier = Modifier.size(42.dp),
                    strokeWidth = 3.5.dp
                )
            }
        }

        // Minimalist horizontal Gesture HUD for Brightness & Volume (anti-AI-slop)
        AnimatedVisibility(
            visible = showGestureHud,
            enter = fadeIn(animationSpec = tween(150)),
            exit = fadeOut(animationSpec = tween(200)),
            modifier = Modifier.align(BiasAlignment(0f, -0.70f))
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xCC111116))
                    .border(0.6.dp, Color(0x33FFFFFF), RoundedCornerShape(16.dp))
                    .padding(horizontal = 14.dp, vertical = 7.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    val activeType = gestureType ?: lastActiveGestureType
                    val icon = if (activeType == GestureType.BRIGHTNESS) {
                        Icons.Filled.BrightnessMedium
                    } else {
                        if (gestureValue == 0f) Icons.AutoMirrored.Filled.VolumeMute else Icons.AutoMirrored.Filled.VolumeUp
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = CrimsonPlay,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .width(100.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color(0x33FFFFFF))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(fraction = gestureValue)
                                .background(CrimsonPlay)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "${(gestureValue * 100).roundToInt()}%",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Animated Controls Overlay
        AnimatedVisibility(
            visible = isControlsVisible,
            enter = fadeIn(animationSpec = tween(200)),
            exit = fadeOut(animationSpec = tween(200)),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // TOP BAR with gradient scrim
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.85f),
                                    Color.Transparent
                                )
                            )
                        )
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Back Button
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBackIos,
                                contentDescription = "Kembali",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Drama Title & Episode Info
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp)
                        ) {
                            Text(
                                text = dramaTitle,
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (!episodeTitle.isNullOrBlank()) {
                                Text(
                                    text = episodeTitle,
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                // CENTER CONTROLS (Prev Episode, Rewind 10s, Play/Pause with Buffering, Forward 10s, Next Episode)
                // Tombol tetap muncul meskipun loading agar user bisa combo +10s / -10s
                Row(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Previous Episode (hanya ditampilkan saat fullscreen landscape)
                    if (isFullscreen) {
                        IconButton(
                            onClick = {
                                onPlayPreviousEpisode()
                                registerInteraction()
                            },
                            enabled = hasPreviousEpisode,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.SkipPrevious,
                                contentDescription = "Episode Sebelumnya",
                                tint = if (hasPreviousEpisode) Color.White else Color.White.copy(alpha = 0.35f),
                                modifier = Modifier.size(30.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(20.dp))
                    }

                    // Rewind 10s (-10)
                    IconButton(
                        onClick = {
                            applySeekDelta(-10_000L)
                        },
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Replay10,
                            contentDescription = "Mundur 10 detik",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(if (isFullscreen) 20.dp else 36.dp))

                    // Play/Pause Center Button with Loading Spinner
                    Box(
                        modifier = Modifier.size(56.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isBuffering) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(56.dp),
                                color = CrimsonPlay,
                                strokeWidth = 3.dp
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.5f))
                                .clickable {
                                    onTogglePlayPause()
                                    registerInteraction()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = if (isPlaying) "Jeda" else "Putar",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(if (isFullscreen) 20.dp else 36.dp))

                    // Forward 10s (+10)
                    IconButton(
                        onClick = {
                            applySeekDelta(10_000L)
                        },
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Forward10,
                            contentDescription = "Maju 10 detik",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    // Next Episode (hanya ditampilkan saat fullscreen landscape)
                    if (isFullscreen) {
                        Spacer(modifier = Modifier.width(20.dp))

                        IconButton(
                            onClick = {
                                onPlayNextEpisode()
                                registerInteraction()
                            },
                            enabled = hasNextEpisode,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.SkipNext,
                                contentDescription = "Episode Selanjutnya",
                                tint = if (hasNextEpisode) Color.White else Color.White.copy(alpha = 0.35f),
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }
                }

                // BOTTOM CONTROLS with gradient scrim
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.85f)
                                )
                            )
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    // Inline Row: [Current Time] [Thin Progress Bar with Red Dot] [Total Duration] [Fullscreen Button]
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Current Playback Time (e.g. "02:04")
                        Text(
                            text = formatPlaybackTime(displayedPosition, durationMs),
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        // Sleek Thin Seekbar matching Image 2
                        ThinVideoSeekBar(
                            positionMs = displayedPosition,
                            durationMs = durationMs,
                            onSeekStarted = {
                                isScrubbing = true
                                optimisticSeekPositionMs = null
                                scrubPositionMs = displayedPosition
                                registerInteraction()
                            },
                            onSeekProgress = { progressMs ->
                                scrubPositionMs = progressMs
                                registerInteraction()
                            },
                            onSeekFinished = { finalPositionMs ->
                                isScrubbing = false
                                optimisticSeekPositionMs = finalPositionMs
                                onSeekTo(finalPositionMs)
                                registerInteraction()
                            },
                            modifier = Modifier.weight(1f)
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        // Total Duration (e.g. "45:30")
                        Text(
                            text = formatPlaybackTime(durationMs, durationMs),
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        // Settings Icon: Opens settings menu matching Image 3
                        IconButton(
                            onClick = {
                                if (isSettingsOpen) {
                                    onDismissSettings()
                                } else {
                                    onOpenSettings()
                                }
                                registerInteraction()
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Settings,
                                contentDescription = "Pengaturan",
                                tint = if (isSettingsOpen) CrimsonPlay else Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Fullscreen Toggle Button
                        IconButton(
                            onClick = {
                                onToggleFullscreen()
                                registerInteraction()
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = if (isFullscreen) Icons.Filled.FullscreenExit else Icons.Filled.Fullscreen,
                                contentDescription = if (isFullscreen) "Keluar Layar Penuh" else "Layar Penuh",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        }

        // POP-OVER SETTINGS MENU anchored to bottom-right
        PlayerSettingsMenu(
            isOpen = isSettingsOpen,
            onDismiss = onDismissSettings,
            qualities = qualities,
            selectedQuality = selectedQuality,
            onSelectQuality = onSelectQuality,
            subtitles = subtitles,
            selectedSubtitleId = selectedSubtitleId,
            onSelectSubtitle = onSelectSubtitle,
            playbackSpeed = playbackSpeed,
            onSelectSpeed = onSelectSpeed,
            isAutoNext = isAutoNext,
            onToggleAutoNext = onToggleAutoNext,
            videoZoom = videoZoom,
            onUpdateZoom = onUpdateZoom,
            subtitleStyle = subtitleStyle,
            onSelectFontFamily = onSelectFontFamily,
            onSelectOutlineStyle = onSelectOutlineStyle,
            onUpdateFontSize = onUpdateFontSize,
            onUpdatePosition = onUpdatePosition,
            onUpdateBgOpacity = onUpdateBgOpacity,
            onSetBgOpacity = onSetBgOpacity,
            onUpdateLineSpacing = onUpdateLineSpacing,
            onUpdateBgPadding = onUpdateBgPadding,
            onUpdateTextColor = onUpdateTextColor,
            onUpdateBgColor = onUpdateBgColor
        )
    }
}

/**
 * Sleek, thin video seekbar matching Image 2:
 * Razor-thin 2.5dp line (CrimsonPlay active, semi-transparent white inactive)
 * with a clean 12dp red circular scrubber dot.
 */
@Composable
fun ThinVideoSeekBar(
    positionMs: Long,
    durationMs: Long,
    onSeekStarted: () -> Unit,
    onSeekProgress: (Long) -> Unit,
    onSeekFinished: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var isDragging by remember { mutableStateOf(false) }
    var dragProgressFraction by remember { mutableFloatStateOf(0f) }

    val safeDuration = durationMs.coerceAtLeast(1L)
    val currentFraction = (positionMs.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f)
    val displayFraction = if (isDragging) dragProgressFraction else currentFraction

    val barHeight = 2.5.dp
    val thumbRadius = 6.dp

    Box(
        modifier = modifier
            .height(28.dp)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    isDragging = true
                    val width = size.width.toFloat().coerceAtLeast(1f)
                    val initialFraction = (down.position.x / width).coerceIn(0f, 1f)
                    dragProgressFraction = initialFraction
                    onSeekStarted()
                    onSeekProgress((initialFraction * safeDuration).toLong())

                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: break
                        if (!change.pressed) {
                            change.consume()
                            break
                        }
                        val dragFraction = (change.position.x / width).coerceIn(0f, 1f)
                        dragProgressFraction = dragFraction
                        onSeekProgress((dragFraction * safeDuration).toLong())
                        change.consume()
                    }

                    isDragging = false
                    onSeekFinished((dragProgressFraction * safeDuration).toLong())
                }
            },
        contentAlignment = Alignment.CenterStart
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(barHeight)
        ) {
            val trackHeight = size.height
            val activeWidth = size.width * displayFraction

            // Inactive track (subtle semi-transparent white)
            drawLine(
                color = Color.White.copy(alpha = 0.28f),
                start = Offset(0f, trackHeight / 2),
                end = Offset(size.width, trackHeight / 2),
                strokeWidth = trackHeight,
                cap = StrokeCap.Round
            )

            // Active track (CrimsonPlay)
            if (activeWidth > 0f) {
                drawLine(
                    color = CrimsonPlay,
                    start = Offset(0f, trackHeight / 2),
                    end = Offset(activeWidth, trackHeight / 2),
                    strokeWidth = trackHeight,
                    cap = StrokeCap.Round
                )
            }
        }

        // Circular scrubber red thumb
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(thumbRadius * 2)
        ) {
            val thumbX = size.width * displayFraction
            drawCircle(
                color = CrimsonPlay,
                radius = thumbRadius.toPx(),
                center = Offset(thumbX, size.height / 2)
            )
        }
    }
}

/**
 * Format milliseconds to MM:SS or HH:MM:SS format
 */
private fun formatPlaybackTime(currentMs: Long, totalDurationMs: Long): String {
    val totalSeconds = (currentMs.coerceAtLeast(0L) / 1000L)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60

    val totalDurationSeconds = (totalDurationMs.coerceAtLeast(0L) / 1000L)
    val hasHours = totalDurationSeconds >= 3600

    return if (hasHours) {
        String.format("%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}