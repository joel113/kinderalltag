package de.kinderalltag.domain

import java.time.*
import org.junit.Assert.*
import org.junit.Test

class ItemOperationsTest {
    private val day = LocalDate.of(2026, 11, 18)

    private fun at(date: LocalDate = day, hour: Int) =
        date.atTime(hour, 0).atZone(FAMILY_ZONE).toInstant()

    private val task =
        TaskRevision(
            "legacy",
            "task",
            "johann",
            "Original",
            times = listOf(LocalTime.of(8, 0), LocalTime.of(18, 0)),
            start = day.minusDays(30),
            effective = day.minusDays(30),
        )

    private fun original(date: LocalDate = day, hour: Int = 18) =
        Occurrence(task, date, LocalTime.of(hour, 0))

    private fun editTask(
        id: String = "edit",
        scope: OperationScope = OperationScope.FOLLOWING,
        anchor: Occurrence = original(),
        draft: TaskRevision =
            task.copy(
                id = "draft",
                title = "Changed",
                times = listOf(LocalTime.of(9, 0), LocalTime.of(19, 0)),
            ),
        hour: Int = 12,
    ): ItemOperation {
        val mapped = ManagedTasks.mapSlots(anchor.revision, draft)
        return ItemOperation(
            id,
            ItemType.TASK,
            task.taskId,
            OperationAction.EDIT,
            scope,
            anchor.id,
            anchor.originalDate,
            anchor.originalTime,
            "legacy",
            mapped.title,
            actualDate = anchor.date,
            actualStartMillis = anchor.instant.toEpochMilli(),
            childId = "johann",
            task = mapped,
            createdMillis = at(hour = hour).toEpochMilli(),
            version = hour.toLong(),
        )
    }

    private fun deleteTask(
        id: String = "delete",
        scope: OperationScope = OperationScope.SINGLE,
        anchor: Occurrence = original(),
    ) =
        ItemOperation(
            id,
            ItemType.TASK,
            "task",
            OperationAction.DELETE,
            scope,
            anchor.id,
            anchor.originalDate,
            anchor.originalTime,
            "legacy",
            task.title,
            actualDate = anchor.date,
            actualStartMillis = anchor.instant.toEpochMilli(),
            childId = "johann",
            createdMillis = at(hour = 12).toEpochMilli(),
        )

    private fun restore(operation: ItemOperation, id: String = "restore") =
        operation.copy(
            id = id,
            action = OperationAction.RESTORE,
            restoresId = operation.id,
            createdMillis = at(hour = 14).toEpochMilli(),
        )

    private fun tasks(
        date: LocalDate = day,
        operations: List<ItemOperation> = emptyList(),
        events: List<CareEvent> = emptyList(),
    ) = ManagedTasks.occurrences(listOf(task), events, operations, date)

    private fun complete(occurrence: Occurrence) =
        CareEvent(
            "completion",
            occurrence.id,
            occurrence.date,
            EventKind.COMPLETE,
            "a",
            "Papa",
            at(hour = 11).toEpochMilli(),
            taskSnapshot = occurrence,
        )

    private val series =
        AppointmentSeries(
            "series",
            AppointmentDetails("Original visit", AppointmentRecipient.JOHANN, day.minusDays(2)),
            repeat = AppointmentRepeat.DAYS,
        )

    private fun appointment(date: LocalDate = day.plusDays(1)) =
        AppointmentOccurrence(series, date, series.details.copy(date = date))

    private fun appointmentEdit(
        scope: OperationScope = OperationScope.FOLLOWING,
        anchor: AppointmentOccurrence = appointment(),
        draft: AppointmentSeries =
            AppointmentSeries(
                "draft",
                series.details.copy(title = "Changed visit", date = anchor.details.date),
                repeat = AppointmentRepeat.DAYS,
            ),
        id: String = "edit-appointment",
    ) =
        ItemOperation(
            id,
            ItemType.APPOINTMENT,
            "series",
            OperationAction.EDIT,
            scope,
            anchor.id,
            anchor.originalDate,
            anchor.originTime,
            "series",
            draft.details.title,
            actualDate = anchor.details.date,
            actualStartMillis = anchor.details.startInstant.toEpochMilli(),
            recipient = AppointmentRecipient.JOHANN,
            appointment = draft,
            createdMillis = at(hour = 12).toEpochMilli(),
        )

