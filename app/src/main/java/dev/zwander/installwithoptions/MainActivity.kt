package dev.zwander.installwithoptions

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import dev.icerock.moko.mvvm.flow.compose.collectAsMutableState
import dev.zwander.installwithoptions.components.Footer
import dev.zwander.installwithoptions.data.DataModel
import dev.zwander.installwithoptions.data.InstallOption
import dev.zwander.installwithoptions.data.MutableOption
import dev.zwander.installwithoptions.data.rememberInstallOptions
import dev.zwander.installwithoptions.data.rememberMutableOptions
import dev.zwander.installwithoptions.ui.theme.InstallWithOptionsTheme
import dev.zwander.installwithoptions.util.ElevatedPermissionHandler
import dev.zwander.installwithoptions.util.LocalShellInterface
import dev.zwander.installwithoptions.util.handleIncomingUris
import dev.zwander.installwithoptions.util.plus
import dev.zwander.installwithoptions.util.rememberPackageInstaller
import dev.zwander.installwithoptions.util.shizukuRootAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity(), CoroutineScope by MainScope() {
    private val permissionHandler by lazy {
        ElevatedPermissionHandler(
            context = this,
            finishCallback = ::finish,
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            navigationBarStyle = SystemBarStyle.auto(Color.Transparent.toArgb(), Color.Transparent.toArgb()),
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        super.onCreate(savedInstanceState)

        permissionHandler.onCreate()

        setContent {
            CompositionLocalProvider(
                LocalShellInterface provides shizukuRootAdapter.rememberShellInterface(),
            ) {
                InstallWithOptionsTheme {
                    MainContent(
                        modifier = Modifier
                            .fillMaxSize(),
                    )

                    permissionHandler.PermissionTracker()
                }
            }
        }

        checkIntentForPackage(intent)
    }

    override fun onNewIntent(intent: Intent) {
        checkIntentForPackage(intent)
        super.onNewIntent(intent)
    }

    override fun onDestroy() {
        super.onDestroy()

        permissionHandler.onDestroy()
        cacheDir.deleteRecursively()
    }

    private fun checkIntentForPackage(intent: Intent) {
        intent.data?.let {
            launch(Dispatchers.IO) {
                handleIncomingUris(listOf(it))
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainContent(modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()

    var selectedFiles by DataModel.selectedFiles.collectAsMutableState()
    var showingSelectedFiles by remember {
        mutableStateOf(false)
    }
    var selectedOptions by DataModel.selectedOptions.collectAsMutableState()
    val isImporting by DataModel.isImporting.collectAsState()

    val context = LocalContext.current
    val fileSelector =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.OpenMultipleDocuments()) { uris ->
            scope.launch(Dispatchers.IO) {
                context.handleIncomingUris(uris)
            }
        }
    val options = (rememberInstallOptions() + rememberMutableOptions()).sortedBy {
        context.resources.getString(it.labelResource)
    }
    val (install, isInstalling, total, completed) = rememberPackageInstaller(selectedFiles)

    Box(
        modifier = modifier,
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
        ) {
            Scaffold(
                modifier = Modifier
                    .fillMaxSize(),
                bottomBar = {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        tonalElevation = BottomAppBarDefaults.ContainerElevation,
                        color = BottomAppBarDefaults.containerColor,
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .navigationBarsPadding()
                                .imePadding(),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp, start = 8.dp, end = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        try {
                                            fileSelector.launch(arrayOf("*/*"))
                                        } catch (_: ActivityNotFoundException) {
                                            Toast.makeText(context, R.string.error_selecting_files, Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .border(
                                                border = ButtonDefaults.outlinedButtonBorder(true),
                                                shape = CircleShape,
                                            )
                                            .clip(CircleShape)
                                            .then(
                                                if (selectedFiles.isNotEmpty()) {
                                                    Modifier.clickable {
                                                        showingSelectedFiles = true
                                                    }
                                                } else {
                                                    Modifier
                                                },
                                            ),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(text = selectedFiles.flatMap { it.value }.size.toString())
                                    }

                                    Spacer(modifier = Modifier.size(8.dp))

                                    Text(text = stringResource(id = R.string.choose_files))
                                }

                                OutlinedButton(
                                    onClick = { install() },
                                    enabled = selectedFiles.isNotEmpty(),
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Text(text = stringResource(id = R.string.install))
                                }
                            }

                            Footer(
                                modifier = Modifier
                                    .fillMaxWidth(),
                            )
                        }
                    }
                },
                content = { contentPadding ->
                    val nonBottom = PaddingValues(
                        top = contentPadding.calculateTopPadding(),
                        start = contentPadding.calculateStartPadding(LocalLayoutDirection.current) + 8.dp,
                        end = contentPadding.calculateEndPadding(LocalLayoutDirection.current) + 8.dp,
                        bottom = 8.dp,
                    )
                    val bottom = PaddingValues(bottom = contentPadding.calculateBottomPadding())

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = nonBottom +
                                WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal)
                                    .asPaddingValues(),
                    ) {
                        items(items = options, key = { it.labelResource }) { option ->
                            when (option) {
                                is InstallOption -> {
                                    OptionItem(
                                        option = option,
                                        isSelected = selectedOptions.contains(option),
                                        onSelectedChange = {
                                            selectedOptions = if (it) {
                                                if (!selectedOptions.contains(option)) {
                                                    selectedOptions + option
                                                } else {
                                                    selectedOptions
                                                }
                                            } else {
                                                selectedOptions - option
                                            }
                                        },
                                    )
                                }

                                is MutableOption<*> -> {
                                    option.Render(modifier = Modifier.fillMaxWidth())
                                }

                                else -> {}
                            }
                        }
                    }
                },
            )
        }

        AnimatedVisibility(
            visible = isInstalling,
            modifier = Modifier.fillMaxSize(),
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable(
                        interactionSource = remember {
                            MutableInteractionSource()
                        },
                        indication = null,
                        enabled = true,
                        onClick = {},
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    CircularProgressIndicator(
                        color = Color.White,
                    )
                    Text(
                        text = "$completed / $total",
                        color = Color.White,
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = isImporting,
            modifier = Modifier.fillMaxSize(),
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable(
                        interactionSource = remember {
                            MutableInteractionSource()
                        },
                        indication = null,
                        enabled = true,
                        onClick = {},
                    ),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    color = Color.White,
                )
            }
        }
    }

    if (showingSelectedFiles) {
        AlertDialog(
            onDismissRequest = { showingSelectedFiles = false },
            confirmButton = {
                TextButton(onClick = { showingSelectedFiles = false }) {
                    Text(text = stringResource(id = android.R.string.ok))
                }
            },
            title = {
                Text(text = stringResource(id = R.string.selected_files))
            },
            text = {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    selectedFiles.forEach { (pkg, files) ->
                        stickyHeader {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(color = MaterialTheme.colorScheme.surfaceContainerHigh),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = pkg,
                                    fontWeight = FontWeight.Bold,
                                    textDecoration = TextDecoration.Underline,
                                    modifier = Modifier.weight(1f),
                                )

                                IconButton(
                                    onClick = {
                                        selectedFiles -= pkg
                                    },
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Delete,
                                        contentDescription = stringResource(id = R.string.remove),
                                    )
                                }
                            }
                        }

                        items(items = files) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(color = MaterialTheme.colorScheme.surfaceContainerHigh),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = it.name ?: it.uri.toString(),
                                    modifier = Modifier.weight(1f),
                                )

                                if (files.size > 1) {
                                    IconButton(
                                        onClick = {
                                            selectedFiles = selectedFiles.toMutableMap().apply {
                                                val newList = this[pkg]?.minus(it)

                                                if (newList.isNullOrEmpty()) {
                                                    remove(pkg)
                                                } else {
                                                    this[pkg] = newList
                                                }
                                            }
                                        },
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Delete,
                                            contentDescription = stringResource(id = R.string.remove),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        )
    }
}

@Composable
fun OptionItem(
    option: InstallOption,
    isSelected: Boolean,
    onSelectedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedCard(
        onClick = { onSelectedChange(!isSelected) },
        modifier = modifier,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(8.dp),
        ) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = onSelectedChange,
            )

            Column(
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = stringResource(id = option.labelResource),
                    style = MaterialTheme.typography.titleMedium,
                )

                Text(
                    text = stringResource(id = option.descResource),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}
