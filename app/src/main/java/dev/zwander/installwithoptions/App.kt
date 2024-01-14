package dev.zwander.installwithoptions

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.os.Build
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.performance.BugsnagPerformance
import com.getkeepsafe.relinker.ReLinker
import org.lsposed.hiddenapibypass.HiddenApiBypass

class App : Application() {
    companion object {
        @SuppressLint("StaticFieldLeak")
        lateinit var context: Context
            private set
    }

    override fun onCreate() {
        super.onCreate()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            HiddenApiBypass.setHiddenApiExemptions("")
        }

        ReLinker.loadLibrary(this, "bugsnag-ndk")
        ReLinker.loadLibrary(this, "bugsnag-plugin-android-anr")

        Bugsnag.start(this)
        BugsnagPerformance.start(this)

        context = this
    }
}
