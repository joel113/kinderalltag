package de.kinderalltag.domain

import de.kinderalltag.text.*
import java.time.*
import java.time.temporal.ChronoUnit

/** Explicit recipients. Legacy names remain readable without changing historical snapshots. */
@ConsistentCopyVisibility
data class AppointmentRecipient
private constructor(val childIds: Set<String>, val household: Boolean = false) :
    java.io.Serializable {
    fun matches(sectionId: String?): Boolean =
        sectionId == null || if (household) sectionId == "haushalt" else sectionId in childIds

    fun visibleFor(children: List<Child>): Boolean =
        household || children.any { !it.archived && it.id in childIds }

    fun toggleChild(id: String): AppointmentRecipient {
        val ids = if (id in childIds) childIds - id else childIds + id
        return if (ids.size in 1..10) children(ids) else this
    }

    val name: String
        get() =
            when (this) {
                FERDINAND -> "FERDINAND"
                JOHANN -> "JOHANN"
                BOTH_CHILDREN -> "BOTH_CHILDREN"
                HOUSEHOLD -> "HOUSEHOLD"
                else -> "CHILDREN:" + childIds.sorted().joinToString(",")
            }

    // Keep legacy wire values for unchanged selections: immutable audit equality depends on it.
    fun stored(): Any =
        if (name.startsWith("CHILDREN:"))
            mapOf("kind" to "CHILDREN", "childIds" to childIds.sorted())
        else name

    companion object {
        val FERDINAND = AppointmentRecipient(setOf("ferdinand"))
        val JOHANN = AppointmentRecipient(setOf("johann"))
        val BOTH_CHILDREN = AppointmentRecipient(setOf("ferdinand", "johann"))
        val HOUSEHOLD = AppointmentRecipient(emptySet(), true)

        fun children(ids: Set<String>): AppointmentRecipient {
            require(
                ids.size in 1..10 &&
                    ids.all { it.matches(Regex("[A-Za-z0-9_-]{1,100}")) && it != "haushalt" }
            )
            return AppointmentRecipient(ids.toSortedSet())
        }

        fun valueOf(value: String): AppointmentRecipient =
            when (value) {
                "FERDINAND" -> FERDINAND
                "JOHANN" -> JOHANN
                "BOTH_CHILDREN" -> BOTH_CHILDREN
                "HOUSEHOLD" -> HOUSEHOLD
                else -> {
                    require(value.startsWith("CHILDREN:"))
                    children(value.removePrefix("CHILDREN:").split(",").toSet())
                }
            }

        fun fromStored(value: Any?): AppointmentRecipient =
            when (value) {
                is String -> valueOf(value)
                is Map<*, *> -> {
                    require(value.keys == setOf("kind", "childIds") && value["kind"] == "CHILDREN")
                    val ids = value["childIds"] as List<*>
                    require(ids.all { it is String } && ids.toSet().size == ids.size)
                    children(ids.filterIsInstance<String>().toSet())
                }
                else -> error("Invalid recipient")
            }
    }
}

enum class AppointmentRepeat {
    ONCE,
    DAYS,
    WEEKS,
    MONTHS,
    YEARS,
}

