package com.dramix.app.ui.navigation

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Shorts : Screen("shorts?providerId={providerId}&dramaId={dramaId}") {
        fun createRoute(providerId: String? = null, dramaId: String? = null): String {
            return if (!providerId.isNullOrBlank() && !dramaId.isNullOrBlank()) {
                "shorts?providerId=$providerId&dramaId=$dramaId"
            } else {
                "shorts"
            }
        }
    }
    data object LiveTv : Screen("live_tv")
    data object Profile : Screen("profile")
    data object Search : Screen("search")
    data object DownloadManager : Screen("download_manager")

    data object VodPlayer : Screen("vod_player/{providerId}/{dramaId}") {
        fun createRoute(providerId: String, dramaId: String): String {
            return "vod_player/$providerId/$dramaId"
        }
    }

    data object LiveTvPlayer : Screen("live_tv_player/{providerId}/{channelId}") {
        fun createRoute(providerId: String, channelId: String): String {
            return "live_tv_player/$providerId/$channelId"
        }
    }
}
