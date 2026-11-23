package de.kinderalltag.domain

import de.kinderalltag.text.*
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
        requireLocalized(text.toByteArray(Charsets.UTF_8).size <= MAX_BYTES) {
            message(TextKey.THE_CALENDAR_FILE_IS_LARGER_THAN_1_MB)
        }
        val lines = mutableListOf<String>()
        text.removePrefix("\uFEFF").replace("\r\n", "\n").replace('\r', '\n').split('\n').forEach {
            line ->
            if ((line.startsWith(' ') || line.startsWith('\t')) && lines.isNotEmpty())
                lines[lines.lastIndex] += line.drop(1)
            else lines += line
        }
        requireLocalized(
            lines.any { it.equals("BEGIN:VCALENDAR", true) } &&
                lines.any { it.equals("END:VCALENDAR", true) }
        ) {
            message(TextKey.NOT_A_VALID_ICALENDAR_FILE_ICS)
        }
        val imported = mutableListOf<CalendarImport>()
        val errors = mutableListOf<String>()
        var event: MutableList<String>? = null
        var depth = 0
        var count = 0
        lines.forEach { line ->
            when {
                line.equals("BEGIN:VEVENT", true) -> {
                    requireLocalized(++count <= MAX_EVENTS) {
                        message(TextKey.PLEASE_IMPORT_NO_MORE_THAN_100_APPOINTMENTS_AT_ONCE)
                    }
                    requireLocalized(event == null) { message(TextKey.INVALID_NESTED_APPOINTMENTS) }
                    event = mutableListOf()
                    depth = 0
                }
                line.equals("END:VEVENT", true) && event != null -> {
                    try {
                        imported += parseEvent(requireNotNull(event), now)
                    } catch (e: Exception) {
                        errors +=
                            text(
                                TextKey.APPOINTMENT_8435F7,
                                count,
                                e.message ?: text(TextKey.CALENDAR_FORMAT_NOT_SUPPORTED),
                            )
                    }
                    event = null
                }
                event != null && line.startsWith("BEGIN:", true) -> depth++
                event != null && line.startsWith("END:", true) -> depth--
                event != null && depth == 0 -> event!!.add(line)
                event != null && depth == 1 && line.startsWith("TRIGGER", true) -> event!!.add(line)
            }
        }
        requireLocalized(event == null && count > 0) {
            message(TextKey.THE_CALENDAR_FILE_CONTAINS_NO_COMPLETE_APPOINTMENT)
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
            requireLocalized(values.size <= 1) { message(TextKey.APPEARS_MORE_THAN_ONCE, name) }
            return values.firstOrNull()
        }
        requireLocalized(one("STATUS")?.value?.uppercase(Locale.ROOT) != "CANCELLED") {
            message(TextKey.THIS_APPOINTMENT_IS_CANCELLED)
        }
        requireLocalized(
            "RECURRENCE-ID" !in properties && "EXDATE" !in properties && "RDATE" !in properties
        ) {
            message(TextKey.PLEASE_SHARE_SERIES_WITH_INDIVIDUAL_EXCEPTIONS_AS_SEPARATE_APPOINTMENTS)
        }
        val start = requireNotNullLocalized(one("DTSTART")) { message(TextKey.START_DATE_MISSING) }
        val allDay = start.parameters["VALUE"] == "DATE" || start.value.length == 8
        val startDate =
            if (allDay) LocalDate.parse(start.value, dateFormat)
            else instant(start).atZone(FAMILY_ZONE).toLocalDate()
        requireLocalized(startDate.year in 1..9999) { message(TextKey.THIS_YEAR_IS_NOT_SUPPORTED) }
        val startTime =
            if (allDay) LocalTime.of(9, 0) else instant(start).atZone(FAMILY_ZONE).toLocalTime()
        val warnings = mutableListOf<String>()
        val end = one("DTEND")
        val duration = one("DURATION")
        requireLocalized(end == null || duration == null) {
            message(TextKey.DTEND_AND_DURATION_CANNOT_APPEAR_TOGETHER)
        }
        val endTime: LocalTime
        if (allDay) {
            duration?.let {
                requireLocalized(Duration.parse(it.value) == Duration.ofDays(1)) {
                    message(TextKey.MULTI_DAY_APPOINTMENTS_ARE_NOT_SUPPORTED_YET)
                }
            }
            val exclusive =
                end?.let { LocalDate.parse(it.value, dateFormat) }
                    ?: duration?.let { startDate.plusDays(Duration.parse(it.value).toDays()) }
                    ?: startDate.plusDays(1)
            requireLocalized(exclusive == startDate.plusDays(1)) {
                message(TextKey.MULTI_DAY_APPOINTMENTS_ARE_NOT_SUPPORTED_YET)
            }
            endTime = LocalTime.of(10, 0)
        } else {
            val endInstant =
                end?.let(::instant)
                    ?: instant(start)
                        .plus(duration?.let { Duration.parse(it.value) } ?: Duration.ofHours(1))
            val endLocal = endInstant.atZone(FAMILY_ZONE)
            requireLocalized(endLocal.toLocalDate() == startDate && endInstant > instant(start)) {
                message(TextKey.PLEASE_SHARE_AN_APPOINTMENT_THAT_STARTS_AND_ENDS_ON_THE_SAME_DAY)
            }
            endTime = endLocal.toLocalTime()
            if (end == null && duration == null)
                warnings +=
                    text(TextKey.NO_END_TIME_SPECIFIED_PLEASE_CHECK_THE_SUGGESTED_ONE_HOUR_DURATION)
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
                        text(
                            TextKey
                                .PLEASE_SELECT_THE_REMINDER_AGAIN_THE_CALENDAR_FILE_USES_A_DIFFERENT_REMI
                        )
                    defaultReminder
                }
            }
        val details =
            AppointmentDetails(
                unescape(one("SUMMARY")?.value ?: text(TextKey.APPOINTMENT)),
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
            requireLocalized(entries.map { it.first }.distinct().size == entries.size) {
                message(TextKey.DUPLICATE_RECURRENCE_FIELDS)
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
                text(
                    TextKey.THIS_RECURRENCE_RULE_IS_NOT_SUPPORTED_YET_PLEASE_SHARE_A_SINGLE_APPOINTM
                )
            }
            repeat =
                when (r["FREQ"]) {
                    "DAILY" -> AppointmentRepeat.DAYS
                    "WEEKLY" -> AppointmentRepeat.WEEKS
                    "MONTHLY" -> AppointmentRepeat.MONTHS
                    "YEARLY" -> AppointmentRepeat.YEARS
                    else ->
                        throw LocalizedStateException(
                            message(TextKey.THIS_RECURRENCE_IS_NOT_SUPPORTED_YET)
                        )
                }
            interval = r["INTERVAL"]?.toInt() ?: 1
            requireLocalized(interval in 1..1000) {
                message(TextKey.THE_INTERVAL_MUST_BE_BETWEEN_1_AND_1000)
            }
            val weekday =
                listOf("MO", "TU", "WE", "TH", "FR", "SA", "SU")[startDate.dayOfWeek.value - 1]
            requireLocalized(
                r["WKST"] == null || r["WKST"] in listOf("MO", "TU", "WE", "TH", "FR", "SA", "SU")
            ) {
                message(TextKey.INVALID_WEEK_START)
            }
            requireLocalized(
                r["BYDAY"] == null || (repeat == AppointmentRepeat.WEEKS && r["BYDAY"] == weekday)
            ) {
                message(
                    TextKey.PLEASE_SHARE_MULTIPLE_OR_SPECIAL_WEEKDAYS_AS_INDIVIDUAL_APPOINTMENTS
                )
            }
            requireLocalized(
                r["BYMONTHDAY"] == null ||
                    (repeat in listOf(AppointmentRepeat.MONTHS, AppointmentRepeat.YEARS) &&
                        r["BYMONTHDAY"] == startDate.dayOfMonth.toString())
            ) {
                message(TextKey.THIS_DAY_OF_MONTH_RULE_IS_NOT_SUPPORTED)
            }
            requireLocalized(
                r["BYMONTH"] == null ||
                    (repeat == AppointmentRepeat.YEARS &&
                        r["BYMONTH"] == startDate.monthValue.toString())
            ) {
                message(TextKey.THIS_MONTHLY_RULE_IS_NOT_SUPPORTED)
            }
            requireLocalized(!("COUNT" in r && "UNTIL" in r)) {
                message(TextKey.COUNT_AND_UNTIL_CANNOT_APPEAR_TOGETHER)
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
                requireLocalized(count in 1..10000) {
                    message(TextKey.PLEASE_IMPORT_NO_MORE_THAN_10000_OCCURRENCES)
                }
                until = AppointmentSeries("count", details, repeat, interval).dateAt(count - 1L)
            }
            if (
                (repeat == AppointmentRepeat.MONTHS && startDate.dayOfMonth > 28) ||
                    (repeat == AppointmentRepeat.YEARS &&
                        startDate.monthValue == 2 &&
                        startDate.dayOfMonth == 29)
            ) {
                warnings +=
                    text(
                        TextKey
                            .PLEASE_CHECK_THE_RHYTHM_KINDERALLTAG_USES_THE_LAST_DAY_OF_THE_MONTH_WHEN
                    )
            }
            if (
                !allDay &&
                    (start.value.endsWith('Z') ||
                        start.parameters["TZID"]?.let { it != FAMILY_ZONE.id } == true)
            ) {
                warnings +=
                    text(
                        TextKey
                            .THE_SERIES_WILL_USE_EUROPE_BERLIN_PLEASE_CHECK_THE_TIME_AND_DAYLIGHT_SAV
                    )
            }
        }
        requireLocalized(until == null || until!!.year in 1..9999) {
            message(TextKey.THE_SERIES_END_IS_OUTSIDE_THE_SUPPORTED_DATE_RANGE)
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
            else
                warnings +=
                    text(TextKey.THIS_SERIES_HAS_ALREADY_ENDED_PLEASE_CHECK_THE_DATE_AND_RECURRENCE)
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
                        .getOrElse {
                            throw LocalizedStateException(message(TextKey.TIMEZONE_NOT_SUPPORTED))
                        }
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
