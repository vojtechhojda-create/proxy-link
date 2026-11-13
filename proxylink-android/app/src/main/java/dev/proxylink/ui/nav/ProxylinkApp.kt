package dev.proxylink.ui.nav

import androidx.compose.animation.core.tween
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import dev.proxylink.R
import dev.proxylink.ui.theme.UiStore
import dev.proxylink.core.Link
import dev.proxylink.ui.components.fmtSize
import dev.proxylink.ui.screens.DashboardScreen
import dev.proxylink.ui.screens.NfcScreen
import dev.proxylink.ui.screens.ScannerScreen
import dev.proxylink.ui.screens.SettingsScreen
import dev.proxylink.ui.screens.SettingsSection
import dev.proxylink.ui.screens.ShareScreen

private const val ROUTE_SCAN = "scan"
private const val ROUTE_NFC = "nfc"
private const val ROUTE_SECTION = "settings/{section}"

enum class Tab(val route: String, @StringRes val label: Int, val icon: ImageVector) {
    DASHBOARD("dashboard", R.string.tab_dashboard, Icons.Default.Dashboard),
    SHARE("share", R.string.tab_share, Icons.Default.Share),
    SETTINGS("settings", R.string.settings, Icons.Default.Settings),
}

private fun NavHostController.openTab(tab: Tab) = navigate(tab.route) {
    popUpTo(Tab.DASHBOARD.route) { saveState = true }
    launchSingleTop = true
    restoreState = true
}

@Composable
fun ProxylinkApp() {
    val nav = rememberNavController()
    val current = nav.currentBackStackEntryAsState().value?.destination?.route

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (current != null && current != ROUTE_SCAN && current != ROUTE_NFC) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                    Tab.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = current == tab.route || current.startsWith(tab.route + "/"),
                            onClick = { nav.openTab(tab) },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(stringResource(tab.label)) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = Tab.DASHBOARD.route,
            modifier = Modifier.padding(padding),
            // with "remove animations" on in the system the screens change at once
            enterTransition = { if (UiStore.animations) fadeIn(tween(180)) + slideInHorizontally(tween(180)) { it / 14 } else EnterTransition.None },
            exitTransition = { if (UiStore.animations) fadeOut(tween(120)) else ExitTransition.None },
            popEnterTransition = { if (UiStore.animations) fadeIn(tween(180)) else EnterTransition.None },
            popExitTransition = { if (UiStore.animations) fadeOut(tween(120)) + slideOutHorizontally(tween(120)) { it / 14 } else ExitTransition.None },
        ) {
            composable(Tab.DASHBOARD.route) {
                DashboardScreen(onScan = { nav.navigate(ROUTE_SCAN) }, onNfc = { nav.navigate(ROUTE_NFC) }, onShowAll = { nav.openTab(Tab.SHARE) })
            }
            composable(Tab.SHARE.route) { ShareScreen() }
            composable(Tab.SETTINGS.route) { SettingsScreen(onOpen = { nav.navigate("settings/$it") }) }
            composable(ROUTE_SECTION) { entry ->
                SettingsSection(
                    section = entry.arguments?.getString("section") ?: "",
                    onBack = { nav.popBackStack() },
                    onScan = { nav.navigate(ROUTE_SCAN) },
                    onOpen = { nav.navigate("settings/$it") },
                )
            }
            composable(ROUTE_NFC) {
                NfcScreen(
                    onClose = { nav.popBackStack() },
                    onScan = { nav.popBackStack(); nav.navigate(ROUTE_SCAN) },
                )
            }
            composable(ROUTE_SCAN) {
                ScannerScreen(
                    onResult = { raw -> Link.pairFromQr(raw) },
                    onClose = { nav.popBackStack() },
                )
            }
        }
    }

    // a request that needs approval (when "accept automatically" is off), shown on any screen
    Link.requests.firstOrNull()?.let { r ->
        AlertDialog(
            onDismissRequest = {},
            title = { Text(stringResource(R.string.incoming_from, r.from.ifBlank { Link.pcName })) },
            text = { Text(if (r.kind == "file") "${r.name} (${fmtSize(r.size)})" else stringResource(R.string.incoming_kind, r.kind, r.name)) },
            confirmButton = { TextButton(onClick = { Link.accept(r) }) { Text(stringResource(R.string.accept)) } },
            dismissButton = { TextButton(onClick = { Link.deny(r) }) { Text(stringResource(R.string.deny)) } },
        )
    }
}
