package de.kinderalltag.text

import java.util.Locale

enum class QuantityKey(val resourceName: String, val one: String, val other: String) {
    TASK_PROGRESS(
        "task_progress",
        "%1\$d von %2\$d Aufgabe erledigt",
        "%1\$d von %2\$d Aufgaben erledigt",
    ),
    APPOINTMENT_PROGRESS(
        "appointment_progress",
        "%1\$d von %2\$d Termin absolviert",
        "%1\$d von %2\$d Termine absolviert",
    ),
    IMPORT_QUEUED(
        "import_queued",
        "%1\$d Einkauf zur Synchronisierung vorgemerkt.",
        "%1\$d Einkäufe zur Synchronisierung vorgemerkt.",
    ),
    DAY_EVENTS("day_events", "Tagesübersicht: %1\$d Ereignis", "Tagesübersicht: %1\$d Ereignisse"),
}

object QuantityCatalog {
    @PublishedApi
    internal val scopedRenderer = ThreadLocal<(QuantityKey, Int, Array<out Any?>) -> String>()
    @Volatile var renderer: ((QuantityKey, Int, Array<out Any?>) -> String)? = null

    inline fun <T> withRenderer(
        noinline renderer: (QuantityKey, Int, Array<out Any?>) -> String,
        block: () -> T,
    ): T {
        val previous = scopedRenderer.get()
        scopedRenderer.set(renderer)
        return try {
            block()
        } finally {
            scopedRenderer.set(previous)
        }
    }
}

fun quantity(key: QuantityKey, count: Int, vararg arguments: Any?): String =
    (QuantityCatalog.scopedRenderer.get() ?: QuantityCatalog.renderer)?.invoke(
        key,
        count,
        arguments,
    ) ?: String.format(Locale.GERMAN, if (count == 1) key.one else key.other, *arguments)
