package de.kinderalltag

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import de.kinderalltag.data.FamilyRepository
import de.kinderalltag.data.CalendarInbox
import de.kinderalltag.notifications.ReminderScheduler
import de.kinderalltag.notifications.ActivityNotifications
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect

class KinderApplication : Application() {
    val calendarInbox by lazy { CalendarInbox(this) }
    var repository: FamilyRepository? = null
        private set
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    override fun onCreate() {
        super.onCreate()
        ReminderScheduler.createChannel(this)
        ActivityNotifications.createChannel(this)
        if (FirebaseApp.initializeApp(this) != null) {
            repository = FamilyRepository(FirebaseAuth.getInstance(), FirebaseFirestore.getInstance())
            scope.launch {
                repository!!.state.collect { state ->
                    ActivityNotifications.update(this@KinderApplication, state)
                    if (state.authorized && state.loaded && state.error == null) ReminderScheduler.update(this@KinderApplication, state)
                    else if (state.uid == null || (!state.authorized && state.accessResolved)) ReminderScheduler.clear(this@KinderApplication)
                }
            }
        }
    }
}
