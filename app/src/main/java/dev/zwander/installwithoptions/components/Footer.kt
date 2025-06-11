package dev.zwander.installwithoptions.components

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.zwander.installwithoptions.R
import dev.zwander.installwithoptions.data.DataModel
import dev.zwander.installwithoptions.data.IOptionItem
import dev.zwander.installwithoptions.data.Settings
import dev.zwander.installwithoptions.util.launchUrl
import kotlinx.coroutines.launch
import tk.zwander.patreonsupportersretrieval.data.SupporterInfo
import tk.zwander.patreonsupportersretrieval.util.DataParser

private val footerItems = listOf(
    FooterItem(
        painter = { painterResource(id = R.drawable.web) },
        description = R.string.website,
        onClick = { launchUrl("https://zwander.dev") },
    ),
    FooterItem(
        painter = { painterResource(id = R.drawable.github) },
        description = R.string.github,
        onClick = { launchUrl("https://github.com/zacharee/InstallWithOptions") },
    ),
    FooterItem(
        painter = { painterResource(id = R.drawable.translate) },
        description = R.string.translate,
        onClick = { launchUrl("https://crowdin.com/project/install-with-options") },
    ),
    FooterItem(
        painter = { painterResource(id = R.drawable.mastodon) },
        description = R.string.mastodon,
        onClick = { launchUrl("https://androiddev.social/@Wander1236") },
    ),
    FooterItem(
        painter = { painterResource(id = R.drawable.patreon) },
        description = R.string.patreon,
        onClick = { launchUrl("https://patreon.com/zacharywander") },
    ),
    FooterItem(
        painter = { painterResource(id = R.drawable.outline_attach_money_24) },
        description = R.string.donate,
        onClick = { launchUrl("https://www.paypal.com/donate/?hosted_button_id=EWAPDSENZ7U44") },
    )
)

@Composable
fun Footer(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    val options = remember {
        listOf<IOptionItem>(
            IOptionItem.BasicOptionItem.BooleanItem(
                label = context.resources.getString(R.string.enable_crash_reports),
                desc = context.resources.getString(R.string.enable_crash_reports_desc),
                key = Settings.Keys.enableCrashReports,
            ),
            IOptionItem.ActionOptionItem(
                label = context.resources.getString(R.string.clear_cache),
                desc = context.resources.getString(R.string.clear_cache_desc),
                action = {
                    context.cacheDir.deleteRecursively()
                    context.cacheDir.mkdir()
                    DataModel.selectedFiles.value = mapOf()
                },
                listKey = "clear_cache",
            ),
        )
    }

    var showingSupportersDialog by remember {
        mutableStateOf(false)
    }
    var showingSettingsDialog by remember {
        mutableStateOf(false)
    }
    var supporters by remember {
        mutableStateOf(listOf<SupporterInfo>())
    }

    LaunchedEffect(key1 = showingSupportersDialog) {
        if (showingSupportersDialog) {
            supporters = DataParser.getInstance(context).parseSupporters()
        }
    }

    LazyRow(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        items(items = footerItems) { footerItem ->
            FooterItem(
                painter = footerItem.painter(),
                description = stringResource(id = footerItem.description),
                onClick = { footerItem.onClick(context) },
            )
        }

        item {
            FooterItem(
                painter = rememberVectorPainter(image = Icons.Default.Favorite),
                description = stringResource(id = R.string.supporters),
                onClick = { showingSupportersDialog = true },
            )
        }

        item {
            FooterItem(
                painter = rememberVectorPainter(image = Icons.Default.Settings),
                description = stringResource(id = R.string.settings),
                onClick = { showingSettingsDialog = true },
            )
        }
    }

    if (showingSupportersDialog) {
        AlertDialog(
            onDismissRequest = { showingSupportersDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        showingSupportersDialog = false
                    },
                ) {
                    Text(text = stringResource(id = android.R.string.ok))
                }
            },
            title = {
                Text(text = stringResource(id = R.string.supporters))
            },
            text = {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(items = supporters, key = { it.link }) { supporter ->
                        ClickableCard(
                            text = supporter.name,
                            onClick = { context.launchUrl(supporter.link) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            },
        )
    }

    if (showingSettingsDialog) {
        AlertDialog(
            title = { Text(text = stringResource(id = R.string.settings)) },
            text = {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(items = options, key = { it.listKey }) { item ->
                        Box(
                            modifier = Modifier.widthIn(max = 400.dp),
                        ) {
                            when (item) {
                                is IOptionItem.ActionOptionItem -> {
                                    ActionPreference(item = item)
                                }

                                is IOptionItem.BasicOptionItem.BooleanItem -> {
                                    BooleanPreference(item = item)
                                }

                                // TODO: Layouts for other settings types.
                            }
                        }
                    }
                }
            },
            onDismissRequest = { showingSettingsDialog = false },
            confirmButton = {
                TextButton(onClick = { showingSettingsDialog = false }) {
                    Text(text = stringResource(id = android.R.string.ok))
                }
            },
        )
    }
}

@Composable
fun FooterItem(
    painter: Painter,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier,
    ) {
        Icon(
            painter = painter,
            contentDescription = description,
        )
    }
}

@Composable
private fun BooleanPreference(
    item: IOptionItem.BasicOptionItem.BooleanItem,
    modifier: Modifier = Modifier,
) {
    var state by item.key.collectAsMutableState()

    Card(
        modifier = modifier,
        onClick = {
            state = !state
        },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LabelDesc(
                item = item,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp),
            )

            Switch(
                checked = state,
                onCheckedChange = {
                    state = it
                },
            )
        }
    }
}

@Composable
private fun ActionPreference(
    item: IOptionItem.ActionOptionItem,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()

    Card(
        modifier = modifier,
        onClick = { scope.launch { item.action() } },
    ) {
        LabelDesc(
            item = item,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun LabelDesc(
    item: IOptionItem,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
    ) {
        Text(
            text = item.label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
        )

        item.desc?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

private data class FooterItem(
    val painter: @Composable () -> Painter,
    @StringRes val description: Int,
    val onClick: Context.() -> Unit,
)
