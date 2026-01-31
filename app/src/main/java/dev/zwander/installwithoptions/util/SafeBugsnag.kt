package dev.zwander.installwithoptions.util

import com.bugsnag.android.BreadcrumbType
import com.bugsnag.android.Bugsnag

object SafeBugsnag {
    fun leaveBreadcrumb(
        message: String,
        metadata: Map<String, Any>? = mapOf(),
        breadcrumbType: BreadcrumbType = BreadcrumbType.MANUAL,
    ) {
        if (Bugsnag.isStarted()) {
            Bugsnag.leaveBreadcrumb(message, metadata, breadcrumbType)
        }
    }
}
