package it.matteocolombo.tacca.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import it.matteocolombo.tacca.TaccaApplication
import it.matteocolombo.tacca.data.repo.CalibrationRepository
import it.matteocolombo.tacca.domain.Calibration
import it.matteocolombo.tacca.ui.navigation.idArg
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class DetailUiState(
    val loading: Boolean = true,
    val calibration: Calibration? = null,
    val sameCoffee: List<Calibration> = emptyList(),
) {
    val notFound: Boolean get() = !loading && calibration == null
}

class DetailViewModel(savedStateHandle: SavedStateHandle, calibrations: CalibrationRepository) : ViewModel() {
    private val id: Long = savedStateHandle.idArg("calibrationId") ?: -1L

    val uiState: StateFlow<DetailUiState> =
        combine(calibrations.observe(id), calibrations.observeAll()) { current, all ->
            DetailUiState(
                loading = false,
                calibration = current,
                sameCoffee = if (current == null) emptyList() else all
                    .filter { it.coffee.id == current.coffee.id && it.id != current.id }
                    .sortedWith(compareBy({ it.method.sortOrder }, { it.grinder.name })),
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetailUiState())

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val c = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as TaccaApplication).container
                DetailViewModel(createSavedStateHandle(), c.calibrations)
            }
        }
    }
}
