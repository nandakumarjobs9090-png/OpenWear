package com.openwear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material.icons.outlined.Watch
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.openwear.ui.activity.ActivityScreen
import com.openwear.ui.debug.DebugScreen
import com.openwear.ui.device.DeviceScreen
import com.openwear.ui.health.HealthScreen
import com.openwear.ui.home.HomeScreen
import com.openwear.ui.navigation.Screen
import com.openwear.ui.navigation.bottomNavItems
import com.openwear.ui.sleep.SleepScreen
import com.openwear.ui.theme.OpenWearTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OpenWearTheme {
                OpenWearMainApp()
            }
        }
    }
}

@Composable
fun OpenWearMainApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val showBottomBar = currentDestination?.route in bottomNavItems.map { it.screen.route }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomBar,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                OpenWearBottomBar(
                    currentDestination = currentDestination,
                    onNavigate = { screen ->
                        navController.navigate(screen.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
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
            startDestination = Screen.Today.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            enterTransition = {
                fadeIn(animationSpec = tween(200)) +
                slideInHorizontally(animationSpec = tween(200)) { it / 10 }
            },
            exitTransition = {
                fadeOut(animationSpec = tween(200)) +
                slideOutHorizontally(animationSpec = tween(200)) { -it / 10 }
            },
            popEnterTransition = {
                fadeIn(animationSpec = tween(200)) +
                slideInHorizontally(animationSpec = tween(200)) { -it / 10 }
            },
            popExitTransition = {
                fadeOut(animationSpec = tween(200)) +
                slideOutHorizontally(animationSpec = tween(200)) { it / 10 }
            }
        ) {
            composable(Screen.Today.route) {
                HomeScreen(
                    onNavigateToDevice = { navController.navigate(Screen.Device.route) }
                )
            }
            composable(Screen.Fitness.route) {
                ActivityScreen()
            }
            composable(Screen.Sleep.route) {
                SleepScreen()
            }
            composable(Screen.Health.route) {
                HealthScreen()
            }
            composable(Screen.Device.route) {
                DeviceScreen(
                    onNavigateToDebug = { navController.navigate(Screen.Debug.route) }
                )
            }
            composable(Screen.Debug.route) {
                DebugScreen()
            }
        }
    }
}

@Composable
private fun OpenWearBottomBar(
    currentDestination: androidx.navigation.NavDestination?,
    onNavigate: (Screen) -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        bottomNavItems.forEach { item ->
            val isSelected = currentDestination?.hierarchy?.any {
                it.route == item.screen.route
            } == true

            NavigationBarItem(
                icon = {
                    Crossfade(targetState = isSelected, label = "nav_icon") { selected ->
                        Icon(
                            imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                            contentDescription = item.label
                        )
                    }
                },
                label = { Text(item.label) },
                selected = isSelected,
                onClick = { onNavigate(item.screen) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }

        // Device tab (not in main bottom nav items, separate icon)
        val isDeviceSelected = currentDestination?.route == Screen.Device.route
        NavigationBarItem(
            icon = {
                Icon(
                    imageVector = if (isDeviceSelected)
                        Icons.Filled.Watch
                    else
                        Icons.Outlined.Watch,
                    contentDescription = "Device"
                )
            },
            label = { Text("Device") },
            selected = isDeviceSelected,
            onClick = { onNavigate(Screen.Device) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                indicatorColor = MaterialTheme.colorScheme.primaryContainer
            )
        )
    }
}