    private fun appointments(
        date: LocalDate,
        operations: List<ItemOperation>,
        exceptions: List<AppointmentException> = emptyList(),
    ) =
        ManagedAppointments.occurrences(
            listOf(series),
            exceptions,
            emptyList(),
            operations,
            date,
            date,
        )

    @Test
    fun immediateEditPreservesStartedTasksAndChangesUpcomingSlotsWithStableIds() {
        val edit = editTask()
        val result = tasks(operations = listOf(edit))
        assertEquals(listOf(LocalTime.of(8, 0), LocalTime.of(19, 0)), result.map { it.time })
        assertEquals(listOf("Original", "Changed"), result.map { it.revision.title })
        assertEquals(listOf(original(hour = 8).id, original().id), result.map { it.id })
        assertEquals(
            listOf(LocalTime.of(9, 0), LocalTime.of(19, 0)),
            tasks(day.plusDays(1), listOf(edit)).map { it.time },
        )
    }

    @Test
    fun aCompletedFutureTaskRetainsSnapshotEvenWhenSeriesChanges() {
        val completed = original()
        val edit = editTask()
        val result = tasks(operations = listOf(edit), events = listOf(complete(completed)))
        assertEquals(completed, result.single { it.id == completed.id })
        assertEquals(
            "Changed",
            tasks(day.plusDays(1), listOf(edit), listOf(complete(completed))).first().revision.title,
        )
    }

    @Test
    fun legacyConfirmationWithoutSnapshotAlsoPinsItsOriginalOccurrence() {
        val completed = original()
        val event = complete(completed).copy(taskSnapshot = null)
        assertEquals(
            completed,
            tasks(operations = listOf(editTask()), events = listOf(event)).single {
                it.id == completed.id
            },
        )
    }

    @Test
    fun overdueOneOffCanMoveRepeatedlyWithoutGhostsAndPreservesCompletionSnapshots() {
        val oneOff =
            task.copy(frequency = Frequency.ONCE, start = day, times = listOf(LocalTime.of(8, 0)))
        val original = Occurrence(oneOff, day, oneOff.times.single())
        val first =
            editTask(
                scope = OperationScope.SINGLE,
                anchor = original,
                draft = oneOff.copy(id = "tomorrow", start = day.plusDays(1)),
                hour = 12,
            )
        fun result(
            date: LocalDate,
            operations: List<ItemOperation>,
            events: List<CareEvent> = emptyList(),
        ) = ManagedTasks.occurrences(listOf(oneOff), events, operations, date)
        assertTrue(result(day, listOf(first)).isEmpty())
        val moved = result(day.plusDays(1), listOf(first)).single()
        assertEquals(original.id, moved.id)
        assertEquals(day, moved.originalDate)
        val second =
            first.copy(
                id = "later",
                actualDate = moved.date,
                actualStartMillis = moved.instant.toEpochMilli(),
                createdMillis = at(day.plusDays(1), 12).toEpochMilli(),
                version = 13,
                task = first.task!!.copy(id = "day-after", start = day.plusDays(2)),
            )
        assertTrue(result(day, listOf(first, second)).isEmpty())
        assertTrue(result(day.plusDays(1), listOf(first, second)).isEmpty())
        assertEquals(original.id, result(day.plusDays(2), listOf(first, second)).single().id)
        val completed = complete(moved)
        assertEquals(moved, result(moved.date, listOf(first, second), listOf(completed)).single())
        assertTrue(result(day.plusDays(2), listOf(first, second), listOf(completed)).isEmpty())
    }

    @Test
    fun overdueRecurringInstancesStayProtectedButOneOffsCanMoveBackwards() {
        val recurring = original(hour = 8)
        val edit =
            editTask(
                scope = OperationScope.SINGLE,
                anchor = recurring,
                draft =
                    task.copy(
                        frequency = Frequency.ONCE,
                        start = day.plusDays(1),
                        times = listOf(recurring.time),
                    ),
            )
        assertTrue(tasks(day, listOf(edit)).any { it.id == recurring.id })
        assertFalse(tasks(day.plusDays(1), listOf(edit)).any { it.id == recurring.id })
        val oneOff =
            task.copy(frequency = Frequency.ONCE, start = day, times = listOf(recurring.time))
        val past = oneOff.copy(start = day.minusDays(1))
        val backwards = edit.copy(task = past)
        assertTrue(
            ManagedTasks.occurrences(listOf(oneOff), emptyList(), listOf(backwards), day).isEmpty()
        )
        val moved =
            ManagedTasks.occurrences(listOf(oneOff), emptyList(), listOf(backwards), past.start)
                .single()
        assertEquals(recurring.id, moved.id)
        assertEquals(past.start, moved.date)
        assertEquals(day, moved.originalDate)
    }

