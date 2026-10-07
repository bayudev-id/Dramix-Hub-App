package com.dramix.app.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dramix.app.core.database.entity.DownloadRecordEntity
import com.dramix.app.ui.theme.CrimsonPlay
import com.dramix.app.ui.theme.MidnightBorder
import com.dramix.app.ui.theme.MidnightCard
import com.dramix.app.ui.theme.PureBlack
import com.dramix.app.ui.theme.Slate400
import com.dramix.app.ui.theme.Slate50

@Composable
fun DownloadManagerScreen(
    viewModel: ProfileViewModel,
    onNavigateBack: () -> Unit,
    onPlayDownload: (DownloadRecordEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    DownloadManagerContent(
        downloads = uiState.downloads,
        onNavigateBack = onNavigateBack,
        onPauseDownload = { viewModel.pauseDownload(it) },
        onResumeDownload = { viewModel.resumeDownload(it) },
        onDeleteDownload = { viewModel.deleteDownload(it) },
        onPlayDownload = onPlayDownload,
        modifier = modifier
    )
}

@Composable
fun DownloadManagerContent(
    downloads: List<DownloadRecordEntity>,
    onNavigateBack: () -> Unit,
    onPauseDownload: (DownloadRecordEntity) -> Unit,
    onResumeDownload: (DownloadRecordEntity) -> Unit,
    onDeleteDownload: (DownloadRecordEntity) -> Unit,
    onPlayDownload: (DownloadRecordEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PureBlack)
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Kembali",
                    tint = Slate50
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            Text(
                text = "Manajer Unduhan",
                color = Slate50,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        if (downloads.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(MidnightCard),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            tint = Slate400,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Belum Ada Unduhan Offline",
                        color = Slate50,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Episode yang Anda unduh akan muncul di sini dan dapat diputar tanpa kuota internet.",
                        color = Slate400,
                        fontSize = 12.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(downloads, key = { it.id }) { item ->
                    DownloadItemCard(
                        item = item,
                        onPause = { onPauseDownload(item) },
                        onResume = { onResumeDownload(item) },
                        onDelete = { onDeleteDownload(item) },
                        onPlay = { onPlayDownload(item) }
                    )
                }
            }
        }
    }
}

@Composable
fun DownloadItemCard(
    item: DownloadRecordEntity,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onDelete: () -> Unit,
    onPlay: () -> Unit,
    modifier: Modifier = Modifier
) {
    val statusUpper = item.status.uppercase()
    val isCompleted = statusUpper == "COMPLETED"
    val isDownloading = statusUpper == "DOWNLOADING"
    val isQueued = statusUpper == "QUEUED"
    val isPaused = statusUpper == "PAUSED"
    val isFailed = statusUpper == "FAILED"

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MidnightCard)
            .border(1.dp, MidnightBorder, RoundedCornerShape(10.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.dramaTitle,
                    color = Slate50,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                val epInfo = item.episodeTitle?.let { "Ep ${item.episodeNumber}: $it" }
                    ?: "Episode ${item.episodeNumber}"
                Text(
                    text = epInfo,
                    color = Slate400,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Status Badge
            val statusColor = when {
                isCompleted -> Color(0xFF10B981)
                isDownloading -> CrimsonPlay
                isPaused -> Color(0xFFF59E0B)
                isFailed -> Color(0xFFEF4444)
                else -> Slate400
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(statusColor.copy(alpha = 0.15f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = statusUpper,
                    color = statusColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Progress bar for active / paused downloads
        if (isDownloading || isQueued || isPaused) {
            Spacer(modifier = Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { item.progressPercentage / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = if (isPaused) Color(0xFFF59E0B) else CrimsonPlay,
                trackColor = MidnightBorder
            )

            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${item.progressPercentage}%",
                    color = Slate400,
                    fontSize = 11.sp
                )
                if (item.totalBytes > 0) {
                    val downloadedMb = item.bytesDownloaded / (1024 * 1024)
                    val totalMb = item.totalBytes / (1024 * 1024)
                    Text(
                        text = "$downloadedMb MB / $totalMb MB",
                        color = Slate400,
                        fontSize = 11.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Action Buttons Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Delete button (always available)
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Hapus",
                    tint = Slate400,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            when {
                isCompleted -> {
                    // Play Button
                    IconButton(
                        onClick = onPlay,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF10B981))
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Putar",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                isDownloading || isQueued -> {
                    // Pause Button
                    IconButton(
                        onClick = onPause,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MidnightBorder)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Pause,
                            contentDescription = "Jeda",
                            tint = Slate50,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                isPaused -> {
                    // Resume Button
                    IconButton(
                        onClick = onResume,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(CrimsonPlay)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Lanjutkan",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                isFailed -> {
                    // Retry Button
                    IconButton(
                        onClick = onResume,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEF4444))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Coba Lagi",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
