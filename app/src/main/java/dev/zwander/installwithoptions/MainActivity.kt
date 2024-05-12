package dev.zwander.installwithoptions

import android.content.Intent
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import dev.icerock.moko.mvvm.flow.compose.collectAsMutableState
import dev.zwander.installwithoptions.components.Footer
import dev.zwander.installwithoptions.data.DataModel
import dev.zwander.installwithoptions.data.InstallOption
import dev.zwander.installwithoptions.data.MutableOption
import dev.zwander.installwithoptions.data.rememberInstallOptions
import dev.zwander.installwithoptions.data.rememberMutableOptions
import dev.zwander.installwithoptions.ui.theme.InstallWithOptionsTheme
import dev.zwander.installwithoptions.util.ElevatedPermissionHandler
import dev.zwander.installwithoptions.util.plus
import dev.zwander.installwithoptions.util.rememberPackageInstaller

class MainActivity : AppCompatActivity() {
    private val permissionHandler by lazy {
        ElevatedPermissionHandler(
            context = this,
            finishCallback = ::finish,
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            navigationBarStyle = SystemBarStyle.dark(Color.Transparent.toArgb()),
        )
        super.onCreate(savedInstanceState)

        permissionHandler.onCreate()

        setContent {
            InstallWithOptionsTheme {
                MainContent(
                    modifier = Modifier
                        .fillMaxSize(),
                )

                permissionHandler.PermissionTracker()
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
    }

    private fun checkIntentForPackage(intent: Intent) {
        if (intent.type == "application/vnd.android.package-archive") {
            val apkUri = intent.data ?: return
            val file = DocumentFile.fromSingleUri(this, apkUri) ?: return

            DataModel.selectedFiles.value += file
        }
    }
}

@Composable
fun MainContent(modifier: Modifier = Modifier) {
    var selectedFiles by DataModel.selectedFiles.collectAsMutableState()
    var showingSelectedFiles by remember {
        mutableStateOf(false)
    }
    var selectedOptions by DataModel.selectedOptions.collectAsMutableState()

    val context = LocalContext.current
    val fileSelector =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.OpenMultipleDocuments()) { uris ->
            selectedFiles = uris.mapNotNull { uri ->
                DocumentFile.fromSingleUri(context, uri)
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
                    .fillMaxSize()
                    .imePadding(),
                bottomBar = {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        tonalElevation = BottomAppBarDefaults.ContainerElevation,
                        color = BottomAppBarDefaults.containerColor,
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    WindowInsets.systemBars
                                        .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
                                        .asPaddingValues()
                                ),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp, start = 8.dp, end = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                OutlinedButton(
                                    onClick = { fileSelector.launch(arrayOf("application/vnd.android.package-archive")) },
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
                                        Text(text = selectedFiles.size.toString())
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
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = contentPadding +
                                WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal)
                                    .asPaddingValues(),
                    ) {
                        items(items = options, key = { it.labelResource }) { option ->
                            when (option) {
                                is InstallOption -> {
                                    OptionItem(
                                        option = option,
                                        isSelected = selectedOptions?.contains(option) == true,
                                        onSelectedChange = {
                                            selectedOptions = if (it) {
                                                if (selectedOptions?.contains(option) == false) {
                                                    (selectedOptions ?: listOf()) + option
                                                } else {
                                                    selectedOptions
                                                }
                                            } else {
                                                (selectedOptions ?: listOf()) - option
                                            }
                                        },
                                    )
                                }

                                is MutableOption<*> -> {
                                    val value by option.value.collectAsState()
                                    when (value) {
                                        is String? -> {
                                            @Suppress("UNCHECKED_CAST")
                                            TextOptionItem(option = option as MutableOption<String>)
                                        }
                                    }
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
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(items = selectedFiles) {
                        Text(text = it.name ?: it.uri.toString())
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

@Composable
fun TextOptionItem(
    option: MutableOption<String>,
    modifier: Modifier = Modifier,
) {
    var state by option.value.collectAsMutableState()

    OutlinedCard(
        modifier = modifier,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(8.dp),
        ) {
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

                Spacer(modifier = Modifier.size(4.dp))

                OutlinedTextField(
                    value = state ?: "",
                    onValueChange = { state = it },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