    @Test
    fun futureRecurringExecutionCanMoveIntoThePastWithoutChangingOtherExecutionsOrCompletions() {
        val anchor = original()
        val pastDay = day.minusDays(3)
        val edit =
            editTask(
                scope = OperationScope.SINGLE,
                draft =
                    task.copy(
                        id = "backwards",
                        frequency = Frequency.ONCE,
                        start = pastDay,
                        times = listOf(LocalTime.of(10, 0)),
                    ),
            )
        assertFalse(tasks(day, listOf(edit)).any { it.id == anchor.id })
        val moved = tasks(pastDay, listOf(edit)).single { it.id == anchor.id }
        assertEquals(pastDay, moved.date)
        assertEquals(day, moved.originalDate)
        assertEquals(LocalTime.of(10, 0), moved.time)
        assertEquals(tasks(day.plusDays(1)), tasks(day.plusDays(1), listOf(edit)))
        val completion = complete(moved)
        val laterEdit =
            edit.copy(
                id = "later",
                createdMillis = at(hour = 14).toEpochMilli(),
                task = edit.task!!.copy(start = day.plusDays(2)),
            )
        assertEquals(
            moved,
            tasks(pastDay, listOf(edit, laterEdit), listOf(completion)).single {
                it.id == anchor.id
            },
        )
        assertFalse(
            tasks(day.plusDays(2), listOf(edit, laterEdit), listOf(completion)).any {
                it.id == anchor.id
            }
        )
    }

    @Test
    fun singleTaskMoveKeepsIdentityAndDoesNotAffectOtherTimeOrDay() {
        val draft =
            task.copy(
                id = "single",
                title = "Moved",
                frequency = Frequency.ONCE,
                start = day.plusDays(1),
                times = listOf(LocalTime.of(10, 0)),
            )
        val edit = editTask(scope = OperationScope.SINGLE, draft = draft)
        assertEquals(listOf(LocalTime.of(8, 0)), tasks(operations = listOf(edit)).map { it.time })
        val moved = tasks(day.plusDays(1), listOf(edit)).single { it.id == original().id }
        assertEquals(LocalTime.of(10, 0), moved.time)
        assertEquals(day, moved.originalDate)
        assertEquals(LocalTime.of(18, 0), moved.originalTime)
        assertEquals(3, tasks(day.plusDays(1), listOf(edit)).size)
    }

    @Test
    fun repeatedSingleMovesDoNotLeaveGhostsOnOldDays() {
        val first =
            editTask(
                scope = OperationScope.SINGLE,
                draft =
                    task.copy(
                        id = "first",
                        frequency = Frequency.ONCE,
                        start = day.plusDays(1),
                        times = listOf(LocalTime.of(10, 0)),
                    ),
            )
        val second =
            first.copy(
                id = "second",
                task = first.task!!.copy(id = "second-draft", start = day.plusDays(2)),
                createdMillis = at(hour = 13).toEpochMilli(),
            )
        assertFalse(tasks(day, listOf(first, second)).any { it.id == original().id })
        assertFalse(tasks(day.plusDays(1), listOf(first, second)).any { it.id == original().id })
        assertTrue(tasks(day.plusDays(2), listOf(first, second)).any { it.id == original().id })
    }

    @Test
    fun explicitSingleOverrideWinsOverLaterSeriesEdit() {
        val single =
            editTask(
                scope = OperationScope.SINGLE,
                draft =
                    task.copy(
                        id = "single",
                        title = "One-off",
                        frequency = Frequency.ONCE,
                        start = day,
                        times = listOf(LocalTime.of(20, 0)),
                    ),
            )
        val later = editTask(id = "later", hour = 13)
        assertEquals(
            "One-off",
            tasks(operations = listOf(single, later))
                .single { it.id == original().id }
                .revision
                .title,
        )
    }

