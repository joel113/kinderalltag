package de.kinderalltag.data

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import de.kinderalltag.domain.*
import de.kinderalltag.text.*
import java.time.*
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

data class FamilyState(
    val uid: String? = null,
    val name: String = "",
    val authorized: Boolean = false,
    val accessResolved: Boolean = false,
    val loaded: Boolean = false,
    val children: List<Child> = emptyList(),
    val childRegistryReady: Boolean = false,
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
    val listOrders: List<FamilyListOrder> = emptyList(),
    val fromCache: Boolean = true,
    val pending: Boolean = false,
    val error: String? = null,
) {
    val sections: List<Child>
        get() = children.filterNot { it.id == HOUSEHOLD_SECTION.id } + HOUSEHOLD_SECTION
}

private val DATA_COLLECTIONS =
    setOf(
        "children",
        "childRegistry",
        "revisions",
        "events",
        "members",
        "appointmentSeries",
        "appointmentExceptions",
        "appointmentActivity",
        "itemOperations",
        "listItems",
        "listActivity",
        "listLabels",
        "listOrders",
    )

class FamilyRepository(private val auth: FirebaseAuth, private val db: FirebaseFirestore) {
    private val mutable =
        MutableStateFlow(
            FamilyState(
                uid = auth.currentUser?.uid,
                name = auth.currentUser?.displayName ?: text(TextKey.PARENT),
            )
        )
    val state = mutable.asStateFlow()
    val lists = FamilyLists(auth, db) { mutable.value }
    private val household = db.collection("households").document("family")
    private var membership: ListenerRegistration? = null
    private val listeners = mutableListOf<ListenerRegistration>()
    private val cacheFlags = mutableMapOf<String, Boolean>()
    private val pendingFlags = mutableMapOf<String, Boolean>()
    private val collectionErrors = mutableMapOf<String, SyncIssue>()
    private var membershipIssue: SyncIssue? = null
    private var generation = 0

    init {
        auth.addAuthStateListener { observeUser() }
    }

    private fun stopData() {
        listeners.forEach { it.remove() }
        listeners.clear()
        cacheFlags.clear()
        pendingFlags.clear()
        collectionErrors.clear()
        membershipIssue = null
    }

    fun refreshPresentation() {
        val issues = membershipIssue?.let(::listOf) ?: collectionErrors.values.toList()
        mutable.value = mutable.value.copy(error = syncErrorSummary(issues))
    }

    fun retrySync() {
        observeUser(keepData = true)
    }

