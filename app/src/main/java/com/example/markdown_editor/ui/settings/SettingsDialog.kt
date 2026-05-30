package com.example.markdown_editor.ui.settings

import android.content.res.Resources
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.markdown_editor.R
import com.example.markdown_editor.domain.models.Settings
import com.example.markdown_editor.domain.usecases.sync.ValidSyncProvider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDialog(
    onDismissRequest: () -> Unit,
    settings: Settings,
    openYandexLink: () -> Unit,
    onReverseLayoutChange: (Boolean) -> Unit,
    onSyncProviderChange: (ValidSyncProvider) -> Unit,
    onOauthTokenChange: (String) -> Unit,
) {
    val resources = LocalResources.current
    var dropdownExpanded by remember { mutableStateOf(false) }
    val currentProviderString = getStringFromValidSyncProvider(resources, settings.syncProvider)

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding(),
            ) {
                TopAppBar(
                    title = { Text(stringResource(R.string.settings)) },
                    navigationIcon = {
                        IconButton(onClick = onDismissRequest) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.close),
                            )
                        }
                    },
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(
                        text = stringResource(R.string.general),
                        style = MaterialTheme.typography.titleMedium,
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = stringResource(R.string.reverse_drawer_layout),
                            modifier = Modifier.fillMaxWidth(0.8f),
                        )
                        Switch(
                            checked = settings.reverseLayout,
                            onCheckedChange = onReverseLayoutChange,
                        )
                    }

                    HorizontalDivider()

                    Text(
                        text = stringResource(R.string.synchronization),
                        style = MaterialTheme.typography.titleMedium,
                    )

                    ExposedDropdownMenuBox(
                        expanded = dropdownExpanded,
                        onExpandedChange = { dropdownExpanded = it },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        OutlinedTextField(
                            value = currentProviderString,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.provider)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                            modifier = Modifier
                                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth(),
                        )

                        DropdownMenuPopup(
                            expanded = dropdownExpanded,
                            onDismissRequest = { dropdownExpanded = false },
                            modifier = Modifier.exposedDropdownSize(matchAnchorWidth = true),
                        ) {
                            DropdownMenuGroup(
                                shapes = MenuDefaults.groupShape(index = 0, count = 1),
                            ) {
                                DropdownMenuItem(
                                    selected = settings.syncProvider == ValidSyncProvider.NONE,
                                    text = {
                                        Text(
                                            getStringFromValidSyncProvider(
                                                resources,
                                                ValidSyncProvider.NONE,
                                            ),
                                        )
                                    },
                                    shapes = MenuDefaults.itemShape(index = 0, count = 2),
                                    onClick = {
                                        onSyncProviderChange(ValidSyncProvider.NONE)
                                        dropdownExpanded = false
                                    },
                                )
                                DropdownMenuItem(
                                    selected = settings.syncProvider == ValidSyncProvider.YANDEX,
                                    text = {
                                        Text(
                                            getStringFromValidSyncProvider(
                                                resources,
                                                ValidSyncProvider.YANDEX,
                                            ),
                                        )
                                    },
                                    shapes = MenuDefaults.itemShape(index = 1, count = 2),
                                    onClick = {
                                        onSyncProviderChange(ValidSyncProvider.YANDEX)
                                        dropdownExpanded = false
                                    },
                                )
                            }
                        }
                    }

                    when (settings.syncProvider) {
                        ValidSyncProvider.YANDEX -> {
                            OutlinedButton(
                                onClick = openYandexLink,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(stringResource(R.string.get_oauth_token))
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                    contentDescription = stringResource(R.string.open_link),
                                )
                            }
                            OutlinedTextField(
                                value = settings.yandexOauthToken,
                                onValueChange = onOauthTokenChange,
                                label = { Text(stringResource(R.string.oauth_token)) },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }

                        ValidSyncProvider.NONE -> {}
                    }
                }
            }
        }
    }
}

fun getStringFromValidSyncProvider(resources: Resources, provider: ValidSyncProvider): String {
    val result = when (provider) {
        ValidSyncProvider.NONE -> resources.getString(R.string.none_sync)
        ValidSyncProvider.YANDEX -> resources.getString(R.string.yandex_disk)
    }
    return result
}
