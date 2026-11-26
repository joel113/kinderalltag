package de.kinderalltag.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import de.kinderalltag.domain.*
import de.kinderalltag.text.*
import java.util.UUID
import kotlinx.coroutines.tasks.await

internal fun FamilyListItem.listMap(): Map<String, Any?> =
    mapOf(
        "id" to id,
        "kind" to kind.name,
        "title" to title,
        "notes" to notes,
        "recipient" to recipient?.stored(),
        "labelIds" to labelIds.sorted(),
        "version" to version,
        "deleted" to deleted,
        "lastActivityId" to lastActivityId,
    )

internal fun decodeListItem(data: Map<*, *>) =
    FamilyListItem(
            data["id"] as String,
            FamilyListKind.valueOf(data["kind"] as String),
            data["title"] as String,
            data["notes"] as String,
            data["recipient"]?.let(AppointmentRecipient::fromStored),
            (data["labelIds"] as List<*>).map { it as String }.toSet(),
            (data["version"] as Number).toLong(),
            data["deleted"] as Boolean,
            data["lastActivityId"] as String,
        )
        .also { it.validate() }

internal fun FamilyLabel.labelMap(): Map<String, Any?> =
    mapOf(
        "id" to id,
        "group" to group.name,
        "name" to name,
        "color" to color.name,
        "active" to active,
        "version" to version,
    )

internal fun decodeLabel(data: Map<*, *>) =
    FamilyLabel(
            data["id"] as String,
            LabelGroup.valueOf(data["group"] as String),
            data["name"] as String,
            BearColor.valueOf(data["color"] as String),
            data["active"] as Boolean,
            (data["version"] as Number).toLong(),
        )
        .also { it.validate() }

internal fun decodeListActivity(doc: DocumentSnapshot) =
    ListActivity(
        doc.id,
        requireNotNull(doc.getString("itemId")),
        ListAction.valueOf(requireNotNull(doc.getString("action"))),
        decodeListItem(doc.get("snapshot") as Map<*, *>),
        (doc.get("labels") as List<*>).map { decodeLabel(it as Map<*, *>) },
        requireNotNull(doc.getString("authorId")),
        requireNotNull(doc.getString("authorName")),
        requireNotNull(doc.getLong("clientMillis")),
        doc.getTimestamp("createdAt")?.toDate()?.time,
        doc.getString("targetId"),
        doc.metadata.hasPendingWrites(),
    )

