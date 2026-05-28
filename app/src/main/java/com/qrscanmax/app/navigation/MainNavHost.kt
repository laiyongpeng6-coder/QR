package com.qrscanmax.app.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.qrscanmax.core.data.datastore.OnboardingPreferences

/**
 * Main navigation host for the QR Scan Max application.
 *
 * Implements conditional navigation logic:
 * 1. On first launch (onboarding not complete) → displays the onboarding graph.
 * 2. On subsequent launches → displays the main tab-based navigation with a
 *    [Scaffold] containing [TabNavigation] as the bottom bar.
 *
 * Each tab destination preserves its state across tab switches using
 * `saveState` and `restoreState` navigation options.
 *
 * AI Continuity Note:
 * - To add a new tab screen, register a `composable(NavRoutes.Tab.NewTab.route)` block
 *   inside the NavHost and add the corresponding [TabItem] in [TabNavigation].
 * - To integrate the real onboarding screen, replace the placeholder `OnboardingPlaceholder`
 *   composable with the actual `OnboardingScreen` from `:feature:onboarding`.
 * - Feature screens (HistoryScreen, ScannerScreen, GeneratorScreen) should replace
 *   the placeholder Text composables once their respective modules are implemented.
 *
 * @param onboardingPreferences DataStore-backed preferences to check onboarding completion.
 */
@Composable
fun MainNavHost(
    onboardingPreferences: OnboardingPreferences
) {
    val isOnboardingComplete by onboardingPreferences.isOnboardingComplete
        .collectAsState(initial = true)

    if (!isOnboardingComplete) {
        OnboardingPlaceholder()
    } else {
        MainTabScaffold()
    }
}

/**
 * Placeholder for the onboarding flow.
 *
 * Will be replaced by the actual OnboardingScreen composable from
 * `:feature:onboarding` module once task 7.3 is implemented.
 */
@Composable
private fun OnboardingPlaceholder() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("Onboarding")
    }
}

/**
 * Main scaffold with bottom tab navigation and a NavHost for tab content.
 *
 * Uses [rememberNavController] to manage navigation state and preserves
 * each tab's back-stack independently via `saveState`/`restoreState`.
 */
@Composable
private fun MainTabScaffold() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            TabNavigation(
                currentRoute = currentRoute,
                onTabSelected = { route ->
                    navController.navigate(route) {
                        // Pop up to the start destination to avoid building up
                        // a large back-stack when switching tabs.
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        // Avoid multiple copies of the same destination
                        launchSingleTop = true
                        // Restore state when re-selecting a previously selected tab
                        restoreState = true
                    }
                }
            )
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = NavRoutes.Tab.Scan.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(NavRoutes.Tab.History.route) {
                HistoryPlaceholder()
            }
            composable(NavRoutes.Tab.Scan.route) {
                ScanPlaceholder()
            }
            composable(NavRoutes.Tab.Create.route) {
                CreatePlaceholder()
            }
        }
    }
}

/**
 * Placeholder for the History tab screen.
 * Replace with `HistoryScreen` from `:feature:history` once implemented.
 */
@Composable
private fun HistoryPlaceholder() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("History")
    }
}

/**
 * Placeholder for the Scan tab screen.
 * Replace with `ScannerScreen` from `:feature:scanner` once implemented.
 */
@Composable
private fun ScanPlaceholder() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("Scan")
    }
}

/**
 * Placeholder for the Create tab screen.
 * Replace with `GeneratorInputScreen` from `:feature:generator` once implemented.
 */
@Composable
private fun CreatePlaceholder() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("Create")
    }
}
