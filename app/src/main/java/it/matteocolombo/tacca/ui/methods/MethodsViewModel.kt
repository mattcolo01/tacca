package it.matteocolombo.tacca.ui.methods

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import it.matteocolombo.tacca.TaccaApplication
import it.matteocolombo.tacca.data.repo.CalibrationRepository
import it.matteocolombo.tacca.data.repo.HomeFilterStore
import it.matteocolombo.tacca.data.repo.MethodRepository
import it.matteocolombo.tacca.domain.BuiltInMethod
import it.matteocolombo.tacca.domain.Method
import it.matteocolombo.tacca.domain.MethodIcon
import it.matteocolombo.tacca.domain.TextMatch
import it.matteocolombo.tacca.domain.Tint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MethodItem(val method: Method, val calibrationCount: Int)

sealed interface MethodSheetState {
    data object New : MethodSheetState
    data class Edit(val method: Method, val calibrationCount: Int) : MethodSheetState
}

data class MethodsUiState(
    val loading: Boolean = true,
    val items: List<MethodItem> = emptyList(),
    val sheet: MethodSheetState? = null,
    /**
     * Normalized custom names only: this view model has no [android.content.res.Resources], so it
     * cannot localize built-in method names. MethodsEntry merges in the localized built-in display
     * names (e.g. "V60") before this set reaches the sheet, so the duplicate check there also
     * catches a new/renamed method colliding with a built-in one.
     */
    val existingNames: Set<String> = emptySet(),
    val saving: Boolean = false,
)

/** Null when [input] equals the localized name of [builtIn], so the method keeps following the app language. */
fun customNameFor(input: String, builtIn: BuiltInMethod?, localizedBuiltInName: String?): String? {
    val trimmed = input.trim()
    return if (builtIn != null && trimmed == localizedBuiltInName?.trim()) null else trimmed
}

class MethodsViewModel(
    private val methods: MethodRepository,
    calibrations: CalibrationRepository,
    private val homeFilter: HomeFilterStore,
) : ViewModel() {
    private val sheet = MutableStateFlow<MethodSheetState?>(null)
    private val saving = MutableStateFlow(false)

    val uiState: StateFlow<MethodsUiState> =
        combine(methods.observeMethods(), calibrations.observeAll(), sheet, saving) { list, cals, sh, sv ->
            val counts = cals.groupingBy { it.method.id }.eachCount()
            MethodsUiState(
                loading = false,
                items = list.map { MethodItem(it, counts[it.id] ?: 0) },
                sheet = sh,
                existingNames = list.mapNotNull { it.customName }.map(TextMatch::normalize).toSet(),
                saving = sv,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MethodsUiState())

    fun openNew() {
        sheet.value = MethodSheetState.New
    }

    fun openEdit(method: Method) {
        viewModelScope.launch { sheet.value = MethodSheetState.Edit(method, methods.calibrationCount(method.id)) }
    }

    fun dismissSheet() {
        sheet.value = null
    }

    fun save(customName: String?, icon: MethodIcon, tint: Tint) {
        if (saving.value) return
        val current = sheet.value
        if (current !is MethodSheetState.Edit && customName.isNullOrBlank()) return
        saving.value = true
        viewModelScope.launch {
            try {
                if (current is MethodSheetState.Edit) methods.update(current.method.id, customName, icon, tint)
                else methods.add(customName!!, icon, tint)
                sheet.value = null
            } finally {
                saving.value = false
            }
        }
    }

    fun delete() {
        val s = sheet.value as? MethodSheetState.Edit ?: return
        viewModelScope.launch {
            methods.delete(s.method.id)
            sheet.value = null
        }
    }

    fun showOnHome(methodId: Long) = homeFilter.showMethod(methodId)

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val c = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as TaccaApplication).container
                MethodsViewModel(c.methods, c.calibrations, c.homeFilter)
            }
        }
    }
}
