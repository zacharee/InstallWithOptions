@file:Suppress("DEPRECATION")

package dev.zwander.installwithoptions.util

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.IPackageInstallerSession
import android.content.pm.IPackageManager
import android.content.pm.PackageInstaller
import android.content.res.AssetFileDescriptor
import android.os.FileBridge
import android.os.ParcelFileDescriptor
import android.os.ServiceManager
import android.os.UserHandle
import android.util.Log
import dev.zwander.installwithoptions.BuildConfig
import dev.zwander.installwithoptions.IOptionsApplier
import kotlin.random.Random

class InternalInstaller(private val context: Context) {
    private val packageInstaller =
        IPackageManager.Stub.asInterface(ServiceManager.getService("package"))
            .packageInstaller

    fun installPackage(
        fileDescriptors: Array<AssetFileDescriptor>,
        options: IntArray,
        splits: Boolean,
        applier: IOptionsApplier,
    ) {
        if (splits) {
            installPackagesInSession(fileDescriptors, options, applier)
        } else {
            fileDescriptors.forEach { fd ->
                installPackagesInSession(arrayOf(fd), options, applier)
            }
        }
    }

    @SuppressLint("InlinedApi")
    private fun installPackagesInSession(
        fileDescriptors: Array<AssetFileDescriptor>,
        options: IntArray,
        applier: IOptionsApplier,
    ) {
        var session: IPackageInstallerSession? = null

        try {
            val params = PackageInstaller.SessionParams(
                PackageInstaller.SessionParams.MODE_FULL_INSTALL,
            ).run {
                options.reduceOrNull { acc, i -> acc or i }?.let { flags -> installFlags = flags }
                applier.applyOptions(this)
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
                        FileBridge.FileBridgeOutputStream(this)
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