package dev.zwander.installwithoptions.components

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.zwander.installwithoptions.R
import dev.zwander.installwithoptions.util.launchUrl
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
        painter = { painterResource(id = R.drawable.mastodon) },
        description = R.string.mastodon,
        onClick = { launchUrl("https://androiddev.social/@Wander1236") },
    ),
    FooterItem(
        painter = { painterResource(id = R.drawable.patreon) },
        description = R.string.patreon,
        onClick = { launchUrl("https://patreon.com/zacharywander") },
    ),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Footer(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    var showingSupportersDialog by remember {
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
                        OutlinedCard(
                            onClick = { context.launchUrl(supporter.link) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Box(
                                modifier = Modifier.fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = supporter.name,
                                    modifier = Modifier,
                                )
                            }
                        }
                    }
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

private data class FooterItem(
    val painter: @Composable () -> Painter,
    @StringRes val description: Int,
    val onClick: Context.() -> Unit,
)
