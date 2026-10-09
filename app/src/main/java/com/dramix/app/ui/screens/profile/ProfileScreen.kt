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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dramix.app.ui.theme.CrimsonPlay
import com.dramix.app.ui.theme.MidnightBorder
import com.dramix.app.ui.theme.MidnightCard
import com.dramix.app.ui.theme.PureBlack
import com.dramix.app.ui.theme.Slate400
import com.dramix.app.ui.theme.Slate50

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onNavigateToVodPlayer: (providerId: String, dramaId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.activationSuccessMessage) {
        uiState.activationSuccessMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.dismissSuccessMessage()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PureBlack)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Screen Header Title
            item {
                Text(
                    text = "Akun & Lisensi",
                    color = Slate50,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            // Device Identity Card
            item {
                DeviceIdentityCard(
                    displayName = uiState.userDisplayName,
                    deviceIdFormatted = uiState.deviceIdFormatted,
                    isVip = uiState.isVip,
                    expiresAtFormatted = uiState.expiresAtFormatted
                )
            }

            // License Card: Show Active Status or Activation Form
            if (uiState.isVip) {
                item {
                    VipActiveCard(
                        planName = uiState.planName ?: "VIP Member",
                        expiresAtFormatted = uiState.expiresAtFormatted
                    )
                }
            } else {
                item {
                    LicenseActivationCard(
                        licenseKey = uiState.licenseKeyInput,
                        onLicenseKeyChange = { viewModel.onLicenseKeyChange(it) },
                        isActivating = uiState.isActivating,
                        onActivateClick = { viewModel.activateLicense() },
                        errorMessage = uiState.activationErrorMessage
                    )
                }
            }

            // Local Library Section Header
            item {
                Text(
                    text = "Perpustakaan Lokal",
                    color = Slate50,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            // Menu Items
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MidnightCard)
                        .border(1.dp, MidnightBorder, RoundedCornerShape(12.dp))
                ) {
                    ProfileMenuItem(
                        icon = Icons.Default.History,
                        title = "Riwayat Menonton",
                        subtitle = "${uiState.watchHistory.size} tontonan tersimpan",
                        onClick = { viewModel.openWatchHistory() }
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(MidnightBorder)
                    )

                    ProfileMenuItem(
                        icon = Icons.Default.Bookmark,
                        title = "Daftar Favorit",
                        subtitle = "${uiState.bookmarks.size} drama tersimpan",
                        onClick = { viewModel.openBookmarks() }
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(MidnightBorder)
                    )

                    ProfileMenuItem(
                        icon = Icons.Default.Download,
                        title = "Manajer Unduhan",
                        subtitle = "${uiState.downloads.size} video offline",
                        onClick = { viewModel.openDownloads() }
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(MidnightBorder)
                    )

                    ProfileMenuItem(
                        icon = Icons.Default.CleaningServices,
                        title = "Pembersihan Cache",
                        subtitle = "Ukuran cache: ${uiState.cacheSizeMb}",
                        onClick = { viewModel.openClearCacheDialog() }
                    )
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        )

        // Bottom Sheets & Dialogs
        if (uiState.showHistorySheet) {
            WatchHistoryBottomSheet(
                historyList = uiState.watchHistory,
                onItemClick = { item ->
                    viewModel.closeWatchHistory()
                    onNavigateToVodPlayer(item.providerId, item.dramaId)
                },
                onClearAllClick = { viewModel.clearAllWatchHistory() },
                onDismissRequest = { viewModel.closeWatchHistory() }
            )
        }

        if (uiState.showBookmarksSheet) {
            BookmarksBottomSheet(
                bookmarks = uiState.bookmarks,
                onItemClick = { item ->
                    viewModel.closeBookmarks()
                    onNavigateToVodPlayer(item.providerId, item.dramaId)
                },
                onDeleteClick = { item -> viewModel.deleteBookmark(item) },
                onDismissRequest = { viewModel.closeBookmarks() }
            )
        }

        if (uiState.showDownloadsSheet) {
            DownloadsBottomSheet(
                downloads = uiState.downloads,
                onDismissRequest = { viewModel.closeDownloads() },
                onPauseDownload = { viewModel.pauseDownload(it) },
                onResumeDownload = { viewModel.resumeDownload(it) },
                onDeleteDownload = { viewModel.deleteDownload(it) },
                onPlayDownload = { record ->
                    onNavigateToVodPlayer(record.providerId, record.dramaId)
                }
            )
        }

        if (uiState.showClearCacheDialog) {
            ClearCacheConfirmDialog(
                cacheSizeMb = uiState.cacheSizeMb,
                onConfirm = { viewModel.clearCache() },
                onDismiss = { viewModel.dismissClearCacheDialog() }
            )
        }
    }
}

@Composable
private fun VipActiveCard(
    planName: String,
    expiresAtFormatted: String?
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                androidx.compose.ui.graphics.Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFF1F2937),
                        Color(0xFF374151)
                    )
                )
            )
            .border(
                width = 1.5.dp,
                color = Color(0xFFF59E0B).copy(alpha = 0.4f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(16.dp)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFFF59E0B).copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Verified,
                        contentDescription = "VIP Active",
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Text(
                    text = planName,
                    color = Slate50,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Akses Premium Penuh",
                color = Slate400,
                fontSize = 13.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFF59E0B).copy(alpha = 0.12f))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = expiresAtFormatted ?: "Lifetime Access",
                    color = Color(0xFFFBBF24),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun DeviceIdentityCard(
    displayName: String,
    deviceIdFormatted: String,
    isVip: Boolean,
    expiresAtFormatted: String?
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MidnightCard)
            .border(1.dp, MidnightBorder, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Minimalist Default Avatar
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(PureBlack)
                    .border(1.dp, MidnightBorder, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Avatar Pengguna",
                    tint = Slate400,
                    modifier = Modifier.size(30.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = displayName,
                        color = Slate50,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    if (isVip) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = "VIP Terverifikasi",
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "ID Perangkat: $deviceIdFormatted",
                    color = Slate400,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                // VIP Status Badge
                if (isVip) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFFF59E0B).copy(alpha = 0.2f))
                            .border(1.dp, Color(0xFFF59E0B), RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = expiresAtFormatted ?: "VIP MEMBER",
                            color = Color(0xFFF59E0B),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(PureBlack)
                            .border(1.dp, MidnightBorder, RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "FREE (Ep. 1–3 Gratis)",
                            color = Slate400,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileMenuItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(PureBlack),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = CrimsonPlay,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Slate50,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = Slate400,
                fontSize = 12.sp
            )
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = null,
            tint = Slate400.copy(alpha = 0.6f),
            modifier = Modifier.size(14.dp)
        )
    }
}
