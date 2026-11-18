package de.kinderalltag.data

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import de.kinderalltag.domain.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import java.time.*
import java.util.UUID

data class FamilyState(
    val uid: String? = null,
    val name: String = "",
    val authorized: Boolean = false,
    val accessResolved: Boolean = false,
    val loaded: Boolean = false,
    val children: List<Child> = emptyList(),
    val parents: List<Parent> = emptyList(),
    val appointments: List<AppointmentSeries> = emptyList(),
    val appointmentExceptions: List<AppointmentException> = emptyList(),
    val appointmentActivity: List<AppointmentActivity> = emptyList(),
    val revisions: List<TaskRevision> = emptyList(),
    val events: List<CareEvent> = emptyList(),
    val itemOperations: List<ItemOperation> = emptyList(),
    val listItems: List<FamilyListItem> = emptyList(),
    val listActivity: List<ListActivity> = emptyList(),
    val listLabels: List<FamilyLabel> = emptyList(),
    val fromCache: Boolean = true,
    val pending: Boolean = false,
    val error: String? = null,
) {
    val sections: List<Child>
        get() = children.filterNot { it.id == HOUSEHOLD_SECTION.id } + HOUSEHOLD_SECTION
}

private val DATA_COLLECTIONS = setOf("children", "revisions", "events", "members", "appointmentSeries", "appointmentExceptions", "appointmentActivity", "itemOperations", "listItems", "listActivity", "listLabels")

class FamilyRepository(private val auth: FirebaseAuth, private val db: FirebaseFirestore) {
    private val mutable = MutableStateFlow(FamilyState(uid = auth.currentUser?.uid,
        name = auth.currentUser?.displayName ?: "Elternteil"))
    val state = mutable.asStateFlow()
    val lists = FamilyLists(auth, db) { mutable.value }
    private val household = db.collection("households").document("family")
    private var membership: ListenerRegistration? = null
    private val listeners = mutableListOf<ListenerRegistration>()
    private val cacheFlags = mutableMapOf<String, Boolean>()
    private val pendingFlags = mutableMapOf<String, Boolean>()
    private val collectionErrors = mutableMapOf<String, SyncIssue>()
    private var generation = 0

    init { auth.addAuthStateListener { observeUser() } }

    private fun stopData() {
        listeners.forEach { it.remove() }; listeners.clear()
        cacheFlags.clear(); pendingFlags.clear()
        collectionErrors.clear()
    }
    fun retrySync() { observeUser(keepData = true) }

    private fun observeUser(keepData: Boolean = false) {
        membership?.remove(); stopData()
        val currentGeneration = ++generation
        val user = auth.currentUser
        val previous = mutable.value
        mutable.value = if (keepData && previous.uid == user?.uid && previous.authorized) {
            previous.copy(loaded = false, fromCache = true, error = null)
        } else FamilyState(uid = user?.uid, name = user?.displayName ?: "Elternteil")
        if (user == null) return
        membership = household.collection("members").document(user.uid).addSnapshotListener(MetadataChanges.INCLUDE) { doc, error ->
            if (currentGeneration != generation || auth.currentUser?.uid != user.uid) return@addSnapshotListener
            if (error != null || doc?.exists() != true) {
                if (error != null) Log.e("KinderalltagSync", "Membership listener failed: ${error.code}", error)
                stopData()
                mutable.value = FamilyState(uid = user.uid, name = user.displayName ?: "Elternteil",
                    accessResolved = error != null || doc?.metadata?.isFromCache == false,
                    error = error?.let { syncErrorSummary(listOf(SyncIssue("membership", it.code.name))) })
            } else if (listeners.isEmpty()) {
                mutable.value = mutable.value.copy(authorized = true, accessResolved = true, error = null)
                observeCollection("children") { snapshot ->
                    mutable.value = mutable.value.copy(children = snapshot.documents.map {
                        Child(it.id, requireNotNull(it.getString("name")), BearColor.fromStored(it.getString("bearColor"), it.id))
                    }.sortedBy { it.name })
                }
                observeCollection("revisions") { snapshot ->
                    mutable.value = mutable.value.copy(revisions = snapshot.documents.map(::decodeRevision))
                }
                observeCollection("events") { snapshot ->
                    mutable.value = mutable.value.copy(events = snapshot.documents.map(::decodeEvent))
                }
                observeCollection("members") { snapshot ->
                    mutable.value = mutable.value.copy(parents = snapshot.documents.map {
                        Parent(it.id, it.getString("name") ?: "Elternteil")
                    }.sortedBy { it.name })
                }
                observeCollection("appointmentSeries") { snapshot ->
                    mutable.value = mutable.value.copy(appointments = snapshot.documents.map(::decodeAppointment))
                }
                observeCollection("appointmentExceptions") { snapshot ->
                    mutable.value = mutable.value.copy(appointmentExceptions = snapshot.documents.map(::decodeAppointmentException))
                }
                observeCollection("appointmentActivity") { snapshot ->
                    mutable.value = mutable.value.copy(appointmentActivity = snapshot.documents.map(::decodeAppointmentActivity))
                }
                observeCollection("listItems") { snapshot -> mutable.value = mutable.value.copy(listItems = snapshot.documents.map { decodeListItem(requireNotNull(it.data)) }) }
                observeCollection("listLabels") { snapshot -> mutable.value = mutable.value.copy(listLabels = snapshot.documents.map { decodeLabel(requireNotNull(it.data)) }) }
                observeCollection("listActivity") { snapshot -> mutable.value = mutable.value.copy(listActivity = snapshot.documents.map(::decodeListActivity)) }
                observeCollection("itemOperations") { snapshot ->
                    mutable.value = mutable.value.copy(itemOperations = snapshot.documents.map(::decodeItemOperation))
                }
            }
        }
    }

