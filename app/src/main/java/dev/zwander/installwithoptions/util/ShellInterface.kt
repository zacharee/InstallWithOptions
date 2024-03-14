package dev.zwander.installwithoptions.util

import android.app.ActivityThread
import android.content.Context
import android.content.res.AssetFileDescriptor
import android.os.Looper
import androidx.annotation.Keep
import androidx.core.os.UserHandleCompat
import dev.zwander.installwithoptions.BuildConfig
import dev.zwander.installwithoptions.IOptionsApplier
import dev.zwander.installwithoptions.IShellInterface
import rikka.shizuku.Shizuku
import kotlin.system.exitProcess

class ShellInterface(context: Context) : IShellInterface.Stub() {
    private val realContext = context.createPackageContext("com.android.shell", 0)
    private val installer = InternalInstaller(realContext)

    @Suppress("INACCESSIBLE_TYPE")
    @Keep
    constructor() : this(kotlin.run {
        val systemContext = ActivityThread.systemMain().systemContext as Context
        val userHandle = UserHandleCompat.getUserHandleForUid(Shizuku.getUid())

        systemContext.createPackageContextAsUser(
            BuildConfig.APPLICATION_ID,
            Context.CONTEXT_INCLUDE_CODE or Context.CONTEXT_IGNORE_SECURITY,
            userHandle
        )
    })

    override fun install(
        fileDescriptors: Array<AssetFileDescriptor>,
        options: IntArray,
        splits: Boolean,
        applier: IOptionsApplier,
    ) {
        if (Looper.myLooper() == null) {
            Looper.prepare()
        }

        installer.installPackage(fileDescriptors, options, splits, applier)
    }

    override fun destroy() {
        exitProcess(0)
    }
}