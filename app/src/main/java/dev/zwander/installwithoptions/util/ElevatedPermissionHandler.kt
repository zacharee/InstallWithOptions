package dev.zwander.installwithoptions.util

import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import com.topjohnwu.superuser.Shell
import dev.zwander.installwithoptions.R
import dev.zwander.installwithoptions.data.DataModel
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuProvider

class ElevatedPermissionHandler(
    private val context: Context,
    private val finishCallback: () -> Unit,
) : Shizuku.OnRequestPermissionResultListener {
    fun onCreate() {
        DataModel.rootGranted.value = Shell.getShell().isRoot
        Shizuku.addRequestPermissionResultListener(this)
    }

    fun onDestroy() {
        Shizuku.removeRequestPermissionResultListener(this)
    }

    @Composable
    fun PermissionTracker() {
        val rootState by DataModel.rootGranted.collectAsState()
        val shizukuState by ShizukuUtils.rememberShizukuState()

        LaunchedEffect(key1 = shizukuState, key2 = rootState) {
            if (!rootState) {
                if (shizukuState == ShizukuState.RUNNING) {
                    try {
                        if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                            Shizuku.requestPermission(100)
                        } else {
                            DataModel.shizukuAvailable.value = true
                        }
                    } catch (_: Exception) {
                        finishCallback()
                    }
                }
            }
        }

        if (!rootState) {
            when (shizukuState) {
                ShizukuState.NOT_INSTALLED -> {
                    AlertDialog(
                        onDismissRequest = {},
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    context.launchUrl("https://shizuku.rikka.app/")
                                },
                            ) {
                                Text(text = stringResource(id = R.string.download))
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = { finishCallback() },
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
                                        context.startActivity(context.packageManager.getLaunchIntentForPackage(
                                            ShizukuProvider.MANAGER_APPLICATION_ID))
                                    } catch (_: Exception) {}
                                },
                            ) {
                                Text(text = stringResource(id = R.string.open_shizuku))
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = { finishCallback() },
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

    override fun onRequestPermissionResult(requestCode: Int, grantResult: Int) {
        if (grantResult != PackageManager.PERMISSION_GRANTED) {
            finishCallback()
        } else {
            DataModel.shizukuAvailable.value = true
        }
    }
}
