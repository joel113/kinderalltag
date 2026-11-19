package de.kinderalltag.domain

import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth

enum class CalendarView {
    DAY,
    WEEK,
    MONTH,
}

data class CalendarOverviewState(
    val focus: LocalDate,
    val view: CalendarView = CalendarView.MONTH,
    val filterActive: Boolean = false,
) {
    fun changeView(value: CalendarView) = copy(view = value, filterActive = true)

    fun selectDay(date: LocalDate) =
        copy(focus = date, view = CalendarView.DAY, filterActive = true)

    fun move(direction: Int): CalendarOverviewState {
        require(direction == -1 || direction == 1)
        val date =
            when (view) {
                CalendarView.DAY -> focus.plusDays(direction.toLong())
                CalendarView.WEEK -> focus.plusWeeks(direction.toLong())
                CalendarView.MONTH -> focus.plusMonths(direction.toLong())
            }
        return copy(focus = date, filterActive = true)
    }

    fun reset(today: LocalDate) =
        copy(focus = today, view = CalendarView.MONTH, filterActive = false)

    val period: HistoryPeriod
        get() =
            when (view) {
                CalendarView.DAY -> HistoryPeriod(focus, focus)
                CalendarView.WEEK ->
                    focus.minusDays(focus.dayOfWeek.value - 1L).let {
                        HistoryPeriod(it, it.plusDays(6))
                    }
                CalendarView.MONTH ->
                    YearMonth.from(focus).let { HistoryPeriod(it.atDay(1), it.atEndOfMonth()) }
            }

    val visibleDates: List<LocalDate>
        get() =
            if (view == CalendarView.MONTH) monthGridDates(YearMonth.from(focus))
            else
                (0L until if (view == CalendarView.WEEK) 7L else 1L).map {
                    period.from.plusDays(it)
                }
}

fun monthGridDates(month: YearMonth): List<LocalDate> {
    val first = month.atDay(1)
    val start = first.minusDays(first.dayOfWeek.value - 1L)
    val days = ((first.dayOfWeek.value - 1 + month.lengthOfMonth() + 6) / 7) * 7
    return (0L until days.toLong()).map { start.plusDays(it) }
}

fun tasksInCalendarPeriod(
    revisions: List<TaskRevision>,
    period: HistoryPeriod,
    sectionId: String? = null,
    uid: String? = null,
    onlyMine: Boolean = false,
    events: List<CareEvent> = emptyList(),
    operations: List<ItemOperation> = emptyList(),
): List<TaskRevision> {
    require(period.to >= period.from)
    val tasks = linkedMapOf<String, TaskRevision>()
    var date = period.from
    while (date <= period.to) {
        ManagedTasks.occurrences(revisions, events, operations, date)
            .map { it.revision }
            .filter {
                it.isDue(date) &&
                    (sectionId == null || it.childId == sectionId) &&
                    (!onlyMine || isResponsible(it.responsibleParentId, uid))
            }
            .forEach { tasks[it.taskId] = it }
        date = date.plusDays(1)
    }
    return tasks.values.toList()
}

data class CalendarPreviewItem(
    val id: String,
    val title: String,
    val times: List<LocalTime>,
    val allDay: Boolean = false,
)

fun taskCalendarPreviews(
    revisions: List<TaskRevision>,
    dates: List<LocalDate>,
    sectionId: String?,
    uid: String?,
    onlyMine: Boolean,
    events: List<CareEvent> = emptyList(),
    operations: List<ItemOperation> = emptyList(),
): Map<LocalDate, List<CalendarPreviewItem>> =
    dates.associateWith { date ->
        ManagedTasks.occurrences(revisions, events, operations, date)
            .filter { occurrence ->
                val it = occurrence.revision
                it.isDue(date) &&
                    (sectionId == null || it.childId == sectionId) &&
                    (!onlyMine || isResponsible(it.responsibleParentId, uid))
            }
            .groupBy { it.revision.taskId }
            .map { (id, occurrences) ->
                CalendarPreviewItem(
                    id,
                    occurrences.first().revision.title,
                    occurrences.map { it.time }.distinct().sorted(),
                )
            }
            .sortedWith(
                compareBy<CalendarPreviewItem> { it.times.firstOrNull() ?: LocalTime.MAX }
                    .thenBy { it.title }
                    .thenBy { it.id }
            )
    }

fun taskCalendarCounts(
    revisions: List<TaskRevision>,
    dates: List<LocalDate>,
    sectionId: String?,
    uid: String?,
    onlyMine: Boolean,
): Map<LocalDate, Int> =
    taskCalendarPreviews(revisions, dates, sectionId, uid, onlyMine).mapValues { it.value.size }

fun appointmentCalendarPreviews(
    series: List<AppointmentSeries>,
    exceptions: List<AppointmentException>,
    dates: List<LocalDate>,
    sectionId: String?,
    uid: String?,
    onlyMine: Boolean,
    activity: List<AppointmentActivity> = emptyList(),
    operations: List<ItemOperation> = emptyList(),
): Map<LocalDate, List<CalendarPreviewItem>> {
    if (dates.isEmpty()) return emptyMap()
    return ManagedAppointments.occurrences(
            series,
            exceptions,
            activity,
            operations,
            dates.min(),
            dates.max(),
        )
        .filter {
            it.details.recipient.matches(sectionId) &&
                (!onlyMine || isResponsible(it.details.responsibleParentId, uid))
        }
        .groupBy { it.details.date }
        .mapValues { (_, items) ->
            items.map {
                CalendarPreviewItem(
                    it.id,
                    it.details.title,
                    if (it.details.allDay) emptyList() else listOf(it.details.startTime),
                    it.details.allDay,
                )
            }
        }
}

fun appointmentCalendarCounts(
    series: List<AppointmentSeries>,
    exceptions: List<AppointmentException>,
    dates: List<LocalDate>,
    sectionId: String?,
    uid: String?,
    onlyMine: Boolean,
): Map<LocalDate, Int> =
    appointmentCalendarPreviews(series, exceptions, dates, sectionId, uid, onlyMine).mapValues {
        it.value.size
    }
