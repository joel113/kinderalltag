package de.kinderalltag.domain

import java.time.*
import org.junit.Assert.*
import org.junit.Test

class FamilyRemindersTest {
    private val day = LocalDate.of(2026, 11, 16)
    private val now = day.atStartOfDay(FAMILY_ZONE).toInstant()

    private fun task(id: String = "task", parent: String? = null) =
        TaskRevision(
            id,
            id,
            "johann",
            "Aufgabe",
            start = day,
            effective = day,
            responsibleParentId = parent,
        )

    private fun appointment(date: LocalDate = day, parent: String? = null) =
        AppointmentSeries(
            "appointment",
            AppointmentDetails(
                "Arzt",
                AppointmentRecipient.BOTH_CHILDREN,
                date,
                responsibleParentId = parent,
            ),
        )

    private fun plan(
        tasks: List<TaskRevision> = emptyList(),
        appointments: List<AppointmentSeries> = emptyList(),
        exceptions: List<AppointmentException> = emptyList(),
        pending: Set<String> = emptySet(),
        at: Instant = now,
    ) =
        FamilyReminders.plan(
            "a",
            emptyList(),
            tasks,
            emptyList(),
            appointments,
            exceptions,
            at,
            pending,
        )

    @Test
    fun remindersRespectResponsibilityIncludingLegacySharedTasks() {
        assertFalse(plan(listOf(task(parent = "b"))).isNotEmpty())
        assertEquals(7, plan(listOf(task(parent = "a"))).size)
        assertEquals(7, plan(listOf(task())).size)
        assertTrue(plan(appointments = listOf(appointment(parent = "b"))).isEmpty())
        assertEquals(1, plan(appointments = listOf(appointment())).size)
        assertFalse(isResponsible("b", "a"))
        assertTrue(isResponsible(null, "a"))
        assertFalse(isResponsible(null, null))
    }

    @Test
    fun reassignmentDoesNotFallBackToAnOlderTaskRevision() {
        val old = task(parent = "a").copy(effective = day.minusDays(1))
        val updated = old.copy(id = "updated", effective = day, responsibleParentId = "b")
        assertTrue(plan(listOf(old, updated)).isEmpty())
    }

    @Test
    fun unrelatedParentsCannotConsumeTheAlarmLimit() {
        val otherTasks = (0..350).map { task("other-$it", "b") }
        assertEquals(7, plan(otherTasks + task("mine", "a")).size)
    }

    @Test
    fun taskAndAppointmentRemindersAreMergedChronologicallyAndBounded() {
        val reminders = plan(listOf(task()), listOf(appointment()))
        assertEquals(8, reminders.size)
        assertTrue(reminders.zipWithNext().all { (a, b) -> a.at <= b.at })
        val limited =
            FamilyReminders.plan(
                "a",
                emptyList(),
                listOf(task()),
                emptyList(),
                listOf(appointment()),
                emptyList(),
                now,
                limit = 1,
            )
        assertEquals(1, limited.size)
        assertEquals("task", limited.single().kind)
    }

    @Test
    fun advanceRemindersCanFallOnThePreviousDate() {
        val appointment =
            appointment(day.plusDays(1))
                .copy(
                    details =
                        appointment()
                            .details
                            .copy(
                                date = day.plusDays(1),
                                startTime = LocalTime.of(0, 10),
                                endTime = LocalTime.of(1, 0),
                                reminderMinutes = 30,
                            )
                )
        val reminder = plan(appointments = listOf(appointment)).single()
        assertEquals(day.plusDays(1).toString(), reminder.date)
        assertEquals(day.atTime(23, 40).atZone(FAMILY_ZONE).toInstant(), reminder.at)
        assertEquals(appointment.details.endInstant, reminder.expiresAt)
    }

    @Test
    fun allDayRemindersUseNineOnThePreviousDay() {
        val appointment =
            appointment(day.plusDays(1))
                .copy(
                    details =
                        appointment()
                            .details
                            .copy(date = day.plusDays(1), allDay = true, reminderMinutes = 1440)
                )
        assertEquals(
            day.atTime(9, 0).atZone(FAMILY_ZONE).toInstant(),
            plan(appointments = listOf(appointment)).single().at,
        )
    }

    @Test
    fun canceledMovedAndDisabledAppointmentsReconcilePendingAlarms() {
        val appointment = appointment()
        val occurrence =
            AppointmentSchedule.occurrences(listOf(appointment), emptyList(), day, day).single()
        assertTrue(
            plan(
                    appointments = listOf(appointment),
                    exceptions = listOf(AppointmentException(appointment.id, day, null)),
                    pending = setOf(occurrence.id),
                )
                .isEmpty()
        )
        assertTrue(
            plan(
                    appointments =
                        listOf(
                            appointment.copy(
                                details = appointment.details.copy(reminderMinutes = null)
                            )
                        )
                )
                .isEmpty()
        )
        val late = appointment.details.reminderInstant!!.plusSeconds(60)
        assertTrue(plan(appointments = listOf(appointment), at = late).isEmpty())
        assertEquals(
            1,
            plan(appointments = listOf(appointment), at = late, pending = setOf(occurrence.id)).size,
        )
        assertTrue(
            plan(
                    appointments = listOf(appointment),
                    at = appointment.details.endInstant,
                    pending = setOf(occurrence.id),
                )
                .isEmpty()
        )
    }
}
