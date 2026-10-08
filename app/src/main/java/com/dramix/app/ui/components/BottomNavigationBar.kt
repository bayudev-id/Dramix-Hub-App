package com.dramix.app.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.dramix.app.ui.navigation.Screen
import com.dramix.app.ui.theme.CrimsonPlay
import com.dramix.app.ui.theme.GoldVip
import com.dramix.app.ui.theme.MidnightBorder
import com.dramix.app.ui.theme.PureBlack
import com.dramix.app.ui.theme.Slate400

data class BottomNavItem(
    val title: String,
    val icon: ImageVector,
    val route: String,
    val destinationRoute: String = route,
    val isProfile: Boolean = false
)

val BottomNavItems = listOf(
    BottomNavItem(
        title = "Beranda",
        icon = Icons.Default.Home,
        route = Screen.Home.route,
        destinationRoute = Screen.Home.route
    ),
    BottomNavItem(
        title = "Drama Pendek",
        icon = Icons.Default.PlayCircleOutline,
        route = Screen.Shorts.createRoute(),
        destinationRoute = Screen.Shorts.route
    ),
    BottomNavItem(
        title = "Live TV",
        icon = Icons.Default.Tv,
        route = Screen.LiveTv.route,
        destinationRoute = Screen.LiveTv.route
    ),
    BottomNavItem(
        title = "Saya",
        icon = Icons.Default.Person,
        route = Screen.Profile.route,
        destinationRoute = Screen.Profile.route,
        isProfile = true
    )
)

@Composable
fun BottomNavigationBar(
    currentRoute: String?,
    onNavigateToRoute: (String) -> Unit,
    modifier: Modifier = Modifier,
    isVip: Boolean = false
) {
    NavigationBar(
        modifier = modifier
            .fillMaxWidth()
            .border(width = 1.dp, color = MidnightBorder),
        containerColor = PureBlack,
        contentColor = Slate400,
        tonalElevation = 0.dp,
        windowInsets = NavigationBarDefaults.windowInsets
    ) {
        BottomNavItems.forEach { item ->
            val isSelected = currentRoute == item.destinationRoute ||
                currentRoute == item.route ||
                (item.destinationRoute.startsWith("shorts") && currentRoute?.startsWith("shorts") == true)

            NavigationBarItem(
                selected = isSelected,
                onClick = {
                    if (!isSelected) {
                        onNavigateToRoute(item.route)
                    }
                },
                icon = {
                    if (item.isProfile && isVip) {
                        BadgedBox(
                            badge = {
                                Badge(
                                    containerColor = GoldVip,
                                    modifier = Modifier.size(6.dp)
                                )
                            }
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.title,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    } else {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.title,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                label = {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = CrimsonPlay,
                    selectedTextColor = CrimsonPlay,
                    unselectedIconColor = Slate400,
                    unselectedTextColor = Slate400,
                    indicatorColor = Color.Transparent
                )
            )
        }
    }
}