    private fun observeUser(keepData: Boolean = false) {
        membership?.remove()
        stopData()
        val currentGeneration = ++generation
        val user = auth.currentUser
        val previous = mutable.value
        mutable.value =
            if (keepData && previous.uid == user?.uid && previous.authorized) {
                previous.copy(loaded = false, fromCache = true, error = null)
            } else FamilyState(uid = user?.uid, name = user?.displayName ?: text(TextKey.PARENT))
        if (user == null) return
        membership =
            household.collection("members").document(user.uid).addSnapshotListener(
                MetadataChanges.INCLUDE
            ) { doc, error ->
                if (currentGeneration != generation || auth.currentUser?.uid != user.uid)
                    return@addSnapshotListener
                if (error != null || doc?.exists() != true) {
                    if (error != null)
                        Log.e(
                            "KinderalltagSync",
                            "Membership listener failed: ${error.code}",
                            error,
                        )
                    stopData()
                    membershipIssue = error?.let { SyncIssue("membership", it.code.name) }
                    mutable.value =
                        FamilyState(
                            uid = user.uid,
                            name = user.displayName ?: text(TextKey.PARENT),
                            accessResolved = error != null || doc?.metadata?.isFromCache == false,
                            error =
                                error?.let {
                                    syncErrorSummary(listOf(SyncIssue("membership", it.code.name)))
                                },
                        )
                } else if (listeners.isEmpty()) {
                    mutable.value =
                        mutable.value.copy(authorized = true, accessResolved = true, error = null)
                    initializeChildren().addOnFailureListener { failure ->
                        if (currentGeneration == generation) {
                            Log.e(
                                "KinderalltagSync",
                                "Child registry initialization failed",
                                failure,
                            )
                            mutable.value =
                                mutable.value.copy(error = text(TextKey.CHILD_SETUP_FAILED))
                        }
                    }
                    observeCollection("childRegistry") { snapshot ->
                        mutable.value =
                            mutable.value.copy(
                                childRegistryReady =
                                    snapshot.documents.any {
                                        it.id == "state" && it.getLong("schemaVersion") == 2L
                                    }
                            )
                    }
                    observeCollection("children") { snapshot ->
                        mutable.value =
                            mutable.value.copy(
                                children =
                                    snapshot.documents
                                        .map { decodeChild(it.id, requireNotNull(it.data)) }
                                        .sortedBy { it.name }
                            )
                    }
                    observeCollection("revisions") { snapshot ->
                        mutable.value =
                            mutable.value.copy(revisions = snapshot.documents.map(::decodeRevision))
                    }
                    observeCollection("events") { snapshot ->
                        mutable.value =
                            mutable.value.copy(events = snapshot.documents.map(::decodeEvent))
                    }
                    observeCollection("members") { snapshot ->
                        mutable.value =
                            mutable.value.copy(
                                parents =
                                    snapshot.documents
                                        .map {
                                            Parent(
                                                it.id,
                                                it.getString("name") ?: text(TextKey.PARENT),
                                            )
                                        }
                                        .sortedBy { it.name }
                            )
                    }
                    observeCollection("appointmentSeries") { snapshot ->
                        mutable.value =
                            mutable.value.copy(
                                appointments = snapshot.documents.map(::decodeAppointment)
                            )
                    }
                    observeCollection("appointmentExceptions") { snapshot ->
                        mutable.value =
                            mutable.value.copy(
                                appointmentExceptions =
                                    snapshot.documents.map(::decodeAppointmentException)
                            )
                    }
                    observeCollection("appointmentActivity") { snapshot ->
                        mutable.value =
                            mutable.value.copy(
                                appointmentActivity =
                                    snapshot.documents.map(::decodeAppointmentActivity)
                            )
                    }
                    observeCollection("listItems") { snapshot ->
                        mutable.value =
                            mutable.value.copy(
                                listItems =
                                    snapshot.documents.map {
                                        decodeListItem(requireNotNull(it.data))
                                    }
                            )
                    }
                    observeCollection("listOrders") { snapshot ->
                        mutable.value =
                            mutable.value.copy(
                                listOrders =
                                    snapshot.documents.map { doc ->
                                        FamilyListOrder(
                                                FamilyListKind.valueOf(doc.id),
                                                (doc.get("itemIds") as List<*>).map {
                                                    it as String
                                                },
                                                requireNotNull(doc.getLong("version")),
                                            )
                                            .also { it.validate() }
                                    }
                            )
                    }
                    observeCollection("listLabels") { snapshot ->
                        mutable.value =
                            mutable.value.copy(
                                listLabels =
                                    snapshot.documents.map { decodeLabel(requireNotNull(it.data)) }
                            )
                    }
                    observeCollection("listActivity") { snapshot ->
                        mutable.value =
                            mutable.value.copy(
                                listActivity = snapshot.documents.map(::decodeListActivity)
                            )
                    }
                    observeCollection("itemOperations") { snapshot ->
                        mutable.value =
                            mutable.value.copy(
                                itemOperations = snapshot.documents.map(::decodeItemOperation)
                            )
                    }
                }
            }
    }