    @Test
    fun completedMovedTaskRemainsPinnedAtItsActualDay() {
        val single =
            editTask(
                scope = OperationScope.SINGLE,
                draft =
                    task.copy(
                        id = "single",
                        title = "One-off",
                        frequency = Frequency.ONCE,
                        start = day.plusDays(1),
                        times = listOf(LocalTime.of(10, 0)),
                    ),
            )
        val moved = tasks(day.plusDays(1), listOf(single)).single { it.id == original().id }
        val event = complete(moved)
        assertFalse(
            tasks(operations = listOf(single, editTask(hour = 13)), events = listOf(event)).any {
                it.id == moved.id
            }
        )
        assertEquals(
            moved,
            tasks(day.plusDays(1), listOf(single, editTask(hour = 13)), listOf(event)).single {
                it.id == moved.id
            },
        )
    }

    @Test
    fun deletionCanHidePastOrCompletedTaskWithoutErasingItsConfirmation() {
        val completed = original(hour = 8)
        val event = complete(completed)
        val deleted = deleteTask(anchor = completed)
        assertFalse(
            tasks(operations = listOf(deleted), events = listOf(event)).any {
                it.id == completed.id
            }
        )
        assertEquals(1, Schedule.activeCompletions(listOf(event), completed.id).size)
        assertEquals(
            completed,
            tasks(operations = listOf(deleted, restore(deleted)), events = listOf(event)).single {
                it.id == completed.id
            },
        )
    }

    @Test
    fun singleDeletionDoesNotHideOtherDailyTimeAndFollowingUsesOriginalOrder() {
        val single = deleteTask(anchor = original(hour = 8))
        assertEquals(
            listOf(LocalTime.of(18, 0)),
            tasks(operations = listOf(single)).map { it.time },
        )
        val following = deleteTask(scope = OperationScope.FOLLOWING)
        assertEquals(
            listOf(LocalTime.of(8, 0)),
            tasks(operations = listOf(following)).map { it.time },
        )
        assertTrue(tasks(day.plusDays(1), listOf(following)).isEmpty())
        assertEquals(2, tasks(day.minusDays(1), listOf(following)).size)
    }

    @Test
    fun overlappingDeletionsAreIndependentAndRestoreDoesNotUndoLaterEdits() {
        val one = deleteTask()
        val all = deleteTask("all", OperationScope.ALL)
        assertTrue(tasks(operations = listOf(one, all, restore(one))).isEmpty())
        val edit = editTask(hour = 13)
        val restored = tasks(operations = listOf(one, edit, restore(one)))
        assertEquals("Changed", restored.single { it.id == original().id }.revision.title)
        assertTrue(deletionsOverlap(one, all))
        assertFalse(deletionsOverlap(one, deleteTask("other", anchor = original(hour = 8))))
    }

    @Test
    fun restorationCannotCancelDeletionOfAnotherItem() {
        val deleted = deleteTask()
        assertEquals(
            listOf(deleted),
            activeDeletions(listOf(deleted, restore(deleted).copy(itemId = "other"))),
        )
    }

    @Test
    fun slotMappingRetainsUnchangedSlotsAndPairsChangedTimesDeterministically() {
        val draft =
            task.copy(
                id = "new",
                times = listOf(LocalTime.of(18, 0), LocalTime.of(9, 0), LocalTime.of(22, 0)),
            )
        val mapped = ManagedTasks.mapSlots(task, draft)
        assertEquals(listOf("legacy|64800", "legacy|28800", "new|79200"), mapped.slotOrigins)
    }

    @Test
    fun appointmentSingleMoveIsVisibleOnlyAtReplacementDateWithOriginalIdentity() {
        val anchor = appointment()
        val target = day.plusDays(5)
        val edit =
            appointmentEdit(
                OperationScope.SINGLE,
                anchor,
                AppointmentSeries(
                    "single",
                    anchor.details.copy(
                        title = "Moved",
                        date = target,
                        startTime = LocalTime.of(11, 0),
                        endTime = LocalTime.NOON,
                    ),
                ),
            )
        assertFalse(appointments(anchor.details.date, listOf(edit)).any { it.id == anchor.id })
        val moved = appointments(target, listOf(edit)).single { it.id == anchor.id }
        assertEquals("Moved", moved.details.title)
        assertEquals(anchor.originalDate, moved.originalDate)
        assertEquals(target, moved.details.date)
    }

    @Test
    fun appointmentFollowingEditPreservesEarlierDatesAndChangesFutureOnes() {
        val edit = appointmentEdit()
        assertEquals("Original visit", appointments(day, listOf(edit)).single().details.title)
        assertEquals(
            "Changed visit",
            appointments(day.plusDays(1), listOf(edit)).single().details.title,
        )
        assertEquals(
            "Changed visit",
            appointments(day.plusDays(20), listOf(edit)).single().details.title,
        )
    }

