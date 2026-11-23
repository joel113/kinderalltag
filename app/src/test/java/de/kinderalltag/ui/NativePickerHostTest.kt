package de.kinderalltag.ui

import android.app.Application
import android.app.DatePickerDialog
import android.app.Dialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ContextThemeWrapper
import de.kinderalltag.R
import java.util.Locale
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [32, 35])
class NativePickerHostTest {
    class PickerActivity : AppCompatActivity() {
        override fun onCreate(savedInstanceState: Bundle?) {
            setTheme(R.style.Theme_Kinderalltag)
            super.onCreate(savedInstanceState)
        }
    }

    @Test
    fun localizedDateAndTimePickersKeepTheActivityWindowManagerAndShow() {
        val controller = Robolectric.buildActivity(PickerActivity::class.java).setup().visible()
        try {
            val activity = controller.get()
            listOf(Locale.GERMANY, Locale.UK).forEach { locale ->
                val source =
                    ContextThemeWrapper(activity, R.style.Theme_Kinderalltag).apply {
                        applyOverrideConfiguration(Configuration().apply { setLocale(locale) })
                    }
                val host = requireNotNull(nativePickerHost(source))
                assertSame(activity, host.activity)
                assertSame(
                    activity.getSystemService(Context.WINDOW_SERVICE),
                    host.context.getSystemService(Context.WINDOW_SERVICE),
                )
                assertEquals(
                    locale.language,
                    host.context.resources.configuration.locales[0].language,
                )
                val date = DatePickerDialog(host.context, null, 2026, 10, 23)
                val time = TimePickerDialog(host.context, null, 18, 30, true)
                try {
                    assertTrue(host.show(date))
                    assertSame(activity, date.ownerActivity)
                    assertTrue(date.isShowing)
                    assertTrue(host.show(time))
                    assertSame(activity, time.ownerActivity)
                    assertTrue(time.isShowing)
                } finally {
                    date.dismiss()
                    time.dismiss()
                }
            }
        } finally {
            controller.pause().stop().destroy()
        }
    }

    @Test
    fun expiredWindowTokenClosesThePickerWithoutCrashing() {
        val controller = Robolectric.buildActivity(PickerActivity::class.java).setup().visible()
        try {
            val host = requireNotNull(nativePickerHost(controller.get()))
            val dialog =
                object : Dialog(host.context) {
                    override fun show() {
                        throw WindowManager.BadTokenException(
                            "Window invalidated during recreation"
                        )
                    }
                }
            assertFalse(host.show(dialog))
        } finally {
            controller.pause().stop().destroy()
        }
    }

    @Test
    fun applicationAndConfigurationOnlyContextsAreNotDialogHosts() {
        val app = RuntimeEnvironment.getApplication()
        assertNull(nativePickerHost(app))
        assertNull(nativePickerHost(app.createConfigurationContext(Configuration())))
    }

    @Test
    fun finishingActivityDoesNotShowAPicker() {
        val controller = Robolectric.buildActivity(PickerActivity::class.java).setup().visible()
        val activity = controller.get()
        val host = requireNotNull(nativePickerHost(activity))
        val dialog = DatePickerDialog(host.context, null, 2026, 10, 23)
        activity.finish()
        assertNull(nativePickerHost(activity))
        assertFalse(host.show(dialog))
        assertFalse(dialog.isShowing)
        controller.pause().stop().destroy()
    }

    @Test
    fun recreatedActivityGetsANewHostAndTheDestroyedHostIsRejected() {
        val controller = Robolectric.buildActivity(PickerActivity::class.java).setup().visible()
        try {
            val old = requireNotNull(nativePickerHost(controller.get()))
            val stale = DatePickerDialog(old.context, null, 2026, 10, 23)
            controller.recreate()
            assertTrue(old.activity.isDestroyed)
            assertFalse(old.show(stale))
            val current = requireNotNull(nativePickerHost(controller.get()))
            val dialog = DatePickerDialog(current.context, null, 2026, 10, 24)
            try {
                assertNotSame(old.activity, current.activity)
                assertTrue(current.show(dialog))
                assertTrue(dialog.isShowing)
            } finally {
                dialog.dismiss()
            }
        } finally {
            controller.pause().stop().destroy()
        }
    }
}