    private fun observeCollection(name: String, apply: (QuerySnapshot) -> Unit) {
        val expectedUid = mutable.value.uid
        val currentGeneration = generation
        listeners +=
            household.collection(name).addSnapshotListener(MetadataChanges.INCLUDE) {
                snapshot,
                error ->
                if (
                    currentGeneration != generation ||
                        auth.uid != expectedUid ||
                        !mutable.value.authorized
                )
                    return@addSnapshotListener
                if (error != null) {
                    Log.e(
                        "KinderalltagSync",
                        "Collection listener failed: $name (${error.code})",
                        error,
                    )
                    collectionErrors[name] = SyncIssue(name, error.code.name)
                    cacheFlags.remove(name)
                    mutable.value =
                        mutable.value.copy(
                            loaded = false,
                            fromCache = true,
                            error = syncErrorSummary(collectionErrors.values),
                        )
                } else if (snapshot != null) {
                    try {
                        apply(snapshot)
                        cacheFlags[name] = snapshot.metadata.isFromCache
                        pendingFlags[name] = snapshot.metadata.hasPendingWrites()
                        collectionErrors.remove(name)
                        mutable.value =
                            mutable.value.copy(
                                loaded = cacheFlags.keys.containsAll(DATA_COLLECTIONS),
                                fromCache =
                                    !cacheFlags.keys.containsAll(DATA_COLLECTIONS) ||
                                        cacheFlags.values.any { it },
                                pending =
                                    pendingFlags.values.any { it } ||
                                        (!cacheFlags.keys.containsAll(DATA_COLLECTIONS) &&
                                            mutable.value.pending),
                                error = syncErrorSummary(collectionErrors.values),
                            )
                    } catch (e: Exception) {
                        Log.e("KinderalltagSync", "Invalid stored data in collection: $name", e)
                        collectionErrors[name] = SyncIssue(name, "INVALID_DATA")
                        cacheFlags.remove(name)
                        mutable.value =
                            mutable.value.copy(
                                loaded = false,
                                fromCache = true,
                                error = syncErrorSummary(collectionErrors.values),
                            )
                    }
                }
            }
    }

    suspend fun saveRevision(revision: TaskRevision) = saveRevisions(listOf(revision))

    suspend fun saveRevisions(revisions: List<TaskRevision>) {
        require(revisions.size in 1..10)
        revisions.forEach { revision ->
            revision.validate()
            requireLocalized(mutable.value.revisions.none { it.taskId == revision.taskId }) {
                message(TextKey.PLEASE_CHANGE_EXISTING_TASKS_THROUGH_EDIT_AND_SELECT_AN_OCCURRENCE)
            }
            requireLocalized(
                revision.effective >= LocalDate.now(FAMILY_ZONE) &&
                    revision.start >= LocalDate.now(FAMILY_ZONE)
            ) {
                message(TextKey.NEW_TASKS_START_NO_EARLIER_THAN_TODAY)
            }
        }
        val uid = requireNotNull(auth.currentUser?.uid)
        // Transactions require a server connection. Definitions never enter the offline write
        // queue.
        db.runTransaction { tx ->
                check(tx.get(household.collection("members").document(uid)).exists())
                revisions.forEach { revision ->
                    val child = mutable.value.children.firstOrNull { it.id == revision.childId }
                    require(revision.childId == "haushalt" || child != null && !child.archived)
                    tx.set(
                        household.collection("revisions").document(revision.id),
                        revision.encode(uid),
                    )
                }
            }
            .await()
    }

    /** Additive, idempotent migration. Child documents and immutable history are untouched. */
    private fun initializeChildren() =
        db.runTransaction { tx ->
            val ref = household.collection("childRegistry").document("state")
            val registry = tx.get(ref)
            if (!registry.exists()) {
                val legacy =
                    listOf("ferdinand", "johann").filter {
                        tx.get(household.collection("children").document(it)).exists()
                    }
                tx.set(
                    ref,
                    mapOf(
                        "schemaVersion" to 2,
                        "version" to 0,
                        "knownIds" to legacy,
                        "activeIds" to legacy,
                        "changedId" to "",
                    ),
                )
            }
        }

