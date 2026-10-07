package com.dramix.app.ui.screens.profile

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dramix.app.core.database.dao.BookmarkDao
import com.dramix.app.core.database.dao.DownloadRecordDao
import com.dramix.app.core.database.dao.WatchHistoryDao
import com.dramix.app.core.database.entity.BookmarkEntity
import com.dramix.app.core.database.entity.DownloadRecordEntity
import com.dramix.app.core.database.entity.WatchHistoryEntity
import com.dramix.app.core.security.DeviceIdentifier
import com.dramix.app.data.source.local.LicensePreferences
import com.dramix.app.domain.repository.LicenseRepository
import com.dramix.app.player.engine.CacheManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ProfileUiState(
    val deviceIdFormatted: String = "DRM-UNKNOWN",
    val userDisplayName: String = "Pengguna Tamu",
    val isVip: Boolean = false,
    val expiresAtFormatted: String? = null,
    val planName: String? = null,
    val licenseKeyInput: String = "",
    val isActivating: Boolean = false,
    val activationSuccessMessage: String? = null,
    val activationErrorMessage: String? = null,
    val cacheSizeMb: String = "0.0 MB",
    val watchHistory: List<WatchHistoryEntity> = emptyList(),
    val bookmarks: List<BookmarkEntity> = emptyList(),
    val downloads: List<DownloadRecordEntity> = emptyList(),
    val showHistorySheet: Boolean = false,
    val showBookmarksSheet: Boolean = false,
    val showDownloadsSheet: Boolean = false,
    val showClearCacheDialog: Boolean = false
)

class ProfileViewModel(
    private val licenseRepository: LicenseRepository,
    private val licensePreferences: LicensePreferences,
    private val deviceIdentifier: DeviceIdentifier,
    private val watchHistoryDao: WatchHistoryDao,
    private val bookmarkDao: BookmarkDao,
    private val downloadRecordDao: DownloadRecordDao,
    private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        initializeDeviceInfo()
        observeVipStatus()
        observeLocalLibrary()
        refreshCacheSize()
    }

    private fun initializeDeviceInfo() {
        val rawDeviceId = deviceIdentifier.getDeviceId()
        val formatted = if (rawDeviceId.length >= 8) {
            "DRM-${rawDeviceId.substring(0, 4).uppercase()}-${rawDeviceId.substring(4, 8).uppercase()}"
        } else {
            "DRM-${rawDeviceId.uppercase()}"
        }

        val suffix = if (rawDeviceId.length >= 4) rawDeviceId.takeLast(4).uppercase() else "0000"
        val displayName = "Dramix User #$suffix"

        _uiState.value = _uiState.value.copy(
            deviceIdFormatted = formatted,
            userDisplayName = displayName
        )
    }

    private fun observeVipStatus() {
        viewModelScope.launch {
            licenseRepository.observeVipStatus().collect { isVip ->
                val expiresAt = licensePreferences.getExpiresAt()
                val plan = licensePreferences.getPlanName()
                val expFormatted = if (expiresAt != null && expiresAt > 0L) {
                    val sdf = SimpleDateFormat("dd MMM yyyy", Locale.forLanguageTag("id-ID"))
                    "Aktif s/d ${sdf.format(Date(expiresAt))}"
                } else if (isVip) {
                    "VIP Lifetime Aktif"
                } else {
                    null
                }

                _uiState.value = _uiState.value.copy(
                    isVip = isVip,
                    expiresAtFormatted = expFormatted,
                    planName = plan
                )
            }
        }
    }

    private fun observeLocalLibrary() {
        viewModelScope.launch {
            watchHistoryDao.getLatestWatchedDramas(50).collect { list ->
                _uiState.value = _uiState.value.copy(watchHistory = list)
            }
        }

        viewModelScope.launch {
            bookmarkDao.getAllBookmarks().collect { list ->
                _uiState.value = _uiState.value.copy(bookmarks = list)
            }
        }

        viewModelScope.launch {
            downloadRecordDao.getAllDownloads().collect { list ->
                _uiState.value = _uiState.value.copy(downloads = list)
            }
        }
    }

    fun onLicenseKeyChange(key: String) {
        _uiState.value = _uiState.value.copy(
            licenseKeyInput = key,
            activationErrorMessage = null,
            activationSuccessMessage = null
        )
    }

    fun activateLicense() {
        val key = _uiState.value.licenseKeyInput.trim()
        if (key.isBlank() || _uiState.value.isActivating) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isActivating = true,
                activationErrorMessage = null,
                activationSuccessMessage = null
            )

            val result = licenseRepository.activateLicense(key)
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    isActivating = false,
                    licenseKeyInput = "",
                    activationSuccessMessage = "Lisensi VIP Berhasil Diaktifkan!",
                    activationErrorMessage = null
                )
            } else {
                val err = result.exceptionOrNull()?.localizedMessage ?: "Gagal mengaktivasi lisensi"
                _uiState.value = _uiState.value.copy(
                    isActivating = false,
                    activationErrorMessage = err,
                    activationSuccessMessage = null
                )
            }
        }
    }

    fun refreshCacheSize() {
        viewModelScope.launch {
            val bytes = CacheManager.getCacheSizeBytes(context)
            val mb = bytes.toDouble() / (1024 * 1024)
            _uiState.value = _uiState.value.copy(
                cacheSizeMb = String.format(Locale.US, "%.1f MB", mb)
            )
        }
    }

    fun clearCache() {
        viewModelScope.launch {
            CacheManager.clearCache(context)
            _uiState.value = _uiState.value.copy(
                cacheSizeMb = "0.0 MB",
                showClearCacheDialog = false
            )
        }
    }

    fun deleteBookmark(bookmark: BookmarkEntity) {
        viewModelScope.launch {
            bookmarkDao.deleteBookmark(bookmark.dramaId, bookmark.providerId)
        }
    }

    fun clearAllWatchHistory() {
        viewModelScope.launch {
            watchHistoryDao.clearAllHistory()
        }
    }

    fun openWatchHistory() {
        _uiState.value = _uiState.value.copy(showHistorySheet = true)
    }

    fun closeWatchHistory() {
        _uiState.value = _uiState.value.copy(showHistorySheet = false)
    }

    fun openBookmarks() {
        _uiState.value = _uiState.value.copy(showBookmarksSheet = true)
    }

    fun closeBookmarks() {
        _uiState.value = _uiState.value.copy(showBookmarksSheet = false)
    }

    fun openDownloads() {
        _uiState.value = _uiState.value.copy(showDownloadsSheet = true)
    }

    fun closeDownloads() {
        _uiState.value = _uiState.value.copy(showDownloadsSheet = false)
    }

    fun openClearCacheDialog() {
        refreshCacheSize()
        _uiState.value = _uiState.value.copy(showClearCacheDialog = true)
    }

    fun dismissClearCacheDialog() {
        _uiState.value = _uiState.value.copy(showClearCacheDialog = false)
    }

    fun dismissSuccessMessage() {
        _uiState.value = _uiState.value.copy(activationSuccessMessage = null)
    }
}
