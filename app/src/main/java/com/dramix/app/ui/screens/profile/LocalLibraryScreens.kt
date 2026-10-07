package com.dramix.app.ui.screens.profile

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkRemove
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.dramix.app.core.database.entity.BookmarkEntity
import com.dramix.app.core.database.entity.DownloadRecordEntity
import com.dramix.app.core.database.entity.WatchHistoryEntity
import com.dramix.app.ui.theme.CrimsonPlay
import com.dramix.app.ui.theme.MidnightBorder
import com.dramix.app.ui.theme.MidnightCard
import com.dramix.app.ui.theme.PureBlack
import com.dramix.app.ui.theme.Slate400
import com.dramix.app.ui.theme.Slate50

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchHistoryBottomSheet(
    historyList: List<WatchHistoryEntity>,
    onItemClick: (WatchHistoryEntity) -> Unit,
    onClearAllClick: () -> Unit,
    onDismissRequest: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = PureBlack,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Slate400.copy(alpha = 0.4f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Riwayat Menonton",
                    color = Slate50,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Row {
                    if (historyList.isNotEmpty()) {
                        IconButton(onClick = onClearAllClick) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "Hapus Semua",
                                tint = Slate400
                            )
                        }
                    }
                    IconButton(onClick = onDismissRequest) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Tutup",
                            tint = Slate400
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (historyList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Belum ada riwayat tontonan",
                        color = Slate400,
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(historyList, key = { it.id }) { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MidnightCard)
                                .border(1.dp, MidnightBorder, RoundedCornerShape(8.dp))
                                .clickable { onItemClick(item) }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(width = 54.dp, height = 72.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(PureBlack)
                            ) {
                                AsyncImage(
                                    model = item.dramaPoster,
                                    contentDescription = item.dramaTitle,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.dramaTitle,
                                    color = Slate50,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                val epLabel = item.episodeTitle ?: "Episode ${item.episodeNumber}"
                                Text(
                                    text = epLabel,
                                    color = CrimsonPlay,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                val posSec = item.positionMs / 1000
                                val durSec = item.durationMs / 1000
                                val progressPercent = if (durSec > 0) ((posSec * 100) / durSec).toInt() else 0

                                Text(
                                    text = "Ditonton $progressPercent% (${posSec / 60}m / ${durSec / 60}m)",
                                    color = Slate400,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarksBottomSheet(
    bookmarks: List<BookmarkEntity>,
    onItemClick: (BookmarkEntity) -> Unit,
    onDeleteClick: (BookmarkEntity) -> Unit,
    onDismissRequest: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = PureBlack,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Slate400.copy(alpha = 0.4f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Daftar Favorit",
                    color = Slate50,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                IconButton(onClick = onDismissRequest) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Tutup",
                        tint = Slate400
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (bookmarks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Belum ada drama favorit tersimpan",
                        color = Slate400,
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(bookmarks, key = { it.id }) { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MidnightCard)
                                .border(1.dp, MidnightBorder, RoundedCornerShape(8.dp))
                                .clickable { onItemClick(item) }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(width = 54.dp, height = 72.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(PureBlack)
                            ) {
                                AsyncImage(
                                    model = item.posterUrl,
                                    contentDescription = item.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.title,
                                    color = Slate50,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    item.rating?.let { rating ->
                                        if (rating.isNotBlank()) {
                                            Text(
                                                text = "★ $rating",
                                                color = Color(0xFFF59E0B),
                                                fontSize = 12.sp
                                            )
                                        }
                                    }

                                    item.totalEpisodes?.let { total ->
                                        Text(
                                            text = "$total Episode",
                                            color = Slate400,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }

                            IconButton(onClick = { onDeleteClick(item) }) {
                                Icon(
                                    imageVector = Icons.Default.BookmarkRemove,
                                    contentDescription = "Hapus Favorit",
                                    tint = Slate400
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsBottomSheet(
    downloads: List<DownloadRecordEntity>,
    onDismissRequest: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = PureBlack,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Slate400.copy(alpha = 0.4f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Manajer Unduhan Offline",
                    color = Slate50,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                IconButton(onClick = onDismissRequest) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Tutup",
                        tint = Slate400
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (downloads.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Belum ada video diunduh",
                        color = Slate400,
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(downloads, key = { it.id }) { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MidnightCard)
                                .border(1.dp, MidnightBorder, RoundedCornerShape(8.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                val title = item.episodeTitle ?: "${item.dramaTitle} - Ep ${item.episodeNumber}"
                                Text(
                                    text = title,
                                    color = Slate50,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = "Status: ${item.status.uppercase()} (${item.progressPercentage}%)",
                                    color = if (item.status == "completed") Color(0xFF10B981) else Slate400,
                                    fontSize = 12.sp
                                )
                            }

                            if (item.status == "completed") {
                                Icon(
                                    imageVector = Icons.Default.DownloadDone,
                                    contentDescription = "Selesai",
                                    tint = Color(0xFF10B981)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ClearCacheConfirmDialog(
    cacheSizeMb: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Bersihkan Cache Video?",
                color = Slate50,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = "Ukuran cache video saat ini adalah $cacheSizeMb. Membersihkan cache hanya menghapus file temporary streaming dan tidak akan menghapus riwayat atau drama favorit Anda.",
                color = Slate400,
                lineHeight = 20.sp
            )
        },
        containerColor = MidnightCard,
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = CrimsonPlay)
            ) {
                Text(text = "Bersihkan", color = Color.White)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text(text = "Batal", color = Slate400)
            }
        }
    )
}
