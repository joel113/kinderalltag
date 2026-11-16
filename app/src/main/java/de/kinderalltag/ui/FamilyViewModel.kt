package de.kinderalltag.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.kinderalltag.data.FamilyRepository
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
    val today = MutableStateFlow(LocalDate.now(FAMILY_ZONE))
    init {
        viewModelScope.launch {
            while (true) { today.value = LocalDate.now(FAMILY_ZONE); delay(30_000) }
        }
    }
    fun save(revision: TaskRevision, done: () -> Unit) = viewModelScope.launch {
        busy.value = true
        try { repository.saveRevision(revision); done() }
        catch (e: Exception) { message.value = if (e is IllegalArgumentException) e.message else "Speichern fehlgeschlagen. Bitte Verbindung und Familienzugang prüfen." }
        finally { busy.value = false }
    }
    fun archive(revision: TaskRevision) {
        save(revision.copy(id = UUID.randomUUID().toString(), effective = LocalDate.now(FAMILY_ZONE).plusDays(1), active = false)) {}
    }
    fun saveChild(child: Child, done: () -> Unit) = viewModelScope.launch {
        busy.value = true
        try { repository.saveChild(child); done() }
        catch (_: Exception) { message.value = "Kinderprofil konnte nicht gespeichert werden. Verbindung und Zugangsregeln prüfen." }
        finally { busy.value = false }
    }
    fun complete(occurrence: Occurrence) {
        repository.acknowledge(occurrence) { message.value = it }
    }
    fun undo(occurrence: Occurrence) {
        repository.retract(occurrence) { message.value = it }
    }
}
