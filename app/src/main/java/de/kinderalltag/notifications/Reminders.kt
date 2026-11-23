package de.kinderalltag.notifications

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import de.kinderalltag.KinderApplication
import de.kinderalltag.MainActivity
import de.kinderalltag.R
import de.kinderalltag.data.*
import de.kinderalltag.domain.*
import de.kinderalltag.text.*
import java.time.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject

object ReminderScheduler {
    const val CHANNEL = "care_reminders"
    private const val TICK = "calendar_refresh"

    fun refresh(context: Context) {
        (context.applicationContext as? KinderApplication)?.repository?.state?.value?.let {
            update(context, it)
        }
    }

    fun expiresAt(item: JSONObject): Long =
        item.optLong(
            "expiresAt",
            LocalDate.parse(item.getString("date"))
                .plusDays(1)
                .atStartOfDay(FAMILY_ZONE)
                .toInstant()
                .toEpochMilli(),
        )

    private fun prefs(context: Context) =
        context.getSharedPreferences("reminders", Context.MODE_PRIVATE)

    fun enabled(context: Context) = prefs(context).getBoolean("enabled", false)

    fun setEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean("enabled", enabled).apply()
        if (!enabled) clear(context)
    }

    fun createChannel(context: Context) {
        context
            .getSystemService(NotificationManager::class.java)
            .createNotificationChannel(
                NotificationChannel(
                    CHANNEL,
                    text(TextKey.TASKS_APPOINTMENTS),
                    NotificationManager.IMPORTANCE_DEFAULT,
                )
            )
    }

    private fun intent(context: Context, id: String): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            0,
            Intent(context, ReminderReceiver::class.java)
                .setData(Uri.parse("kinderalltag://alarm/$id")),
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
        manager.activeNotifications
            .filter { it.notification.channelId == CHANNEL }
            .forEach { manager.cancel(it.tag, it.id) }
        prefs(context).edit().remove("records").remove("deliveredAppointments").apply()
    }

    // Completion cancellation must not depend on unrelated collections finishing their sync.
    fun cancelCompleted(context: Context, events: List<CareEvent>) {
        val completed =
            events
                .groupBy { it.occurrenceId }
                .filter { (id, history) -> Schedule.activeCompletions(history, id).isNotEmpty() }
                .keys
        if (completed.isEmpty()) return
        val alarm = context.getSystemService(AlarmManager::class.java)
        val manager = context.getSystemService(NotificationManager::class.java)
        val saved = JSONArray(prefs(context).getString("records", "[]"))
        val retained = JSONArray()
        for (index in 0 until saved.length()) {
            val item = saved.getJSONObject(index)
            val id = item.getString("id")
            if (id in completed) alarm.cancel(intent(context, id)) else retained.put(item)
        }
        manager.activeNotifications
            .filter { it.notification.channelId == CHANNEL && it.tag in completed }
            .forEach { manager.cancel(it.tag, it.id) }
        if (retained.length() != saved.length())
            prefs(context).edit().putString("records", retained.toString()).apply()
    }

    fun update(context: Context, state: FamilyState) {
        if (!enabled(context) || !state.loaded) return
        val now = Instant.now()
        val old = JSONArray(prefs(context).getString("records", "[]"))
        val oldIds = (0 until old.length()).map { old.getJSONObject(it).getString("id") }.toSet()
        val upcoming =
            FamilyReminders.plan(
                state.uid,
                state.sections,
                state.revisions,
                state.events,
                state.appointments,
                state.appointmentExceptions,
                now,
                oldIds,
                operations = state.itemOperations,
                activity = state.appointmentActivity,
            )
        val alarm = context.getSystemService(AlarmManager::class.java)
        val ids = upcoming.map { it.id }.toSet()
        for (i in 0 until old.length()) {
            val id = old.getJSONObject(i).getString("id")
            if (id !in ids) alarm.cancel(intent(context, id))
        }
        cancelCompleted(context, state.events)
        context
            .getSystemService(NotificationManager::class.java)
            .activeNotifications
            .filter {
                it.notification.channelId == CHANNEL && it.tag?.startsWith("appointment_") != true
            }
            .forEach { notification ->
                val tag = notification.tag ?: return@forEach
                val date =
                    runCatching {
                            LocalDate.parse(tag.substringBeforeLast('_').substringAfterLast('_'))
                        }
                        .getOrNull()
                val occurrence =
                    date?.let { day -> state.taskOccurrences(day).firstOrNull { it.id == tag } }
                if (occurrence == null || occurrence.instant > now)
                    context
                        .getSystemService(NotificationManager::class.java)
                        .cancel(tag, notification.id)
            }
        val records = JSONArray()
        upcoming.forEach { occurrence ->
            records.put(
                JSONObject()
                    .put("id", occurrence.id)
                    .put("date", occurrence.date)
                    .put("at", occurrence.at.toEpochMilli())
                    .put("title", occurrence.title)
                    .put("itemTitle", occurrence.itemTitle)
                    .put("subjectId", occurrence.subjectId ?: JSONObject.NULL)
                    .put("recipient", occurrence.recipient?.name ?: JSONObject.NULL)
                    .put("subjectName", occurrence.subjectName ?: JSONObject.NULL)
                    .put("kind", occurrence.kind)
                    .put("expiresAt", occurrence.expiresAt.toEpochMilli())
                    .put("fingerprint", occurrence.fingerprint)
            )
        }
        val delivered = JSONArray(prefs(context).getString("deliveredAppointments", "[]"))
        val retained = JSONArray()
        for (index in 0 until delivered.length()) {
            val item = delivered.getJSONObject(index)
            val date = LocalDate.parse(item.getString("date"))
            val occurrence =
                state.appointmentOccurrences(date, date).firstOrNull {
                    it.id == item.getString("id")
                }
            if (
                occurrence != null &&
                    isResponsible(occurrence.details.responsibleParentId, state.uid) &&
                    occurrence.details.endInstant > now &&
                    occurrence.reminderFingerprint() == item.optString("fingerprint")
            ) {
                retained.put(item)
            } else
                context
                    .getSystemService(NotificationManager::class.java)
                    .cancel(item.getString("id"), 0)
        }
        prefs(context)
            .edit()
            .putString("records", records.toString())
            .putString("deliveredAppointments", retained.toString())
            .apply()
        restore(context)
    }

    fun restore(context: Context) {
        if (!enabled(context)) return
        val alarm = context.getSystemService(AlarmManager::class.java)
        val records = JSONArray(prefs(context).getString("records", "[]"))
        for (i in 0 until records.length()) {
            val item = records.getJSONObject(i)
            if (expiresAt(item) > System.currentTimeMillis())
                alarm.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    maxOf(item.getLong("at"), System.currentTimeMillis() + 1000),
                    intent(context, item.getString("id")),
                )
        }
        val tomorrow = LocalDate.now(FAMILY_ZONE).plusDays(1).atTime(0, 1).atZone(FAMILY_ZONE)
        alarm.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            tomorrow.toInstant().toEpochMilli(),
            intent(context, TICK),
        )
    }

    fun record(context: Context, id: String): JSONObject? {
        val records = JSONArray(prefs(context).getString("records", "[]"))
        return (0 until records.length())
            .map { records.getJSONObject(it) }
            .firstOrNull { it.getString("id") == id }
    }

    fun consume(context: Context, id: String) {
        val records = JSONArray(prefs(context).getString("records", "[]"))
        val remaining = JSONArray()
        for (index in 0 until records.length()) {
            val item = records.getJSONObject(index)
            if (item.getString("id") != id) remaining.put(item)
            else if (item.optString("kind") == "appointment") {
                val delivered = JSONArray(prefs(context).getString("deliveredAppointments", "[]"))
                delivered.put(item)
                prefs(context)
                    .edit()
                    .putString("deliveredAppointments", delivered.toString())
                    .apply()
            }
        }
        prefs(context).edit().putString("records", remaining.toString()).apply()
        context.getSystemService(AlarmManager::class.java).cancel(intent(context, id))
    }
}

