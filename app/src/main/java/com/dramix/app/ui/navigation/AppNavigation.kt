package com.dramix.app.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.dramix.app.ui.components.BottomNavigationBar
import com.dramix.app.ui.screens.home.HomeScreen
import com.dramix.app.ui.screens.home.HomeViewModel
import com.dramix.app.ui.screens.player_shorts.ShortsPlayerScreen
import com.dramix.app.ui.screens.player_shorts.ShortsPlayerViewModel
import com.dramix.app.ui.screens.player_tv.LiveTvPlayerScreen
import com.dramix.app.ui.screens.player_tv.LiveTvPlayerViewModel
import com.dramix.app.ui.screens.player_vod.VodPlayerScreen
import com.dramix.app.ui.screens.player_vod.VodPlayerViewModel
import com.dramix.app.ui.theme.PureBlack
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val mainTabs = listOf(
        Screen.Home.route,
        Screen.Shorts.route,
        Screen.LiveTv.route,
        Screen.Profile.route
    )
    val shouldShowBottomBar = currentRoute in mainTabs

    Scaffold(
        bottomBar = {
            if (shouldShowBottomBar) {
                BottomNavigationBar(
                    currentRoute = currentRoute,
                    onNavigateToRoute = { route ->
                        navController.navigate(route) {
                            popUpTo(Screen.Home.route) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(PureBlack)
        ) {
            composable(Screen.Home.route) {
                val homeViewModel: HomeViewModel = koinViewModel()
                HomeScreen(
                    viewModel = homeViewModel,
                    onNavigateToVodPlayer = { providerId, dramaId ->
                        navController.navigate(Screen.VodPlayer.createRoute(providerId, dramaId))
                    },
                    onNavigateToShorts = { providerId, dramaId ->
                        navController.navigate(Screen.Shorts.createRoute(providerId, dramaId))
                    },
                    onNavigateToSearch = {
                        navController.navigate(Screen.Search.route)
                    }
                )
            }

            composable(
                route = Screen.Shorts.route,
                arguments = listOf(
                    navArgument("providerId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                    navArgument("dramaId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { backStackEntry ->
                val providerId = backStackEntry.arguments?.getString("providerId")
                val dramaId = backStackEntry.arguments?.getString("dramaId")
                val shortsViewModel: ShortsPlayerViewModel = koinViewModel { parametersOf(providerId, dramaId) }
                ShortsPlayerScreen(
                    viewModel = shortsViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToProfile = { navController.navigate(Screen.Profile.route) }
                )
            }

            composable(Screen.LiveTv.route) {
                val tvViewModel: LiveTvPlayerViewModel = koinViewModel { parametersOf(null, null) }
                LiveTvPlayerScreen(
                    viewModel = tvViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Profile.route) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = "Profil & Aktivasi Lisensi", color = Color.White)
                }
            }

            composable(
                route = Screen.VodPlayer.route,
                arguments = listOf(
                    navArgument("providerId") { type = NavType.StringType },
                    navArgument("dramaId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val providerId = backStackEntry.arguments?.getString("providerId") ?: ""
                val dramaId = backStackEntry.arguments?.getString("dramaId") ?: ""
                val vodViewModel: VodPlayerViewModel = koinViewModel { parametersOf(providerId, dramaId) }
                VodPlayerScreen(
                    viewModel = vodViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToProfile = { navController.navigate(Screen.Profile.route) }
                )
            }

            composable(
                route = Screen.LiveTvPlayer.route,
                arguments = listOf(
                    navArgument("providerId") { type = NavType.StringType },
                    navArgument("channelId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val providerId = backStackEntry.arguments?.getString("providerId") ?: ""
                val channelId = backStackEntry.arguments?.getString("channelId") ?: ""
                val tvViewModel: LiveTvPlayerViewModel = koinViewModel { parametersOf(providerId, channelId) }
                LiveTvPlayerScreen(
                    viewModel = tvViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Search.route) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = "Pencarian Multi-Provider", color = Color.White)
                }
            }
        }
    }
}
