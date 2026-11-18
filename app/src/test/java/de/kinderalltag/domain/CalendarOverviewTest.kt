package de.kinderalltag.domain

import org.junit.Assert.*
import org.junit.Test
import java.time.*

class CalendarOverviewTest {
    private val date = LocalDate.of(2026, 11, 16)
    private fun task(id: String = "t1", child: String = "johann") =
        TaskRevision("r$id", id, child, "Test", start = date, effective = date)
    private fun series(id: String = "s1", recipient: AppointmentRecipient = AppointmentRecipient.JOHANN) =
        AppointmentSeries(id, AppointmentDetails("Termin", recipient, date))

    @Test fun monthGridHasCompleteMondayFirstWeeksAndIncludesEveryDay() {
        listOf(YearMonth.of(2026, 11), YearMonth.of(2024, 2), YearMonth.of(2026, 2)).forEach { month ->
            val grid = monthGridDates(month)
            assertTrue(grid.size in 28..42)
            assertEquals(0, grid.size % 7)
            assertEquals(DayOfWeek.MONDAY, grid.first().dayOfWeek)
            assertEquals(DayOfWeek.SUNDAY, grid.last().dayOfWeek)
            assertTrue(month.atDay(1) in grid)
            assertTrue(month.atEndOfMonth() in grid)
            assertEquals(grid.size, grid.distinct().size)
            grid.zipWithNext().forEach { (a, b) -> assertEquals(a.plusDays(1), b) }
        }
    }
    @Test fun mondayMonthStartsOnItsFirstDayAndLeapDayIsIncluded() {
        assertEquals(LocalDate.of(2026, 6, 1), monthGridDates(YearMonth.of(2026, 6)).first())
        assertTrue(LocalDate.of(2024, 2, 29) in monthGridDates(YearMonth.of(2024, 2)))
    }
    @Test fun taskCalendarCountsDefinitionsOnceEvenWithMultipleTimes() {
        val task = task().copy(times = listOf(LocalTime.of(8, 0), LocalTime.of(18, 0)))
        val counts = taskCalendarCounts(listOf(task), listOf(date), null, "a", false)
        assertEquals(1, counts[date])
        assertEquals(2, Schedule.occurrences(listOf(task), date).size)
    }
    @Test fun taskCalendarRespectsWeekdaysEndDatesAndEffectivePauses() {
        val daily = task()
        val paused = daily.copy(id = "pause", effective = date.plusDays(1), active = false)
        val weekday = task("weekly").copy(frequency = Frequency.WEEKDAYS, weekdays = setOf(DayOfWeek.WEDNESDAY), end = date.plusDays(3))
        val counts = taskCalendarCounts(listOf(daily, paused, weekday), (0L..7L).map(date::plusDays), null, "a", false)
        assertEquals(1, counts[date])
        assertEquals(0, counts[date.plusDays(1)])
        assertEquals(1, counts[date.plusDays(2)])
        assertEquals(0, counts[date.plusDays(7)])
    }
    @Test fun taskCalendarMatchesChildAndResponsibilityFilters() {
        val shared = task()
        val other = task("other", "ferdinand").copy(responsibleParentId = "b")
        val mine = task("mine", "haushalt").copy(responsibleParentId = "a")
        assertEquals(3, taskCalendarCounts(listOf(shared, other, mine), listOf(date), null, "a", false)[date])
        assertEquals(2, taskCalendarCounts(listOf(shared, other, mine), listOf(date), null, "a", true)[date])
        assertEquals(0, taskCalendarCounts(listOf(shared, other, mine), listOf(date), "ferdinand", "a", true)[date])
        assertEquals(1, taskCalendarCounts(listOf(shared, other, mine), listOf(date), "johann", "a", true)[date])
    }
    @Test fun appointmentsCountMovedDatesAndExcludeCancelledOccurrences() {
        val recurring = series().copy(repeat = AppointmentRepeat.DAYS)
        val cancelled = AppointmentException("s1", date.plusDays(1), null)
        val moved = AppointmentException("s1", date.minusDays(1), recurring.details.copy(date = date.plusDays(2)))
        val earlier = recurring.copy(details = recurring.details.copy(date = date.minusDays(1)))
        val counts = appointmentCalendarCounts(listOf(earlier), listOf(cancelled, moved), (0L..3L).map(date::plusDays), null, "a", false)
        assertEquals(1, counts[date])
        assertNull(counts[date.plusDays(1)])
        assertEquals(2, counts[date.plusDays(2)])
        assertEquals(1, counts[date.plusDays(3)])
    }
    @Test fun appointmentCalendarMatchesBothChildrenHouseholdAndParentFilters() {
        val both = series("both", AppointmentRecipient.BOTH_CHILDREN)
        val home = series("home", AppointmentRecipient.HOUSEHOLD).copy(details =
            series("home", AppointmentRecipient.HOUSEHOLD).details.copy(responsibleParentId = "b"))
        assertEquals(2, appointmentCalendarCounts(listOf(both, home), emptyList(), listOf(date), null, "a", false)[date])
        assertEquals(1, appointmentCalendarCounts(listOf(both, home), emptyList(), listOf(date), "ferdinand", "a", true)[date])
        assertEquals(1, appointmentCalendarCounts(listOf(both, home), emptyList(), listOf(date), "johann", "a", true)[date])
        assertNull(appointmentCalendarCounts(listOf(both, home), emptyList(), listOf(date), "haushalt", "a", true)[date])
        assertTrue(appointmentCalendarCounts(listOf(both), emptyList(), emptyList(), null, "a", false).isEmpty())
    }
    @Test fun monthGridOmitsUnusedWeeks() {
        assertEquals(28, monthGridDates(YearMonth.of(2021, 2)).size)
        assertEquals(35, monthGridDates(YearMonth.of(2026, 4)).size)
        assertEquals(42, monthGridDates(YearMonth.of(2026, 8)).size)
    }
    @Test fun viewsKeepFocusAndDefineDayWeekAndMonthPeriods() {
        val initial = CalendarOverviewState(date)
        assertEquals(CalendarView.MONTH, initial.view)
        assertFalse(initial.filterActive)
        val day = initial.changeView(CalendarView.DAY)
        assertTrue(day.filterActive)
        assertEquals(HistoryPeriod(date, date), day.period)
        assertEquals(listOf(date), day.visibleDates)
        val week = day.changeView(CalendarView.WEEK)
        assertEquals(HistoryPeriod(date, date.plusDays(6)), week.period)
        assertEquals(7, week.visibleDates.size)
        assertEquals(date, week.focus)
        val month = week.changeView(CalendarView.MONTH)
        assertEquals(HistoryPeriod(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30)), month.period)
    }
    @Test fun arrowsAdvanceBySelectedPeriodAndHandleLeapMonthEnds() {
        val state = CalendarOverviewState(date)
        assertEquals(date.plusDays(1), state.changeView(CalendarView.DAY).move(1).focus)
        assertEquals(date.minusWeeks(1), state.changeView(CalendarView.WEEK).move(-1).focus)
        assertEquals(date.plusMonths(1), state.move(1).focus)
        assertEquals(LocalDate.of(2024, 2, 29), CalendarOverviewState(LocalDate.of(2024, 1, 31)).move(1).focus)
        assertEquals(LocalDate.of(2027, 1, 16), CalendarOverviewState(LocalDate.of(2026, 12, 16)).move(1).focus)
    }
    @Test fun weekCanCrossYearBoundaryAndStartsOnMonday() {
        val week = CalendarOverviewState(LocalDate.of(2026, 1, 1), CalendarView.WEEK)
        assertEquals(LocalDate.of(2025, 12, 29), week.period.from)
        assertEquals(LocalDate.of(2026, 1, 4), week.period.to)
        assertEquals(DayOfWeek.MONDAY, week.visibleDates.first().dayOfWeek)
    }
    @Test fun selectingGridDateOpensDayAndResetRestoresUnfilteredMonth() {
        val adjacentDate = LocalDate.of(2026, 12, 1)
        val selected = CalendarOverviewState(date).selectDay(adjacentDate)
        assertEquals(CalendarView.DAY, selected.view)
        assertEquals(adjacentDate, selected.focus)
        assertTrue(selected.filterActive)
        val reset = selected.reset(date)
        assertEquals(CalendarView.MONTH, reset.view)
        assertEquals(date, reset.focus)
        assertFalse(reset.filterActive)
    }
    @Test fun periodTaskListDeduplicatesAndUsesOnlyActuallyScheduledDays() {
        val daily = task()
        val edited = daily.copy(id = "new", title = "New title", effective = date.plusDays(2))
        val once = task("once").copy(frequency = Frequency.ONCE, start = date.plusDays(6))
        val later = task("later").copy(frequency = Frequency.ONCE, start = date.plusDays(7))
        val period = CalendarOverviewState(date, CalendarView.WEEK).period
        val result = tasksInCalendarPeriod(listOf(daily, edited, once, later), period)
        assertEquals(setOf("t1", "once"), result.map { it.taskId }.toSet())
        assertEquals("new", result.single { it.taskId == "t1" }.id)
        assertEquals(listOf(daily), tasksInCalendarPeriod(listOf(daily, edited, once, later), HistoryPeriod(date, date)))
    }
    @Test fun periodTaskListRespectsPausesAndWeekdaySchedules() {
        val daily = task()
        val paused = daily.copy(id = "pause", active = false, effective = date.plusDays(1))
        val weekday = task("weekly").copy(frequency = Frequency.WEEKDAYS, weekdays = setOf(DayOfWeek.FRIDAY))
        assertTrue(tasksInCalendarPeriod(listOf(daily, paused, weekday), HistoryPeriod(date.plusDays(1), date.plusDays(3))).isEmpty())
        assertEquals(setOf("t1", "weekly"), tasksInCalendarPeriod(listOf(daily, paused, weekday),
            HistoryPeriod(date, date.plusDays(6))).map { it.taskId }.toSet())
    }

    @Test fun periodFiltersApplyBeforeDeduplicatingChangingAssignments() {
        val first = task().copy(responsibleParentId = "a")
        val changed = first.copy(id = "changed", effective = date.plusDays(2), childId = "ferdinand", responsibleParentId = "b")
        val period = CalendarOverviewState(date, CalendarView.WEEK).period
        assertEquals(listOf(first), tasksInCalendarPeriod(listOf(first, changed), period, "johann", "a", true))
        assertEquals(listOf(changed), tasksInCalendarPeriod(listOf(first, changed), period, "ferdinand", "b", true))
        assertTrue(tasksInCalendarPeriod(listOf(first, changed), period, "ferdinand", "a", true).isEmpty())
    }

    @Test fun taskPreviewsRetainTitlesAllTimesAndStableChronologicalOrder() {
        val later = task("later").copy(title = "Late routine", times = listOf(LocalTime.of(18, 0), LocalTime.of(9, 0)))
        val earlier = task("earlier").copy(title = "Early routine", times = listOf(LocalTime.of(8, 0)))
        val household = task("home", "haushalt")
        val previews = taskCalendarPreviews(listOf(later, household, earlier), listOf(date), "johann", "a", false).getValue(date)
        assertEquals(listOf("Early routine", "Late routine"), previews.map { it.title })
        assertEquals(listOf(LocalTime.of(9, 0), LocalTime.of(18, 0)), previews.last().times)
        assertEquals("later", previews.last().id)
        assertEquals(2, previews.size)
        assertTrue(previews.none { it.allDay })
    }
    @Test fun taskPreviewsFollowEffectiveTitleChangesAndPauses() {
        val first = task().copy(title = "Original")
        val changed = first.copy(id = "edited", title = "Changed", effective = date.plusDays(1))
        val paused = changed.copy(id = "paused", active = false, effective = date.plusDays(2))
        val previews = taskCalendarPreviews(listOf(paused, changed, first), (0L..2L).map(date::plusDays), null, "a", false)
        assertEquals("Original", previews.getValue(date).single().title)
        assertEquals("Changed", previews.getValue(date.plusDays(1)).single().title)
        assertTrue(previews.getValue(date.plusDays(2)).isEmpty())
    }
    @Test fun appointmentPreviewsIncludeAllDayAndMovedTitlesWithoutCanceledEntries() {
        val allDay = series("all").copy(details = series("all").details.copy(title = "All day", allDay = true))
        val movedSeries = series("moved")
        val moved = AppointmentException("moved", date, movedSeries.details.copy(title = "Moved visit", date = date.plusDays(1)))
        val cancelled = series("cancelled")
        val exceptions = listOf(moved, AppointmentException("cancelled", date, null))
        val previews = appointmentCalendarPreviews(listOf(allDay, movedSeries, cancelled), exceptions,
            listOf(date, date.plusDays(1)), null, "a", false)
        val first = previews.getValue(date).single()
        assertEquals("All day", first.title)
        assertTrue(first.allDay)
        assertTrue(first.times.isEmpty())
        val next = previews.getValue(date.plusDays(1)).single()
        assertEquals("Moved visit", next.title)
        assertEquals(listOf(LocalTime.of(9, 0)), next.times)
        assertEquals("appointment_moved_$date", next.id)
    }

}
