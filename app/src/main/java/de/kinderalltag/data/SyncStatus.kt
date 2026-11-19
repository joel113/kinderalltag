package de.kinderalltag.data

internal data class SyncIssue(val source: String, val code: String)

private fun sourceLabel(source: String): String =
    when (source) {
        "children" -> "Kinderprofile"
        "revisions" -> "Aufgaben"
        "events" -> "Bestätigungen"
        "members" -> "Elternliste"
        "appointmentSeries" -> "Termine"
        "appointmentExceptions" -> "Terminänderungen"
        "appointmentActivity" -> "Terminverlauf"
        "itemOperations" -> "Änderungen & Papierkorb"
        "listItems" -> "Einkäufe & Geschenkideen"
        "listActivity" -> "Listenverlauf"
        "listLabels" -> "Listenlabels"
        "membership" -> "Familienzugang"
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
                    "Zugriff auf $sources verweigert. Bitte die aktuellen Firestore-Regeln veröffentlichen und erneut versuchen."
                "UNAUTHENTICATED" -> "Anmeldung für $sources abgelaufen. Bitte erneut anmelden."
                "UNAVAILABLE",
                "DEADLINE_EXCEEDED" ->
                    "Keine Serververbindung für $sources. Gespeicherte Daten bleiben verfügbar. Bitte erneut versuchen."
                "RESOURCE_EXHAUSTED" ->
                    "Serverlimit für $sources erreicht. Bitte Firebase-Nutzung prüfen."
                "INVALID_DATA" ->
                    "Gespeicherte Daten für $sources sind ungültig. Bitte das Datenformat prüfen."
                else ->
                    "Synchronisierung von $sources fehlgeschlagen ($code). Bitte erneut versuchen."
            }
        }
}

internal fun careWriteError(code: String?): String =
    when (code) {
        "PERMISSION_DENIED" ->
            "Firestore verweigert diese Bestätigung. Bitte Firebase-Projekt, Familienzugang und App-Check-Einstellungen prüfen."
        "UNAUTHENTICATED" ->
            "Deine Anmeldung ist abgelaufen. Bitte erneut anmelden und die Bestätigung wiederholen."
        "UNAVAILABLE",
        "DEADLINE_EXCEEDED" ->
            "Die Bestätigung konnte nicht synchronisiert werden. Bitte die Verbindung prüfen und erneut versuchen."
        else -> "Bestätigung konnte nicht gespeichert werden. Bitte erneut versuchen."
    }