    private fun observeCollection(name: String, apply: (QuerySnapshot) -> Unit) {
        val expectedUid = mutable.value.uid
        val currentGeneration = generation
        listeners += household.collection(name).addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
            if (currentGeneration != generation || auth.uid != expectedUid || !mutable.value.authorized) return@addSnapshotListener
            if (error != null) {
                Log.e("KinderalltagSync", "Collection listener failed: $name (${error.code})", error)
                collectionErrors[name] = SyncIssue(name, error.code.name)
                cacheFlags.remove(name)
                mutable.value = mutable.value.copy(loaded = false, fromCache = true, error = syncErrorSummary(collectionErrors.values))
            } else if (snapshot != null) {
                try {
                    apply(snapshot)
                    cacheFlags[name] = snapshot.metadata.isFromCache
                    pendingFlags[name] = snapshot.metadata.hasPendingWrites()
                    collectionErrors.remove(name)
                    mutable.value = mutable.value.copy(
                        loaded = cacheFlags.keys.containsAll(DATA_COLLECTIONS),
                        fromCache = !cacheFlags.keys.containsAll(DATA_COLLECTIONS) || cacheFlags.values.any { it },
                        pending = pendingFlags.values.any { it } || (!cacheFlags.keys.containsAll(DATA_COLLECTIONS) && mutable.value.pending),
                        error = syncErrorSummary(collectionErrors.values))
                } catch (e: Exception) {
                    Log.e("KinderalltagSync", "Invalid stored data in collection: $name", e)
                    collectionErrors[name] = SyncIssue(name, "INVALID_DATA")
                    cacheFlags.remove(name)
                    mutable.value = mutable.value.copy(loaded = false, fromCache = true, error = syncErrorSummary(collectionErrors.values))
                }
            }
        }
    }

    suspend fun saveRevision(revision: TaskRevision) {
        revision.validate()
        require(mutable.value.revisions.none { it.taskId == revision.taskId }) { "Bestehende Aufgaben bitte über Bearbeiten mit einer Ausführungsauswahl ändern." }
        require(revision.effective >= LocalDate.now(FAMILY_ZONE) && revision.start >= LocalDate.now(FAMILY_ZONE)) { "Neue Aufgaben beginnen frühestens heute." }
        val uid = requireNotNull(auth.currentUser?.uid)
        // Transactions require a server connection. Definitions never enter the offline write queue.
        db.runTransaction { tx ->
            check(tx.get(household.collection("members").document(uid)).exists())
            tx.set(household.collection("revisions").document(revision.id), revision.encode(uid))
        }.await()
    }

    suspend fun saveChild(child: Child) {
        require(child.name.isNotBlank() && child.name.length <= 60)
        db.runTransaction { tx ->
            check(tx.get(household.collection("members").document(requireNotNull(auth.uid))).exists())
            tx.set(household.collection("children").document(child.id), mapOf(
                "name" to child.name.trim(), "bearColor" to child.bearColor.name))
        }.await()
    }

    fun acknowledge(occurrence: Occurrence, completedById: String, onError: (String) -> Unit) {
        val parent = mutable.value.parents.firstOrNull { it.id == completedById }
        if (parent == null) {
            onError("Elternteil nicht verfügbar. Bitte Familienzugang neu laden.")
            return
        }
        writeEvents(occurrence, listOf(null), onError, parent)
    }
    fun retract(occurrence: Occurrence, onError: (String) -> Unit) {
        val targets = Schedule.activeCompletions(mutable.value.events, occurrence.id).map { it.id }
        if (targets.isNotEmpty()) writeEvents(occurrence, targets, onError)
    }
    private fun writeEvents(occurrence: Occurrence, targets: List<String?>, onError: (String) -> Unit, completedBy: Parent? = null) {
        val user = auth.currentUser ?: return
        val batch = db.batch()
        targets.forEach { target ->
            val targetEvent = target?.let { id -> mutable.value.events.firstOrNull { it.id == id } }
            if (target != null && targetEvent == null) {
                onError("Die Bestätigung wurde inzwischen geändert. Bitte schließen und neu öffnen.")
                return
            }
            val data = mutableMapOf<String, Any?>(
                "occurrenceId" to occurrence.id, "date" to (targetEvent?.date ?: occurrence.date).toString(),
                "kind" to if (target == null) "COMPLETE" else "RETRACT",
                "authorId" to user.uid, "authorName" to (user.displayName ?: "Elternteil").take(120),
                "clientMillis" to System.currentTimeMillis(), "createdAt" to FieldValue.serverTimestamp(), "targetId" to target)
            val snapshot = if (target == null) occurrence else targetEvent?.taskSnapshot
            snapshot?.let { data["taskSnapshot"] = it.snapshotMap() }
            if (target == null && completedBy != null) {
                data["completedById"] = completedBy.id
                data["completedByName"] = completedBy.name.take(120)
            }
            batch.set(household.collection("events").document(UUID.randomUUID().toString()), data)
        }
        batch.commit().addOnFailureListener { onError("Bestätigung konnte nicht gespeichert werden. Bitte erneut versuchen.") }
    }
    suspend fun applyOperation(request: ItemOperation, expectedVersion: Long) {
        val user = requireNotNull(auth.currentUser)
        val state = mutable.value
        require(state.loaded && !state.fromCache && state.error == null) { "Für diese Änderung ist eine aktuelle Serververbindung erforderlich." }
        val now = Instant.now()
        if (request.action == OperationAction.EDIT) {
            require(activeDeletions(state.itemOperations).none { it.covers(request.type, request.itemId, request.anchorId, request.anchorDate, request.anchorTime) }) {
                "Dieser Eintrag wurde gelöscht. Bitte zuerst im Papierkorb wiederherstellen."
            }
            if (request.type == ItemType.TASK) {
                val original = state.taskOccurrences(request.actualDate).firstOrNull { it.id == request.anchorId }
                val resuming = state.taskDefinitions(now, includePaused = true).any { it.taskId == request.itemId && !it.active } && request.task?.active == true
                require((original != null && original.instant > now && Schedule.activeCompletions(state.events, original.id).isEmpty()) ||
                    (resuming && Instant.ofEpochMilli(request.actualStartMillis) > now)) {
                    "Begonnene oder bestätigte Ausführungen bleiben unverändert. Bitte eine kommende Ausführung wählen."
                }
                requireNotNull(request.task).validate()
                if (request.scope == OperationScope.SINGLE) require(request.task.times.size == 1 &&
                    request.task.start.atTime(request.task.times.single()).atZone(FAMILY_ZONE).toInstant() > now) { "Bitte Datum und Uhrzeit in der Zukunft wählen." }
            } else {
                requireNotNull(request.appointment).validate()
                val source = state.appointmentOccurrences(request.actualDate, request.actualDate)
                    .firstOrNull { it.id == request.anchorId }
                // Moved occurrences may be far from their original day; the editor carries their current start.
                require(request.appointment.details.copy(date = request.appointment.activeFrom).startInstant > now)
                require(source != null && source.details.startInstant > now) { "Begonnene Termine bleiben unverändert." }
            }
        }
        if (request.action == OperationAction.RESTORE) require(activeDeletions(state.itemOperations).any { it.id == request.restoresId }) {
            "Dieser Eintrag wurde bereits wiederhergestellt."
        }
        val operation = request.copy(authorId = user.uid, authorName = (user.displayName ?: "Elternteil").take(120), version = expectedVersion + 1)
        db.runTransaction { tx ->
            check(tx.get(household.collection("members").document(user.uid)).exists())
            val control = household.collection("itemControls").document(operation.key)
            val current = tx.get(control)
            check((current.getLong("version") ?: 0L) == expectedVersion) { "Der Eintrag wurde inzwischen geändert. Bitte schließen und neu öffnen." }
            val sourceCollection = if (operation.type == ItemType.TASK) "revisions" else "appointmentSeries"
            check(tx.get(household.collection(sourceCollection).document(operation.sourceId)).exists()) { "Der ursprüngliche Eintrag ist nicht mehr verfügbar." }
            if (operation.action == OperationAction.RESTORE) check(tx.get(household.collection("itemOperations").document(requireNotNull(operation.restoresId))).exists())
            tx.set(household.collection("itemOperations").document(operation.id), operation.encodeOperation())
            tx.set(control, mapOf("version" to operation.version, "operationId" to operation.id,
                "authorId" to user.uid, "updatedAt" to FieldValue.serverTimestamp()))
        }.await()
    }

    fun signOut() { auth.signOut() }

    suspend fun createAppointment(draft: AppointmentSeries) {
        draft.validate()
        require(draft.details.copy(date = draft.activeFrom).startInstant > Instant.now()) { "Neue Termine müssen in der Zukunft liegen." }
        val user = requireNotNull(auth.currentUser)
        db.runTransaction { tx ->
            check(tx.get(household.collection("members").document(user.uid)).exists())
            val ref = household.collection("appointmentSeries").document(draft.id)
            check(!tx.get(ref).exists()) { "Termin existiert bereits. Bitte neu laden." }
            tx.set(ref, draft.encodeAppointment(user.uid))
            val activity = AppointmentActivity(UUID.randomUUID().toString(), draft.id, draft.activeFrom,
                AppointmentActivityKind.ADDED, false, draft.details, null, user.uid,
                (user.displayName ?: "Elternteil").take(120), 0)
            tx.set(household.collection("appointmentActivity").document(activity.id), activity.encodeActivity())
        }.await()
    }

}