    suspend fun saveChild(child: Child) {
        requireLocalized(child.name.isNotBlank() && child.name.trim().length <= 60) {
            message(TextKey.CHILD_NAME_REQUIRED)
        }
        require(child.id != "haushalt" && child.id.matches(Regex("[A-Za-z0-9_-]{1,100}")))
        val registryRef = household.collection("childRegistry").document("state")
        val childRef = household.collection("children").document(child.id)
        db.runTransaction { tx ->
                check(
                    tx.get(household.collection("members").document(requireNotNull(auth.uid)))
                        .exists()
                )
                val registry = tx.get(registryRef)
                check(registry.exists()) { text(TextKey.CHILD_SETUP_FAILED) }
                val old = tx.get(childRef)
                requireLocalized((old.getLong("version") ?: 0) == child.version) {
                    message(TextKey.CHILD_CHANGED_RELOAD)
                }
                val active =
                    (registry.get("activeIds") as List<*>).filterIsInstance<String>().toMutableSet()
                val known =
                    (registry.get("knownIds") as List<*>).filterIsInstance<String>().toMutableSet()
                if (child.archived) active.remove(child.id) else active.add(child.id)
                requireLocalized(active.size <= 10) { message(TextKey.TEN_ACTIVE_CHILDREN) }
                known.add(child.id)
                tx.set(
                    registryRef,
                    mapOf(
                        "schemaVersion" to 2,
                        "version" to requireNotNull(registry.getLong("version")) + 1,
                        "knownIds" to known.sorted(),
                        "activeIds" to active.sorted(),
                        "changedId" to child.id,
                    ),
                )
                tx.set(
                    childRef,
                    mapOf(
                        "name" to child.name.trim(),
                        "bearColor" to child.bearColor.name,
                        "icon" to child.icon.name,
                        "archived" to child.archived,
                        "version" to if (old.exists()) child.version + 1 else 0,
                    ),
                )
            }
            .await()
    }

    fun acknowledge(occurrence: Occurrence, completedById: String, onError: (String) -> Unit) {
        val parent = mutable.value.parents.firstOrNull { it.id == completedById }
        if (parent == null) {
            onError(text(TextKey.PARENT_UNAVAILABLE_PLEASE_RELOAD_FAMILY_ACCESS))
            return
        }
        writeEvents(occurrence, listOf(null), onError, parent)
    }

    fun retract(occurrence: Occurrence, onError: (String) -> Unit) {
        val targets = Schedule.activeCompletions(mutable.value.events, occurrence.id).map { it.id }
        if (targets.isNotEmpty()) writeEvents(occurrence, targets, onError)
    }

    private fun writeEvents(
        occurrence: Occurrence,
        targets: List<String?>,
        onError: (String) -> Unit,
        completedBy: Parent? = null,
    ) {
        val user = auth.currentUser ?: return
        val batch = db.batch()
        targets.forEach { target ->
            val targetEvent = target?.let { id -> mutable.value.events.firstOrNull { it.id == id } }
            if (target != null && targetEvent == null) {
                onError(text(TextKey.THE_CONFIRMATION_HAS_CHANGED_PLEASE_CLOSE_AND_REOPEN_IT))
                return
            }
            val data =
                mutableMapOf<String, Any?>(
                    "occurrenceId" to occurrence.id,
                    "date" to (targetEvent?.date ?: occurrence.date).toString(),
                    "kind" to if (target == null) "COMPLETE" else "RETRACT",
                    "authorId" to user.uid,
                    "authorName" to (user.displayName ?: text(TextKey.PARENT)).take(120),
                    "clientMillis" to System.currentTimeMillis(),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "targetId" to target,
                )
            val snapshot = if (target == null) occurrence else targetEvent?.taskSnapshot
            snapshot?.let { data["taskSnapshot"] = it.snapshotMap() }
            if (target == null && completedBy != null) {
                data["completedById"] = completedBy.id
                data["completedByName"] = completedBy.name.take(120)
            }
            batch.set(household.collection("events").document(UUID.randomUUID().toString()), data)
        }
        batch.commit().addOnFailureListener { failure ->
            val code = (failure as? FirebaseFirestoreException)?.code?.name
            val sourceKnown =
                mutable.value.revisions.any {
                    it.id == occurrence.sourceRevisionId && it.taskId == occurrence.revision.taskId
                }
            Log.e(
                "KinderalltagCare",
                "Care write rejected: code=$code, correction=${targets.any { it != null }}, sourceKnown=$sourceKnown, loaded=${mutable.value.loaded}, fromCache=${mutable.value.fromCache}",
                failure,
            )
            onError(careWriteError(code))
        }
    }

