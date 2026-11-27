package com.jaimesorrow.wiccan.ui.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavBackStackEntry
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
import com.jaimesorrow.wiccan.ui.brand.wiccanEmblemColor
import com.jaimesorrow.wiccan.ui.legal.LegalDocumentId
import com.jaimesorrow.wiccan.ui.legal.LegalDocumentScreen
import com.jaimesorrow.wiccan.ui.legal.WiccanLegalDocuments
import com.jaimesorrow.wiccan.ui.paywall.PaywallScreen
import com.jaimesorrow.wiccan.ui.settings.SettingsScreen
import com.jaimesorrow.wiccan.ui.settings.SettingsViewModel
import com.jaimesorrow.wiccan.ui.theme.WiccanPaletteColors
import com.jaimesorrow.wiccan.ui.today.TodayScreen
import com.wiccan.app.R

private data class BottomNavItem(
    val route: String,
    @StringRes val labelRes: Int,
    @DrawableRes val iconRes: Int,
)

private val bottomNavItems = listOf(
    BottomNavItem(WiccanDestinations.HOME, R.string.nav_home, R.drawable.ic_nav_home),
    BottomNavItem(WiccanDestinations.TODAY, R.string.nav_today, R.drawable.ic_nav_today),
    BottomNavItem(WiccanDestinations.ARCHIVE, R.string.nav_grimoire, R.drawable.ic_nav_grimoire),
    BottomNavItem(WiccanDestinations.SETTINGS, R.string.nav_settings, R.drawable.ic_nav_settings),
)

/** What a settings destination needs from navigation. */
internal data class SettingsLinks(
    val accountOnly: Boolean,
    val onBack: () -> Unit,
    val onTermsClick: () -> Unit,
    val onPrivacyClick: () -> Unit,
    val onContentSafetyClick: () -> Unit,
    val onPotionSafetyClick: () -> Unit,
)

/**
 * The composable shown for each destination. Production wires the real
 * Hilt-backed screens; navigation tests wire lightweight stand-ins so the
 * real routes, callbacks, back behavior and deep links can be exercised
 * without Firebase.
 */
internal class WiccanNavScreens(
    val dailyContent: @Composable (
        graphEntry: NavBackStackEntry,
        onOpenToday: (() -> Unit)?,
        onBack: (() -> Unit)?,
        onOpenGrimoire: () -> Unit,
        onOpenPaywall: () -> Unit,
    ) -> Unit,
    val archive: @Composable (onNavigateToDetail: (String) -> Unit) -> Unit,
    val settings: @Composable (links: SettingsLinks) -> Unit,
    val archiveDetail: @Composable (contentId: String, onBack: () -> Unit) -> Unit,
    val legalDocument: @Composable (documentId: LegalDocumentId, onBack: () -> Unit) -> Unit,
    val subscribe: @Composable (onBack: () -> Unit, onAccount: () -> Unit) -> Unit,
)

private fun productionScreens(settingsViewModel: SettingsViewModel) = WiccanNavScreens(
    dailyContent = { graphEntry, onOpenToday, onBack, onOpenGrimoire, onOpenPaywall ->
        TodayScreen(
            viewModel = hiltViewModel(graphEntry),
            onOpenToday = onOpenToday,
            onBack = onBack,
            onOpenGrimoire = onOpenGrimoire,
            onOpenPaywall = onOpenPaywall,
        )
    },
    archive = { onNavigateToDetail -> ArchiveScreen(onNavigateToDetail = onNavigateToDetail) },
    settings = { links ->
        SettingsScreen(
            viewModel = settingsViewModel,
            accountOnly = links.accountOnly,
            onBack = links.onBack,
            onTermsClick = links.onTermsClick,
            onPrivacyClick = links.onPrivacyClick,
            onContentSafetyClick = links.onContentSafetyClick,
            onPotionSafetyClick = links.onPotionSafetyClick,
        )
    },
    // ArchiveDetailViewModel reads the contentId from SavedStateHandle itself.
    archiveDetail = { _, onBack -> ArchiveDetailScreen(onBack = onBack) },
    legalDocument = { documentId, onBack ->
        LegalDocumentScreen(document = WiccanLegalDocuments.get(documentId), onBack = onBack)
    },
    subscribe = { _, onAccount ->
        PaywallScreen(onAccountClick = onAccount)
    },
)

/**
 * The signed-in app shell: bottom navigation across the three primary
 * destinations (Home, Grimoire, Settings), with Today, Paywall and
 * Archive detail as secondary routes reached by navigating in, not by
 * tab. [startOnPaywall] preserves the legacy `wiccan://premium` link as
 * subscription management. The root gate runs before this shell exists.
 */
