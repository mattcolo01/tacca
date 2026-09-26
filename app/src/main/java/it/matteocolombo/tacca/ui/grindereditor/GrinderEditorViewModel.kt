package it.matteocolombo.tacca.ui.grindereditor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import it.matteocolombo.tacca.TaccaApplication
import it.matteocolombo.tacca.data.repo.GrinderRepository
import it.matteocolombo.tacca.domain.GrindSetting
import it.matteocolombo.tacca.domain.GrinderSpec
import it.matteocolombo.tacca.domain.SettingRules
import it.matteocolombo.tacca.domain.TextMatch
import it.matteocolombo.tacca.ui.navigation.idArg
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GrinderEditorUiState(
    val loaded: Boolean = false,
    val isNew: Boolean = true,
    val name: String = "",
    val multiTurn: Boolean = true,
    val stepsPerTurn: Int? = SettingRules.DEFAULT_STEPS_PER_TURN,
    /** Normalized names of the other grinders. */
    val existingNames: Set<String> = emptySet(),
    val outOfRangeCount: Int = 0,
    val calibrationCount: Int = 0,
    val saving: Boolean = false,
    val savedId: Long? = null,
    val deleted: Boolean = false,
) {
    val spec: GrinderSpec get() = GrinderSpec(multiTurn, stepsPerTurn)
    val canSave: Boolean get() = loaded && !saving && name.isNotBlank()
    val isDuplicateName: Boolean get() = name.isNotBlank() && TextMatch.normalize(name) in existingNames
    val preview: GrindSetting get() = SettingRules.clamp(GrindSetting(2, 12), spec)
}

class GrinderEditorViewModel(savedStateHandle: SavedStateHandle, private val grinders: GrinderRepository) : ViewModel() {
    private val grinderId: Long? = savedStateHandle.idArg("grinderId")
    private val state = MutableStateFlow(GrinderEditorUiState(isNew = grinderId == null))
    val uiState: StateFlow<GrinderEditorUiState> = state.asStateFlow()
    private var rangeJob: Job? = null

    init {
        viewModelScope.launch {
            val all = grinders.observeGrinders().first()
            val existing = grinderId?.let { id -> all.firstOrNull { it.id == id } }
            val count = grinderId?.let { grinders.calibrationCount(it) } ?: 0
            state.update { s ->
                s.copy(
                    loaded = true,
                    name = existing?.name ?: s.name,
                    multiTurn = existing?.spec?.multiTurn ?: s.multiTurn,
                    stepsPerTurn = if (existing != null) existing.spec.stepsPerTurn else s.stepsPerTurn,
                    existingNames = all.filter { it.id != grinderId }.map { TextMatch.normalize(it.name) }.toSet(),
                    calibrationCount = count,
                )
            }
        }
    }

    fun onNameChange(value: String) = state.update { it.copy(name = value) }

    fun onMultiTurnChange(value: Boolean) {
        state.update { it.copy(multiTurn = value) }
        refreshOutOfRange()
    }

    fun incrementSteps() {
        state.update { s ->
            s.copy(stepsPerTurn = s.stepsPerTurn?.let { (it + 1).coerceAtMost(SettingRules.MAX_STEPS_PER_TURN) } ?: SettingRules.DEFAULT_STEPS_PER_TURN)
        }
        refreshOutOfRange()
    }

    /** Stepping below the minimum means "unknown". */
    fun decrementSteps() {
        state.update { s -> s.copy(stepsPerTurn = s.stepsPerTurn?.let { if (it <= SettingRules.MIN_STEPS_PER_TURN) null else it - 1 }) }
        refreshOutOfRange()
    }

    fun setStepsUnknown() {
        state.update { it.copy(stepsPerTurn = null) }
        refreshOutOfRange()
    }

    private fun refreshOutOfRange() {
        val id = grinderId ?: return
        val spec = state.value.spec
        rangeJob?.cancel()
        rangeJob = viewModelScope.launch {
            val n = grinders.countOutOfRange(id, spec)
            state.update { it.copy(outOfRangeCount = n) }
        }
    }

    fun save() {
        val s = state.value
        if (!s.canSave) return
        state.update { it.copy(saving = true) }
        viewModelScope.launch {
            val id = if (grinderId == null) {
                grinders.add(s.name, s.spec)
            } else {
                grinders.update(grinderId, s.name, s.spec)
                grinderId
            }
            state.update { it.copy(saving = false, savedId = id) }
        }
    }

    fun delete() {
        val id = grinderId ?: return
        viewModelScope.launch {
            grinders.delete(id)
            state.update { it.copy(deleted = true) }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val c = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as TaccaApplication).container
                GrinderEditorViewModel(createSavedStateHandle(), c.grinders)
            }
        }
    }
}
