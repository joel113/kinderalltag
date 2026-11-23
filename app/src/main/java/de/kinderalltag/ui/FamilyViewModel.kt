package de.kinderalltag.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.kinderalltag.data.*
import de.kinderalltag.domain.*
import de.kinderalltag.text.*
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class FamilyViewModel(val repository: FamilyRepository) : ViewModel() {
    internal val appointmentUi = AppointmentUiState()
    internal val taskEditorRevision = androidx.compose.runtime.mutableStateOf<TaskRevision?>(null)
    internal val taskEditorOpen = androidx.compose.runtime.mutableStateOf(false)
    internal val taskEditorSelection =
        androidx.compose.runtime.mutableStateOf<TaskEditSelection?>(null)
    internal val listUi = FamilyListUiState()

    val state = repository.state
    val message = MutableStateFlow<String?>(null)

    fun refreshPresentation() {
        repository.refreshPresentation()
        clearTransientMessages()
    }

    fun clearTransientMessages() {
        message.value = null
        appointmentError.value = null
        taskError.value = null
        careError.value = null
        operationError.value = null
        listError.value = null
    }

    val busy = MutableStateFlow(false)
    val appointmentError = MutableStateFlow<String?>(null)
    val taskError = MutableStateFlow<String?>(null)
    val careError = MutableStateFlow<String?>(null)
    val today = MutableStateFlow(LocalDate.now(FAMILY_ZONE))
    val clock = MutableStateFlow(java.time.Instant.now())

    init {
        viewModelScope.launch {
            state
                .map { it.uid }
                .distinctUntilChanged()
                .drop(1)
                .collect {
                    careError.value = null
                    appointmentUi.editor = null
                    appointmentUi.selected = null
                    appointmentUi.deletion = null
                    taskEditorRevision.value = null
                    taskEditorSelection.value = null
                    taskEditorOpen.value = false
                    listUi.editor = null
                    listUi.selected = null
                    listUi.voiceImportOpen = false
                }
        }
        viewModelScope.launch {
            while (true) {
                clock.value = java.time.Instant.now()
                today.value = LocalDate.now(FAMILY_ZONE)
                delay(30_000)
            }
        }
    }

    fun save(revision: TaskRevision, done: () -> Unit) =
        viewModelScope.launch {
            busy.value = true
            taskError.value = null
            try {
                repository.saveRevision(revision)
                done()
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                taskError.value =
                    if (e is IllegalArgumentException) e.message
                    else text(TextKey.SAVING_FAILED_PLEASE_CHECK_YOUR_CONNECTION_AND_FAMILY_ACCESS)
                message.value = taskError.value
            } finally {
                busy.value = false
            }
        }

    val operationError = MutableStateFlow<String?>(null)

    fun applyOperation(operation: ItemOperation, version: Long, done: () -> Unit) =
        viewModelScope.launch {
            busy.value = true
            operationError.value = null
            try {
                repository.applyOperation(operation, version)
                done()
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                operationError.value =
                    generateSequence<Throwable>(e) { it.cause }
                        .firstOrNull {
                            it is IllegalArgumentException || it is IllegalStateException
                        }
                        ?.message
                        ?: text(
                            TextKey
                                .THE_CHANGE_COULD_NOT_BE_SAVED_PLEASE_CHECK_YOUR_CONNECTION_AND_ACCESS_RU
                        )
                message.value = operationError.value
            } finally {
                busy.value = false
            }
        }

    internal fun saveTaskEdit(selection: TaskEditSelection, draft: TaskRevision, done: () -> Unit) {
        val base =
            if (selection.scope == OperationScope.FOLLOWING)
                state.value.taskDefinitions(java.time.Instant.now(), true).firstOrNull {
                    it.taskId == selection.occurrence.revision.taskId
                } ?: selection.occurrence.revision
            else selection.occurrence.revision
        val mapped =
            if (selection.scope == OperationScope.SINGLE)
                draft.copy(slotOrigins = listOf(selection.occurrence.slotOrigin()))
            else ManagedTasks.mapSlots(base, draft)
        applyOperation(
            taskOperation(selection.occurrence, selection.scope, OperationAction.EDIT, mapped),
            selection.version,
            done,
        )
    }

    fun archive(revision: TaskRevision) {
        val now = java.time.Instant.now()
        val occurrence =
            state.value.futureTaskOccurrences(revision.taskId, now).firstOrNull {
                Schedule.activeCompletions(state.value.events, it.id).isEmpty()
            }
        if (occurrence == null) {
            message.value = text(TextKey.NO_UPCOMING_UNCONFIRMED_OCCURRENCE_FOUND)
            return
        }
        val draft =
            ManagedTasks.mapSlots(
                revision,
                revision.copy(id = UUID.randomUUID().toString(), active = false),
            )
        applyOperation(
            taskOperation(occurrence, OperationScope.FOLLOWING, OperationAction.EDIT, draft),
            state.value.version(ItemType.TASK, revision.taskId),
        ) {}
    }

    fun saveChild(child: Child, done: () -> Unit) =
        viewModelScope.launch {
            busy.value = true
            try {
                repository.saveChild(child)
                done()
            } catch (_: Exception) {
                message.value =
                    text(
                        TextKey
                            .THE_CHILD_PROFILE_COULD_NOT_BE_SAVED_CHECK_YOUR_CONNECTION_AND_ACCESS_RU
                    )
            } finally {
                busy.value = false
            }
        }

    fun complete(occurrence: Occurrence, completedById: String) {
        careError.value = null
        repository.acknowledge(occurrence, completedById) {
            careError.value = it
            message.value = it
        }
    }

    fun retrySync() {
        repository.retrySync()
    }

    fun undo(occurrence: Occurrence) {
        careError.value = null
        repository.retract(occurrence) {
            careError.value = it
            message.value = it
        }
    }

    fun saveAppointment(
        original: AppointmentOccurrence?,
        draft: AppointmentSeries?,
        following: Boolean,
        operationVersion: Long = 0,
        done: () -> Unit,
    ) =
        viewModelScope.launch {
            busy.value = true
            appointmentError.value = null
            try {
                if (original == null) repository.createAppointment(requireNotNull(draft))
                else {
                    requireNotNull(draft)
                    repository.applyOperation(
                        appointmentOperation(
                            state.value,
                            original,
                            if (following) OperationScope.FOLLOWING else OperationScope.SINGLE,
                            OperationAction.EDIT,
                            draft,
                        ),
                        operationVersion,
                    )
                }
                done()
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                appointmentError.value =
                    if (e is IllegalArgumentException || e is IllegalStateException) e.message
                    else
                        text(
                            TextKey
                                .THE_APPOINTMENT_COULD_NOT_BE_SAVED_CHECK_YOUR_CONNECTION_AND_ACCESS_RULE
                        )
            } finally {
                busy.value = false
            }
        }

    val listError = MutableStateFlow<String?>(null)

    fun saveListItem(item: FamilyListItem, creating: Boolean, done: () -> Unit) {
        if (creating) {
            listError.value = null
            try {
                repository.lists.create(item) {
                    listError.value = it
                    message.value = it
                }
                done()
            } catch (e: Exception) {
                listError.value = e.message
                message.value = e.message
            }
        } else
            listChange {
                repository.lists.change(item, ListAction.CHANGED)
                done()
            }
    }

    fun importShopping(titles: List<String>, labelIds: Set<String>, done: () -> Unit) {
        listError.value = null
        try {
            val currentOpen =
                listItems(
                    state.value.listItems,
                    state.value.listActivity,
                    FamilyListKind.SHOPPING,
                    false,
                )
            val fresh =
                shoppingImportTitles(titles.joinToString("\n"), currentOpen.map { it.title })
            validateShoppingImport(fresh)
            val drafts =
                fresh.map { title ->
                    FamilyListItem(
                        UUID.randomUUID().toString(),
                        FamilyListKind.SHOPPING,
                        title,
                        labelIds = labelIds,
                    )
                }
            repository.lists.createMany(drafts) {
                listError.value = it
                message.value = it
            }
            done()
            message.value = quantity(QuantityKey.IMPORT_QUEUED, drafts.size, drafts.size)
        } catch (e: Exception) {
            listError.value = e.message ?: text(TextKey.SHOPPING_ITEMS_COULD_NOT_BE_ADDED)
        }
    }

    fun buy(item: FamilyListItem, bought: Boolean) {
        listError.value = null
        try {
            repository.lists.setBought(item, bought) {
                listError.value = it
                message.value = it
            }
        } catch (e: Exception) {
            listError.value = e.message ?: text(TextKey.THE_PURCHASE_STATUS_COULD_NOT_BE_CHANGED)
            message.value = listError.value
        }
    }

    fun reorderList(
        kind: FamilyListKind,
        ids: List<String>,
        version: Long,
        done: (Boolean) -> Unit,
    ) =
        viewModelScope.launch {
            busy.value = true
            listError.value = null
            try {
                repository.lists.reorder(kind, ids, version)
                done(true)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                listError.value =
                    generateSequence<Throwable>(e) { it.cause }
                        .firstOrNull {
                            it is IllegalArgumentException || it is IllegalStateException
                        }
                        ?.message
                        ?: text(
                            TextKey
                                .THE_ORDER_COULD_NOT_BE_SAVED_PLEASE_CHECK_YOUR_CONNECTION_AND_RULES
                        )
                message.value = listError.value
                done(false)
            } finally {
                busy.value = false
            }
        }

    fun changeListItem(item: FamilyListItem, action: ListAction, done: () -> Unit = {}) =
        listChange {
            repository.lists.change(item, action)
            done()
        }

    fun saveListLabel(label: FamilyLabel, creating: Boolean, done: () -> Unit) = listChange {
        repository.lists.saveLabel(label, creating)
        done()
    }

    fun addListStarters(group: LabelGroup) = listChange { repository.lists.addStarters(group) }

    private fun listChange(block: suspend () -> Unit) =
        viewModelScope.launch {
            busy.value = true
            listError.value = null
            try {
                block()
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                listError.value =
                    generateSequence<Throwable>(e) { it.cause }
                        .firstOrNull {
                            it is IllegalArgumentException || it is IllegalStateException
                        }
                        ?.message
                        ?: text(TextKey.THE_CHANGE_FAILED_PLEASE_CHECK_YOUR_CONNECTION_AND_RULES)
                message.value = listError.value
            } finally {
                busy.value = false
            }
        }
}
