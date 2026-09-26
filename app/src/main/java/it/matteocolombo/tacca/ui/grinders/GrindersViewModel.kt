package it.matteocolombo.tacca.ui.grinders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import it.matteocolombo.tacca.TaccaApplication
import it.matteocolombo.tacca.data.repo.CalibrationRepository
import it.matteocolombo.tacca.data.repo.GrinderRepository
import it.matteocolombo.tacca.domain.Grinder
import it.matteocolombo.tacca.domain.Tint
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class GrinderItem(val grinder: Grinder, val calibrationCount: Int, val tint: Tint)

data class GrindersUiState(val loading: Boolean = true, val items: List<GrinderItem> = emptyList())

class GrindersViewModel(grinders: GrinderRepository, calibrations: CalibrationRepository) : ViewModel() {
    val uiState: StateFlow<GrindersUiState> =
        combine(grinders.observeGrinders(), calibrations.observeAll()) { list, cals ->
            val counts = cals.groupingBy { it.grinder.id }.eachCount()
            GrindersUiState(
                loading = false,
                items = list.mapIndexed { i, g -> GrinderItem(g, counts[g.id] ?: 0, Tint.rotating(i)) },
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GrindersUiState())

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val c = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as TaccaApplication).container
                GrindersViewModel(c.grinders, c.calibrations)
            }
        }
    }
}