    suspend fun applyOperation(request: ItemOperation, expectedVersion: Long) {
        val user = requireNotNull(auth.currentUser)
        val state = mutable.value
        requireLocalized(state.loaded && !state.fromCache && state.error == null) {
            message(TextKey.THIS_CHANGE_REQUIRES_A_CURRENT_SERVER_CONNECTION)
        }
        val now = Instant.now()
        if (request.action == OperationAction.EDIT) {
            requireLocalized(
                activeDeletions(state.itemOperations).none {
                    it.covers(
                        request.type,
                        request.itemId,
                        request.anchorId,
                        request.anchorDate,
                        request.anchorTime,
                    )
                }
            ) {
                message(TextKey.THIS_ENTRY_WAS_DELETED_PLEASE_RESTORE_IT_FROM_TRASH_FIRST)
            }
            if (request.type == ItemType.TASK) {
                val original =
                    state.taskOccurrences(request.actualDate).firstOrNull {
                        it.id == request.anchorId
                    }
                val resuming =
                    state.taskDefinitions(now, includePaused = true).any {
                        it.taskId == request.itemId && !it.active
                    } && request.task?.active == true
                require(
                    (original != null &&
                        (original.instant > now ||
                            (request.scope == OperationScope.SINGLE &&
                                isOneOffTaskSource(
                                    state.revisions,
                                    request.itemId,
                                    request.sourceId,
                                ))) &&
                        Schedule.activeCompletions(state.events, original.id).isEmpty()) ||
                        (resuming && Instant.ofEpochMilli(request.actualStartMillis) > now)
                ) {
                    text(
                        TextKey
                            .CONFIRMED_OCCURRENCES_AND_RECURRING_TASKS_THAT_HAVE_STARTED_REMAIN_UNCHA
                    )
                }
                requireNotNull(request.task).validate()
                if (request.scope == OperationScope.SINGLE)
                    requireLocalized(request.task.times.size == 1) {
                        message(TextKey.PLEASE_SELECT_EXACTLY_ONE_TIME_FOR_THIS_OCCURRENCE)
                    }
            } else {
                requireNotNull(request.appointment).validate()
                val source =
                    state
                        .appointmentOccurrences(request.actualDate, request.actualDate)
                        .firstOrNull { it.id == request.anchorId }
                // Moved occurrences may be far from their original day; the editor carries their
                // current start.
                require(
                    request.appointment.details
                        .copy(date = request.appointment.activeFrom)
                        .startInstant > now
                )
                requireLocalized(source != null && source.details.startInstant > now) {
                    message(TextKey.APPOINTMENTS_THAT_HAVE_STARTED_REMAIN_UNCHANGED)
                }
            }
        }
        if (request.action == OperationAction.RESTORE)
            requireLocalized(
                activeDeletions(state.itemOperations).any { it.id == request.restoresId }
            ) {
                message(TextKey.THIS_ENTRY_HAS_ALREADY_BEEN_RESTORED)
            }
        val operation =
            request.copy(
                authorId = user.uid,
                authorName = (user.displayName ?: text(TextKey.PARENT)).take(120),
                version = expectedVersion + 1,
            )
        db.runTransaction { tx ->
                check(tx.get(household.collection("members").document(user.uid)).exists())
                val control = household.collection("itemControls").document(operation.key)
                val current = tx.get(control)
                checkLocalized((current.getLong("version") ?: 0L) == expectedVersion) {
                    message(TextKey.THIS_ENTRY_HAS_CHANGED_PLEASE_CLOSE_AND_REOPEN_IT)
                }
                val sourceCollection =
                    if (operation.type == ItemType.TASK) "revisions" else "appointmentSeries"
                checkLocalized(
                    tx.get(household.collection(sourceCollection).document(operation.sourceId))
                        .exists()
                ) {
                    message(TextKey.THE_ORIGINAL_ENTRY_IS_NO_LONGER_AVAILABLE)
                }
                if (operation.action == OperationAction.RESTORE)
                    check(
                        tx.get(
                                household
                                    .collection("itemOperations")
                                    .document(requireNotNull(operation.restoresId))
                            )
                            .exists()
                    )
                tx.set(
                    household.collection("itemOperations").document(operation.id),
                    operation.encodeOperation(),
                )
                tx.set(
                    control,
                    mapOf(
                        "version" to operation.version,
                        "operationId" to operation.id,
                        "authorId" to user.uid,
                        "updatedAt" to FieldValue.serverTimestamp(),
                    ),
                )
            }
            .await()
    }

