package de.kinderalltag.data

import android.content.Context
import android.net.Uri
import de.kinderalltag.domain.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class CalendarInboxState(
    val imports: List<CalendarImport> = emptyList(),
    val index: Int = 0,
    val batchId: String = "",
    val error: String? = null,
) {
    val current: CalendarImport?
        get() = imports.getOrNull(index)

    val key: String?
        get() = current?.let { "$batchId:$index" }

    val remaining: Int
        get() = (imports.size - index).coerceAtLeast(0)
}

/** Incoming files stay private until the signed-in parent reviews and saves each appointment. */
class CalendarInbox(context: Context) {
    private val context = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()
    private val file = File(context.filesDir, "pending-calendar.ics")
    private val preferences = context.getSharedPreferences("calendar_inbox", Context.MODE_PRIVATE)
    private val mutable = MutableStateFlow(CalendarInboxState())
    private var initialized = false
    val state = mutable.asStateFlow()

    init {
        scope.launch { mutex.withLock { ensureLoaded() } }
    }

    private fun ensureLoaded() {
        if (!initialized) {
            load()
            initialized = true
        }
    }

    fun receive(uris: List<Uri>, text: String? = null) {
        scope.launch {
            mutex.withLock {
                ensureLoaded()
                try {
                    val incoming = mutableListOf<String>()
                    require(uris.size <= CalendarExchange.MAX_EVENTS) {
                        "Bitte weniger Kalenderdateien auswählen."
                    }
                    var bytes = 0
                    uris.forEach { uri ->
                        require(uri.scheme == "content") {
                            "Bitte eine Kalenderdatei über die Dateiauswahl teilen."
                        }
                        val output = ByteArrayOutputStream()
                        context.contentResolver.openInputStream(uri).use { input ->
                            requireNotNull(input) { "Kalenderdatei konnte nicht geöffnet werden." }
                            val buffer = ByteArray(8192)
                            while (true) {
                                val count = input.read(buffer)
                                if (count < 0) break
                                bytes += count
                                require(bytes <= CalendarExchange.MAX_BYTES) {
                                    "Kalenderdateien dürfen zusammen höchstens 1 MB groß sein."
                                }
                                output.write(buffer, 0, count)
                            }
                        }
                        incoming += output.toString(Charsets.UTF_8.name())
                    }
                    text?.let { incoming += it }
                    require(incoming.isNotEmpty()) { "Keine Kalenderdatei empfangen." }
                    val previous =
                        if (mutable.value.current != null && file.exists()) file.readText() else ""
                    val combined =
                        (listOf(previous).filter { it.isNotBlank() } + incoming).joinToString(
                            "\r\n"
                        )
                    val result = CalendarExchange.parse(combined)
                    require(result.appointments.isNotEmpty()) {
                        result.errors.joinToString("\n").ifBlank {
                            "Keine unterstützten Termine gefunden."
                        }
                    }
                    val index = if (previous.isEmpty()) 0 else mutable.value.index
                    val batch =
                        if (previous.isEmpty()) UUID.randomUUID().toString()
                        else mutable.value.batchId
                    val temporary = File(file.parentFile, "pending-calendar.tmp")
                    temporary.writeText(combined)
                    check(temporary.renameTo(file)) {
                        "Kalenderimport konnte nicht zwischengespeichert werden."
                    }
                    preferences.edit().putInt("index", index).putString("batch", batch).commit()
                    mutable.value =
                        CalendarInboxState(
                            result.appointments,
                            index,
                            batch,
                            result.errors.takeIf { it.isNotEmpty() }?.joinToString("\n"),
                        )
                } catch (e: Exception) {
                    mutable.value =
                        mutable.value.copy(
                            error = e.message ?: "Kalenderdatei konnte nicht gelesen werden."
                        )
                }
            }
        }
    }

    fun consume(key: String, onConsumed: () -> Unit = {}) {
        scope.launch {
            mutex.withLock {
                ensureLoaded()
                if (mutable.value.key != key) return@withLock
                val next = mutable.value.index + 1
                if (next >= mutable.value.imports.size) {
                    file.delete()
                    preferences.edit().clear().commit()
                    mutable.value = CalendarInboxState()
                } else {
                    preferences.edit().putInt("index", next).commit()
                    mutable.value = mutable.value.copy(index = next)
                }
            }
            withContext(Dispatchers.Main) { onConsumed() }
        }
    }

    fun dismissError() {
        mutable.update { it.copy(error = null) }
    }

    fun reportError(message: String) {
        mutable.update { it.copy(error = message) }
    }

    private fun load() {
        if (!file.exists()) return
        try {
            require(file.length() <= CalendarExchange.MAX_BYTES) { "Kalenderdatei ist zu groß." }
            val result = CalendarExchange.parse(file.readText())
            val index = preferences.getInt("index", 0)
            mutable.value =
                CalendarInboxState(
                    result.appointments,
                    index,
                    preferences.getString("batch", null) ?: UUID.randomUUID().toString(),
                    result.errors.takeIf { it.isNotEmpty() }?.joinToString("\n"),
                )
        } catch (e: Exception) {
            mutable.value =
                CalendarInboxState(
                    error = e.message ?: "Kalenderimport konnte nicht geladen werden."
                )
        }
    }
}
