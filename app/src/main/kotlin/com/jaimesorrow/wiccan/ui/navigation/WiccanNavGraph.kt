package com.jaimesorrow.wiccan.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navDeepLink
import com.jaimesorrow.wiccan.ui.archive.ARCHIVE_DETAIL_CONTENT_ID_ARG
import com.jaimesorrow.wiccan.ui.archive.ArchiveDetailScreen
import com.jaimesorrow.wiccan.ui.archive.ArchiveScreen
import com.jaimesorrow.wiccan.ui.paywall.PaywallScreen
import com.jaimesorrow.wiccan.ui.settings.SettingsScreen
import com.jaimesorrow.wiccan.ui.today.TodayScreen

private data class BottomNavItem(val route: String, val label: String)

private val bottomNavItems = listOf(
    BottomNavItem(WiccanDestinations.TODAY, "Today"),
    BottomNavItem(WiccanDestinations.ARCHIVE, "Book of Shadows"),
    BottomNavItem(WiccanDestinations.SETTINGS, "Settings"),
)

/**
 * The signed-in app shell: bottom navigation across the three primary
 * destinations (Today, Book of Shadows, Settings), with Paywall and
 * Archive detail as secondary routes reached by navigating in, not by
 * tab. [startOnPaywall] lets an external entry point (the locked widget's
 * `wiccan://premium` deep link) open straight onto Paywall.
 */
@Composable
fun WiccanAppShell(modifier: Modifier = Modifier, startOnPaywall: Boolean = false) {
    val navController = rememberNavController()

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = { WiccanBottomBar(navController) },
    ) { insets ->
        NavHost(
            navController = navController,
            startDestination = if (startOnPaywall) WiccanDestinations.PAYWALL else WiccanDestinations.TODAY,
            modifier = Modifier.padding(insets),
        ) {
            composable(
                WiccanDestinations.TODAY,
                deepLinks = listOf(navDeepLink { uriPattern = WiccanDeepLinks.TODAY_URI }),
            ) {
                TodayScreen()
            }
            composable(WiccanDestinations.ARCHIVE) {
                ArchiveScreen(
                    onNavigateToDetail = { contentId ->
                        navController.navigate(WiccanDestinations.archiveDetail(contentId))
                    },
                )
            }
            composable(WiccanDestinations.SETTINGS) {
                SettingsScreen(onGoPremiumClick = { navController.navigate(WiccanDestinations.PAYWALL) })
            }
            composable(
                WiccanDestinations.PAYWALL,
                deepLinks = listOf(navDeepLink { uriPattern = WiccanDeepLinks.PREMIUM_URI }),
            ) {
                PaywallScreen(onBack = { navController.popBackStackIfPossible() })
            }
            composable(WiccanDestinations.ARCHIVE_DETAIL_ROUTE) {
                // contentId itself is read from SavedStateHandle inside ArchiveDetailViewModel;
                // it's declared here only so this destination's argument name is documented.
                requireNotNull(it.arguments?.getString(ARCHIVE_DETAIL_CONTENT_ID_ARG))
                ArchiveDetailScreen(onBack = { navController.popBackStackIfPossible() })
            }
        }
    }
}

private fun NavHostController.popBackStackIfPossible() {
    if (!popBackStack()) {
        navigate(WiccanDestinations.TODAY) { popUpTo(0) }
    }
}

@Composable
private fun WiccanBottomBar(navController: NavHostController) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination

    val isOnTopLevelDestination = bottomNavItems.any { item ->
        currentRoute?.hierarchy?.any { it.route == item.route } == true
    }
    if (!isOnTopLevelDestination) return

    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
        bottomNavItems.forEach { item ->
            val selected = currentRoute?.hierarchy?.any { it.route == item.route } == true
            NavigationBarItem(
                selected = selected,
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = {},
                label = { Text(item.label) },
            )
        }
    }
}
