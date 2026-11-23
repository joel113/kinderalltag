package de.kinderalltag.data

import android.content.Context
import android.net.Uri
import de.kinderalltag.domain.*
import de.kinderalltag.text.*
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
                    requireLocalized(uris.size <= CalendarExchange.MAX_EVENTS) {
                        message(TextKey.PLEASE_SELECT_FEWER_CALENDAR_FILES)
                    }
                    var bytes = 0
                    uris.forEach { uri ->
                        requireLocalized(uri.scheme == "content") {
                            message(TextKey.PLEASE_SHARE_A_CALENDAR_FILE_USING_THE_FILE_PICKER)
                        }
                        val output = ByteArrayOutputStream()
                        context.contentResolver.openInputStream(uri).use { input ->
                            requireNotNullLocalized(input) {
                                message(TextKey.THE_CALENDAR_FILE_COULD_NOT_BE_OPENED)
                            }
                            val buffer = ByteArray(8192)
                            while (true) {
                                val count = input.read(buffer)
                                if (count < 0) break
                                bytes += count
                                requireLocalized(bytes <= CalendarExchange.MAX_BYTES) {
                                    message(TextKey.CALENDAR_FILES_MAY_NOT_EXCEED_1_MB_IN_TOTAL)
                                }
                                output.write(buffer, 0, count)
                            }
                        }
                        incoming += output.toString(Charsets.UTF_8.name())
                    }
                    text?.let { incoming += it }
                    requireLocalized(incoming.isNotEmpty()) {
                        message(TextKey.NO_CALENDAR_FILE_RECEIVED)
                    }
                    val previous =
                        if (mutable.value.current != null && file.exists()) file.readText() else ""
                    val combined =
                        (listOf(previous).filter { it.isNotBlank() } + incoming).joinToString(
                            "\r\n"
                        )
                    val result = CalendarExchange.parse(combined)
                    require(result.appointments.isNotEmpty()) {
                        result.errors.joinToString("\n").ifBlank {
                            text(TextKey.NO_SUPPORTED_APPOINTMENTS_FOUND)
                        }
                    }
                    val index = if (previous.isEmpty()) 0 else mutable.value.index
                    val batch =
                        if (previous.isEmpty()) UUID.randomUUID().toString()
                        else mutable.value.batchId
                    val temporary = File(file.parentFile, "pending-calendar.tmp")
                    temporary.writeText(combined)
                    checkLocalized(temporary.renameTo(file)) {
                        message(TextKey.THE_CALENDAR_IMPORT_COULD_NOT_BE_CACHED)
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
                            error = e.message ?: text(TextKey.THE_CALENDAR_FILE_COULD_NOT_BE_READ)
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
            requireLocalized(file.length() <= CalendarExchange.MAX_BYTES) {
                message(TextKey.THE_CALENDAR_FILE_IS_TOO_LARGE)
            }
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
                    error = e.message ?: text(TextKey.THE_CALENDAR_IMPORT_COULD_NOT_BE_LOADED)
                )
        }
    }
}
