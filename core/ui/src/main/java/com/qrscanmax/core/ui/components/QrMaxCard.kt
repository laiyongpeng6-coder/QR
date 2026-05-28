package com.qrscanmax.core.ui.components

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Styled card component with elevation and rounded corners using Material 3.
 *
 * Used throughout the app for content containers such as scan results,
 * history items, and generator input sections.
 *
 * @param modifier Optional [Modifier] for layout customization.
 * @param elevation The shadow elevation of the card. Defaults to 2dp.
 * @param content The composable content displayed inside the card.
 */
@Composable
fun QrMaxCard(
    modifier: Modifier = Modifier,
    elevation: Dp = 2.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = elevation
        ),
        content = content
    )
}
