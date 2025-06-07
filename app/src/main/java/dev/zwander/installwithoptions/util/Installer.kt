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
import dev.zwander.installwithoptions.IShellInterface
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
    val totalPackages: Int,
    val completed: Int,
)

@Composable
fun rememberShellInterface(): IShellInterface? {
    val context = LocalContext.current
    val rootAdapter = remember {
        ShizukuRootAdapter(context)
    }
    return rootAdapter.rememberShellInterface()
}

@Composable
fun rememberPackageInstaller(files: Map<String, List<DocumentFile>>): Installer {
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

    val shellInterface = rememberShellInterface()

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
                                Intent::class.java,
                            )

                            if (requestIntent != null) {
                                permissionStarter.launch(requestIntent)
                            } else {
                                statuses = statuses.toMutableList().apply {
                                    removeIf { it.packageName == packageName }
                                } + InstallResult(
                                    status = InstallStatus.FAILURE,
                                    packageName = packageName,
                                    message = context.resources.getString(R.string.permission_intent_was_null),
                                )
                            }
                        }

                        PackageInstaller.STATUS_SUCCESS -> {
                            statuses = statuses.toMutableList().apply {
                                removeIf { it.packageName == packageName }
                            } + InstallResult(
                                status = InstallStatus.SUCCESS,
                                packageName = packageName,
                                message = context.resources.getString(R.string.success),
                            )
                        }

                        else -> {
                            statuses = statuses.toMutableList().apply {
                                removeIf { it.packageName == packageName }
                            } + InstallResult(
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

    fun installPackage(files: Map<String, List<DocumentFile>>, options: List<InstallOption>) {
        if (shellInterface != null) {
            isInstalling = true
        }

        scope.launch(Dispatchers.IO) {
            try {
                shellInterface?.install(
                    files.map { (k, v) ->
                        k to v.map {
                            context.contentResolver.openAssetFileDescriptor(
                                it.uri,
                                "r",
                            )
                        }
                    }.toMap(),
                    options.map { it.value }.toIntArray(),
                    applier,
                    MutableOption.InstallerPackage.settingsKey.getValue(),
                    MutableOption.TargetUser.settingsKey.getValue(),
                )
            } catch (e: Exception) {
                statuses = files.flatMap { (_, v) ->
                    v.map {
                        InstallResult(
                            status = InstallStatus.FAILURE,
                            packageName = it.name ?: it.uri.toString(),
                            message = e.localizedMessage ?: e.message ?: e.toString(),
                        )
                    }
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
                                        } catch (_: Throwable) {
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
            },
        )
    }

    return Installer(
        install = remember(files.hashCode(), options.hashCode()) {
            {
                installPackage(files, options)
            }
        },
        isInstalling = isInstalling,
        totalPackages = files.size,
        completed = statuses.size,
    )
}
