package dev.proxylink.ui.nav

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import dev.proxylink.ui.onboarding.UpgradeScreen
import dev.proxylink.ui.onboarding.UpgradeViewModel
import dev.proxylink.ui.screens.ClipboardScreen
import dev.proxylink.ui.screens.HomeScreen
import dev.proxylink.ui.screens.SettingsScreen

const val UPGRADE_ROUTE = "upgrade"

enum class Tab(val route: String, val label: String, val icon: ImageVector) {
    HOME("home", "home", Icons.Default.Home),
    CLIP("clip", "clipboard", Icons.Default.ContentPaste),
    SETTINGS("settings", "settings", Icons.Default.Settings),
}

@Composable
fun ProxylinkApp(vm: UpgradeViewModel = viewModel()) {
    val nav = rememberNavController()
    val seen by vm.hasSeen.collectAsStateWithLifecycle()

    // decided once from the stored flag, so the graph is not rebuilt when the flag flips after "continue free"
    var start by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(seen) {
        if (start == null && seen != null) start = if (seen == true) Tab.HOME.route else UPGRADE_ROUTE
    }
    val startRoute = start
    if (startRoute == null) {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
        return
    }

    val current = nav.currentBackStackEntryAsState().value?.destination?.route

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (current != null && current != UPGRADE_ROUTE) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                    Tab.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = current == tab.route,
                            onClick = {
                                nav.navigate(tab.route) {
                                    popUpTo(Tab.HOME.route) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                                indicatorColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.secondary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = startRoute,
            modifier = Modifier.padding(padding),
            enterTransition = { fadeIn(tween(250)) + slideInHorizontally(tween(250)) { it / 8 } },
            exitTransition = { fadeOut(tween(180)) },
        ) {
            composable(Tab.HOME.route) { HomeScreen() }
            composable(Tab.CLIP.route) { ClipboardScreen() }
            composable(Tab.SETTINGS.route) {
                SettingsScreen(onOpenPro = { nav.navigate(UPGRADE_ROUTE) { launchSingleTop = true } })
            }
            composable(UPGRADE_ROUTE) {
                UpgradeScreen(onContinueFree = {
                    vm.continueFree()
                    if (nav.previousBackStackEntry != null) {
                        nav.popBackStack()          // opened from settings: just go back
                    } else {
                        nav.navigate(Tab.HOME.route) {   // first launch: replace the upgrade screen with home
                            popUpTo(UPGRADE_ROUTE) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                })
            }
        }
    }
}
