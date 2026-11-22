package de.kinderalltag.data

import de.kinderalltag.domain.*
import java.time.*
import org.junit.Assert.*
import org.junit.Test

class TaskIntervalCodecTest {
    private val day = LocalDate.of(2026, 11, 22)
    private val task =
        TaskRevision("source", "task", "johann", "Routine", start = day, effective = day)

    @Test
    fun recurrenceAndIntervalSurviveImmutableOccurrenceSnapshotRoundTrips() {
        for (frequency in listOf(Frequency.DAILY, Frequency.WEEKS, Frequency.MONTHS)) {
            val revision = task.copy(frequency = frequency, interval = 3)
            val occurrence = Occurrence(revision, day, revision.times.single())
            val restored = occurrenceSnapshot(occurrence.snapshotMap())
            assertEquals(revision, restored.revision)
            assertEquals(occurrence.id, restored.id)
            assertEquals(occurrence.instant, restored.instant)
        }
    }

    @Test
    fun historicalSnapshotsWithoutAnIntervalDefaultToOne() {
        val old = task.snapshotMap() - "interval"
        assertEquals(task, taskSnapshot(old))
        assertEquals(1, taskSnapshot(old).interval)
    }
}
