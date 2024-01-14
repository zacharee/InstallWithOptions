@file:Suppress("DEPRECATION")

package dev.zwander.installwithoptions.util

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.content.pm.IPackageInstallerSession
import android.content.pm.IPackageManager
import android.content.pm.PackageInstaller
import android.content.res.AssetFileDescriptor
import android.os.FileBridge.FileBridgeOutputStream
import android.os.IBinder
import android.os.ParcelFileDescriptor
import android.os.ServiceManager
import android.os.UserHandle
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import androidx.documentfile.provider.DocumentFile
import com.google.gson.GsonBuilder
import dev.zwander.installwithoptions.BuildConfig
import dev.zwander.installwithoptions.IShellInterface
import dev.zwander.installwithoptions.R
import dev.zwander.installwithoptions.data.DataModel
import dev.zwander.installwithoptions.data.DataModel.shizukuGranted
import dev.zwander.installwithoptions.data.InstallOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import rikka.shizuku.Shizuku
import kotlin.random.Random

private const val INSTALL_STATUS_ACTION =
    "${BuildConfig.APPLICATION_ID}.intent.action.INSTALL_STATUS"

data class Installer(
    val install: () -> Unit,
    val isInstalling: Boolean,
)

@Composable
fun rememberPackageInstaller(files: List<DocumentFile>): Installer {
    val context = LocalContext.current
    val shizukuGranted by shizukuGranted.collectAsState()
    val scope = rememberCoroutineScope()
    val permissionStarter = rememberLauncherForActivityResult(contract = ActivityResultContracts.StartActivityForResult()) {
        Log.e("InstallWithOptions", "permission result ${GsonBuilder().create().toJson(it)}")
    }

    var statuses by remember {
        mutableStateOf<List<Pair<String, String>>>(listOf())
    }
    var isInstalling by remember {
        mutableStateOf(false)
    }

    val options by DataModel.selectedOptions.collectAsState()

    var showingConfirmation by remember {
        mutableStateOf(false)
    }
    var shellInterface by remember {
        mutableStateOf<IShellInterface?>(null)
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
                                statuses + (packageName to context.resources.getString(R.string.success))
                        }

                        else -> {
                            statuses = statuses + (packageName to message)
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
            shellInterface?.install(
                files.map {
                    context.contentResolver.openAssetFileDescriptor(
                        it.uri,
                        "r",
                    )
                },
                options.map { it.value },
                split,
            )
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

    DisposableEffect(key1 = shizukuGranted) {
        val args = Shizuku.UserServiceArgs(ComponentName(context, ShellInterface::class.java))
            .tag("shell_service")
            .processNameSuffix("shell_service")
            .debuggable(BuildConfig.DEBUG)
            .daemon(false)

        val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                shellInterface = IShellInterface.Stub.asInterface(service)
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                shellInterface = null
            }
        }

        if (shizukuGranted) {
            Shizuku.bindUserService(args, connection)
        }

        onDispose {
            if (shizukuGranted) {
                Shizuku.unbindUserService(args, connection, true)
            } else {
                shellInterface = null
            }
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
                Text(text = stringResource(id = R.string.installation_complete))
            },
            text = {
                SelectionContainer {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        items(items = s) {
                            Text(
                                text = stringResource(
                                    id = R.string.status_item,
                                    try {
                                        context.packageManager.getApplicationInfo(it.first, 0)
                                            .loadLabel(context.packageManager).toString()
                                    } catch (e: Throwable) {
                                        it.first
                                    },
                                    it.second,
                                )
                            )
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

class InternalInstaller(private val context: Context) {
    private val packageInstaller =
        IPackageManager.Stub.asInterface(ServiceManager.getService("package"))
            .packageInstaller

    fun installPackage(
        fileDescriptors: List<AssetFileDescriptor>,
        options: List<Int>,
        splits: Boolean
    ) {
        if (splits) {
            installPackagesInSession(fileDescriptors, options)
        } else {
            fileDescriptors.forEach { fd ->
                installPackagesInSession(listOf(fd), options)
            }
        }
    }

    @SuppressLint("InlinedApi")
    private fun installPackagesInSession(
        fileDescriptors: List<AssetFileDescriptor>,
        options: List<Int>
    ) {
        var session: IPackageInstallerSession? = null

        try {
            val params = PackageInstaller.SessionParams(
                PackageInstaller.SessionParams.MODE_FULL_INSTALL,
            ).apply {
                options.reduceOrNull { acc, i -> acc or i }?.let { flags -> installFlags = flags }
            }
            val sessionId =
                packageInstaller.createSession(params, "system", "system", UserHandle.myUserId())
            session = packageInstaller.openSession(sessionId)
            val statusIntent = PendingIntent.getBroadcast(
                context, Random.nextInt(),
                Intent(INSTALL_STATUS_ACTION).apply {
                    `package` = BuildConfig.APPLICATION_ID
                },
                PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_ALLOW_UNSAFE_IMPLICIT_INTENT,
            )

            fileDescriptors.forEachIndexed { index, fd ->
                val writer = session?.openWrite(
                    "file_${index}",
                    0,
                    fd.length,
                )?.run {
                    if (PackageInstaller.ENABLE_REVOCABLE_FD) {
                        ParcelFileDescriptor.AutoCloseOutputStream(this)
                    } else {
                        FileBridgeOutputStream(this)
                    }
                }

                writer?.use { output ->
                    fd.createInputStream()?.use { input ->
                        input.copyTo(output)
                    }
                }
            }

            session?.commit(statusIntent.intentSender, false)
        } catch (e: Throwable) {
            e.printStackTrace()
            Log.e("InstallWithOptions", "error", e)
            session?.abandon()
        }
    }
}
