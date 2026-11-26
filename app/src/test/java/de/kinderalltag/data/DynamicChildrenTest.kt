package de.kinderalltag.data

import de.kinderalltag.domain.*
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class DynamicChildrenTest {
    @Test
    fun legacyProfilesKeepTheirIdsColoursAndDefaultBears() {
        val johann = decodeChild("johann", mapOf("name" to "Johann"))
        val ferdinand =
            decodeChild("ferdinand", mapOf("name" to "Ferdinand", "bearColor" to "GREEN"))
        assertEquals("johann", johann.id)
        assertEquals(BearColor.ROSE, johann.bearColor)
        assertEquals(BearColor.GREEN, ferdinand.bearColor)
        assertEquals(ChildIconId.BEAR, johann.icon)
        assertFalse(johann.archived)
        assertEquals(0L, johann.version)
    }

    @Test
    fun dynamicProfilesRoundTripAppearanceAndArchiveState() {
        val child =
            decodeChild(
                "stable-id",
                mapOf(
                    "name" to "New name",
                    "bearColor" to "TEAL",
                    "icon" to "FOX",
                    "archived" to true,
                    "version" to 7,
                ),
            )
        assertEquals(ChildIconId.FOX, child.icon)
        assertEquals(BearColor.TEAL, child.bearColor)
        assertTrue(child.archived)
        assertEquals(7L, child.version)
    }

    @Test
    fun legacySelectionsStayFixedAndPreserveTheirWireSnapshots() {
        listOf("FERDINAND", "JOHANN", "BOTH_CHILDREN", "HOUSEHOLD").forEach {
            assertEquals(it, AppointmentRecipient.fromStored(it).stored())
        }
        val both = AppointmentRecipient.fromStored("BOTH_CHILDREN")
        assertEquals(setOf("ferdinand", "johann"), both.childIds)
        assertFalse(both.matches("new-child"))
        assertTrue(both.matches("johann"))
    }

    @Test
    fun tenExplicitChildrenRoundTripThroughAppointmentAndGiftCodecs() {
        val recipient = AppointmentRecipient.children((1..10).map { "child-$it" }.toSet())
        val date = LocalDate.of(2026, 11, 24)
        val details = AppointmentDetails("Party", recipient, date)
        assertEquals(details, decodeDetails(details.encodeDetails()))
        val gift = FamilyListItem("gift", FamilyListKind.GIFTS, "Book", recipient = recipient)
        assertEquals(gift, decodeListItem(gift.listMap()))
        assertEquals(recipient, AppointmentRecipient.valueOf(recipient.name))
    }

    @Test
    fun invalidSelectionsAreRejected() {
        listOf(emptySet(), (1..11).map { "child-$it" }.toSet(), setOf("haushalt"), setOf("bad/id"))
            .forEach { ids ->
                assertThrows(IllegalArgumentException::class.java) {
                    AppointmentRecipient.children(ids)
                }
            }
        assertThrows(IllegalArgumentException::class.java) {
            AppointmentRecipient.fromStored(
                mapOf("kind" to "CHILDREN", "childIds" to listOf("same", "same"))
            )
        }
    }

    @Test
    fun renamedChildrenKeepOccurrenceAndCompletionIdentity() {
        val date = LocalDate.of(2026, 11, 24)
        val revision =
            TaskRevision("revision", "task", "johann", "Vitamin", start = date, effective = date)
        val before = Schedule.occurrences(listOf(revision), date).single()
        val renamed = decodeChild("johann", mapOf("name" to "New name", "icon" to "TRACTOR"))
        assertEquals(renamed.id, before.revision.childId)
        assertEquals(before.id, Schedule.occurrences(listOf(revision), date).single().id)
    }

    @Test
    fun archivedChildrenSuspendRemindersWithoutDeletingSchedules() {
        val date = LocalDate.of(2026, 11, 24)
        val now = date.atStartOfDay(FAMILY_ZONE).toInstant()
        val task = TaskRevision("r", "t", "johann", "Vitamin", start = date, effective = date)
        val details = AppointmentDetails("Doctor", AppointmentRecipient.JOHANN, date)
        val series = AppointmentSeries("a", details)
        val child = Child("johann", "Johann", archived = true)
        assertTrue(
            FamilyReminders.plan(
                    "parent",
                    listOf(child),
                    listOf(task),
                    emptyList(),
                    listOf(series),
                    emptyList(),
                    now,
                )
                .isEmpty()
        )
        assertFalse(
            FamilyReminders.plan(
                    "parent",
                    listOf(child.copy(archived = false)),
                    listOf(task),
                    emptyList(),
                    listOf(series),
                    emptyList(),
                    now,
                )
                .isEmpty()
        )
        assertTrue(
            AppointmentRecipient.BOTH_CHILDREN.visibleFor(
                listOf(child, Child("ferdinand", "Ferdinand"))
            )
        )
        val occurrence = Schedule.occurrences(listOf(task), date).single()
        assertFalse(
            FamilyReminders.taskNeedsReminder(
                occurrence.id,
                date,
                occurrence.instant,
                "parent",
                listOf(task),
                emptyList(),
                emptyList(),
                occurrence.instant,
                listOf(child),
            )
        )
    }

    @Test
    fun multiChildTasksHaveIndependentIdsSchedulesAndCompletions() {
        val date = LocalDate.of(2026, 11, 24)
        val draft = TaskRevision("r", "t", "johann", "Vitamin", start = date, effective = date)
        var next = 0
        val tasks = tasksForChildren(draft, setOf("johann", "ferdinand")) { "copy-${next++}" }
        val occurrences = Schedule.occurrences(tasks, date)
        assertEquals(2, tasks.map { it.taskId }.toSet().size)
        assertEquals(2, occurrences.map { it.id }.toSet().size)
        assertEquals(setOf("johann", "ferdinand"), tasks.map { it.childId }.toSet())
        assertTrue(tasks.all { it.times == draft.times && it.frequency == draft.frequency })
        val event =
            CareEvent(
                "done",
                occurrences.first().id,
                date,
                EventKind.COMPLETE,
                "parent",
                "Parent",
                1,
            )
        assertEquals(
            1,
            occurrences.count { Schedule.activeCompletions(listOf(event), it.id).isNotEmpty() },
        )
        assertThrows(IllegalArgumentException::class.java) {
            tasksForChildren(draft, setOf("haushalt", "johann")) { "id" }
        }
    }

    @Test
    fun archiveFilteringKeepsExplicitHistoricalAccessAndActiveSharedAppointments() {
        val date = LocalDate.of(2026, 11, 24)
        val children =
            listOf(Child("johann", "Johann", archived = true), Child("ferdinand", "Ferdinand"))
        val task = TaskRevision("r", "t", "johann", "Vitamin", start = date, effective = date)
        assertTrue(
            taskCalendarPreviews(
                    listOf(task),
                    listOf(date),
                    null,
                    "parent",
                    false,
                    children = children,
                )
                .getValue(date)
                .isEmpty()
        )
        assertEquals(
            1,
            taskCalendarPreviews(
                    listOf(task),
                    listOf(date),
                    "johann",
                    "parent",
                    false,
                    children = children,
                )
                .getValue(date)
                .size,
        )
        assertTrue(
            upcomingTaskOccurrences(
                    listOf(task),
                    emptyList(),
                    emptyList(),
                    date,
                    10,
                    children = children,
                )
                .isEmpty()
        )
        val appointments =
            listOf(
                AppointmentSeries(
                    "a",
                    AppointmentDetails("Shared", AppointmentRecipient.BOTH_CHILDREN, date),
                ),
                AppointmentSeries(
                    "b",
                    AppointmentDetails("Archived", AppointmentRecipient.JOHANN, date),
                ),
            )
        assertEquals(
            1,
            appointmentCalendarPreviews(
                    appointments,
                    emptyList(),
                    listOf(date),
                    null,
                    "parent",
                    false,
                    children = children,
                )
                .getValue(date)
                .size,
        )
        assertEquals(
            2,
            appointmentCalendarPreviews(
                    appointments,
                    emptyList(),
                    listOf(date),
                    "johann",
                    "parent",
                    false,
                    children = children,
                )
                .getValue(date)
                .size,
        )
    }

    @Test
    fun queuedAppointmentAlarmsAreRevalidatedAfterArchiving() {
        val date = LocalDate.of(2026, 11, 24)
        val series =
            AppointmentSeries("a", AppointmentDetails("Doctor", AppointmentRecipient.JOHANN, date))
        val occurrence =
            ManagedAppointments.occurrences(
                    listOf(series),
                    emptyList(),
                    emptyList(),
                    emptyList(),
                    date,
                    date,
                )
                .single()
        val at = requireNotNull(occurrence.details.reminderInstant)
        fun needs(children: List<Child>) =
            FamilyReminders.appointmentNeedsReminder(
                occurrence.id,
                date,
                at,
                occurrence.reminderFingerprint(),
                "parent",
                children,
                listOf(series),
                emptyList(),
                emptyList(),
                emptyList(),
                at,
            )
        assertFalse(needs(listOf(Child("johann", "Johann", archived = true))))
        assertTrue(needs(listOf(Child("johann", "Johann"))))
    }

    @Test
    fun malformedArchiveFlagsCannotSilentlyReactivateProfiles() {
        assertThrows(IllegalArgumentException::class.java) {
            decodeChild("johann", mapOf("name" to "Johann", "archived" to "true"))
        }
    }
}