private fun TaskRevision.encode(uid: String): Map<String, Any?> = mapOf(
    "taskId" to taskId, "childId" to childId, "title" to title.trim(), "notes" to notes.trim(),
    "frequency" to frequency.name, "weekdays" to weekdays.map { it.value },
    "times" to times.sorted().map { it.toString() }, "start" to start.toString(),
    "end" to end?.toString(), "effective" to effective.toString(), "active" to active,
    "authorId" to uid, "createdAt" to FieldValue.serverTimestamp(),
    "responsibleParentId" to responsibleParentId,
)

private fun decodeRevision(doc: DocumentSnapshot) = TaskRevision(
    id = doc.id, taskId = requireNotNull(doc.getString("taskId")),
    childId = requireNotNull(doc.getString("childId")), title = requireNotNull(doc.getString("title")),
    notes = doc.getString("notes") ?: "", frequency = Frequency.valueOf(requireNotNull(doc.getString("frequency"))),
    weekdays = (doc.get("weekdays") as? List<*>)?.map { DayOfWeek.of((it as Number).toInt()) }?.toSet() ?: emptySet(),
    times = (doc.get("times") as List<*>).map { LocalTime.parse(it as String) },
    start = LocalDate.parse(doc.getString("start")), end = doc.getString("end")?.let(LocalDate::parse),
    effective = LocalDate.parse(doc.getString("effective")), active = doc.getBoolean("active") ?: true,
    createdMillis = doc.getTimestamp("createdAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)?.toDate()?.time ?: 0,
    authorId = doc.getString("authorId") ?: "",
    responsibleParentId = doc.getString("responsibleParentId"),
)

private fun decodeEvent(doc: DocumentSnapshot) = CareEvent(
    id = doc.id, occurrenceId = requireNotNull(doc.getString("occurrenceId")),
    date = LocalDate.parse(doc.getString("date")), kind = EventKind.valueOf(requireNotNull(doc.getString("kind"))),
    authorId = requireNotNull(doc.getString("authorId")), authorName = doc.getString("authorName") ?: "Elternteil",
    clientMillis = doc.getLong("clientMillis") ?: 0,
    serverMillis = doc.getTimestamp("createdAt")?.toDate()?.time,
    targetId = doc.getString("targetId"), pending = doc.metadata.hasPendingWrites(),
    completedById = doc.getString("completedById"), completedByName = doc.getString("completedByName"),
    taskSnapshot = (doc.get("taskSnapshot") as? Map<*, *>)?.let(::occurrenceSnapshot),
)
