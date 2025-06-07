package dev.zwander.installwithoptions.util

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.os.Build
import android.os.IUserManager
import android.os.Looper
import android.os.ServiceManager
import android.os.UserHandle
import dev.zwander.installwithoptions.IOptionsApplier
import dev.zwander.installwithoptions.IShellInterface
import org.lsposed.hiddenapibypass.HiddenApiBypass
import kotlin.system.exitProcess

@Suppress("RedundantConstructorKeyword")
class ShellInterface constructor() : IShellInterface.Stub() {
    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            HiddenApiBypass.setHiddenApiExemptions("")
        }
    }

    override fun install(
        fileDescriptors: Map<*, *>,
        options: IntArray,
        applier: IOptionsApplier,
        installerPackageName: String?,
        userId: Int,
    ) {
        if (Looper.myLooper() == null) {
            Looper.prepare()
        }

        val actualUserId = userId.takeIf { it != Int.MIN_VALUE } ?: myUserId()

        val installer = InternalInstaller(createContext())

        @Suppress("UNCHECKED_CAST")
        installer.installPackage(
            fileDescriptors as Map<String, List<AssetFileDescriptor>>,
            options,
            applier,
            installerPackageName.takeIf { !it.isNullOrBlank() } ?: "shell",
            actualUserId,
        )
    }

    override fun destroy() {
        exitProcess(0)
    }

    override fun getUserIds(): List<Int> {
        val userManager = IUserManager.Stub.asInterface(ServiceManager.getService(Context.USER_SERVICE))

        val users = userManager.getUsers(false, false, false)
        val profiles = users.flatMap { userManager.getProfiles(it.id, false).map { it.id } }

        return profiles
    }

    private fun createContext(): Context {
        val activityThreadClass = Class.forName("android.app.ActivityThread")
        val systemMain = activityThreadClass.getMethod("systemMain")
            .invoke(null)
        val systemContext = activityThreadClass.getMethod("getSystemContext")
            .invoke(systemMain) as Context

        val context = systemContext::class.java.getMethod(
            "createPackageContextAsUser",
            String::class.java,
            Int::class.java,
            UserHandle::class.java,
        ).invoke(
            systemContext,
            "com.android.shell",
            Context.CONTEXT_INCLUDE_CODE or Context.CONTEXT_IGNORE_SECURITY,
            UserHandle(myUserId()),
        ) as Context

        return context.createPackageContext("com.android.shell", 0)
    }
}

fun myUserId() = UserHandle::class.java.getMethod("myUserId").invoke(null) as Int