class FamilyLists(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore,
    private val state: () -> FamilyState,
) {
    private val household = db.collection("households").document("family")

    suspend fun reorder(kind: FamilyListKind, itemIds: List<String>, expectedVersion: Long) {
        val user = requireNotNull(auth.currentUser)
        val current = state()
        requireLocalized(
            current.loaded && !current.fromCache && !current.pending && current.error == null
        ) {
            message(TextKey.SORTING_REQUIRES_A_CURRENT_SERVER_CONNECTION)
        }
        FamilyListOrder(kind, itemIds, expectedVersion).validate()
        requireLocalized(
            itemIds.toSet() == current.listItems.filter { it.kind == kind }.map { it.id }.toSet()
        ) {
            message(TextKey.THE_LIST_HAS_CHANGED_PLEASE_TRY_MOVING_THE_ITEM_AGAIN)
        }
        db.runTransaction { transaction ->
                val ref = household.collection("listOrders").document(kind.name)
                val saved = transaction.get(ref)
                require((saved.getLong("version") ?: 0L) == expectedVersion) {
                    text(
                        TextKey
                            .THE_ORDER_CHANGED_ON_THE_OTHER_DEVICE_PLEASE_TRY_MOVING_THE_ITEM_AGAIN
                    )
                }
                transaction.set(
                    ref,
                    mapOf(
                        "kind" to kind.name,
                        "itemIds" to itemIds,
                        "version" to expectedVersion + 1,
                        "authorId" to user.uid,
                        "updatedAt" to FieldValue.serverTimestamp(),
                    ),
                )
            }
            .await()
    }

    private fun event(
        item: FamilyListItem,
        action: ListAction,
        id: String,
        target: String? = null,
    ): Map<String, Any?> {
        val user = requireNotNull(auth.currentUser)
        return mapOf(
            "id" to id,
            "itemId" to item.id,
            "action" to action.name,
            "snapshot" to item.listMap(),
            "labels" to
                state()
                    .listLabels
                    .filter { it.id in item.labelIds }
                    .sortedBy { it.id }
                    .map { it.labelMap() },
            "authorId" to user.uid,
            "authorName" to (user.displayName ?: text(TextKey.PARENT)).take(120),
            "clientMillis" to System.currentTimeMillis(),
            "createdAt" to FieldValue.serverTimestamp(),
            "targetId" to target,
        )
    }

    private fun validateLabels(item: FamilyListItem) {
        item.validate()
        item.labelIds.forEach { id ->
            val label = state().listLabels.firstOrNull { it.id == id }
            requireLocalized(
                label != null &&
                    if (item.kind == FamilyListKind.SHOPPING) label.group == LabelGroup.SHOPPING
                    else label.group != LabelGroup.SHOPPING
            ) {
                message(TextKey.LABEL_UNAVAILABLE_PLEASE_RELOAD)
            }
        }
    }

    // Enqueue without awaiting server acknowledgement: Firestore persists offline creations.
    fun create(draft: FamilyListItem, onError: (String) -> Unit) {
        validateLabels(draft)
        val id = UUID.randomUUID().toString()
        val item = draft.copy(version = 0, deleted = false, lastActivityId = id)
        val batch = db.batch()
        batch.set(household.collection("listItems").document(item.id), item.listMap())
        batch.set(
            household.collection("listActivity").document(id),
            event(item, ListAction.ADDED, id),
        )
        batch.commit().addOnFailureListener {
            onError(
                text(TextKey.THE_ENTRY_COULD_NOT_SYNCHRONIZE_PLEASE_CHECK_FAMILY_ACCESS_AND_RULES)
            )
        }
    }

    fun createMany(drafts: List<FamilyListItem>, onError: (String) -> Unit) {
        require(drafts.all { it.kind == FamilyListKind.SHOPPING })
        validateShoppingImport(drafts.map { it.title })
        drafts.forEach(::validateLabels)
        // Each item/audit pair has its own atomic batch, staying within rules document-access
        // limits.
        drafts.forEach {
            create(it) {
                onError(
                    text(
                        TextKey
                            .SOME_SHOPPING_ITEMS_COULD_NOT_SYNCHRONIZE_PLEASE_CHECK_THE_LIST_AND_FAMI
                    )
                )
            }
        }
    }

    fun setBought(item: FamilyListItem, bought: Boolean, onError: (String) -> Unit) {
        val targets =
            if (bought) listOf<String?>(null)
            else activePurchases(state().listActivity, item.id).map { it.id }
        if (targets.isEmpty()) return
        val batch = db.batch()
        targets.forEach { target ->
            val id = UUID.randomUUID().toString()
            batch.set(
                household.collection("listActivity").document(id),
                event(
                    item,
                    if (target == null) ListAction.BOUGHT else ListAction.REOPENED,
                    id,
                    target,
                ),
            )
        }
        batch.commit().addOnFailureListener {
            onError(text(TextKey.THE_PURCHASE_STATUS_COULD_NOT_SYNCHRONIZE_PLEASE_TRY_AGAIN))
        }
    }

    suspend fun change(draft: FamilyListItem, action: ListAction) {
        require(action in listOf(ListAction.CHANGED, ListAction.DELETED, ListAction.RESTORED))
        validateLabels(draft)
        requireOnline()
        val user = requireNotNull(auth.currentUser)
        val id = UUID.randomUUID().toString()
        db.runTransaction { tx ->
                check(tx.get(household.collection("members").document(user.uid)).exists())
                val ref = household.collection("listItems").document(draft.id)
                val old = decodeListItem(requireNotNull(tx.get(ref).data))
                checkLocalized(old.version == draft.version && old.kind == draft.kind) {
                    message(TextKey.THIS_ENTRY_HAS_CHANGED_PLEASE_CLOSE_AND_REOPEN_IT_2192A4)
                }
                requireLocalized(if (action == ListAction.RESTORED) old.deleted else !old.deleted) {
                    message(TextKey.THIS_ENTRY_HAS_ALREADY_CHANGED)
                }
                val updated =
                    (if (action == ListAction.CHANGED) draft else old).copy(
                        version = old.version + 1,
                        deleted = action == ListAction.DELETED,
                        lastActivityId = id,
                    )
                tx.set(ref, updated.listMap())
                tx.set(
                    household.collection("listActivity").document(id),
                    event(updated, action, id),
                )
            }
            .await()
    }

    private fun requireOnline() {
        requireLocalized(
            state().loaded && !state().fromCache && !state().pending && state().error == null
        ) {
            message(TextKey.THIS_CHANGE_REQUIRES_A_CURRENT_SERVER_CONNECTION)
        }
    }

    suspend fun saveLabel(label: FamilyLabel, creating: Boolean) {
        label.validate()
        requireOnline()
        val uid = requireNotNull(auth.uid)
        db.runTransaction { tx ->
                check(tx.get(household.collection("members").document(uid)).exists())
                val ref = household.collection("listLabels").document(label.id)
                val old = tx.get(ref)
                checkLocalized(
                    if (creating) !old.exists() else old.getLong("version") == label.version
                ) {
                    message(TextKey.THIS_LABEL_HAS_CHANGED_PLEASE_REOPEN_IT)
                }
                tx.set(ref, label.copy(version = if (creating) 0 else label.version + 1).labelMap())
            }
            .await()
    }

    suspend fun addStarters(group: LabelGroup) {
        requireOnline()
        val presets = starterLabels(group)
        val uid = requireNotNull(auth.uid)
        db.runTransaction { tx ->
                check(tx.get(household.collection("members").document(uid)).exists())
                val existing =
                    presets.associateWith {
                        tx.get(household.collection("listLabels").document(it.id)).exists()
                    }
                existing
                    .filterValues { !it }
                    .keys
                    .forEach { label ->
                        tx.set(
                            household.collection("listLabels").document(label.id),
                            label.labelMap(),
                        )
                    }
            }
            .await()
    }
}
