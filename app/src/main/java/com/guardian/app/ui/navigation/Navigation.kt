package com.guardian.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.guardian.app.ui.screens.*
import com.guardian.app.viewmodel.GuardianViewModel

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Home : Screen("home", "Shield", Icons.Default.Shield)
    object Blacklist : Screen("blacklist", "Block", Icons.Default.Block)
    object Events : Screen("events", "Events", Icons.Default.History)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
}

val screens = listOf(Screen.Home, Screen.Blacklist, Screen.Events, Screen.Settings)

@Composable
fun GuardianNavigation() {
    val navController = rememberNavController()
    val viewModel: GuardianViewModel = viewModel()
    
    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                
                screens.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.title) },
                        label = { Text(screen.title) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) { HomeScreen(viewModel) }
            composable(Screen.Blacklist.route) { BlacklistScreen(viewModel) }
            composable(Screen.Events.route) { EventsScreen(viewModel) }
            composable(Screen.Settings.route) { SettingsScreen(viewModel) }
        }
    }
}
