package dev.zwander.installwithoptions.util

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.AssetFileDescriptor
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.os.Process
import android.os.UserHandle
import dev.zwander.installwithoptions.IErrorCallback
import dev.zwander.installwithoptions.IOptionsApplier
import dev.zwander.installwithoptions.IShellInterface
import org.lsposed.hiddenapibypass.HiddenApiBypass
import rikka.shizuku.SystemServiceHelper
import kotlin.system.exitProcess

@SuppressLint("PrivateApi")
@Suppress("RedundantConstructorKeyword")
class ShellInterface constructor() : IShellInterface.Stub() {
    private val installer by lazy {
        InternalInstaller(createContext())
    }

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
        errorCallback: IErrorCallback,
    ) {
        if (Looper.myLooper() == null) {
            Looper.prepare()
        }

        val actualUserId = userId.takeIf { it != Int.MIN_VALUE } ?: myUserId()

        try {
            @Suppress("UNCHECKED_CAST")
            installer.installPackage(
                fileDescriptors as Map<String, List<AssetFileDescriptor>>,
                options,
                applier,
                installerPackageName.takeIf { !it.isNullOrBlank() } ?: "shell",
                actualUserId,
            )
        } catch (e: Throwable) {
            errorCallback.onError(e.extractErrorMessage())
        }
    }

    override fun destroy() {
        exitProcess(0)
    }

    override fun getUserIds(): List<Int> {
        val userInfoClass = Class.forName("android.content.pm.UserInfo")
        val userInfoIdField = userInfoClass.getField("id")
        val userManagerInstance = Class.forName("android.os.IUserManager\$Stub")
            .getMethod("asInterface", IBinder::class.java)
            .invoke(null, SystemServiceHelper.getSystemService(Context.USER_SERVICE))
        val users = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            userManagerInstance::class.java.getMethod(
                "getUsers",
                Boolean::class.java,
                Boolean::class.java,
                Boolean::class.java,
            ).invoke(userManagerInstance, false, false, false)
        } else {
            userManagerInstance::class.java.getMethod(
                "getUsers",
                Boolean::class.java,
            ).invoke(userManagerInstance, false)
        } as? List<*>
        val getProfiles = userManagerInstance::class.java.getMethod(
            "getProfiles",
            Int::class.java,
            Boolean::class.java,
        )

        @Suppress("UNCHECKED_CAST")
        val profiles = users?.flatMap { user ->
            (getProfiles.invoke(userManagerInstance, userInfoIdField.get(user) as Int, false) as? List<*>)?.map { userInfoIdField.get(it) as Int }
                ?: listOf()
        } ?: listOf()

        return profiles
    }

    override fun isRootOrSystem(): Boolean {
        val uid = Process.myUid()

        return uid == 1000 || uid == 0
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
            UserHandle::class.java.getConstructor(Int::class.java).newInstance(myUserId()),
        ) as Context

        return context.createPackageContext("com.android.shell", 0)
    }
}

fun myUserId() = UserHandle::class.java.getMethod("myUserId").invoke(null) as Int
