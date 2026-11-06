package com.stickylanguage.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material3.Badge
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.stickylanguage.app.ui.export.ExportScreen
import com.stickylanguage.app.ui.grammar.GrammarScreen
import com.stickylanguage.app.ui.home.HomeScreen
import com.stickylanguage.app.ui.navigation.Routes
import com.stickylanguage.app.ui.reader.ReaderScreen
import com.stickylanguage.app.ui.scan.ScanScreen
import com.stickylanguage.app.ui.settings.SettingsScreen
import kotlinx.coroutines.launch

/** Exposed to screens so hamburger buttons can open the drawer and anyone can show a snackbar. */
class AppChrome(
    val openDrawer: () -> Unit,
    val snackbar: SnackbarHostState,
)

val LocalAppChrome = staticCompositionLocalOf<AppChrome> { error("AppChrome not provided") }

private data class DrawerEntry(val label: String, val icon: ImageVector, val route: Any)

@Composable
fun AppRoot() {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val chrome = remember { AppChrome(openDrawer = { scope.launch { drawerState.open() } }, snackbar = snackbar) }

    val rootVm: AppRootViewModel = hiltViewModel()
    val exportCount by rootVm.exportCount.collectAsStateWithLifecycle()

    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    val entries = listOf(
        DrawerEntry("Home", Icons.Default.Home, Routes.Home),
        DrawerEntry("Settings", Icons.Default.Settings, Routes.Settings),
        DrawerEntry("Export", Icons.Outlined.FileUpload, Routes.Export),
    )

    CompositionLocalProvider(LocalAppChrome provides chrome) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            gesturesEnabled = currentRoute != Routes.Scan::class.qualifiedName,
            drawerContent = {
                ModalDrawerSheet {
                    Column(Modifier.padding(horizontal = 12.dp, vertical = 20.dp)) {
                        Text(
                            "Sticky Language",
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                        Text(
                            "Scan · translate · stick it in Anki",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                        Spacer(Modifier.height(16.dp))
                        HorizontalDivider()
                        Spacer(Modifier.height(8.dp))
                        entries.forEach { entry ->
                            val selected = currentRoute == entry.route::class.qualifiedName
                            NavigationDrawerItem(
                                label = { Text(entry.label) },
                                icon = { Icon(entry.icon, contentDescription = null) },
                                badge = if (entry.route == Routes.Export && exportCount > 0) {
                                    { Badge { Text(exportCount.toString()) } }
                                } else null,
                                selected = selected,
                                onClick = {
                                    scope.launch { drawerState.close() }
                                    navController.navigateTopLevel(entry.route)
                                },
                                colors = NavigationDrawerItemDefaults.colors(),
                                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                            )
                        }
                    }
                }
            },
        ) {
            androidx.compose.material3.Scaffold(
                snackbarHost = { SnackbarHost(snackbar) },
                containerColor = MaterialTheme.colorScheme.background,
            ) { _ ->
                StickyNavHost(navController)
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