@Composable
fun WiccanAppShell(
    modifier: Modifier = Modifier,
    startOnPaywall: Boolean = false,
    openTodayRequested: Boolean = false,
    onOpenTodayHandled: () -> Unit = {},
    settingsViewModel: SettingsViewModel = hiltViewModel(),
) {
    val screens = remember(settingsViewModel) { productionScreens(settingsViewModel) }
    WiccanAppShellContent(
        screens = screens,
        navController = rememberNavController(),
        modifier = modifier,
        startOnPaywall = startOnPaywall,
        openTodayRequested = openTodayRequested,
        onOpenTodayHandled = onOpenTodayHandled,
    )
}

@Composable
internal fun WiccanAppShellContent(
    screens: WiccanNavScreens,
    navController: NavHostController,
    modifier: Modifier = Modifier,
    startOnPaywall: Boolean = false,
    openTodayRequested: Boolean = false,
    onOpenTodayHandled: () -> Unit = {},
) {
    fun openToday() {
        navController.navigate(WiccanDestinations.TODAY) { launchSingleTop = true }
    }

    fun openPaywall() {
        navController.navigate(WiccanDestinations.SUBSCRIBE) { launchSingleTop = true }
    }

    fun openGrimoire() {
        navController.selectTab(WiccanDestinations.ARCHIVE)
    }

    // A notification tap asks for Today from wherever the user is in the shell.
    LaunchedEffect(openTodayRequested) {
        if (openTodayRequested) {
            navController.navigate(WiccanDestinations.TODAY) {
                popUpTo(navController.graph.findStartDestination().id)
                launchSingleTop = true
            }
            onOpenTodayHandled()
        }
    }

    fun settingsLinks(accountOnly: Boolean, onBack: () -> Unit) = SettingsLinks(
        accountOnly = accountOnly,
        onBack = onBack,
        onTermsClick = { navController.navigate(WiccanDestinations.TERMS) },
        onPrivacyClick = { navController.navigate(WiccanDestinations.PRIVACY) },
        onContentSafetyClick = { navController.navigate(WiccanDestinations.CONTENT_SAFETY) },
        onPotionSafetyClick = { navController.navigate(WiccanDestinations.POTION_SAFETY) },
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
            composable(WiccanDestinations.HOME) { entry ->
                val graphEntry = remember(entry) {
                    navController.getBackStackEntry(requireNotNull(entry.destination.parent).id)
                }
                screens.dailyContent(graphEntry, ::openToday, null, ::openGrimoire, ::openPaywall)
            }
            composable(
                WiccanDestinations.TODAY,
                deepLinks = listOf(navDeepLink { uriPattern = WiccanDeepLinks.TODAY_URI }),
            ) { entry ->
                val graphEntry = remember(entry) {
                    navController.getBackStackEntry(requireNotNull(entry.destination.parent).id)
                }
                screens.dailyContent(graphEntry, null, { navController.popBackStackIfPossible() }, ::openGrimoire, ::openPaywall)
            }
            composable(WiccanDestinations.ARCHIVE) {
                screens.archive { contentId -> navController.navigate(WiccanDestinations.archiveDetail(contentId)) }
            }
            composable(WiccanDestinations.SUBSCRIBE) {
                screens.subscribe(
                    { navController.popBackStackIfPossible() },
                    { navController.navigate(WiccanDestinations.SETTINGS) },
                )
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
                WiccanDestinations.POTION_SAFETY to LegalDocumentId.POTION_SAFETY,
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
        navigate(WiccanDestinations.HOME) { popUpTo(0) }
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
        onNavigate = navController::selectTab,
    )
}

private fun NavHostController.selectTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
internal fun WiccanBottomBarContent(currentRoute: String?, onNavigate: (String) -> Unit) {
    val gold = WiccanPaletteColors.Grimoire.Gold
    val border = WiccanPaletteColors.Grimoire.Border
    val muted = WiccanPaletteColors.Grimoire.Muted
    val navShape = RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp)
    Column {
        HorizontalDivider(color = border.copy(alpha = 0.55f), thickness = 1.dp)
        NavigationBar(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, border.copy(alpha = 0.38f), navShape),
            containerColor = WiccanPaletteColors.Grimoire.Background,
            tonalElevation = 0.dp,
        ) {
            bottomNavItems.forEach { item ->
                val selected = currentRoute == item.route
                NavigationBarItem(
                    selected = selected,
                    onClick = {
                        onNavigate(item.route)
                    },
                    icon = {
                        Icon(
                            painter = painterResource(item.iconRes),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = gold,
                        selectedTextColor = gold,
                        unselectedIconColor = muted,
                        unselectedTextColor = muted,
                        indicatorColor = gold.copy(alpha = 0.18f),
                    ),
                    label = { Text(stringResource(item.labelRes)) },
                )
            }
        }
    }
}