data class AppointmentDetails(
    val title: String,
    val recipient: AppointmentRecipient,
    val date: LocalDate,
    val startTime: LocalTime = LocalTime.of(9, 0),
    val endTime: LocalTime = LocalTime.of(10, 0),
    val allDay: Boolean = false,
    val responsibleParentId: String? = null,
    val location: String = "",
    val notes: String = "",
    val reminderMinutes: Int? = 30,
) {
    val startInstant: Instant
        get() =
            date
                .atTime(if (allDay) LocalTime.MIDNIGHT else startTime)
                .atZone(FAMILY_ZONE)
                .toInstant()

    val endInstant: Instant
        get() {
            if (allDay) return date.plusDays(1).atStartOfDay(FAMILY_ZONE).toInstant()
            val resolved = date.atTime(endTime).atZone(FAMILY_ZONE).toInstant()
            return if (resolved > startInstant) resolved
            else startInstant.plus(Duration.between(startTime, endTime))
        }

    val reminderInstant: Instant?
        get() =
            reminderMinutes?.let {
                (if (allDay) date.atTime(9, 0).atZone(FAMILY_ZONE).toInstant() else startInstant)
                    .minusSeconds(it * 60L)
            }

    fun validate() {
        requireLocalized(title.isNotBlank() && title.length <= 120) {
            message(TextKey.TITLE_1_TO_120_CHARACTERS)
        }
        requireLocalized(location.length <= 300 && notes.length <= 2000) {
            message(TextKey.LOCATION_OR_NOTES_ARE_TOO_LONG)
        }
        requireLocalized(allDay || endTime > startTime) {
            message(TextKey.THE_END_MUST_BE_AFTER_THE_START_ON_THE_SAME_DAY)
        }
        requireLocalized(listOf(startTime, endTime).all { it.second == 0 && it.nano == 0 }) {
            message(TextKey.PLEASE_USE_TIMES_WITH_MINUTE_PRECISION)
        }
        requireLocalized(
            reminderMinutes == null ||
                reminderMinutes in if (allDay) listOf(0, 1440) else listOf(0, 15, 30, 60, 1440)
        ) {
            message(TextKey.PLEASE_SELECT_A_VALID_REMINDER)
        }
    }
}

data class AppointmentSeries(
    val id: String,
    val details: AppointmentDetails,
    val repeat: AppointmentRepeat = AppointmentRepeat.ONCE,
    val interval: Int = 1,
    val until: LocalDate? = null,
    // Exclusive original-date boundary. Splitting never changes earlier appointments.
    val cutoff: LocalDate? = null,
    val version: Long = 0,
    val authorId: String = "",
    val fromDate: LocalDate? = null,
    val createdMillis: Long = 0,
    val rootId: String? = null,
) {
    val activeFrom: LocalDate
        get() = fromDate ?: details.date

    fun validate() {
        details.validate()
        requireLocalized(interval in 1..1000) { message(TextKey.INTERVAL_1_TO_1000) }
        requireLocalized(repeat != AppointmentRepeat.ONCE || interval == 1) {
            message(TextKey.ONE_OFF_APPOINTMENTS_HAVE_NO_INTERVAL)
        }
        requireLocalized(activeFrom >= details.date) {
            message(TextKey.THE_SERIES_BEGINS_BEFORE_THE_RECURRENCE_STARTS)
        }
        requireLocalized(until == null || until >= activeFrom) {
            message(TextKey.THE_END_DATE_IS_BEFORE_THE_FIRST_APPOINTMENT)
        }
        requireLocalized(copy(cutoff = null).contains(activeFrom)) {
            message(TextKey.THE_SERIES_START_DOES_NOT_MATCH_THE_INTERVAL)
        }
    }

    fun dateAt(index: Long): LocalDate =
        when (repeat) {
            AppointmentRepeat.ONCE -> details.date
            AppointmentRepeat.DAYS -> details.date.plusDays(index * interval)
            AppointmentRepeat.WEEKS -> details.date.plusWeeks(index * interval)
            AppointmentRepeat.MONTHS -> details.date.plusMonths(index * interval)
            AppointmentRepeat.YEARS -> details.date.plusYears(index * interval)
        }

    private fun nearIndex(date: LocalDate): Long {
        val units =
            when (repeat) {
                AppointmentRepeat.ONCE -> 0
                AppointmentRepeat.DAYS -> ChronoUnit.DAYS.between(details.date, date)
                AppointmentRepeat.WEEKS -> ChronoUnit.WEEKS.between(details.date, date)
                AppointmentRepeat.MONTHS ->
                    ChronoUnit.MONTHS.between(YearMonth.from(details.date), YearMonth.from(date))
                AppointmentRepeat.YEARS -> (date.year - details.date.year).toLong()
            }
        return maxOf(0, units / interval - 1)
    }

    fun nextDate(from: LocalDate): LocalDate? {
        var index = nearIndex(maxOf(from, activeFrom))
        repeat(4) {
            val candidate = dateAt(index++)
            if ((until != null && candidate > until) || (cutoff != null && candidate >= cutoff))
                return null
            if (candidate >= from && candidate >= activeFrom) return candidate
            if (repeat == AppointmentRepeat.ONCE) return null
        }
        return null
    }

    fun contains(date: LocalDate): Boolean = datesBetween(date, date).isNotEmpty()

    fun datesBetween(from: LocalDate, to: LocalDate): List<LocalDate> {
        if (to < from || to < details.date) return emptyList()
        if (repeat == AppointmentRepeat.ONCE)
            return listOf(details.date).filter {
                it in from..to &&
                    it >= activeFrom &&
                    (until == null || it <= until) &&
                    (cutoff == null || it < cutoff)
            }
        val dates = mutableListOf<LocalDate>()
        var index = nearIndex(maxOf(from, activeFrom))
        while (true) {
            val date = dateAt(index++)
            if (date > to || (until != null && date > until) || (cutoff != null && date >= cutoff))
                break
            if (date >= from && date >= activeFrom) dates += date
        }
        return dates
    }
}