    @Test
    fun appointmentSingleOverrideSurvivesFollowingEditAndMovedInWindow() {
        val anchor = appointment(day.plusDays(2))
        val single =
            appointmentEdit(
                OperationScope.SINGLE,
                anchor,
                AppointmentSeries(
                    "single",
                    anchor.details.copy(title = "One-off", date = day.plusDays(7)),
                ),
                "single",
            )
        val following =
            appointmentEdit().copy(id = "later", createdMillis = at(hour = 13).toEpochMilli())
        assertEquals(
            "One-off",
            appointments(day.plusDays(7), listOf(single, following))
                .single { it.id == anchor.id }
                .details
                .title,
        )
        assertFalse(
            appointments(anchor.details.date, listOf(single, following)).any { it.id == anchor.id }
        )
    }

    @Test
    fun movedAlreadyStartedAppointmentIsNotChangedOrDuplicatedBySeriesEdit() {
        val originalDate = day.plusDays(3)
        val moved =
            AppointmentException(
                series.id,
                originalDate,
                series.details.copy(
                    date = day,
                    startTime = LocalTime.of(8, 0),
                    endTime = LocalTime.of(9, 0),
                ),
            )
        val edit = appointmentEdit()
        assertEquals(
            "Original visit",
            appointments(day, listOf(edit), listOf(moved))
                .single { it.originalDate == originalDate }
                .details
                .title,
        )
        assertTrue(appointments(originalDate, listOf(edit), listOf(moved)).isEmpty())
    }

    @Test
    fun wholeSeriesDeletionIncludesKnownLegacySplitsButNotUnrelatedSeries() {
        val boundary = day.plusDays(1)
        val old = series.copy(cutoff = boundary)
        val split =
            series.copy(
                id = "split",
                details = series.details.copy(date = boundary),
                fromDate = boundary,
            )
        val unrelated = series.copy(id = "unrelated")
        val activity =
            AppointmentActivity(
                "split-log",
                series.id,
                boundary,
                AppointmentActivityKind.CHANGED,
                true,
                split.details,
                split.id,
                "a",
                "Papa",
                1,
            )
        val deletion =
            ItemOperation(
                "all",
                ItemType.APPOINTMENT,
                series.id,
                OperationAction.DELETE,
                OperationScope.ALL,
                appointment().id,
                boundary,
                LocalTime.of(9, 0),
                series.id,
                "Visits",
                recipient = AppointmentRecipient.JOHANN,
            )
        val data = listOf(old, split, unrelated)
        assertEquals(
            listOf("unrelated"),
            ManagedAppointments.occurrences(
                    data,
                    emptyList(),
                    listOf(activity),
                    listOf(deletion),
                    day,
                    day.plusDays(4),
                )
                .map { it.sourceSeriesId }
                .distinct(),
        )
        assertEquals(
            setOf(series.id, split.id, unrelated.id),
            ManagedAppointments.occurrences(
                    data,
                    emptyList(),
                    listOf(activity),
                    listOf(deletion, restore(deletion)),
                    day,
                    day.plusDays(4),
                )
                .map { it.sourceSeriesId }
                .toSet(),
        )
    }

    @Test
    fun deletionHistoryAndStatisticsKeepCompletedTaskAndAddAuditEvents() {
        val occurrence = original(hour = 8)
        val event = complete(occurrence)
        val deletion = deleteTask(anchor = occurrence)
        val operations = listOf(deletion, restore(deletion))
        val parents = listOf(Parent("a", "Papa"))
        val entries =
            activityHistory(
                listOf(task),
                listOf(event),
                emptyList(),
                emptyList(),
                emptyList(),
                parents,
                HistoryPeriod(day, day),
                operations = operations,
            )
        assertEquals(
            setOf(HistoryKind.TASK_COMPLETED, HistoryKind.TASK_DELETED, HistoryKind.TASK_RESTORED),
            entries.map { it.kind }.toSet(),
        )
        assertEquals(
            1,
            eventStatistics(entries, listOf(event), parents, listOf(Child("johann", "Johann")))
                .total
                .completedTasks,
        )
        assertEquals("Original", entries.single { it.kind == HistoryKind.TASK_COMPLETED }.title)
    }

