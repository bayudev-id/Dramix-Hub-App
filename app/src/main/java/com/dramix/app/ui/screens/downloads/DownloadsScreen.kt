package com.dramix.app.ui.screens.downloads

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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import java.util.Locale

@Composable
fun DownloadsScreen(
    viewModel: DownloadsViewModel,
    onNavigateToHome: () -> Unit,
    onPlayDownload: (DownloadRecordEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var recordToDelete by remember { mutableStateOf<DownloadRecordEntity?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PureBlack)
    ) {
        // TopBar / Header
        DownloadsHeader(
            totalCompleted = uiState.completedCount,
            totalDownloading = uiState.downloadingCount
        )

        // Storage & summary banner (only if there are downloads)
        if (uiState.allDownloads.isNotEmpty()) {
            StorageSummaryBanner(
                totalBytes = uiState.totalDownloadedBytes,
                completedCount = uiState.completedCount
            )

            // Filter Tabs Row
            FilterTabsRow(
                selectedFilter = uiState.selectedFilter,
                allCount = uiState.allDownloads.size,
                completedCount = uiState.completedCount,
                downloadingCount = uiState.downloadingCount,
                onFilterSelected = { viewModel.setFilter(it) }
            )

            Spacer(modifier = Modifier.height(8.dp))
        }

        // Content: Empty State or Downloads List
        if (uiState.filteredDownloads.isEmpty()) {
            DownloadsEmptyState(
                filter = uiState.selectedFilter,
                hasAnyDownloads = uiState.allDownloads.isNotEmpty(),
                onNavigateToHome = onNavigateToHome
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiState.filteredDownloads, key = { it.mediaId }) { item ->
                    DownloadCard(
                        item = item,
                        onPlay = { onPlayDownload(item) },
                        onPause = { viewModel.pauseDownload(item) },
                        onResume = { viewModel.resumeDownload(item) },
                        onDelete = { recordToDelete = item }
                    )
                }
            }
        }
    }

    // Confirmation Dialog for Delete
    if (recordToDelete != null) {
        val target = recordToDelete!!
        AlertDialog(
            onDismissRequest = { recordToDelete = null },
            containerColor = MidnightCard,
            title = {
                Text(
                    text = "Hapus Unduhan?",
                    color = Slate50,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Text(
                    text = "Apakah Anda yakin ingin menghapus '${target.dramaTitle}' (${target.episodeTitle ?: "Episode ${target.episodeNumber}"}) dari penyimpanan offline?",
                    color = Slate400,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteDownload(target)
                        recordToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonPlay)
                ) {
                    Text(text = "Hapus", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { recordToDelete = null }) {
                    Text(text = "Batal", color = Slate400)
                }
            }
        )
    }
}