data class AppointmentException(
    val seriesId: String,
    val originalDate: LocalDate,
    val replacement: AppointmentDetails?, // null means canceled
    val version: Long = 0,
    val authorId: String = "",
    val updatedMillis: Long = 0,
) {
    val id: String
        get() = "${seriesId}_$originalDate"
}

data class AppointmentOccurrence(
    val series: AppointmentSeries,
    val originalDate: LocalDate,
    val details: AppointmentDetails,
    val sourceSeriesId: String = series.id,
    val originTime: LocalTime =
        if (series.details.allDay) LocalTime.MIDNIGHT else series.details.startTime,
) {
    val id: String
        get() = "appointment_${sourceSeriesId}_$originalDate"
}

object AppointmentSchedule {
    fun canSplit(
        series: AppointmentSeries,
        boundary: LocalDate,
        exceptions: List<AppointmentException>,
        now: Instant,
    ): Boolean =
        exceptions.none {
            it.seriesId == series.id &&
                it.originalDate >= boundary &&
                series.contains(it.originalDate) &&
                it.replacement?.let { details -> details.startInstant <= now } == true
        }

    fun find(
        id: String,
        series: List<AppointmentSeries>,
        exceptions: List<AppointmentException>,
    ): AppointmentOccurrence? {
        if (!id.startsWith("appointment_")) return null
        val key = id.removePrefix("appointment_")
        val original =
            runCatching { LocalDate.parse(key.substringAfterLast('_')) }.getOrNull() ?: return null
        val item =
            series.firstOrNull { it.id == key.substringBeforeLast('_') && it.contains(original) }
                ?: return null
        val exception =
            exceptions.firstOrNull { it.seriesId == item.id && it.originalDate == original }
        val details =
            if (exception != null) exception.replacement else item.details.copy(date = original)
        return details?.let { AppointmentOccurrence(item, original, it) }
    }

    fun occurrences(
        series: List<AppointmentSeries>,
        exceptions: List<AppointmentException>,
        from: LocalDate,
        to: LocalDate,
    ): List<AppointmentOccurrence> {
        val overrides = exceptions.associateBy { it.id }
        return series
            .flatMap { item ->
                // Also inspect moved-in appointments whose original dates fall outside this window.
                val candidates =
                    (item.datesBetween(from, to) +
                            exceptions
                                .filter {
                                    it.seriesId == item.id &&
                                        it.replacement?.date?.let { date -> date in from..to } ==
                                            true
                                }
                                .map { it.originalDate })
                        .distinct()
                candidates
                    .filter { item.contains(it) }
                    .mapNotNull { original ->
                        val exception = overrides["${item.id}_$original"]
                        val details =
                            if (exception != null) exception.replacement
                            else item.details.copy(date = original)
                        details
                            ?.takeIf { it.date in from..to }
                            ?.let { AppointmentOccurrence(item, original, it) }
                    }
            }
            .sortedWith(
                compareBy<AppointmentOccurrence> { it.details.date }
                    .thenBy { if (it.details.allDay) LocalTime.MIDNIGHT else it.details.startTime }
                    .thenBy { it.id }
            )
    }
}