    @Test
    fun remindersRespectDeletionAndRestorationWithoutReplayingPastAlarms() {
        val occurrence = original()
        val deletion = deleteTask(anchor = occurrence)
        fun reminders(ops: List<ItemOperation>) =
            FamilyReminders.plan(
                "a",
                listOf(Child("johann", "Johann")),
                listOf(task),
                emptyList(),
                emptyList(),
                emptyList(),
                at(hour = 12),
                operations = ops,
            )
        assertFalse(reminders(listOf(deletion)).any { it.id == occurrence.id })
        assertTrue(reminders(listOf(deletion, restore(deletion))).any { it.id == occurrence.id })
        assertFalse(
            reminders(listOf(deletion, restore(deletion))).any { it.id == original(hour = 8).id }
        )
    }

    @Test
    fun lateOfflineConfirmationPinsAnOccurrenceRemovedByAnEarlierEdit() {
        val edit =
            editTask(draft = task.copy(id = "remove-slot", times = listOf(LocalTime.of(8, 0))))
        assertFalse(tasks(operations = listOf(edit)).any { it.id == original().id })
        val late = complete(original()).copy(pending = true)
        assertEquals(
            original(),
            tasks(operations = listOf(edit), events = listOf(late)).single {
                it.id == original().id
            },
        )
    }

    @Test
    fun movingAnUpcomingTimeBeforeEditClockDoesNotBackfillThePast() {
        val edit =
            editTask(
                draft =
                    task.copy(
                        id = "earlier",
                        times = listOf(LocalTime.of(8, 0), LocalTime.of(10, 0)),
                    )
            )
        assertEquals(listOf(LocalTime.of(8, 0)), tasks(operations = listOf(edit)).map { it.time })
        assertEquals(
            listOf(LocalTime.of(8, 0), LocalTime.of(10, 0)),
            tasks(day.plusDays(1), listOf(edit)).map { it.time },
        )
    }

    @Test
    fun pauseAndResumeAffectUpcomingSlotsImmediatelyAndDefinitionsUseOperationClock() {
        val pause = editTask(id = "pause", draft = task.copy(id = "paused-draft", active = false))
        assertEquals(listOf(LocalTime.of(8, 0)), tasks(operations = listOf(pause)).map { it.time })
        assertTrue(
            ManagedTasks.definitions(listOf(task), emptyList(), listOf(pause), at(hour = 12))
                .isEmpty()
        )
        assertFalse(
            ManagedTasks.definitions(listOf(task), emptyList(), listOf(pause), at(hour = 12), true)
                .single()
                .active
        )
        val resume =
            editTask(id = "resume", draft = task.copy(id = "resumed", title = "Resumed"), hour = 13)
        assertEquals(
            "Resumed",
            tasks(operations = listOf(pause, resume))
                .single { it.id == original().id }
                .revision
                .title,
        )
        assertEquals(
            "Original",
            tasks(operations = listOf(pause, resume))
                .single { it.id == original(hour = 8).id }
                .revision
                .title,
        )
    }

    @Test
    fun finiteSingleDeletionsDoNotHideAnUnboundedRoutineFromUpcomingDefinitions() {
        val oneTimeDaily = task.copy(times = listOf(LocalTime.of(18, 0)))
        val deleted =
            (0L..20L).map { offset ->
                deleteTask("delete-$offset", anchor = original(day.plusDays(offset)))
            }
        val future =
            ManagedTasks.futureOccurrences(
                listOf(oneTimeDaily),
                emptyList(),
                deleted,
                task.taskId,
                at(hour = 12),
            )
        assertEquals(day.plusDays(21), future.first().date)
        assertEquals(
            1,
            ManagedTasks.definitions(listOf(oneTimeDaily), emptyList(), deleted, at(hour = 12)).size,
        )
    }

    @Test
    fun distantOneOffTasksDoNotRequireEnumeratingAllInterveningDays() {
        val distant =
            task.copy(
                frequency = Frequency.ONCE,
                start = day.plusDays(5000),
                times = listOf(LocalTime.of(18, 0)),
            )
        assertEquals(
            distant.start,
            ManagedTasks.futureOccurrences(
                    listOf(distant),
                    emptyList(),
                    emptyList(),
                    task.taskId,
                    at(hour = 12),
                )
                .single()
                .date,
        )
    }

