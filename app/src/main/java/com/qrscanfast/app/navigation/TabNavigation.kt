package com.qrscanfast.app.navigation

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
import com.qrscanfast.core.ui.R as UiR

/**
 * 底部导航栏 Tab 项数据类。
 */
data class TabItem(
    val route: String,
    val labelResId: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

/**
 * 底部导航栏 — 包含 History、Scan、Create 三个 Tab。
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
