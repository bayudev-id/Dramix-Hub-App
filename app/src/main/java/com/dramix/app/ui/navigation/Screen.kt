package com.dramix.app.ui.navigation

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Shorts : Screen("shorts")
    data object LiveTv : Screen("live_tv")
    data object Profile : Screen("profile")
    data object Search : Screen("search")

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
