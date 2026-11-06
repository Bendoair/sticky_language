package com.stickylanguage.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.stickylanguage.app.ui.components.InkRule
import com.stickylanguage.app.ui.export.ExportScreen
import com.stickylanguage.app.ui.grammar.GrammarScreen
import com.stickylanguage.app.ui.home.HomeScreen
import com.stickylanguage.app.ui.navigation.Routes
import com.stickylanguage.app.ui.reader.ReaderScreen
import com.stickylanguage.app.ui.scan.ScanScreen
import com.stickylanguage.app.ui.settings.SettingsScreen
import com.stickylanguage.app.ui.theme.sticky

/**
 * App chrome shared with screens.
 * [showBottomNav] is true on Home / Settings / Export so child scaffolds can skip the
 * bottom safe-drawing inset (the tab bar already owns it).
 */
class AppChrome(
    val snackbar: SnackbarHostState,
    val showBottomNav: Boolean,
)

val LocalAppChrome = staticCompositionLocalOf<AppChrome> { error("AppChrome not provided") }

private data class TabEntry(
    val label: String,
    val icon: ImageVector,
    val route: Any,
    val iconSize: Dp = 22.dp,
)

private val topLevelRoutes = setOf(
    Routes.Home::class.qualifiedName,
    Routes.Settings::class.qualifiedName,
    Routes.Export::class.qualifiedName,
)

/**
 * Bottom tab bar for Home / Settings / Export. Stack screens (Scan, Reader, Grammar)
 * hide the tabs and use their own back buttons.
 */
@Composable
fun AppRoot() {
    val navController = rememberNavController()
    val snackbar = remember { SnackbarHostState() }
    val c = MaterialTheme.sticky

    val rootVm: AppRootViewModel = hiltViewModel()
    val exportCount by rootVm.exportCount.collectAsStateWithLifecycle()

    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val showBottomNav = currentRoute in topLevelRoutes

    // Export left · Home center · Settings right. Side tabs use a smaller icon.
    val tabs = listOf(
        TabEntry("Export", Icons.Outlined.FileUpload, Routes.Export, iconSize = 18.dp),
        TabEntry("Home", Icons.Default.Home, Routes.Home, iconSize = 24.dp),
        TabEntry("Settings", Icons.Default.Settings, Routes.Settings, iconSize = 18.dp),
    )

    CompositionLocalProvider(LocalAppChrome provides AppChrome(snackbar = snackbar, showBottomNav = showBottomNav)) {
        Scaffold(
            snackbarHost = {
                SnackbarHost(hostState = snackbar) { data ->
                    Snackbar(
                        snackbarData = data,
                        containerColor = c.ink,
                        contentColor = c.paper,
                        actionColor = c.highlight,
                        shape = MaterialTheme.shapes.small,
                    )
                }
            },
            bottomBar = {
                if (showBottomNav) {
                    StickyBottomNav(
                        tabs = tabs,
                        currentRoute = currentRoute,
                        exportCount = exportCount,
                        onSelect = { navController.navigateTopLevel(it) },
                    )
                }
            },
            contentWindowInsets = WindowInsets(0),
            containerColor = c.paper,
        ) { padding ->
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(c.paper),
            ) {
                StickyNavHost(navController)
            }
        }
    }
}

/** Lo-fi tab bar: ink top rule, yellow selected cell, badge on Export. */
@Composable
private fun StickyBottomNav(
    tabs: List<TabEntry>,
    currentRoute: String?,
    exportCount: Int,
    onSelect: (Any) -> Unit,
) {
    val c = MaterialTheme.sticky
    Column(
        Modifier
            .fillMaxWidth()
            .background(c.paper)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal)),
    ) {
        InkRule()
        Row(
            Modifier
                .fillMaxWidth()
                .height(60.dp),
        ) {
            tabs.forEach { tab ->
                val selected = currentRoute == tab.route::class.qualifiedName
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .background(if (selected) c.highlight else c.paper)
                        .clickable { onSelect(tab.route) },
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box {
                            Icon(
                                tab.icon,
                                contentDescription = tab.label,
                                tint = if (selected) c.onHighlight else c.ink,
                                modifier = Modifier.size(tab.iconSize),
                            )
                            if (tab.route == Routes.Export && exportCount > 0) {
                                Text(
                                    exportCount.toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = c.paper,
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .background(c.ink, MaterialTheme.shapes.extraLarge)
                                        .padding(horizontal = 5.dp, vertical = 1.dp),
                                )
                            }
                        }
                        Spacer(Modifier.height(2.dp))
                        Text(
                            tab.label,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (selected) c.onHighlight else c.ink,
                        )
                    }
                }
            }
        }
    }
}

private fun NavHostController.navigateTopLevel(route: Any) {
    navigate(route) {
        popUpTo(graph.startDestinationId) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun StickyNavHost(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Routes.Home) {
        composable<Routes.Home> {
            HomeScreen(
                onScan = { navController.navigate(Routes.Scan) },
                onOpenReader = { navController.navigate(Routes.Reader) },
                onOpenExport = { navController.navigateTopLevel(Routes.Export) },
            )
        }
        composable<Routes.Scan> {
            ScanScreen(
                onClose = { navController.popBackStack() },
                onCaptured = {
                    navController.navigate(Routes.Reader) { popUpTo(Routes.Home) }
                },
            )
        }
        composable<Routes.Reader> {
            ReaderScreen(
                onBack = { navController.popBackStack() },
                onGrammar = { navController.navigate(Routes.Grammar) },
            )
        }
        composable<Routes.Grammar> {
            GrammarScreen(onBack = { navController.popBackStack() })
        }
        composable<Routes.Settings> {
            SettingsScreen()
        }
        composable<Routes.Export> {
            ExportScreen()
        }
    }
}
