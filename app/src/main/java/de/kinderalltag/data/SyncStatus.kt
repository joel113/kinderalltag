package de.kinderalltag.data

import de.kinderalltag.text.*

internal data class SyncIssue(val source: String, val code: String)

private fun sourceLabel(source: String): String =
    when (source) {
        "children" -> text(TextKey.CHILD_PROFILES)
        "revisions" -> text(TextKey.TASKS)
        "events" -> text(TextKey.CONFIRMATIONS)
        "members" -> text(TextKey.PARENTS)
        "appointmentSeries" -> text(TextKey.APPOINTMENTS)
        "appointmentExceptions" -> text(TextKey.APPOINTMENT_CHANGES)
        "appointmentActivity" -> text(TextKey.APPOINTMENT_HISTORY)
        "itemOperations" -> text(TextKey.CHANGES_TRASH)
        "listItems" -> text(TextKey.SHOPPING_GIFT_IDEAS)
        "listActivity" -> text(TextKey.LIST_HISTORY)
        "listLabels" -> text(TextKey.LIST_LABELS)
        "membership" -> text(TextKey.FAMILY_ACCESS)
        else -> source
    }

internal fun syncErrorSummary(issues: Collection<SyncIssue>): String? {
    if (issues.isEmpty()) return null
    return issues
        .groupBy { it.code }
        .entries
        .joinToString("\n") { (code, failures) ->
            val sources = failures.map { sourceLabel(it.source) }.distinct().joinToString(", ")
            when (code) {
                "PERMISSION_DENIED" ->
                    text(
                        TextKey
                            .ACCESS_TO_DENIED_PLEASE_PUBLISH_THE_CURRENT_FIRESTORE_RULES_AND_TRY_AGAI,
                        sources,
                    )
                "UNAUTHENTICATED" ->
                    text(TextKey.YOUR_SIGN_IN_FOR_HAS_EXPIRED_PLEASE_SIGN_IN_AGAIN, sources)
                "UNAVAILABLE",
                "DEADLINE_EXCEEDED" ->
                    text(
                        TextKey
                            .NO_SERVER_CONNECTION_FOR_SAVED_DATA_REMAINS_AVAILABLE_PLEASE_TRY_AGAIN,
                        sources,
                    )
                "RESOURCE_EXHAUSTED" ->
                    text(TextKey.SERVER_LIMIT_REACHED_FOR_PLEASE_CHECK_FIREBASE_USAGE, sources)
                "INVALID_DATA" ->
                    text(TextKey.STORED_DATA_FOR_IS_INVALID_PLEASE_CHECK_THE_DATA_FORMAT, sources)
                else -> text(TextKey.SYNCHRONIZATION_OF_FAILED_PLEASE_TRY_AGAIN, sources, code)
            }
        }
}

internal fun careWriteError(code: String?): String =
    when (code) {
        "PERMISSION_DENIED" ->
            text(TextKey.FIRESTORE_DENIED_THIS_CONFIRMATION_PLEASE_CHECK_THE_FIREBASE_PROJECT_FAM)
        "UNAUTHENTICATED" ->
            text(TextKey.YOUR_SIGN_IN_HAS_EXPIRED_PLEASE_SIGN_IN_AGAIN_AND_REPEAT_THE_CONFIRMATIO)
        "UNAVAILABLE",
        "DEADLINE_EXCEEDED" ->
            text(TextKey.THE_CONFIRMATION_COULD_NOT_SYNCHRONIZE_PLEASE_CHECK_YOUR_CONNECTION_AND)
        else -> text(TextKey.THE_CONFIRMATION_COULD_NOT_BE_SAVED_PLEASE_TRY_AGAIN)
    }
