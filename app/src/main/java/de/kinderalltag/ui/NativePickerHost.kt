package de.kinderalltag.ui

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.util.Log
import android.view.WindowManager
import androidx.appcompat.view.ContextThemeWrapper
import de.kinderalltag.R

internal class NativePickerHost(val activity: Activity, val context: Context) {
    fun show(dialog: Dialog): Boolean {
        if (activity.isFinishing || activity.isDestroyed) return false
        dialog.setOwnerActivity(activity)
        return try {
            dialog.show()
            true
        } catch (error: WindowManager.BadTokenException) {
            // The system can invalidate a window during an activity/language transition.
            Log.w("KinderalltagPicker", "Picker activity window is no longer available", error)
            false
        }
    }
}

internal fun nativePickerHost(context: Context): NativePickerHost? {
    var current = context
    while (current !is Activity) {
        val wrapper = current as? ContextWrapper ?: return null
        val base = wrapper.baseContext
        if (base === current) return null
        current = base
    }
    if (current.isFinishing || current.isDestroyed) return null
    // Override resources on an activity-backed wrapper. A createConfigurationContext base
    // supplies a non-activity WindowManager and loses the token needed by Dialog.show().
    val localized = ContextThemeWrapper(current, R.style.Theme_Kinderalltag)
    localized.applyOverrideConfiguration(
        Configuration().apply { setLocale(presentationLocale(context.resources.configuration)) }
    )
    return NativePickerHost(current, localized)
}