    @Test
    fun legacyAppointmentExceptionsSurviveNewSeriesEdits() {
        val canceledDate = day.plusDays(2)
        val changedDate = day.plusDays(3)
        val exceptions =
            listOf(
                AppointmentException(series.id, canceledDate, null),
                AppointmentException(
                    series.id,
                    changedDate,
                    series.details.copy(title = "Legacy one-off", date = changedDate),
                ),
            )
        val edit = appointmentEdit()
        assertTrue(appointments(canceledDate, listOf(edit), exceptions).isEmpty())
        assertEquals(
            "Legacy one-off",
            appointments(changedDate, listOf(edit), exceptions).single().details.title,
        )
    }

    @Test
    fun cannotApplySingleEditToAlreadyStartedSourceEvenIfReplacementIsFuture() {
        val anchor = appointment(day)
        val edit =
            appointmentEdit(
                OperationScope.SINGLE,
                anchor,
                AppointmentSeries("future", anchor.details.copy(date = day.plusDays(1))),
            )
        assertEquals("Original visit", appointments(day, listOf(edit)).single().details.title)
        assertEquals(1, appointments(day.plusDays(1), listOf(edit)).size)
    }

    @Test
    fun wholeSeriesDeletionDoesNotGuessConnectionsBetweenUnlinkedLegacySeries() {
        val other = series.copy(id = "unlinked")
        val deletion =
            ItemOperation(
                "all",
                ItemType.APPOINTMENT,
                series.id,
                OperationAction.DELETE,
                OperationScope.ALL,
                appointment().id,
                day,
                LocalTime.of(9, 0),
                series.id,
                "Visits",
                recipient = AppointmentRecipient.JOHANN,
            )
        assertEquals(
            listOf("unlinked"),
            ManagedAppointments.occurrences(
                    listOf(series, other),
                    emptyList(),
                    emptyList(),
                    listOf(deletion),
                    day,
                    day,
                )
                .map { it.sourceSeriesId },
        )
    }

    @Test
    fun slotOriginComesFromOriginalIdentityEvenAfterMovedTimeAndDate() {
        val revision = task.copy(id = "source_with_underscores", taskId = "task_with_underscores")
        val occurrence = Occurrence(revision, day, LocalTime.of(18, 0))
        val moved =
            occurrence.copy(
                date = day.plusDays(1),
                time = LocalTime.of(19, 0),
                stableId = occurrence.id,
            )
        assertEquals("source_with_underscores|64800", moved.slotOrigin())
    }

    @Test
    fun immediateEditingAcrossSpringDstKeepsOriginalIdentityAndResolvedInstant() {
        val date = LocalDate.of(2026, 3, 29)
        val routine = task.copy(times = listOf(LocalTime.of(2, 30)), start = date, effective = date)
        val original = Occurrence(routine, date, routine.times.single())
        val draft =
            ManagedTasks.mapSlots(
                routine,
                routine.copy(id = "dst", times = listOf(LocalTime.of(3, 45))),
            )
        val operation =
            ItemOperation(
                "dst",
                ItemType.TASK,
                task.taskId,
                OperationAction.EDIT,
                OperationScope.FOLLOWING,
                original.id,
                date,
                original.time,
                task.id,
                "DST",
                actualStartMillis = original.instant.toEpochMilli(),
                task = draft,
                createdMillis = Instant.parse("2026-03-29T00:30:00Z").toEpochMilli(),
            )
        val result =
            ManagedTasks.occurrences(listOf(routine), emptyList(), listOf(operation), date).single()
        assertEquals(original.id, result.id)
        assertEquals(Instant.parse("2026-03-29T01:45:00Z"), result.instant)
    }

    @Test
    fun acknowledgementsOnDifferentActualDaysCanBeRetractedWithoutChangingIdentity() {
        val first = original()
        val moved =
            first.copy(date = day.plusDays(1), time = LocalTime.of(10, 0), stableId = first.id)
        val a = complete(first)
        val b =
            complete(moved).copy(id = "other", authorId = "b", clientMillis = a.clientMillis + 1)
        val undoA = a.copy(id = "undo-a", kind = EventKind.RETRACT, targetId = a.id)
        val undoB = b.copy(id = "undo-b", kind = EventKind.RETRACT, targetId = b.id)
        assertEquals(first, tasks(events = listOf(a, b)).single { it.id == first.id })
        assertEquals(
            moved,
            tasks(day.plusDays(1), events = listOf(a, b, undoA)).single { it.id == moved.id },
        )
        assertTrue(Schedule.activeCompletions(listOf(a, b, undoA, undoB), first.id).isEmpty())
    }

