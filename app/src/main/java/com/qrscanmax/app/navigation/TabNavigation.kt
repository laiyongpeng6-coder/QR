package com.qrscanmax.app.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.qrscanmax.core.ui.R as UiR

/**
 * Represents a single tab item in the bottom navigation bar.
 *
 * @property route The navigation route associated with this tab.
 * @property labelResId String resource ID for the tab label.
 * @property selectedIcon Icon displayed when the tab is active.
 * @property unselectedIcon Icon displayed when the tab is inactive.
 */
data class TabItem(
    val route: String,
    val labelResId: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

/**
 * Bottom navigation bar for the main screen with three tabs: History, Scan, and Create.
 *
 * Uses Material 3 [NavigationBar] and [NavigationBarItem] components with
 * filled icons for the selected state and outlined icons for unselected state.
 *
 * AI Continuity Note:
 * To add a new tab, create a new [NavRoutes.Tab] entry and append a [TabItem]
 * to the [tabs] list below. The [MainNavHost] must also register a composable
 * for the new route.
 *
 * @param currentRoute The route string of the currently active tab.
 * @param onTabSelected Callback invoked with the route string when a tab is tapped.
 * @param modifier Optional [Modifier] applied to the [NavigationBar].
 */
@Composable
fun TabNavigation(
    currentRoute: String?,
    onTabSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val tabs = listOf(
        TabItem(
            route = NavRoutes.Tab.History.route,
            labelResId = UiR.string.tab_history,
            selectedIcon = Icons.Filled.History,
            unselectedIcon = Icons.Outlined.History
        ),
        TabItem(
            route = NavRoutes.Tab.Scan.route,
            labelResId = UiR.string.tab_scan,
            selectedIcon = Icons.Filled.QrCodeScanner,
            unselectedIcon = Icons.Outlined.QrCodeScanner
        ),
        TabItem(
            route = NavRoutes.Tab.Create.route,
            labelResId = UiR.string.tab_create,
            selectedIcon = Icons.Filled.Add,
            unselectedIcon = Icons.Outlined.Add
        )
    )

    NavigationBar(modifier = modifier) {
        tabs.forEach { tab ->
            val isSelected = currentRoute == tab.route
            NavigationBarItem(
                selected = isSelected,
                onClick = { onTabSelected(tab.route) },
                icon = {
                    Icon(
                        imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                        contentDescription = stringResource(tab.labelResId)
                    )
                },
                label = {
                    Text(text = stringResource(tab.labelResId))
                }
            )
        }
    }
}
