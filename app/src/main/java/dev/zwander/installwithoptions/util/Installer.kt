package dev.zwander.installwithoptions.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageInstaller
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.AppCompatTextView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import androidx.core.text.HtmlCompat
import androidx.documentfile.provider.DocumentFile
import dev.zwander.installwithoptions.BuildConfig
import dev.zwander.installwithoptions.IOptionsApplier
import dev.zwander.installwithoptions.R
import dev.zwander.installwithoptions.data.DataModel
import dev.zwander.installwithoptions.data.InstallOption
import dev.zwander.installwithoptions.data.InstallResult
import dev.zwander.installwithoptions.data.InstallStatus
import dev.zwander.installwithoptions.data.MutableOption
import dev.zwander.installwithoptions.data.Settings
import dev.zwander.installwithoptions.data.getMutableOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

const val INSTALL_STATUS_ACTION =
    "${BuildConfig.APPLICATION_ID}.intent.action.INSTALL_STATUS"

data class Installer(
    val install: () -> Unit,
    val isInstalling: Boolean,
)

@Composable
fun rememberPackageInstaller(files: List<DocumentFile>): Installer {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val permissionStarter =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.StartActivityForResult()) {
            Log.e("InstallWithOptions", "permission result ${Settings.gson.toJson(it)}")
        }

    var statuses by remember {
        mutableStateOf<List<InstallResult>>(listOf())
    }
    var isInstalling by remember {
        mutableStateOf(false)
    }

    val options by DataModel.selectedOptions.collectAsState()

    var showingConfirmation by remember {
        mutableStateOf(false)
    }
    val rootAdapter = remember {
        ShizukuRootAdapter(context)
    }
    val shellInterface = rootAdapter.rememberShellInterface()

    val applier = remember {
        object : IOptionsApplier.Stub() {
            override fun applyOptions(params: PackageInstaller.SessionParams): PackageInstaller.SessionParams {
                getMutableOptions().forEach { it.apply(params) }

                return params
            }
        }
    }

    val receiver = remember {
        object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == INSTALL_STATUS_ACTION) {
                    val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, Int.MIN_VALUE)
                    val message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE) ?: ""
                    val packageName =
                        intent.getStringExtra(PackageInstaller.EXTRA_PACKAGE_NAME) ?: ""

                    when (status) {
                        PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                            val requestIntent = IntentCompat.getParcelableExtra(
                                intent,
                                Intent.EXTRA_INTENT,
                                Intent::class.java
                            )

                            permissionStarter.launch(requestIntent)
                        }

                        PackageInstaller.STATUS_SUCCESS -> {
                            statuses =
                                statuses + InstallResult(
                                    status = InstallStatus.SUCCESS,
                                    packageName = packageName,
                                    message = context.resources.getString(R.string.success),
                                )
                        }

                        else -> {
                            statuses = statuses + InstallResult(
                                status = InstallStatus.FAILURE,
                                packageName = packageName,
                                message = message,
                            )
                        }
                    }
                }
            }
        }
    }

    fun installPackage(files: List<DocumentFile>, options: List<InstallOption>, split: Boolean) {
        if (shellInterface != null) {
            isInstalling = true
        }

        scope.launch(Dispatchers.IO) {
            try {
                shellInterface?.install(
                    files.map {
                        context.contentResolver.openAssetFileDescriptor(
                            it.uri,
                            "r",
                        )
                    }.toTypedArray(),
                    options.map { it.value }.toIntArray(),
                    split,
                    applier,
                    MutableOption.InstallerPackage.settingsKey.getValue(),
                )
            } catch (e: Exception) {
                statuses = files.map {
                    InstallResult(
                        status = InstallStatus.FAILURE,
                        packageName = it.name ?: it.uri.toString(),
                        message = e.localizedMessage ?: e.message ?: e.toString(),
                    )
                }
            }
        }
    }

    LaunchedEffect(key1 = statuses.size) {
        if (statuses.size == files.size) {
            isInstalling = false
        }
    }

    DisposableEffect(key1 = null) {
        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(INSTALL_STATUS_ACTION),
            ContextCompat.RECEIVER_EXPORTED,
        )

        onDispose {
            context.unregisterReceiver(receiver)
        }
    }

    if (showingConfirmation) {
        AlertDialog(
            onDismissRequest = { showingConfirmation = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        installPackage(files, options ?: listOf(), true)
                        showingConfirmation = false
                    },
                ) {
                    Text(text = stringResource(id = R.string.split_app))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        installPackage(files, options ?: listOf(), false)
                        showingConfirmation = false
                    },
                ) {
                    Text(text = stringResource(id = R.string.separate_apps))
                }
            },
            title = {
                Text(text = stringResource(id = R.string.install_question))
            },
            text = {
                Text(text = stringResource(id = R.string.install_question_desc))
            },
        )
    }

    statuses.takeIf { it.isNotEmpty() && it.size == files.size }?.let { s ->
        AlertDialog(
            onDismissRequest = { statuses = listOf() },
            confirmButton = {
                TextButton(onClick = { statuses = listOf() }) {
                    Text(text = stringResource(id = android.R.string.ok))
                }
            },
            title = {
                Text(
                    text = stringResource(
                        id = if (s.any { it.status == InstallStatus.FAILURE }) {
                            R.string.installation_errors
                        } else {
                            R.string.installation_complete
                        },
                    ),
                )
            },
            text = {
                SelectionContainer {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(items = s) { res ->
                            AndroidView(
                                factory = { AppCompatTextView(it) },
                            ) { tv ->
                                tv.text = HtmlCompat.fromHtml(
                                    context.resources.getString(
                                        R.string.status_item,
                                        try {
                                            context.packageManager.getApplicationInfo(res.packageName, 0)
                                                .loadLabel(context.packageManager).toString()
                                        } catch (e: Throwable) {
                                            res.packageName
                                        },
                                        res.message,
                                    ),
                                    HtmlCompat.FROM_HTML_MODE_COMPACT,
                                )
                            }
                        }
                    }
                }
            }
        )
    }

    return Installer(
        install = remember(files.hashCode(), options.hashCode()) {
            {
                if (files.size > 1) {
                    showingConfirmation = true
                } else {
                    installPackage(files, options ?: listOf(), false)
                }
            }
        },
        isInstalling = isInstalling,
    )
}
