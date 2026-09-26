package it.matteocolombo.tacca.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import it.matteocolombo.tacca.ui.coffees.CoffeesEntry
import it.matteocolombo.tacca.ui.components.TaccaBottomBar
import it.matteocolombo.tacca.ui.detail.DetailEntry
import it.matteocolombo.tacca.ui.editor.CalibrationEditorEntry
import it.matteocolombo.tacca.ui.grindereditor.GrinderEditorEntry
import it.matteocolombo.tacca.ui.grinders.GrindersEntry
import it.matteocolombo.tacca.ui.home.HomeEntry
import it.matteocolombo.tacca.ui.methods.MethodsEntry
import it.matteocolombo.tacca.ui.settings.SettingsEntry
import it.matteocolombo.tacca.ui.theme.TaccaTheme

@Composable
fun TaccaApp(navController: NavHostController = rememberNavController()) {
    val backStack by navController.currentBackStackEntryAsState()
    val destination = backStack?.destination
    val currentTab = TopLevelDestination.entries.firstOrNull { tab ->
        destination?.hierarchy?.any { it.hasRoute(tab.route::class) } == true
    }
    Scaffold(
        containerColor = TaccaTheme.colors.background,
        contentWindowInsets = WindowInsets(0),
        bottomBar = { if (currentTab != null) TaccaBottomBar(currentTab, onSelect = navController::navigateToTab) },
    ) { inner ->
        NavHost(
            navController = navController,
            startDestination = HomeRoute,
            modifier = Modifier.padding(bottom = inner.calculateBottomPadding()),
            enterTransition = { fadeIn(tween(150)) },
            exitTransition = { fadeOut(tween(150)) },
        ) {
            composable<HomeRoute> {
                HomeEntry(
                    onOpen = { navController.navigate(DetailRoute(it)) },
                    onNew = { coffeeId, methodId -> navController.navigate(EditorRoute(coffeeId = coffeeId ?: NEW_ID, methodId = methodId ?: NEW_ID)) },
                    onSettings = { navController.navigate(SettingsRoute) },
                )
            }
            composable<CoffeesRoute> { CoffeesEntry(onShowOnHome = navController::showHomeFiltered) }
            composable<MethodsRoute> { MethodsEntry(onShowOnHome = navController::showHomeFiltered) }
            composable<GrindersRoute> {
                GrindersEntry(
                    onGrinderClick = { navController.navigate(GrinderEditorRoute(it)) },
                    onAddClick = { navController.navigate(GrinderEditorRoute()) },
                )
            }
            composable<DetailRoute> {
                DetailEntry(
                    onBack = { navController.popBackStack() },
                    onEdit = { navController.navigate(EditorRoute(calibrationId = it)) },
                    onOpen = { navController.navigate(DetailRoute(it)) },
                )
            }
            composable<EditorRoute> { entry ->
                // A NavBackStackEntry's savedStateHandle is not the editor ViewModel's SavedStateHandle,
                // so the grinder-creation handoff is read here and forwarded into the entry explicitly.
                val newGrinderId by entry.savedStateHandle.getStateFlow<Long?>(NEW_GRINDER_RESULT_KEY, null).collectAsStateWithLifecycle()
                CalibrationEditorEntry(
                    onClose = { navController.popBackStack() },
                    onNewGrinder = { navController.navigate(GrinderEditorRoute()) },
                    onSaved = { id, wasEdit ->
                        if (wasEdit) {
                            navController.popBackStack()
                        } else {
                            navController.navigate(DetailRoute(id)) { popUpTo<EditorRoute> { inclusive = true } }
                        }
                    },
                    onDeleted = { navController.popBackStack(HomeRoute, inclusive = false) },
                    newGrinderId = newGrinderId,
                    onNewGrinderConsumed = { entry.savedStateHandle[NEW_GRINDER_RESULT_KEY] = null },
                )
            }
            composable<GrinderEditorRoute> {
                GrinderEditorEntry(
                    onClose = { navController.popBackStack() },
                    onSaved = { id ->
                        navController.previousBackStackEntry?.savedStateHandle?.set(NEW_GRINDER_RESULT_KEY, id)
                        navController.popBackStack()
                    },
                    onDeleted = { navController.popBackStack() },
                )
            }
            composable<SettingsRoute> { SettingsEntry(onBack = { navController.popBackStack() }) }
        }
    }
}

fun NavController.navigateToTab(tab: TopLevelDestination) = navigate(tab.route) {
    popUpTo(graph.findStartDestination().id) { saveState = true }
    launchSingleTop = true
    restoreState = true
}

/** Opens the Calibrations tab at its root, where the shared home filter has just been set. */
fun NavController.showHomeFiltered() = navigate(HomeRoute) {
    popUpTo(graph.findStartDestination().id) { inclusive = false }
    launchSingleTop = true
}
