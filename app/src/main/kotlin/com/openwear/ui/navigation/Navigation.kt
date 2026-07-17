package com.openwear.ui.navigation

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*

sealed class Screen(val route: String) {
    object Today : Screen("today")
    object Fitness : Screen("fitness")
    object Sleep : Screen("sleep")
    object Health : Screen("health")
    object Device : Screen("device")
    object Settings : Screen("settings")
    object Debug : Screen("debug")
}

data class BottomNavItem(
    val screen: Screen,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

val bottomNavItems = listOf(
    BottomNavItem(Screen.Today, "Today", Icons.Filled.Home, Icons.Outlined.Home),
    BottomNavItem(Screen.Fitness, "Fitness", Icons.Filled.FitnessCenter, Icons.Outlined.FitnessCenter),
    BottomNavItem(Screen.Sleep, "Sleep", Icons.Filled.Bedtime, Icons.Outlined.Bedtime),
    BottomNavItem(Screen.Health, "Health", Icons.Filled.Favorite, Icons.Outlined.FavoriteBorder),
    BottomNavItem(Screen.Device, "Device", Icons.Filled.Watch, Icons.Outlined.Watch),
)
