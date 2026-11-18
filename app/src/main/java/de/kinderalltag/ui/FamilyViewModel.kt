package de.kinderalltag.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.kinderalltag.data.*
import de.kinderalltag.domain.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID

class FamilyViewModel(val repository: FamilyRepository) : ViewModel() {
    val state = repository.state
    val message = MutableStateFlow<String?>(null)
    val busy = MutableStateFlow(false)
    val appointmentError = MutableStateFlow<String?>(null)
    val taskError = MutableStateFlow<String?>(null)
    val today = MutableStateFlow(LocalDate.now(FAMILY_ZONE))
    val clock = MutableStateFlow(java.time.Instant.now())
    init {
        viewModelScope.launch {
            while (true) { clock.value = java.time.Instant.now(); today.value = LocalDate.now(FAMILY_ZONE); delay(30_000) }
        }
    }
    fun save(revision: TaskRevision, done: () -> Unit) = viewModelScope.launch {
        busy.value = true
        taskError.value = null
        try { repository.saveRevision(revision); done() }
        catch (e: kotlinx.coroutines.CancellationException) { throw e }
        catch (e: Exception) {
            taskError.value = if (e is IllegalArgumentException) e.message else "Speichern fehlgeschlagen. Bitte Verbindung und Familienzugang prüfen."
            message.value = taskError.value
        }
        finally { busy.value = false }
    }
    val operationError = MutableStateFlow<String?>(null)
    fun applyOperation(operation: ItemOperation, version: Long, done: () -> Unit) = viewModelScope.launch {
        busy.value = true
        operationError.value = null
        try { repository.applyOperation(operation, version); done() }
        catch (e: kotlinx.coroutines.CancellationException) { throw e }
        catch (e: Exception) {
            operationError.value = generateSequence<Throwable>(e) { it.cause }.firstOrNull {
                it is IllegalArgumentException || it is IllegalStateException
            }?.message ?: "Änderung konnte nicht gespeichert werden. Bitte Verbindung und Zugangsregeln prüfen."
            message.value = operationError.value
        } finally { busy.value = false }
    }
    internal fun saveTaskEdit(selection: TaskEditSelection, draft: TaskRevision, done: () -> Unit) {
        val base = if (selection.scope == OperationScope.FOLLOWING) state.value.taskDefinitions(java.time.Instant.now(), true)
            .firstOrNull { it.taskId == selection.occurrence.revision.taskId } ?: selection.occurrence.revision else selection.occurrence.revision
        val mapped = if (selection.scope == OperationScope.SINGLE) draft.copy(slotOrigins = listOf(selection.occurrence.slotOrigin()))
            else ManagedTasks.mapSlots(base, draft)
        applyOperation(taskOperation(selection.occurrence, selection.scope, OperationAction.EDIT, mapped), selection.version, done)
    }
    fun archive(revision: TaskRevision) {
        val now = java.time.Instant.now()
        val occurrence = state.value.futureTaskOccurrences(revision.taskId, now)
            .firstOrNull { Schedule.activeCompletions(state.value.events, it.id).isEmpty() }
        if (occurrence == null) { message.value = "Keine kommende, unbestätigte Ausführung gefunden."; return }
        val draft = ManagedTasks.mapSlots(revision, revision.copy(id = UUID.randomUUID().toString(), active = false))
        applyOperation(taskOperation(occurrence, OperationScope.FOLLOWING, OperationAction.EDIT, draft), state.value.version(ItemType.TASK, revision.taskId)) {}
    }
    fun saveChild(child: Child, done: () -> Unit) = viewModelScope.launch {
        busy.value = true
        try { repository.saveChild(child); done() }
        catch (_: Exception) { message.value = "Kinderprofil konnte nicht gespeichert werden. Verbindung und Zugangsregeln prüfen." }
        finally { busy.value = false }
    }
    fun complete(occurrence: Occurrence, completedById: String) {
        repository.acknowledge(occurrence, completedById) { message.value = it }
    }
    fun retrySync() { repository.retrySync() }
    fun undo(occurrence: Occurrence) {
        repository.retract(occurrence) { message.value = it }
    }
    fun saveAppointment(original: AppointmentOccurrence?, draft: AppointmentSeries?, following: Boolean,
        operationVersion: Long = 0, done: () -> Unit) = viewModelScope.launch {
        busy.value = true
        appointmentError.value = null
        try {
            if (original == null) repository.createAppointment(requireNotNull(draft))
            else {
                requireNotNull(draft)
                repository.applyOperation(appointmentOperation(state.value, original,
                    if (following) OperationScope.FOLLOWING else OperationScope.SINGLE, OperationAction.EDIT, draft), operationVersion)
            }
            done()
        }
        catch (e: kotlinx.coroutines.CancellationException) { throw e }
        catch (e: Exception) {
            appointmentError.value = if (e is IllegalArgumentException || e is IllegalStateException) e.message
                else "Termin konnte nicht gespeichert werden. Verbindung und Zugangsregeln prüfen."
        } finally { busy.value = false }
    }
}