internal fun localizedReminderTitle(context: Context, item: JSONObject): String {
    if (item.has("itemTitle")) {
        fun field(key: String): String? =
            if (item.isNull(key)) null else item.optString(key).takeIf { it.isNotEmpty() }
        return reminderDisplayTitle(
            item.getString("itemTitle"),
            field("subjectId"),
            field("recipient")?.let {
                runCatching { AppointmentRecipient.valueOf(it) }.getOrNull()
            },
            field("subjectName"),
        )
    }
    // Older payloads contained a preformatted title. Reconstruct from the existing cache if
    // possible.
    val state = (context.applicationContext as? KinderApplication)?.repository?.state?.value
    val day = runCatching { LocalDate.parse(item.optString("date")) }.getOrNull()
    if (state != null && day != null) {
        val id = item.getString("id")
        state
            .taskOccurrences(day)
            .firstOrNull { it.id == id }
            ?.let {
                return reminderDisplayTitle(
                    it.revision.title,
                    it.revision.childId,
                    null,
                    state.sections.firstOrNull { child -> child.id == it.revision.childId }?.name,
                )
            }
        state
            .appointmentOccurrences(day, day)
            .firstOrNull { it.id == id }
            ?.let {
                return reminderDisplayTitle(
                    it.details.title,
                    null,
                    it.details.recipient,
                    appointmentRecipientName(it.details.recipient, state.sections),
                )
            }
    }
    return text(if (item.optString("kind") == "appointment") TextKey.APPOINTMENT else TextKey.TASK)
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (!ReminderScheduler.enabled(context)) return
        val id = intent.data?.lastPathSegment ?: return
        val item = ReminderScheduler.record(context, id)
        // A queued alarm for an older schedule must not consume its newer replacement.
        if (item != null && item.getLong("at") > System.currentTimeMillis()) {
            ReminderScheduler.restore(context)
            return
        }
        ReminderScheduler.consume(context, id)
        ReminderScheduler.restore(context)
        if (id == "calendar_refresh") ReminderScheduler.refresh(context)
        if (item == null) return
        if (item.optString("kind", "task") == "task") {
            val pending = goAsync()
            CoroutineScope(Dispatchers.Main.immediate).launch {
                try {
                    val repository =
                        (context.applicationContext as? KinderApplication)?.repository
                            ?: return@launch
                    val uid = repository.state.value.uid ?: return@launch
                    // Prefer synchronized data after a cold start. A fully loaded cache still
                    // supports offline reminders; incomplete state cannot authorize delivery.
                    withTimeoutOrNull(6_000) {
                        repository.state.first { state ->
                            state.uid != uid ||
                                (state.accessResolved && !state.authorized) ||
                                Schedule.activeCompletions(state.events, id).isNotEmpty() ||
                                (state.loaded && state.authorized && !state.fromCache)
                        }
                    }
                    val state = repository.state.value
                    if (state.uid != uid || !state.authorized || !state.loaded) return@launch
                    if (
                        !FamilyReminders.taskNeedsReminder(
                            id,
                            LocalDate.parse(item.getString("date")),
                            Instant.ofEpochMilli(item.getLong("at")),
                            uid,
                            state.revisions,
                            state.events,
                            state.itemOperations,
                            Instant.now(),
                        )
                    )
                        return@launch
                    showNotification(context, id, item)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.w("KinderalltagReminder", "Task reminder validation failed", e)
                } finally {
                    pending.finish()
                }
            }
            return
        }
        showNotification(context, id, item)
    }

    private fun showNotification(context: Context, id: String, item: JSONObject) {
        if (!ReminderScheduler.enabled(context)) return
        if (ReminderScheduler.expiresAt(item) <= System.currentTimeMillis()) return
        if (
            Build.VERSION.SDK_INT >= 33 &&
                context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
                    PackageManager.PERMISSION_GRANTED
        )
            return
        val open =
            Intent(context, MainActivity::class.java)
                .setData(Uri.parse("kinderalltag://occurrence/$id"))
                .putExtra("date", item.getString("date"))
                .putExtra("occurrence", id)
                .putExtra("kind", item.optString("kind", "task"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val tap =
            PendingIntent.getActivity(
                context,
                0,
                open,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        val notification =
            Notification.Builder(context, ReminderScheduler.CHANNEL)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(localizedReminderTitle(context, item))
                .setContentText(
                    if (item.optString("kind") == "appointment")
                        text(TextKey.OPEN_APPOINTMENT_AND_CHECK_DETAILS)
                    else text(TextKey.OPEN_TASK_AND_CHECK_THE_SHARED_STATUS)
                )
                .setVisibility(Notification.VISIBILITY_PRIVATE)
                .setContentIntent(tap)
                .setAutoCancel(true)
                .build()
        context.getSystemService(NotificationManager::class.java).notify(id, 0, notification)
    }
}

class RescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (
            intent.action in
                setOf(
                    Intent.ACTION_BOOT_COMPLETED,
                    Intent.ACTION_TIME_CHANGED,
                    Intent.ACTION_TIMEZONE_CHANGED,
                    Intent.ACTION_MY_PACKAGE_REPLACED,
                )
        ) {
            ReminderScheduler.restore(context)
            ReminderScheduler.refresh(context)
        }
    }
}