@Composable
private fun DownloadsHeader(
    totalCompleted: Int,
    totalDownloading: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(CrimsonPlay)
            )
            Text(
                text = "Unduhan",
                color = Slate50,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        if (totalDownloading > 0) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(CrimsonPlay.copy(alpha = 0.2f))
                    .border(1.dp, CrimsonPlay, RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "$totalDownloading Mengunduh",
                    color = CrimsonPlay,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun StorageSummaryBanner(
    totalBytes: Long,
    completedCount: Int
) {
    val formattedSize = formatBytes(totalBytes)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MidnightCard)
            .border(1.dp, MidnightBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF10B981).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = null,
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(18.dp)
                    )
                }

                Column {
                    Text(
                        text = "$completedCount Video Tersimpan",
                        color = Slate50,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Ukuran penyimpanan: $formattedSize",
                        color = Slate400,
                        fontSize = 11.sp
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF10B981).copy(alpha = 0.15f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "Offline Siap",
                    color = Color(0xFF10B981),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun FilterTabsRow(
    selectedFilter: DownloadFilterTab,
    allCount: Int,
    completedCount: Int,
    downloadingCount: Int,
    onFilterSelected: (DownloadFilterTab) -> Unit
) {
    val tabs = listOf(
        DownloadFilterTab.ALL to allCount,
        DownloadFilterTab.COMPLETED to completedCount,
        DownloadFilterTab.DOWNLOADING to downloadingCount
    )

    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(tabs) { (filter, count) ->
            val isSelected = filter == selectedFilter
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
                    .clickable { onFilterSelected(filter) }
                    .padding(horizontal = 12.dp, vertical = 5.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${filter.label} ($count)",
                    color = if (isSelected) CrimsonPlay else Slate400,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun DownloadCard(
    item: DownloadRecordEntity,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onDelete: () -> Unit
) {
    val statusUpper = item.status.uppercase()
    val isCompleted = statusUpper == "COMPLETED"
    val isDownloading = statusUpper == "DOWNLOADING"
    val isQueued = statusUpper == "QUEUED"
    val isPaused = statusUpper == "PAUSED"
    val isFailed = statusUpper == "FAILED"

    val statusColor = when {
        isCompleted -> Color(0xFF10B981)
        isDownloading -> CrimsonPlay
        isPaused -> Color(0xFFF59E0B)
        isFailed -> Color(0xFFEF4444)
        else -> Slate400
    }

    val statusLabel = when {
        isCompleted -> "SELESAI"
        isDownloading -> "MENGUNDUH"
        isPaused -> "DIJEDA"
        isQueued -> "MENUNGGU"
        isFailed -> "GAGAL"
        else -> statusUpper
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MidnightCard)
            .border(1.dp, MidnightBorder, RoundedCornerShape(12.dp))
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

                Spacer(modifier = Modifier.height(2.dp))

                val epInfo = item.episodeTitle?.let { "Ep ${item.episodeNumber}: $it" }
                    ?: "Episode ${item.episodeNumber}"
                Text(
                    text = epInfo,
                    color = Slate400,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (item.providerId.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(MidnightBorder.copy(alpha = 0.5f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = item.providerId.uppercase(),
                            color = Slate400,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Status Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(statusColor.copy(alpha = 0.15f))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = statusLabel,
                    color = statusColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Progress bar for ongoing / paused downloads
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
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Delete button (trash can)
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(34.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Hapus",
                    tint = Slate400,
                    modifier = Modifier.size(19.dp)
                )
            }

            // Primary control button (Play, Pause, or Resume)
            when {
                isCompleted -> {
                    Button(
                        onClick = onPlay,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Putar",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Tonton Offline",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                isDownloading || isQueued -> {
                    IconButton(
                        onClick = onPause,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(MidnightBorder)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Pause,
                            contentDescription = "Jeda",
                            tint = Slate50,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }

                isPaused -> {
                    IconButton(
                        onClick = onResume,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF59E0B))
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Lanjutkan",
                            tint = Color.White,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }

                isFailed -> {
                    IconButton(
                        onClick = onResume,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(CrimsonPlay)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Coba Lagi",
                            tint = Color.White,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DownloadsEmptyState(
    filter: DownloadFilterTab,
    hasAnyDownloads: Boolean,
    onNavigateToHome: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(MidnightCard)
                    .border(1.dp, MidnightBorder, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (filter == DownloadFilterTab.COMPLETED) Icons.Default.DownloadDone else Icons.Default.Download,
                    contentDescription = null,
                    tint = Slate400,
                    modifier = Modifier.size(34.dp)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            val title = when {
                !hasAnyDownloads -> "Belum Ada Video Diunduh"
                filter == DownloadFilterTab.COMPLETED -> "Belum Ada Unduhan Selesai"
                filter == DownloadFilterTab.DOWNLOADING -> "Tidak Ada Unduhan Aktif"
                else -> "Belum Ada Unduhan"
            }

            Text(
                text = title,
                color = Slate50,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            val description = when {
                !hasAnyDownloads -> "Unduh episode favorit Anda agar bisa ditonton kapan saja tanpa kuota internet."
                filter == DownloadFilterTab.COMPLETED -> "Video yang telah selesai diunduh akan tersimpan di sini."
                filter == DownloadFilterTab.DOWNLOADING -> "Semua unduhan yang sedang berjalan atau dijeda akan muncul di sini."
                else -> "Video offline Anda akan muncul di tab ini."
            }

            Text(
                text = description,
                color = Slate400,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )

            if (!hasAnyDownloads) {
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onNavigateToHome,
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonPlay),
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = "Jelajahi Beranda",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0L) return "0 MB"
    val mb = bytes.toDouble() / (1024.0 * 1024.0)
    return if (mb >= 1024.0) {
        val gb = mb / 1024.0
        String.format(Locale.US, "%.2f GB", gb)
    } else {
        String.format(Locale.US, "%.1f MB", mb)
    }
}
