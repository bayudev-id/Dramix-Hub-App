package com.dramix.app.ui.components

import android.view.LayoutInflater
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.dramix.app.ui.screens.player_vod.SubtitleStyleConfig

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerSurface(
    player: Player?,
    modifier: Modifier = Modifier,
    useController: Boolean = false,
    subtitleStyle: SubtitleStyleConfig = SubtitleStyleConfig(),
    videoZoom: String = "100%"
) {
    val context = LocalContext.current
    val playerView = remember {
        val view = LayoutInflater.from(context)
            .inflate(com.dramix.app.R.layout.item_player_view, null, false) as PlayerView
        view.apply {
            keepScreenOn = true
            this.useController = useController
            setShowBuffering(if (useController) PlayerView.SHOW_BUFFERING_WHEN_PLAYING else PlayerView.SHOW_BUFFERING_NEVER)
            this.subtitleView?.visibility = android.view.View.GONE
        }
    }

    DisposableEffect(player) {
        playerView.player = player
        playerView.subtitleView?.visibility = android.view.View.GONE

        onDispose {
            playerView.player = null
        }
    }

    DisposableEffect(useController) {
        playerView.useController = useController
        playerView.setShowBuffering(if (useController) PlayerView.SHOW_BUFFERING_WHEN_PLAYING else PlayerView.SHOW_BUFFERING_NEVER)
        onDispose {}
    }

    val zoomScale = remember(videoZoom) {
        if (videoZoom.endsWith("%")) {
            val pct = videoZoom.removeSuffix("%").trim().toFloatOrNull() ?: 100f
            (pct / 100f).coerceIn(1.0f, 3.0f)
        } else {
            1.0f
        }
    }

    LaunchedEffect(videoZoom) {
        when {
            videoZoom.equals("Crop to Fill", ignoreCase = true) -> {
                playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            }
            videoZoom.equals("Fit to Screen", ignoreCase = true) -> {
                playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
            }
            else -> {
                playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
            }
        }
    }

    Box(
        modifier = modifier
            .clipToBounds()
            .background(Color.Black)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds()
        ) {
            AndroidView(
                factory = { playerView },
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = zoomScale
                        scaleY = zoomScale
                    }
            )
        }
    }
}
