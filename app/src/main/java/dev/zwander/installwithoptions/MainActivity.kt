package dev.zwander.installwithoptions

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import dev.icerock.moko.mvvm.flow.compose.collectAsMutableState
import dev.zwander.installwithoptions.components.Footer
import dev.zwander.installwithoptions.data.DataModel
import dev.zwander.installwithoptions.data.DataModel.shizukuGranted
import dev.zwander.installwithoptions.data.InstallOption
import dev.zwander.installwithoptions.data.rememberInstallOptions
import dev.zwander.installwithoptions.ui.theme.InstallWithOptionsTheme
import dev.zwander.installwithoptions.util.ShizukuState
import dev.zwander.installwithoptions.util.ShizukuUtils
import dev.zwander.installwithoptions.util.launchUrl
import dev.zwander.installwithoptions.util.rememberPackageInstaller
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuProvider

class MainActivity : ComponentActivity(), Shizuku.OnRequestPermissionResultListener {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Shizuku.addRequestPermissionResultListener(this)

        enableEdgeToEdge()
        setContent {
            InstallWithOptionsTheme {
                val shizukuState by ShizukuUtils.rememberShizukuState()

                MainContent(
                    modifier = Modifier
                        .fillMaxSize(),
                )

                LaunchedEffect(key1 = shizukuState) {
                    if (shizukuState == ShizukuState.RUNNING) {
                        try {
                            if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                                Shizuku.requestPermission(100)
                            } else {
                                shizukuGranted.value = true
                            }
                        } catch (e: Exception) {
                            finish()
                        }
                    }
                }

                when (shizukuState) {
                    ShizukuState.NOT_INSTALLED -> {
                        AlertDialog(
                            onDismissRequest = {},
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        launchUrl("https://shizuku.rikka.app/")
                                    },
                                ) {
                                    Text(text = stringResource(id = R.string.download))
                                }
                            },
                            dismissButton = {
                                TextButton(
                                    onClick = { finish() },
                                ) {
                                    Text(text = stringResource(id = R.string.close_app))
                                }
                            },
                            title = {
                                Text(text = stringResource(id = R.string.shizuku_not_installed))
                            },
                            text = {
                                Text(text = stringResource(id = R.string.shizuku_not_installed_desc))
                            },
                        )
                    }

                    ShizukuState.INSTALLED_NOT_RUNNING -> {
                        AlertDialog(
                            onDismissRequest = {},
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        try {
                                            startActivity(packageManager.getLaunchIntentForPackage(ShizukuProvider.MANAGER_APPLICATION_ID))
                                        } catch (_: Exception) {
                                        }
                                    },
                                ) {
                                    Text(text = stringResource(id = R.string.open_shizuku))
                                }
                            },
                            dismissButton = {
                                TextButton(
                                    onClick = { finish() },
                                ) {
                                    Text(text = stringResource(id = R.string.close_app))
                                }
                            },
                            title = {
                                Text(text = stringResource(id = R.string.shizuku_not_running))
                            },
                            text = {
                                Text(text = stringResource(id = R.string.shizuku_not_running_desc))
                            },
                        )
                    }

                    else -> {}
                }
            }
        }
    }

    override fun onRequestPermissionResult(requestCode: Int, grantResult: Int) {
        if (grantResult != PackageManager.PERMISSION_GRANTED) {
            finish()
        } else {
            shizukuGranted.value = true
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        Shizuku.removeRequestPermissionResultListener(this)
    }
}

@Composable
fun MainContent(modifier: Modifier = Modifier) {
    var selectedFiles by remember {
        mutableStateOf(listOf<DocumentFile>())
    }
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
    val options = rememberInstallOptions()
    val (install, isInstalling) = rememberPackageInstaller(selectedFiles)

    Box(
        modifier = modifier,
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
                    .imePadding(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(8.dp),
                ) {
                    items(items = options, key = { it.labelResource }) { option ->
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
                }

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
                                    border = ButtonDefaults.outlinedButtonBorder,
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

                Footer(modifier = Modifier.fillMaxWidth())
            }
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
                CircularProgressIndicator()
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
                    items(items = selectedFiles) {
                        Text(text = it.name ?: it.uri.toString())
                    }
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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