    fun signOut() {
        auth.signOut()
    }

    suspend fun createAppointment(draft: AppointmentSeries) {
        draft.validate()
        requireLocalized(draft.details.copy(date = draft.activeFrom).startInstant > Instant.now()) {
            message(TextKey.NEW_APPOINTMENTS_MUST_BE_IN_THE_FUTURE)
        }
        val user = requireNotNull(auth.currentUser)
        db.runTransaction { tx ->
                check(tx.get(household.collection("members").document(user.uid)).exists())
                val ref = household.collection("appointmentSeries").document(draft.id)
                checkLocalized(!tx.get(ref).exists()) {
                    message(TextKey.THE_APPOINTMENT_ALREADY_EXISTS_PLEASE_RELOAD)
                }
                tx.set(ref, draft.encodeAppointment(user.uid))
                val activity =
                    AppointmentActivity(
                        UUID.randomUUID().toString(),
                        draft.id,
                        draft.activeFrom,
                        AppointmentActivityKind.ADDED,
                        false,
                        draft.details,
                        null,
                        user.uid,
                        (user.displayName ?: text(TextKey.PARENT)).take(120),
                        0,
                    )
                tx.set(
                    household.collection("appointmentActivity").document(activity.id),
                    activity.encodeActivity(),
                )
            }
            .await()
    }
}

private fun TaskRevision.encode(uid: String): Map<String, Any?> =
    mapOf(
        "taskId" to taskId,
        "childId" to childId,
        "title" to title.trim(),
        "notes" to notes.trim(),
        "frequency" to frequency.name,
        "interval" to interval,
        "weekdays" to weekdays.map { it.value },
        "times" to times.sorted().map { it.toString() },
        "start" to start.toString(),
        "end" to end?.toString(),
        "effective" to effective.toString(),
        "active" to active,
        "authorId" to uid,
        "createdAt" to FieldValue.serverTimestamp(),
        "responsibleParentId" to responsibleParentId,
    )

private fun decodeRevision(doc: DocumentSnapshot) =
    TaskRevision(
        id = doc.id,
        taskId = requireNotNull(doc.getString("taskId")),
        childId = requireNotNull(doc.getString("childId")),
        title = requireNotNull(doc.getString("title")),
        notes = doc.getString("notes") ?: "",
        frequency = Frequency.valueOf(requireNotNull(doc.getString("frequency"))),
        interval = doc.getLong("interval")?.toInt() ?: 1,
        weekdays =
            (doc.get("weekdays") as? List<*>)?.map { DayOfWeek.of((it as Number).toInt()) }?.toSet()
                ?: emptySet(),
        times = (doc.get("times") as List<*>).map { LocalTime.parse(it as String) },
        start = LocalDate.parse(doc.getString("start")),
        end = doc.getString("end")?.let(LocalDate::parse),
        effective = LocalDate.parse(doc.getString("effective")),
        active = doc.getBoolean("active") ?: true,
        createdMillis =
            doc.getTimestamp("createdAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                ?.toDate()
                ?.time ?: 0,
        authorId = doc.getString("authorId") ?: "",
        responsibleParentId = doc.getString("responsibleParentId"),
    )

private fun decodeEvent(doc: DocumentSnapshot) =
    CareEvent(
        id = doc.id,
        occurrenceId = requireNotNull(doc.getString("occurrenceId")),
        date = LocalDate.parse(doc.getString("date")),
        kind = EventKind.valueOf(requireNotNull(doc.getString("kind"))),
        authorId = requireNotNull(doc.getString("authorId")),
        authorName = doc.getString("authorName") ?: text(TextKey.PARENT),
        clientMillis = doc.getLong("clientMillis") ?: 0,
        serverMillis = doc.getTimestamp("createdAt")?.toDate()?.time,
        targetId = doc.getString("targetId"),
        pending = doc.metadata.hasPendingWrites(),
        completedById = doc.getString("completedById"),
        completedByName = doc.getString("completedByName"),
        taskSnapshot = (doc.get("taskSnapshot") as? Map<*, *>)?.let(::occurrenceSnapshot),
    )
