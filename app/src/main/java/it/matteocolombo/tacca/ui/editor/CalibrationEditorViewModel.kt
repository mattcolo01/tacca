package it.matteocolombo.tacca.ui.editor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import it.matteocolombo.tacca.TaccaApplication
import it.matteocolombo.tacca.data.repo.CalibrationRepository
import it.matteocolombo.tacca.data.repo.CoffeeRepository
import it.matteocolombo.tacca.data.repo.GrinderRepository
import it.matteocolombo.tacca.data.repo.MethodRepository
import it.matteocolombo.tacca.data.repo.SaveCalibration
import it.matteocolombo.tacca.domain.Calibration
import it.matteocolombo.tacca.domain.Coffee
import it.matteocolombo.tacca.domain.GrindSetting
import it.matteocolombo.tacca.domain.Grinder
import it.matteocolombo.tacca.domain.Method
import it.matteocolombo.tacca.domain.SettingRules
import it.matteocolombo.tacca.domain.TextMatch
import it.matteocolombo.tacca.ui.navigation.idArg
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EditorUiState(
    val loaded: Boolean = false,
    val isEdit: Boolean = false,
    /** Most recently used first. */
    val coffees: List<Coffee> = emptyList(),
    val showAllCoffees: Boolean = false,
    val methods: List<Method> = emptyList(),
    val grinders: List<Grinder> = emptyList(),
    val coffeeId: Long? = null,
    val methodId: Long? = null,
    val grinderId: Long? = null,
    val setting: GrindSetting = GrindSetting.ZERO,
    val notes: String = "",
    /** Setting of another calibration that saving would overwrite. */
    val replaces: GrindSetting? = null,
    val coffeeNames: Set<String> = emptySet(),
    val saving: Boolean = false,
    val savedId: Long? = null,
    val deleted: Boolean = false,
) {
    val grinder: Grinder? get() = grinders.firstOrNull { it.id == grinderId }
    val canSave: Boolean
        get() = loaded && !saving && savedId == null &&
            coffees.any { it.id == coffeeId } && methods.any { it.id == methodId } && grinder != null
    val visibleCoffees: List<Coffee>
        get() {
            if (showAllCoffees || coffees.size <= RECENT_COFFEES) return coffees
            val top = coffees.take(RECENT_COFFEES)
            val selected = coffees.firstOrNull { it.id == coffeeId }
            return if (selected != null && selected !in top) top + selected else top
        }
    val hiddenCoffeeCount: Int get() = coffees.size - visibleCoffees.size
    val maxSteps: Int? get() = grinder?.let { SettingRules.maxSteps(it.spec) }
    val turnsEnabled: Boolean get() = grinder?.spec?.multiTurn ?: true
    val canIncrementSteps: Boolean get() = maxSteps?.let { setting.steps < it } ?: true

    companion object {
        const val RECENT_COFFEES = 3
    }
}

class CalibrationEditorViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val coffees: CoffeeRepository,
    methods: MethodRepository,
    grinders: GrinderRepository,
    private val calibrations: CalibrationRepository,
) : ViewModel() {
    private val editingId = savedStateHandle.idArg("calibrationId")
    private val presetCoffee = savedStateHandle.idArg("coffeeId")
    private val presetMethod = savedStateHandle.idArg("methodId")
    private val state = MutableStateFlow(EditorUiState(isEdit = editingId != null))
    val uiState: StateFlow<EditorUiState> = state.asStateFlow()
    private var pendingGrinderId: Long? = null
    private var replacesJob: Job? = null

    init {
        viewModelScope.launch {
            val existing = editingId?.let { calibrations.observe(it).first() }
            combine(coffees.observeCoffees(), methods.observeMethods(), grinders.observeGrinders(), calibrations.observeAll()) { cs, ms, gs, cals ->
                Lists(cs, ms, gs, cals)
            }.collect { lists ->
                val firstLoad = !state.value.loaded
                val pending = pendingGrinderId
                val applyPending = pending != null && lists.grinders.any { it.id == pending }
                if (applyPending) pendingGrinderId = null
                state.update { s -> merge(s, lists, if (firstLoad) existing else null, firstLoad, if (applyPending) pending else null) }
                if (firstLoad || applyPending) refreshReplaces()
            }
        }
    }

    private data class Lists(val coffees: List<Coffee>, val methods: List<Method>, val grinders: List<Grinder>, val calibrations: List<Calibration>)

    private fun merge(s: EditorUiState, l: Lists, existing: Calibration?, firstLoad: Boolean, applyGrinderId: Long?): EditorUiState {
        val lastUse = l.calibrations.groupBy { it.coffee.id }.mapValues { (_, v) -> v.maxOf { it.updatedAt } }
        val ordered = l.coffees.sortedByDescending { maxOf(lastUse[it.id] ?: 0L, it.createdAt) }
        var next = s.copy(
            loaded = true,
            coffees = ordered,
            methods = l.methods,
            grinders = l.grinders,
            coffeeNames = l.coffees.map { TextMatch.normalize(it.name) }.toSet(),
        )
        if (firstLoad) {
            next = if (existing != null) {
                next.copy(
                    coffeeId = existing.coffee.id, methodId = existing.method.id, grinderId = existing.grinder.id,
                    setting = existing.setting, notes = existing.notes,
                )
            } else {
                next.copy(
                    coffeeId = presetCoffee ?: l.coffees.singleOrNull()?.id,
                    methodId = presetMethod,
                    grinderId = l.grinders.singleOrNull()?.id,
                )
            }
        }
        if (applyGrinderId != null) next = selectGrinderIn(next, applyGrinderId)
        return next
    }

    private fun selectGrinderIn(s: EditorUiState, id: Long): EditorUiState {
        val g = s.grinders.firstOrNull { it.id == id }
        return s.copy(grinderId = id, setting = g?.let { SettingRules.clamp(s.setting, it.spec) } ?: s.setting)
    }

    private fun refreshReplaces() {
        val s = state.value
        val c = s.coffeeId
        val m = s.methodId
        val g = s.grinderId
        replacesJob?.cancel()
        if (c == null || m == null || g == null) {
            state.update { it.copy(replaces = null) }
            return
        }
        replacesJob = viewModelScope.launch {
            val existing = calibrations.findExisting(c, m, g)
            state.update { s2 ->
                if (s2.coffeeId == c && s2.methodId == m && s2.grinderId == g) {
                    s2.copy(replaces = existing?.takeIf { e -> e.id != editingId }?.setting)
                } else {
                    s2
                }
            }
        }
    }

    fun selectCoffee(id: Long) {
        state.update { it.copy(coffeeId = id) }
        refreshReplaces()
    }

    fun selectMethod(id: Long) {
        state.update { it.copy(methodId = id) }
        refreshReplaces()
    }

    fun selectGrinder(id: Long) {
        state.update { selectGrinderIn(it, id) }
        refreshReplaces()
    }

    /**
     * Called after a new grinder was created from the grinder editor. Selects it if it has
     * already reached the observed grinder list; otherwise remembers it and applies the
     * selection once a later list emission includes it (see [merge]).
     */
    fun onNewGrinderCreated(id: Long) {
        val inList = state.value.grinders.any { it.id == id }
        if (inList) {
            state.update { selectGrinderIn(it, id) }
        } else {
            pendingGrinderId = id
        }
        refreshReplaces()
    }

    fun turnsUp() = updateSetting { it.copy(turns = it.turns + 1) }
    fun turnsDown() = updateSetting { it.copy(turns = it.turns - 1) }
    fun stepsUp() = updateSetting { it.copy(steps = it.steps + 1) }
    fun stepsDown() = updateSetting { it.copy(steps = it.steps - 1) }

    /** Steppers clamp the whole setting to the selected grinder; loading never clamps. */
    private fun updateSetting(transform: (GrindSetting) -> GrindSetting) = state.update { s ->
        val next = transform(s.setting)
        val clamped = s.grinder?.let { SettingRules.clamp(next, it.spec) }
            ?: GrindSetting(next.turns.coerceAtLeast(0), next.steps.coerceAtLeast(0))
        s.copy(setting = clamped)
    }

    fun onNotesChange(value: String) = state.update { it.copy(notes = value) }

    fun showAllCoffees() = state.update { it.copy(showAllCoffees = true) }

    fun addCoffee(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val id = coffees.add(name)
            state.update { it.copy(coffeeId = id) }
            refreshReplaces()
        }
    }

    fun save() {
        val s = state.value
        if (!s.canSave) return
        state.update { it.copy(saving = true) }
        viewModelScope.launch {
            try {
                val id = calibrations.save(SaveCalibration(editingId, s.coffeeId!!, s.methodId!!, s.grinderId!!, s.setting, s.notes))
                state.update { it.copy(savedId = id) }
            } finally {
                state.update { it.copy(saving = false) }
            }
        }
    }

    fun delete() {
        val id = editingId ?: return
        viewModelScope.launch {
            calibrations.delete(id)
            state.update { it.copy(deleted = true) }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val c = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as TaccaApplication).container
                CalibrationEditorViewModel(createSavedStateHandle(), c.coffees, c.methods, c.grinders, c.calibrations)
            }
        }
    }
}
