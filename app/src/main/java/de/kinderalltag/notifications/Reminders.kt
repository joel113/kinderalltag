package de.kinderalltag.notifications

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import de.kinderalltag.MainActivity
import de.kinderalltag.R
import de.kinderalltag.data.FamilyState
import de.kinderalltag.domain.*
import org.json.JSONArray
import org.json.JSONObject
import java.time.*

object ReminderScheduler {
    const val CHANNEL = "care_reminders"
    private const val TICK = "calendar_refresh"
    private fun prefs(context: Context) = context.getSharedPreferences("reminders", Context.MODE_PRIVATE)
    fun enabled(context: Context) = prefs(context).getBoolean("enabled", false)
    fun setEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean("enabled", enabled).apply()
        if (!enabled) clear(context)
    }
    fun createChannel(context: Context) {
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL, "Aufgabenerinnerungen", NotificationManager.IMPORTANCE_DEFAULT)
        )
    }
    private fun intent(context: Context, id: String): PendingIntent = PendingIntent.getBroadcast(
        context, 0, Intent(context, ReminderReceiver::class.java).setData(Uri.parse("kinderalltag://alarm/$id")),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
    fun clear(context: Context) {
        val alarm = context.getSystemService(AlarmManager::class.java)
        val manager = context.getSystemService(NotificationManager::class.java)
        val records = JSONArray(prefs(context).getString("records", "[]"))
        for (i in 0 until records.length()) {
            val id = records.getJSONObject(i).getString("id")
            alarm.cancel(intent(context, id))
        }
        alarm.cancel(intent(context, TICK))
        manager.cancelAll()
        prefs(context).edit().remove("records").apply()
    }
    fun update(context: Context, state: FamilyState) {
        if (!enabled(context) || !state.loaded) return
        val now = Instant.now()
        val children = state.sections.associate { it.id to it.name }
        val old = JSONArray(prefs(context).getString("records", "[]"))
        val oldIds = (0 until old.length()).map { old.getJSONObject(it).getString("id") }.toSet()
        val upcoming = Schedule.reminders(state.revisions, state.events, now, oldIds)
        val alarm = context.getSystemService(AlarmManager::class.java)
        val ids = upcoming.map { it.id }.toSet()
        for (i in 0 until old.length()) {
            val id = old.getJSONObject(i).getString("id")
            if (id !in ids) alarm.cancel(intent(context, id))
        }
        // Cancel a delivered notification once its completion reaches this phone.
        state.events.groupBy { it.occurrenceId }.forEach { (id, events) ->
            if (Schedule.activeCompletions(events, id).isNotEmpty())
                context.getSystemService(NotificationManager::class.java).cancel(id, 0)
        }
        val records = JSONArray()
        upcoming.forEach { occurrence ->
            records.put(JSONObject().put("id", occurrence.id).put("date", occurrence.date.toString())
                .put("at", occurrence.instant.toEpochMilli())
                .put("title", "${children[occurrence.revision.childId] ?: "Kind"}: ${occurrence.revision.title}"))
        }
        prefs(context).edit().putString("records", records.toString()).apply()
        restore(context)
    }
    fun restore(context: Context) {
        if (!enabled(context)) return
        val alarm = context.getSystemService(AlarmManager::class.java)
        val records = JSONArray(prefs(context).getString("records", "[]"))
        for (i in 0 until records.length()) {
            val item = records.getJSONObject(i)
            if (item.getString("date") >= LocalDate.now(FAMILY_ZONE).toString()) alarm.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP, maxOf(item.getLong("at"), System.currentTimeMillis() + 1000), intent(context, item.getString("id")))
        }
        val tomorrow = LocalDate.now(FAMILY_ZONE).plusDays(1).atTime(0, 1).atZone(FAMILY_ZONE)
        alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, tomorrow.toInstant().toEpochMilli(), intent(context, TICK))
    }
    fun record(context: Context, id: String): JSONObject? {
        val records = JSONArray(prefs(context).getString("records", "[]"))
        return (0 until records.length()).map { records.getJSONObject(it) }.firstOrNull { it.getString("id") == id }
    }
    fun consume(context: Context, id: String) {
        val records = JSONArray(prefs(context).getString("records", "[]"))
        val remaining = JSONArray()
        for (index in 0 until records.length()) {
            val item = records.getJSONObject(index)
            if (item.getString("id") != id) remaining.put(item)
        }
        prefs(context).edit().putString("records", remaining.toString()).apply()
        context.getSystemService(AlarmManager::class.java).cancel(intent(context, id))
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (!ReminderScheduler.enabled(context)) return
        val id = intent.data?.lastPathSegment ?: return
        val item = ReminderScheduler.record(context, id)
        ReminderScheduler.consume(context, id)
        ReminderScheduler.restore(context)
        if (item == null) return
        // Do not deliver yesterday's alarm after a long shutdown.
        if (item.getString("date") != LocalDate.now(FAMILY_ZONE).toString()) return
        if (Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED) return
        val open = Intent(context, MainActivity::class.java)
            .setData(Uri.parse("kinderalltag://occurrence/$id"))
            .putExtra("date", item.getString("date")).putExtra("occurrence", id)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val tap = PendingIntent.getActivity(context, 0, open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = Notification.Builder(context, ReminderScheduler.CHANNEL)
            .setSmallIcon(R.drawable.ic_notification).setContentTitle(item.getString("title"))
            .setContentText("Aufgabe öffnen und gemeinsamen Status prüfen")
            .setVisibility(Notification.VISIBILITY_PRIVATE).setContentIntent(tap).setAutoCancel(true).build()
        context.getSystemService(NotificationManager::class.java).notify(id, 0, notification)
    }
}

class RescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action in setOf(Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_TIME_CHANGED,
                Intent.ACTION_TIMEZONE_CHANGED, Intent.ACTION_MY_PACKAGE_REPLACED))
            ReminderScheduler.restore(context)
    }
}
