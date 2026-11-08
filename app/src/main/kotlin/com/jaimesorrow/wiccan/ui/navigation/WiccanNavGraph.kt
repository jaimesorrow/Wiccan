package com.jaimesorrow.wiccan.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
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
import com.jaimesorrow.wiccan.ui.legal.LegalDocumentId
import com.jaimesorrow.wiccan.ui.legal.LegalDocumentScreen
import com.jaimesorrow.wiccan.ui.legal.WiccanLegalDocuments
import com.jaimesorrow.wiccan.ui.settings.SettingsScreen
import com.jaimesorrow.wiccan.ui.settings.SettingsViewModel
import com.jaimesorrow.wiccan.ui.today.TodayScreen

private data class BottomNavItem(val route: String, val label: String)

private val bottomNavItems = listOf(
    BottomNavItem(WiccanDestinations.TODAY, "Today"),
    BottomNavItem(WiccanDestinations.ARCHIVE, "Grimoire"),
    BottomNavItem(WiccanDestinations.SETTINGS, "Settings"),
)

/** What a settings destination needs from navigation. */
internal data class SettingsLinks(
    val accountOnly: Boolean,
    val onBack: () -> Unit,
    val onTermsClick: () -> Unit,
    val onPrivacyClick: () -> Unit,
    val onContentSafetyClick: () -> Unit,
)

/**
 * The composable shown for each destination. Production wires the real
 * Hilt-backed screens; navigation tests wire lightweight stand-ins so the
 * real routes, callbacks, back behavior and deep links can be exercised
 * without Firebase.
 */
internal class WiccanNavScreens(
    val today: @Composable (onOpenGrimoire: () -> Unit) -> Unit,
    val archive: @Composable (onNavigateToDetail: (String) -> Unit) -> Unit,
    val settings: @Composable (links: SettingsLinks) -> Unit,
    val archiveDetail: @Composable (contentId: String, onBack: () -> Unit) -> Unit,
    val legalDocument: @Composable (documentId: LegalDocumentId, onBack: () -> Unit) -> Unit,
)

private fun productionScreens(settingsViewModel: SettingsViewModel) = WiccanNavScreens(
    today = { onOpenGrimoire -> TodayScreen(onOpenGrimoire = onOpenGrimoire) },
    archive = { onNavigateToDetail -> ArchiveScreen(onNavigateToDetail = onNavigateToDetail) },
    settings = { links ->
        SettingsScreen(
            viewModel = settingsViewModel,
            accountOnly = links.accountOnly,
            onBack = links.onBack,
            onTermsClick = links.onTermsClick,
            onPrivacyClick = links.onPrivacyClick,
            onContentSafetyClick = links.onContentSafetyClick,
        )
    },
    // ArchiveDetailViewModel reads the contentId from SavedStateHandle itself.
    archiveDetail = { _, onBack -> ArchiveDetailScreen(onBack = onBack) },
    legalDocument = { documentId, onBack ->
        LegalDocumentScreen(document = WiccanLegalDocuments.get(documentId), onBack = onBack)
    },
)

/**
 * The signed-in app shell: bottom navigation across the three primary
 * destinations (Today, Grimoire, Settings), with Paywall and
 * Archive detail as secondary routes reached by navigating in, not by
 * tab. [startOnPaywall] preserves the legacy `wiccan://premium` link as
 * subscription management. The root gate runs before this shell exists.
 */
@Composable
fun WiccanAppShell(
    modifier: Modifier = Modifier,
    startOnPaywall: Boolean = false,
    settingsViewModel: SettingsViewModel = hiltViewModel(),
) {
    val screens = remember(settingsViewModel) { productionScreens(settingsViewModel) }
    WiccanAppShellContent(
        screens = screens,
        navController = rememberNavController(),
        modifier = modifier,
        startOnPaywall = startOnPaywall,
    )
}

@Composable
internal fun WiccanAppShellContent(
    screens: WiccanNavScreens,
    navController: NavHostController,
    modifier: Modifier = Modifier,
    startOnPaywall: Boolean = false,
) {
    fun settingsLinks(accountOnly: Boolean, onBack: () -> Unit) = SettingsLinks(
        accountOnly = accountOnly,
        onBack = onBack,
        onTermsClick = { navController.navigate(WiccanDestinations.TERMS) },
        onPrivacyClick = { navController.navigate(WiccanDestinations.PRIVACY) },
        onContentSafetyClick = { navController.navigate(WiccanDestinations.CONTENT_SAFETY) },
    )

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = { WiccanBottomBar(navController) },
    ) { insets ->
        NavHost(
            navController = navController,
            startDestination = WiccanDestinations.startDestination(startOnPaywall),
            modifier = Modifier.padding(insets),
        ) {
            composable(
                WiccanDestinations.TODAY,
                deepLinks = listOf(navDeepLink { uriPattern = WiccanDeepLinks.TODAY_URI }),
            ) {
                screens.today { navController.navigate(WiccanDestinations.ARCHIVE) }
            }
            composable(WiccanDestinations.ARCHIVE) {
                screens.archive { contentId -> navController.navigate(WiccanDestinations.archiveDetail(contentId)) }
            }
            composable(WiccanDestinations.SETTINGS) {
                screens.settings(settingsLinks(accountOnly = false, onBack = {}))
            }
            composable(
                WiccanDestinations.PAYWALL,
                deepLinks = listOf(navDeepLink { uriPattern = WiccanDeepLinks.PREMIUM_URI }),
            ) {
                // Existing widget links remain valid. Subscribers manage
                // their subscription here; everyone else is held at root.
                screens.settings(settingsLinks(accountOnly = true, onBack = { navController.popBackStackIfPossible() }))
            }
            composable(WiccanDestinations.ARCHIVE_DETAIL_ROUTE) {
                val contentId = requireNotNull(it.arguments?.getString(ARCHIVE_DETAIL_CONTENT_ID_ARG))
                screens.archiveDetail(contentId) { navController.popBackStackIfPossible() }
            }
            listOf(
                WiccanDestinations.TERMS to LegalDocumentId.TERMS,
                WiccanDestinations.PRIVACY to LegalDocumentId.PRIVACY,
                WiccanDestinations.CONTENT_SAFETY to LegalDocumentId.CONTENT_SAFETY,
            ).forEach { (route, documentId) ->
                composable(route) {
                    screens.legalDocument(documentId) { navController.popBackStackIfPossible() }
                }
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

    WiccanBottomBarContent(
        currentRoute = currentRoute?.route,
        onNavigate = { route ->
            navController.navigate(route) {
                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        },
    )
}

@Composable
internal fun WiccanBottomBarContent(currentRoute: String?, onNavigate: (String) -> Unit) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
        bottomNavItems.forEach { item ->
            val selected = currentRoute == item.route
            NavigationBarItem(
                selected = selected,
                onClick = {
                    onNavigate(item.route)
                },
                icon = {},
                label = { Text(item.label) },
            )
        }
    }
}
