package com.example.markdown_editor.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MenuPopup(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    content: @Composable (ColumnScope.(interactionSource: MutableInteractionSource) -> Unit),
) {
    val interactionSource = remember { MutableInteractionSource() }
    DropdownMenuPopup(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
    ) { content(interactionSource) }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MenuPopupGroup(
    interactionSource: MutableInteractionSource,
    index: Int,
    count: Int,
    label: String,
    content: @Composable (ColumnScope.() -> Unit)
) {
    DropdownMenuGroup(
        shapes = MenuDefaults.groupShape(index = index, count = count),
        interactionSource = interactionSource,
    ) {
        MenuDefaults.Label { Text(label) }
        HorizontalDivider(
            modifier = Modifier.padding(MenuDefaults.HorizontalDividerPadding)
        )
        content()
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MenuPopupItem(
    text: String,
    supportingText: String? = null,
    index: Int,
    count: Int,
    tint: Color? = null,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    DropdownMenuItem(
        text = { Text(text) },
        supportingText = { if (supportingText != null) Text(supportingText) },
        shapes = MenuDefaults.itemShape(index = index, count = count),
        leadingIcon = {
            Icon(
                imageVector = icon,
                modifier = Modifier.size(MenuDefaults.LeadingIconSize),
                contentDescription = null,
                tint = tint ?: LocalContentColor.current,
            )
        },
        selectedLeadingIcon = {
            Icon(
                Icons.Filled.Delete,
                modifier = Modifier.size(MenuDefaults.LeadingIconSize),
                contentDescription = null,
                tint = tint ?: LocalContentColor.current,
            )
        },
        selected = false,
        colors = MenuDefaults.itemColors(
            textColor = tint ?: Color.Unspecified,
        ),
        onClick = onClick,
    )
}