    @Test
    fun followingMonthlyAppointmentEditRetainsClampedOriginalRhythm() {
        val source =
            AppointmentSeries(
                "monthly",
                AppointmentDetails(
                    "Monthly",
                    AppointmentRecipient.JOHANN,
                    LocalDate.of(2024, 1, 31),
                ),
                AppointmentRepeat.MONTHS,
            )
        val anchor = LocalDate.of(2024, 2, 29)
        val draft =
            source.copy(
                id = "monthly-draft",
                details = source.details.copy(title = "Changed monthly"),
                fromDate = anchor,
            )
        val operation =
            ItemOperation(
                "monthly-edit",
                ItemType.APPOINTMENT,
                source.id,
                OperationAction.EDIT,
                OperationScope.FOLLOWING,
                "appointment_monthly_$anchor",
                anchor,
                LocalTime.of(9, 0),
                source.id,
                "Monthly",
                actualDate = anchor,
                actualStartMillis = source.details.copy(date = anchor).startInstant.toEpochMilli(),
                appointment = draft,
                createdMillis =
                    LocalDate.of(2024, 2, 15).atStartOfDay(FAMILY_ZONE).toInstant().toEpochMilli(),
            )
        val result =
            ManagedAppointments.occurrences(
                    listOf(source),
                    emptyList(),
                    emptyList(),
                    listOf(operation),
                    LocalDate.of(2024, 3, 1),
                    LocalDate.of(2024, 3, 31),
                )
                .single()
        assertEquals(LocalDate.of(2024, 3, 31), result.details.date)
        assertEquals("appointment_monthly_2024-03-31", result.id)
    }

    @Test
    fun legacyFutureConfirmationDoesNotDuplicateItsSlotAfterFollowingEdit() {
        val old = task.copy(times = listOf(LocalTime.of(18, 0)))
        val future =
            old.copy(
                id = "legacy-next",
                effective = day.plusDays(1),
                times = listOf(LocalTime.of(19, 0)),
                createdMillis = 10,
            )
        val pin = Occurrence(old, day.plusDays(1), LocalTime.of(18, 0))
        val draft =
            ManagedTasks.mapSlots(
                future,
                future.copy(id = "new-edit", times = listOf(LocalTime.of(20, 0))),
            )
        val edit = editTask(draft = draft).copy(task = draft)
        val result =
            ManagedTasks.occurrences(
                listOf(old, future),
                listOf(complete(pin)),
                listOf(edit),
                pin.date,
            )
        assertEquals(listOf(pin), result)
    }

    @Test
    fun followingEditMapsToEachLegacyEffectiveVersionWithoutLosingIdentity() {
        val old = task.copy(times = listOf(LocalTime.of(18, 0)))
        val future =
            old.copy(
                id = "legacy-next",
                effective = day.plusDays(1),
                times = listOf(LocalTime.of(19, 0)),
                createdMillis = 10,
            )
        val draft =
            ManagedTasks.mapSlots(
                future,
                future.copy(id = "new-edit", times = listOf(LocalTime.of(20, 0))),
            )
        val edit = editTask(draft = draft).copy(task = draft)
        val today =
            ManagedTasks.occurrences(listOf(old, future), emptyList(), listOf(edit), day).single()
        val tomorrow =
            ManagedTasks.occurrences(
                    listOf(old, future),
                    emptyList(),
                    listOf(edit),
                    day.plusDays(1),
                )
                .single()
        assertEquals(Occurrence(old, day, old.times.single()).id, today.id)
        assertEquals(Occurrence(future, day.plusDays(1), future.times.single()).id, tomorrow.id)
        assertEquals(LocalTime.of(20, 0), today.time)
        assertEquals(LocalTime.of(20, 0), tomorrow.time)
    }

    @Test
    fun confirmedRemovedLegacySlotDoesNotReplaceAnUnchangedRemainingSlot() {
        val future =
            task.copy(
                id = "legacy-next",
                effective = day.plusDays(1),
                times = listOf(LocalTime.of(18, 0)),
                createdMillis = 10,
            )
        val pin = Occurrence(task, day.plusDays(1), LocalTime.of(8, 0))
        val result =
            ManagedTasks.occurrences(
                listOf(task, future),
                listOf(complete(pin)),
                emptyList(),
                pin.date,
            )
        assertEquals(2, result.size)
        assertTrue(result.contains(pin))
        assertTrue(result.any { it.id == Occurrence(future, pin.date, LocalTime.of(18, 0)).id })
    }
}
