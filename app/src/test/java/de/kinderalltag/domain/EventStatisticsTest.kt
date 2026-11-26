package de.kinderalltag.domain

import java.time.*
import org.junit.Assert.*
import org.junit.Test

class EventStatisticsTest {
    private val today = LocalDate.of(2026, 11, 18)
    private val parents = listOf(Parent("a", "Papa"), Parent("b", "Mama"))
    private val children = listOf(Child("ferdinand", "Ferdinand"), Child("johann", "Johann"))
    private val revision =
        TaskRevision(
            "r1",
            "t1",
            "johann",
            "Vitamin D",
            start = today.minusDays(100),
            effective = today.minusDays(100),
        )

    private fun millis(date: LocalDate, hour: Int = 12) =
        date.atTime(hour, 0).atZone(FAMILY_ZONE).toInstant().toEpochMilli()

    private fun completion(
        id: String,
        date: LocalDate = today,
        author: String = "a",
        credited: String? = null,
    ) =
        CareEvent(
            id,
            Occurrence(revision, today, revision.times.single()).id,
            today,
            EventKind.COMPLETE,
            author,
            if (author == "a") "Papa" else "Mama",
            millis(date),
            completedById = credited,
            completedByName = if (credited == "a") "Papa" else if (credited == "b") "Mama" else null,
        )

    private fun appointment(
        id: String,
        recipient: AppointmentRecipient = AppointmentRecipient.BOTH_CHILDREN,
        kind: AppointmentActivityKind = AppointmentActivityKind.ADDED,
        date: LocalDate = today,
    ) =
        AppointmentActivity(
            id,
            id,
            today.plusDays(100),
            kind,
            false,
            AppointmentDetails("Kinderarzt", recipient, today.plusDays(100)),
            null,
            "a",
            "Papa",
            millis(date),
        )

    private fun statistics(
        events: List<CareEvent> = emptyList(),
        activities: List<AppointmentActivity> = emptyList(),
        revisions: List<TaskRevision> = listOf(revision),
        period: HistoryPeriod = statisticsPeriod(today, 90, 0),
    ): EventStatistics {
        val entries =
            activityHistory(
                revisions,
                events,
                emptyList(),
                emptyList(),
                activities,
                parents,
                period,
            )
        return eventStatistics(entries, events, parents, children)
    }

    private fun assertReconciles(result: EventStatistics) {
        listOf(result.parents, result.weekdays).forEach { rows ->
            assertEquals(result.total.events, rows.sumOf { it.counts.events })
            assertEquals(result.total.completedTasks, rows.sumOf { it.counts.completedTasks })
            HistoryKind.entries.forEach { kind ->
                assertEquals(
                    result.total.byKind[kind] ?: 0,
                    rows.sumOf { it.counts.byKind[kind] ?: 0 },
                )
            }
        }
    }

    @Test
    fun periodsAreAdjacentCalendarDaysAcrossDstAndLeapDay() {
        listOf(today, LocalDate.of(2026, 10, 25), LocalDate.of(2024, 3, 1)).forEach { anchor ->
            listOf(7, 30, 90).forEach { days ->
                val current = statisticsPeriod(anchor, days, 0)
                val previous = statisticsPeriod(anchor, days, 1)
                assertEquals(anchor, current.to)
                assertEquals(
                    days.toLong(),
                    java.time.temporal.ChronoUnit.DAYS.between(current.from, current.to.plusDays(1)),
                )
                assertEquals(current.from.minusDays(1), previous.to)
            }
        }
    }

    @Test
    fun emptyDataIncludesZeroParentsSubjectsAndMondayThroughSunday() {
        val result = statistics()
        assertEquals(0, result.total.events)
        assertEquals(0, result.total.completedTasks)
        assertEquals(listOf("a", "b"), result.parents.map { it.id })
        assertEquals(
            listOf("ferdinand", "johann", "haushalt", "shopping"),
            result.subjects.map { it.id },
        )
        assertEquals(DayOfWeek.entries.map { it.name }, result.weekdays.map { it.id })
        assertReconciles(result)
    }

    @Test
    fun creditsSelectedParentWhileOtherActionsUseTheirAuthor() {
        val confirmation = completion("done", credited = "b")
        val result = statistics(listOf(confirmation), listOf(appointment("added")))
        assertEquals(1, result.parents.single { it.id == "b" }.counts.completedTasks)
        assertEquals(1, result.parents.single { it.id == "b" }.counts.events)
        assertEquals(1, result.parents.single { it.id == "a" }.counts.events)
        assertEquals(0, result.parents.single { it.id == "a" }.counts.completedTasks)
        assertReconciles(result)
    }

    @Test
    fun simultaneousConfirmationsCountAsTwoEventsButOneTaskWithStableCredit() {
        val a = completion("a", author = "b")
        val b = completion("b", author = "a")
        val result = statistics(listOf(b, a))
        assertEquals(2, result.total.events)
        assertEquals(1, result.total.completedTasks)
        assertEquals(1, result.parents.single { it.id == "b" }.counts.completedTasks)
        assertEquals(result, statistics(listOf(a, b)))
        assertReconciles(result)
    }

