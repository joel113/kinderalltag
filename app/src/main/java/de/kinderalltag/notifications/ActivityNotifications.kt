package de.kinderalltag.notifications

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import de.kinderalltag.MainActivity
import de.kinderalltag.R
import de.kinderalltag.data.FamilyState
import de.kinderalltag.data.taskOccurrences
import de.kinderalltag.domain.*
import de.kinderalltag.text.*

object ActivityNotifications {
    private const val CHANNEL = "family_activity"
    private const val TAG_PREFIX = "family-activity:"
    private val tracker = FamilyActivityNoticeTracker()
    private var observedUid: String? = null

    private fun prefs(context: Context) =
        context.getSharedPreferences("family_activity_notifications", Context.MODE_PRIVATE)

    fun enabled(context: Context, uid: String?, kind: FamilyNoticeKind): Boolean =
        uid != null && prefs(context).getBoolean("$uid:${kind.name}", false)

    fun setEnabled(context: Context, uid: String?, kind: FamilyNoticeKind, enabled: Boolean) {
        if (uid == null) return
        prefs(context).edit().putBoolean("$uid:${kind.name}", enabled).apply()
        if (!enabled) clear(context, kind)
    }

    fun createChannel(context: Context) {
        context
            .getSystemService(NotificationManager::class.java)
            .createNotificationChannel(
                NotificationChannel(
                    CHANNEL,
                    text(TextKey.FAMILY_UPDATES),
                    NotificationManager.IMPORTANCE_DEFAULT,
                )
            )
    }

    fun clear(context: Context, kind: FamilyNoticeKind? = null) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.activeNotifications
            .filter {
                it.tag?.startsWith(TAG_PREFIX) == true &&
                    (kind == null || it.tag?.startsWith("$TAG_PREFIX${kind.name}:") == true)
            }
            .forEach { manager.cancel(it.tag, it.id) }
    }

    fun update(context: Context, state: FamilyState) {
        val uid = state.uid
        if (observedUid != uid) {
            clear(context)
            observedUid = uid
        }
        if (uid == null || (!state.authorized && state.accessResolved)) {
            tracker.reset()
            clear(context)
            return
        }
        val ready =
            state.authorized &&
                state.loaded &&
                !state.fromCache &&
                !state.pending &&
                state.error == null
        val notices =
            tracker.observe(uid, ready, state.revisions, state.appointmentActivity, state.events)
        val deleted =
            activeDeletions(state.itemOperations).filter { it.scope == OperationScope.ALL }
        fun isDeleted(kind: FamilyNoticeKind, id: String): Boolean {
            if (kind == FamilyNoticeKind.TASK_COMPLETED) {
                val occurrenceId = id.removePrefix("complete:")
                val completion =
                    Schedule.activeCompletions(state.events, occurrenceId)
                        .minWithOrNull(
                            compareBy<CareEvent> {
                                    if (it.clientMillis > 0) it.clientMillis
                                    else it.serverMillis ?: 0
                                }
                                .thenBy { it.id }
                        ) ?: return true
                val date = completion.taskSnapshot?.date ?: completion.date
                return state.taskOccurrences(date).none { it.id == occurrenceId }
            }
            val type =
                if (kind == FamilyNoticeKind.NEW_TASK) ItemType.TASK else ItemType.APPOINTMENT
            val itemId =
                if (kind == FamilyNoticeKind.NEW_TASK) id.removePrefix("task:")
                else {
                    val event =
                        state.appointmentActivity.firstOrNull {
                            it.id == id.removePrefix("appointment:")
                        } ?: return false
                    appointmentRootId(event.seriesId, state.appointments, state.appointmentActivity)
                }
            return deleted.any { it.type == type && it.itemId == itemId }
        }
        val manager = context.getSystemService(NotificationManager::class.java)
        if (ready)
            manager.activeNotifications
                .filter { it.tag?.startsWith(TAG_PREFIX) == true }
                .forEach { existing ->
                    val tag = existing.tag ?: return@forEach
                    FamilyNoticeKind.entries.forEach { kind ->
                        val prefix = "$TAG_PREFIX${kind.name}:"
                        if (tag.startsWith(prefix) && isDeleted(kind, tag.removePrefix(prefix)))
                            manager.cancel(tag, existing.id)
                    }
                }
        notices.forEach { notice ->
            if (isDeleted(notice.kind, notice.id)) return@forEach
            if (!enabled(context, uid, notice.kind) || !manager.areNotificationsEnabled())
                return@forEach
            if (
                Build.VERSION.SDK_INT >= 33 &&
                    context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
                        PackageManager.PERMISSION_GRANTED
            )
                return@forEach
            val page =
                when (notice.kind) {
                    FamilyNoticeKind.NEW_TASK -> "tasks"
                    FamilyNoticeKind.NEW_APPOINTMENT -> "appointments"
                    FamilyNoticeKind.TASK_COMPLETED -> "home"
                }
            val tag = "$TAG_PREFIX${notice.kind.name}:${notice.id}"
            val open =
                Intent(context, MainActivity::class.java)
                    .setData(Uri.parse("kinderalltag://activity/$page/${Uri.encode(notice.id)}"))
                    .putExtra("openPage", page)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            notice.occurrenceId?.let {
                open.putExtra("occurrence", it).putExtra("date", notice.date)
            }
            val tap =
                PendingIntent.getActivity(
                    context,
                    0,
                    open,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
            val author =
                state.parents.firstOrNull { it.id == notice.authorId }?.name
                    ?: text(TextKey.YOUR_PARTNER)
            val notification =
                Notification.Builder(context, CHANNEL)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle(
                        when (notice.kind) {
                            FamilyNoticeKind.NEW_TASK -> text(TextKey.NEW_TASK)
                            FamilyNoticeKind.NEW_APPOINTMENT -> text(TextKey.NEW_APPOINTMENT)
                            FamilyNoticeKind.TASK_COMPLETED -> text(TextKey.TASK_COMPLETED)
                        }
                    )
                    .setContentText("$author: ${notice.title}")
                    .setContentIntent(tap)
                    .setAutoCancel(true)
                    .setVisibility(Notification.VISIBILITY_PRIVATE)
                    .build()
            manager.notify(tag, 0, notification)
        }
    }
}
