package com.dramix.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.dramix.app.ui.navigation.Screen
import com.dramix.app.ui.theme.CrimsonPlay
import com.dramix.app.ui.theme.MidnightBorder
import com.dramix.app.ui.theme.PureBlack
import com.dramix.app.ui.theme.Slate400

data class BottomNavItem(
    val title: String,
    val icon: ImageVector,
    val route: String
)

val BottomNavItems = listOf(
    BottomNavItem(title = "Beranda", icon = Icons.Default.Home, route = Screen.Home.route),
    BottomNavItem(title = "Drama Pendek", icon = Icons.Default.PlayCircleOutline, route = Screen.Shorts.route),
    BottomNavItem(title = "Live TV", icon = Icons.Default.Tv, route = Screen.LiveTv.route),
    BottomNavItem(title = "Saya", icon = Icons.Default.Person, route = Screen.Profile.route)
)

@Composable
fun BottomNavigationBar(
    currentRoute: String?,
    onNavigateToRoute: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .background(PureBlack)
            .border(width = 1.dp, color = MidnightBorder),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        BottomNavItems.forEach { item ->
            val isSelected = currentRoute == item.route
            val itemColor = if (isSelected) CrimsonPlay else Slate400

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(64.dp)
                    .touchTargetMin(48.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        if (currentRoute != item.route) {
                            onNavigateToRoute(item.route)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.title,
                        tint = itemColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = item.title,
                        color = itemColor,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}