    @Test
    fun correctionAfterSelectedPeriodRemovesNetCompletionButRetainsHistoricalEvent() {
        val done = completion("done", today.minusDays(8))
        val correction =
            done.copy(
                id = "undo",
                kind = EventKind.RETRACT,
                targetId = done.id,
                clientMillis = millis(today),
            )
        val result = statistics(listOf(done, correction), period = statisticsPeriod(today, 7, 1))
        assertEquals(1, result.total.events)
        assertEquals(1, result.total.byKind[HistoryKind.TASK_COMPLETED])
        assertEquals(0, result.total.completedTasks)
        assertReconciles(result)
    }

    @Test
    fun retractingOneDuplicateCreditsTheFirstRemainingConfirmation() {
        val first = completion("first", today.minusDays(2), credited = "b")
        val remaining = completion("remaining", today.minusDays(1), credited = "a")
        val correction =
            first.copy(
                id = "undo",
                kind = EventKind.RETRACT,
                targetId = first.id,
                clientMillis = millis(today),
            )
        val result = statistics(listOf(first, remaining, correction))
        assertEquals(3, result.total.events)
        assertEquals(1, result.total.byKind[HistoryKind.TASK_CORRECTED])
        assertEquals(1, result.parents.single { it.id == "a" }.counts.completedTasks)
        assertEquals(0, result.parents.single { it.id == "b" }.counts.completedTasks)
        assertReconciles(result)
    }

    @Test
    fun duplicateInsidePeriodDoesNotRecountEarlierValidConfirmation() {
        val earlier = completion("earlier", today.minusDays(20))
        val later = completion("later")
        val result = statistics(listOf(earlier, later), period = statisticsPeriod(today, 7, 0))
        assertEquals(1, result.total.events)
        assertEquals(0, result.total.completedTasks)
        assertReconciles(result)
    }

    @Test
    fun sharedAppointmentsCountForEachChildButOnlyOnceOverall() {
        val unknown =
            completion("unknown").copy(occurrenceId = "missing", authorId = "", authorName = "")
        val result =
            statistics(
                listOf(unknown),
                listOf(
                    appointment("both"),
                    appointment("house", AppointmentRecipient.HOUSEHOLD),
                    appointment("ferd", AppointmentRecipient.FERDINAND),
                ),
            )
        assertEquals(4, result.total.events)
        assertEquals(1, result.subjects.single { it.id == "haushalt" }.counts.events)
        assertEquals(2, result.subjects.single { it.id == "ferdinand" }.counts.events)
        assertEquals(1, result.subjects.single { it.id == "johann" }.counts.events)
        assertEquals(1, result.subjects.single { it.id == "unknown" }.counts.completedTasks)
        assertEquals("Unbekannt", result.parents.single { it.id == "__unknown__" }.label)
        assertReconciles(result)
    }

    @Test
    fun weekdaysUseFamilyLocalActionTimeRatherThanScheduledDay() {
        val sundayNight = Instant.parse("2026-11-15T23:30:00Z").toEpochMilli() // Monday in Berlin.
        val result = statistics(listOf(completion("done").copy(clientMillis = sundayNight)))
        assertEquals(1, result.weekdays.single { it.id == "MONDAY" }.counts.events)
        assertEquals(0, result.weekdays.single { it.id == "SUNDAY" }.counts.events)
        assertReconciles(result)
    }

    @Test
    fun periodIncludesFirstMidnightAndExcludesFollowingMidnight() {
        val period = statisticsPeriod(today, 7, 0)
        val first = completion("first", period.from).copy(clientMillis = millis(period.from, 0))
        val last = completion("last", today).copy(occurrenceId = "other")
        val outside =
            completion("outside", today.plusDays(1))
                .copy(clientMillis = millis(today.plusDays(1), 0))
        val result = statistics(listOf(first, last, outside), period = period)
        assertEquals(2, result.total.events)
        assertEquals(2, result.total.completedTasks)
        assertReconciles(result)
    }

    @Test
    fun allEventKindsAndLegacyPendingMetadataArePreservedWithoutDuplicateRows() {
        val done = completion("done").copy(pending = true)
        val correction = done.copy(id = "undo", kind = EventKind.RETRACT, targetId = done.id)
        val activities = AppointmentActivityKind.entries.map { appointment(it.name, kind = it) }
        val edited = revision.copy(createdMillis = millis(today), authorId = "former")
        val entries =
            activityHistory(
                    listOf(edited),
                    listOf(done, correction),
                    emptyList(),
                    emptyList(),
                    activities,
                    parents,
                    statisticsPeriod(today, 90, 0),
                )
                .map {
                    if (it.kind == HistoryKind.APPOINTMENT_ADDED) it.copy(legacy = true) else it
                }
        val result = eventStatistics(entries + entries, listOf(done, correction), parents, children)
        assertEquals(6, result.total.events)
        assertEquals(
            setOf(
                HistoryKind.TASK_COMPLETED,
                HistoryKind.TASK_CORRECTED,
                HistoryKind.TASK_UPDATED,
                HistoryKind.APPOINTMENT_ADDED,
                HistoryKind.APPOINTMENT_CHANGED,
                HistoryKind.APPOINTMENT_CANCELLED,
            ),
            result.total.byKind.keys,
        )
        assertEquals(1, result.reconstructedEvents)
        assertEquals(2, result.pendingEvents)
        assertEquals(1, result.parents.single { it.id == "former" }.counts.events)
        assertReconciles(result)
    }
}
