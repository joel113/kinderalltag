package de.kinderalltag.domain

import java.time.*
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle
import java.util.Locale
import java.util.UUID

data class CalendarImport(val series: AppointmentSeries, val warnings: List<String> = emptyList())

data class CalendarImportResult(val appointments: List<CalendarImport>, val errors: List<String>)

/**
 * A bounded iCalendar subset matching the app's single-day appointments. Unsupported rules are
 * rejected.
 */
object CalendarExchange {
    const val MAX_BYTES = 1024 * 1024
    const val MAX_EVENTS = 100
    private val dateFormat =
        DateTimeFormatter.ofPattern("uuuuMMdd").withResolverStyle(ResolverStyle.STRICT)
    private val timeFormat =
        DateTimeFormatter.ofPattern("uuuuMMdd'T'HHmmss").withResolverStyle(ResolverStyle.STRICT)
    private val utcFormat =
        DateTimeFormatter.ofPattern("uuuuMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC)

    fun export(
        occurrence: AppointmentOccurrence,
        now: Instant = Instant.now(),
        recipientLabel: String? = null,
    ): String {
        val d = occurrence.details
        val lines =
            mutableListOf(
                "BEGIN:VCALENDAR",
                "VERSION:2.0",
                "PRODID:-//Kinderalltag//Termine//DE",
                "CALSCALE:GREGORIAN",
                "BEGIN:VEVENT",
                "UID:${UUID.nameUUIDFromBytes(occurrence.id.toByteArray(Charsets.UTF_8))}@kinderalltag.local",
                "DTSTAMP:${utcFormat.format(now)}",
            )
        if (d.allDay) {
            lines += "DTSTART;VALUE=DATE:${dateFormat.format(d.date)}"
            lines += "DTEND;VALUE=DATE:${dateFormat.format(d.date.plusDays(1))}"
        } else {
            lines += "DTSTART:${utcFormat.format(d.startInstant)}"
            lines += "DTEND:${utcFormat.format(d.endInstant)}"
        }
        lines += "SUMMARY:${escape(d.title)}"
        if (d.location.isNotBlank()) lines += "LOCATION:${escape(d.location)}"
        if (recipientLabel != null) lines += "CATEGORIES:${escape(recipientLabel)}"
        if (d.notes.isNotBlank()) lines += "DESCRIPTION:${escape(d.notes)}"
        lines += listOf("END:VEVENT", "END:VCALENDAR")
        return lines.flatMap(::fold).joinToString("\r\n", postfix = "\r\n")
    }

    fun parse(text: String, now: Instant = Instant.now()): CalendarImportResult {
        require(text.toByteArray(Charsets.UTF_8).size <= MAX_BYTES) {
            "Kalenderdatei ist größer als 1 MB."
        }
        val lines = mutableListOf<String>()
        text.removePrefix("\uFEFF").replace("\r\n", "\n").replace('\r', '\n').split('\n').forEach {
            line ->
            if ((line.startsWith(' ') || line.startsWith('\t')) && lines.isNotEmpty())
                lines[lines.lastIndex] += line.drop(1)
            else lines += line
        }
        require(
            lines.any { it.equals("BEGIN:VCALENDAR", true) } &&
                lines.any { it.equals("END:VCALENDAR", true) }
        ) {
            "Keine gültige iCalendar-Datei (.ics)."
        }
        val imported = mutableListOf<CalendarImport>()
        val errors = mutableListOf<String>()
        var event: MutableList<String>? = null
        var depth = 0
        var count = 0
        lines.forEach { line ->
            when {
                line.equals("BEGIN:VEVENT", true) -> {
                    require(++count <= MAX_EVENTS) {
                        "Bitte höchstens 100 Termine auf einmal importieren."
                    }
                    require(event == null) { "Ungültige verschachtelte Termine." }
                    event = mutableListOf()
                    depth = 0
                }
                line.equals("END:VEVENT", true) && event != null -> {
                    try {
                        imported += parseEvent(requireNotNull(event), now)
                    } catch (e: Exception) {
                        errors +=
                            "Termin $count: ${e.message ?: "Kalenderformat nicht unterstützt."}"
                    }
                    event = null
                }
                event != null && line.startsWith("BEGIN:", true) -> depth++
                event != null && line.startsWith("END:", true) -> depth--
                event != null && depth == 0 -> event!!.add(line)
                event != null && depth == 1 && line.startsWith("TRIGGER", true) -> event!!.add(line)
            }
        }
        require(event == null && count > 0) {
            "Die Kalenderdatei enthält keinen vollständigen Termin."
        }
        val unique =
            imported.distinctBy { item ->
                listOf(
                    item.series.details,
                    item.series.repeat,
                    item.series.interval,
                    item.series.until,
                    item.series.activeFrom,
                )
            }
        return CalendarImportResult(unique, errors)
    }

    private data class Property(
        val name: String,
        val parameters: Map<String, String>,
        val value: String,
    )

    private fun property(line: String): Property? {
        var quoted = false
        val separator =
            line.indexOfFirst { char ->
                if (char == '"') quoted = !quoted
                char == ':' && !quoted
            }
        if (separator < 0) return null
        val parts = line.substring(0, separator).split(';')
        return Property(
            parts.first().uppercase(Locale.ROOT),
            parts.drop(1).associate {
                it.substringBefore('=').uppercase(Locale.ROOT) to
                    it.substringAfter('=', "").trim('"')
            },
            line.substring(separator + 1),
        )
    }

    private fun parseEvent(lines: List<String>, now: Instant): CalendarImport {
        val properties = lines.mapNotNull(::property).groupBy { it.name }
        fun one(name: String): Property? {
            val values = properties[name].orEmpty()
            require(values.size <= 1) { "$name ist mehrfach vorhanden." }
            return values.firstOrNull()
        }
        require(one("STATUS")?.value?.uppercase(Locale.ROOT) != "CANCELLED") {
            "Dieser Termin ist abgesagt."
        }
        require(
            "RECURRENCE-ID" !in properties && "EXDATE" !in properties && "RDATE" !in properties
        ) {
            "Serien mit einzelnen Ausnahmen bitte als einzelne Termine teilen."
        }
        val start = requireNotNull(one("DTSTART")) { "Startdatum fehlt." }
        val allDay = start.parameters["VALUE"] == "DATE" || start.value.length == 8
        val startDate =
            if (allDay) LocalDate.parse(start.value, dateFormat)
            else instant(start).atZone(FAMILY_ZONE).toLocalDate()
        require(startDate.year in 1..9999) { "Das Jahr wird nicht unterstützt." }
        val startTime =
            if (allDay) LocalTime.of(9, 0) else instant(start).atZone(FAMILY_ZONE).toLocalTime()
        val warnings = mutableListOf<String>()
        val end = one("DTEND")
        val duration = one("DURATION")
        require(end == null || duration == null) {
            "DTEND und DURATION dürfen nicht zusammen vorkommen."
        }
        val endTime: LocalTime
        if (allDay) {
            duration?.let {
                require(Duration.parse(it.value) == Duration.ofDays(1)) {
                    "Mehrtagestermine werden noch nicht unterstützt."
                }
            }
            val exclusive =
                end?.let { LocalDate.parse(it.value, dateFormat) }
                    ?: duration?.let { startDate.plusDays(Duration.parse(it.value).toDays()) }
                    ?: startDate.plusDays(1)
            require(exclusive == startDate.plusDays(1)) {
                "Mehrtagestermine werden noch nicht unterstützt."
            }
            endTime = LocalTime.of(10, 0)
        } else {
            val endInstant =
                end?.let(::instant)
                    ?: instant(start)
                        .plus(duration?.let { Duration.parse(it.value) } ?: Duration.ofHours(1))
            val endLocal = endInstant.atZone(FAMILY_ZONE)
            require(endLocal.toLocalDate() == startDate && endInstant > instant(start)) {
                "Bitte einen Termin mit Beginn und Ende am selben Tag teilen."
            }
            endTime = endLocal.toLocalTime()
            if (end == null && duration == null)
                warnings +=
                    "Keine Endzeit angegeben: Bitte die vorgeschlagene Dauer von einer Stunde prüfen."
        }
        val trigger = properties["TRIGGER"].orEmpty()
        val defaultReminder = if (allDay) 1440 else 30
        val reminder =
            if (trigger.isEmpty()) defaultReminder
            else {
                val seconds =
                    runCatching { -Duration.parse(trigger.first().value).seconds }.getOrNull()
                val minutes = seconds?.takeIf { it >= 0 && it % 60 == 0L }?.div(60)?.toInt()
                val allowed = if (allDay) listOf(0, 1440) else listOf(0, 15, 30, 60, 1440)
                if (
                    !allDay &&
                        trigger.size == 1 &&
                        trigger.first().parameters["RELATED"] != "END" &&
                        minutes in allowed
                )
                    minutes
                else {
                    warnings +=
                        "Bitte die Erinnerung neu wählen; die Kalenderdatei verwendet eine andere Erinnerungsart."
                    defaultReminder
                }
            }
        val details =
            AppointmentDetails(
                unescape(one("SUMMARY")?.value ?: "Termin"),
                AppointmentRecipient.HOUSEHOLD,
                startDate,
                startTime,
                endTime,
                allDay,
                location = unescape(one("LOCATION")?.value.orEmpty()),
                notes = unescape(one("DESCRIPTION")?.value.orEmpty()),
                reminderMinutes = reminder,
            )
        var repeat = AppointmentRepeat.ONCE
        var interval = 1
        var until: LocalDate? = null
        one("RRULE")?.let { rule ->
            val entries =
                rule.value.uppercase(Locale.ROOT).split(';').map {
                    it.substringBefore('=') to it.substringAfter('=', "")
                }
            require(entries.map { it.first }.distinct().size == entries.size) {
                "Doppelte Wiederholungsangaben."
            }
            val r = entries.toMap()
            require(
                r.keys.all {
                    it in
                        setOf(
                            "FREQ",
                            "INTERVAL",
                            "UNTIL",
                            "COUNT",
                            "BYDAY",
                            "BYMONTHDAY",
                            "BYMONTH",
                            "WKST",
                        )
                }
            ) {
                "Diese Wiederholungsregel wird noch nicht unterstützt. Bitte einen einzelnen Termin teilen."
            }
            repeat =
                when (r["FREQ"]) {
                    "DAILY" -> AppointmentRepeat.DAYS
                    "WEEKLY" -> AppointmentRepeat.WEEKS
                    "MONTHLY" -> AppointmentRepeat.MONTHS
                    "YEARLY" -> AppointmentRepeat.YEARS
                    else -> error("Diese Wiederholung wird noch nicht unterstützt.")
                }
            interval = r["INTERVAL"]?.toInt() ?: 1
            require(interval in 1..1000) { "Intervall muss zwischen 1 und 1000 liegen." }
            val weekday =
                listOf("MO", "TU", "WE", "TH", "FR", "SA", "SU")[startDate.dayOfWeek.value - 1]
            require(
                r["WKST"] == null || r["WKST"] in listOf("MO", "TU", "WE", "TH", "FR", "SA", "SU")
            ) {
                "Ungültiger Wochenbeginn."
            }
            require(
                r["BYDAY"] == null || (repeat == AppointmentRepeat.WEEKS && r["BYDAY"] == weekday)
            ) {
                "Mehrere oder besondere Wochentage bitte als einzelne Termine teilen."
            }
            require(
                r["BYMONTHDAY"] == null ||
                    (repeat in listOf(AppointmentRepeat.MONTHS, AppointmentRepeat.YEARS) &&
                        r["BYMONTHDAY"] == startDate.dayOfMonth.toString())
            ) {
                "Diese Monatstagsregel wird nicht unterstützt."
            }
            require(
                r["BYMONTH"] == null ||
                    (repeat == AppointmentRepeat.YEARS &&
                        r["BYMONTH"] == startDate.monthValue.toString())
            ) {
                "Diese Monatsregel wird nicht unterstützt."
            }
            require(!("COUNT" in r && "UNTIL" in r)) {
                "COUNT und UNTIL dürfen nicht zusammen vorkommen."
            }
            r["UNTIL"]?.let {
                if (it.length == 8) until = LocalDate.parse(it, dateFormat)
                else {
                    val instant = instant(Property("UNTIL", start.parameters, it))
                    val date = instant.atZone(FAMILY_ZONE).toLocalDate()
                    until =
                        if (details.copy(date = date).startInstant > instant) date.minusDays(1)
                        else date
                }
            }
            r["COUNT"]?.let {
                val count = it.toInt()
                require(count in 1..10000) { "Bitte höchstens 10000 Wiederholungen importieren." }
                until = AppointmentSeries("count", details, repeat, interval).dateAt(count - 1L)
            }
            if (
                (repeat == AppointmentRepeat.MONTHS && startDate.dayOfMonth > 28) ||
                    (repeat == AppointmentRepeat.YEARS &&
                        startDate.monthValue == 2 &&
                        startDate.dayOfMonth == 29)
            ) {
                warnings +=
                    "Bitte den Rhythmus prüfen: Kinderalltag nutzt bei fehlenden Monatstagen den letzten Tag des Monats."
            }
            if (
                !allDay &&
                    (start.value.endsWith('Z') ||
                        start.parameters["TZID"]?.let { it != FAMILY_ZONE.id } == true)
            ) {
                warnings +=
                    "Die Serie wird in Europe/Berlin übernommen. Bitte Uhrzeit und Sommerzeitwechsel prüfen."
            }
        }
        require(until == null || until!!.year in 1..9999) {
            "Das Ende der Serie liegt außerhalb des unterstützten Datumsbereichs."
        }
        var series =
            AppointmentSeries(UUID.randomUUID().toString(), details, repeat, interval, until)
        series.validate()
        if (repeat != AppointmentRepeat.ONCE && details.startInstant <= now) {
            val today = now.atZone(FAMILY_ZONE).toLocalDate()
            var next = series.nextDate(today)
            if (next != null && details.copy(date = next).startInstant <= now)
                next = series.nextDate(today.plusDays(1))
            if (next != null) series = series.copy(fromDate = next)
            else warnings += "Diese Serie ist bereits beendet. Bitte Datum und Wiederholung prüfen."
        }
        return CalendarImport(series, warnings)
    }

    private fun instant(property: Property): Instant {
        val value = property.value
        val utc = value.endsWith('Z')
        val raw = if (utc) value.dropLast(1) else value
        val local = LocalDateTime.parse(if (raw.length == 13) raw + "00" else raw, timeFormat)
        val zone =
            if (utc) ZoneOffset.UTC
            else
                property.parameters["TZID"]?.let {
                    runCatching { ZoneId.of(it) }
                        .getOrElse { error("Zeitzone wird nicht unterstützt.") }
                } ?: FAMILY_ZONE
        return local.atZone(zone).toInstant()
    }

    private fun escape(text: String): String =
        text
            .replace("\\", "\\\\")
            .replace("\r", "")
            .replace("\n", "\\n")
            .replace(";", "\\;")
            .replace(",", "\\,")

    private fun unescape(text: String): String {
        val result = StringBuilder()
        var i = 0
        while (i < text.length) {
            if (text[i] == '\\' && i + 1 < text.length) {
                i++
                result.append(if (text[i] in listOf('n', 'N')) '\n' else text[i])
            } else result.append(text[i])
            i++
        }
        return result.toString()
    }

    private fun fold(line: String): List<String> {
        val result = mutableListOf<String>()
        var current = StringBuilder()
        var bytes = 0
        line.codePoints().toArray().forEach { point ->
            val text = String(Character.toChars(point))
            val count = text.toByteArray(Charsets.UTF_8).size
            if (bytes + count > 75) {
                result += current.toString()
                current = StringBuilder(" ")
                bytes = 1
            }
            current.append(text)
            bytes += count
        }
        result += current.toString()
        return result
    }
}
