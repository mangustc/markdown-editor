package com.example.markdown_editor.ui.components

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconButtonShapes
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TooltipIconButton(
    onClick: () -> Unit,
    icon: ImageVector,
    tooltip: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tooltipAnchorPosition: TooltipAnchorPosition = TooltipAnchorPosition.Above,
    colors: IconButtonColors = IconButtonDefaults.iconButtonColors(),
    shapes: IconButtonShapes = IconButtonDefaults.shapes(),
) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(tooltipAnchorPosition),
        tooltip = { PlainTooltip { Text(tooltip) } },
        state = rememberTooltipState(),
    ) {
        IconButton(
            onClick = onClick,
            enabled = enabled,
            colors = colors,
            shapes = shapes,
            modifier = modifier,
        ) {
            Icon(
                icon,
                contentDescription = tooltip,
            )
        }
    }
}
