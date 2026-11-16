package de.kinderalltag.data

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
    val revisions: List<TaskRevision> = emptyList(),
    val events: List<CareEvent> = emptyList(),
    val fromCache: Boolean = true,
    val pending: Boolean = false,
    val error: String? = null,
) {
    val sections: List<Child>
        get() = children.filterNot { it.id == HOUSEHOLD_SECTION.id } + HOUSEHOLD_SECTION
}

class FamilyRepository(private val auth: FirebaseAuth, private val db: FirebaseFirestore) {
    private val mutable = MutableStateFlow(FamilyState(uid = auth.currentUser?.uid,
        name = auth.currentUser?.displayName ?: "Elternteil"))
    val state = mutable.asStateFlow()
    private val household = db.collection("households").document("family")
    private var membership: ListenerRegistration? = null
    private val listeners = mutableListOf<ListenerRegistration>()
    private val cacheFlags = mutableMapOf<String, Boolean>()
    private val pendingFlags = mutableMapOf<String, Boolean>()
    private val collectionErrors = mutableMapOf<String, String>()

    init { auth.addAuthStateListener { observeUser() } }

    private fun stopData() {
        listeners.forEach { it.remove() }; listeners.clear()
        cacheFlags.clear(); pendingFlags.clear()
        collectionErrors.clear()
    }
    private fun observeUser() {
        membership?.remove(); stopData()
        val user = auth.currentUser
        mutable.value = FamilyState(uid = user?.uid, name = user?.displayName ?: "Elternteil")
        if (user == null) return
        membership = household.collection("members").document(user.uid).addSnapshotListener { doc, error ->
            if (auth.currentUser?.uid != user.uid) return@addSnapshotListener
            if (error != null || doc?.exists() != true) {
                stopData()
                mutable.value = FamilyState(uid = user.uid, name = user.displayName ?: "Elternteil",
                    accessResolved = error != null || doc?.metadata?.isFromCache == false,
                    error = if (error != null) "Familienzugang konnte nicht geladen werden." else null)
            } else if (!mutable.value.authorized) {
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
            }
        }
    }

    private fun observeCollection(name: String, apply: (QuerySnapshot) -> Unit) {
        val expectedUid = mutable.value.uid
        listeners += household.collection(name).addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
            if (auth.uid != expectedUid || !mutable.value.authorized) return@addSnapshotListener
            if (error != null) {
                collectionErrors[name] = "Synchronisierung fehlgeschlagen. Zugang und Verbindung prüfen."
                mutable.value = mutable.value.copy(error = collectionErrors.values.firstOrNull())
            } else if (snapshot != null) {
                try {
                    apply(snapshot)
                    cacheFlags[name] = snapshot.metadata.isFromCache
                    pendingFlags[name] = snapshot.metadata.hasPendingWrites()
                    collectionErrors.remove(name)
                    mutable.value = mutable.value.copy(
                        loaded = cacheFlags.size == 3,
                        fromCache = cacheFlags.size < 3 || cacheFlags.values.any { it },
                        pending = pendingFlags.values.any { it }, error = collectionErrors.values.firstOrNull())
                } catch (_: Exception) {
                    collectionErrors[name] = "Gespeicherte Daten haben ein ungültiges Format."
                    mutable.value = mutable.value.copy(error = collectionErrors.values.firstOrNull())
                }
            }
        }
    }

    suspend fun saveRevision(revision: TaskRevision) {
        revision.validate()
        val earliest = LocalDate.now(FAMILY_ZONE).plusDays(if (mutable.value.revisions.any { it.taskId == revision.taskId }) 1 else 0)
        require(revision.effective >= earliest) { "Der Tag hat gewechselt. Bitte Aufgabe neu öffnen." }
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

    fun acknowledge(occurrence: Occurrence, onError: (String) -> Unit) {
        writeEvents(occurrence, listOf(null), onError)
    }
    fun retract(occurrence: Occurrence, onError: (String) -> Unit) {
        val targets = Schedule.activeCompletions(mutable.value.events, occurrence.id).map { it.id }
        if (targets.isNotEmpty()) writeEvents(occurrence, targets, onError)
    }
    private fun writeEvents(occurrence: Occurrence, targets: List<String?>, onError: (String) -> Unit) {
        val user = auth.currentUser ?: return
        val batch = db.batch()
        targets.forEach { target ->
            batch.set(household.collection("events").document(UUID.randomUUID().toString()), mapOf(
                "occurrenceId" to occurrence.id, "date" to occurrence.date.toString(),
                "kind" to if (target == null) "COMPLETE" else "RETRACT",
                "authorId" to user.uid, "authorName" to (user.displayName ?: "Elternteil").take(120),
                "clientMillis" to System.currentTimeMillis(), "createdAt" to FieldValue.serverTimestamp(),
                "targetId" to target,
            ))
        }
        batch.commit().addOnFailureListener { onError("Bestätigung konnte nicht gespeichert werden. Bitte erneut versuchen.") }
    }
    fun signOut() { auth.signOut() }
}

private fun TaskRevision.encode(uid: String): Map<String, Any?> = mapOf(
    "taskId" to taskId, "childId" to childId, "title" to title.trim(), "notes" to notes.trim(),
    "frequency" to frequency.name, "weekdays" to weekdays.map { it.value },
    "times" to times.sorted().map { it.toString() }, "start" to start.toString(),
    "end" to end?.toString(), "effective" to effective.toString(), "active" to active,
    "authorId" to uid, "createdAt" to FieldValue.serverTimestamp(),
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
)

private fun decodeEvent(doc: DocumentSnapshot) = CareEvent(
    id = doc.id, occurrenceId = requireNotNull(doc.getString("occurrenceId")),
    date = LocalDate.parse(doc.getString("date")), kind = EventKind.valueOf(requireNotNull(doc.getString("kind"))),
    authorId = requireNotNull(doc.getString("authorId")), authorName = doc.getString("authorName") ?: "Elternteil",
    clientMillis = doc.getLong("clientMillis") ?: 0,
    serverMillis = doc.getTimestamp("createdAt")?.toDate()?.time,
    targetId = doc.getString("targetId"), pending = doc.metadata.hasPendingWrites(),
)
