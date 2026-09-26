package it.matteocolombo.tacca.ui.coffees

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import it.matteocolombo.tacca.TaccaApplication
import it.matteocolombo.tacca.data.repo.CalibrationRepository
import it.matteocolombo.tacca.data.repo.CoffeeRepository
import it.matteocolombo.tacca.data.repo.HomeFilterStore
import it.matteocolombo.tacca.domain.Coffee
import it.matteocolombo.tacca.domain.Method
import it.matteocolombo.tacca.domain.TextMatch
import it.matteocolombo.tacca.domain.Tint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CoffeeItem(val coffee: Coffee, val calibrationCount: Int, val methods: List<Method>, val tint: Tint)

sealed interface CoffeeSheetState {
    data object New : CoffeeSheetState
    data class Edit(val coffee: Coffee, val calibrationCount: Int) : CoffeeSheetState
}

data class CoffeesUiState(
    val loading: Boolean = true,
    val query: String = "",
    val totalCount: Int = 0,
    val items: List<CoffeeItem> = emptyList(),
    val sheet: CoffeeSheetState? = null,
    val existingNames: Set<String> = emptySet(),
    val saving: Boolean = false,
)

class CoffeesViewModel(
    private val coffees: CoffeeRepository,
    calibrations: CalibrationRepository,
    private val homeFilter: HomeFilterStore,
) : ViewModel() {
    private val query = MutableStateFlow("")
    private val sheet = MutableStateFlow<CoffeeSheetState?>(null)
    private val saving = MutableStateFlow(false)

    val uiState: StateFlow<CoffeesUiState> =
        combine(coffees.observeCoffees(), calibrations.observeAll(), query, sheet, saving) { list, cals, q, sh, sv ->
            val byCoffee = cals.groupBy { it.coffee.id }
            val items = list.mapIndexed { i, coffee ->
                val own = byCoffee[coffee.id].orEmpty()
                CoffeeItem(coffee, own.size, own.map { it.method }.distinctBy { it.id }.sortedBy { it.sortOrder }, Tint.rotating(i))
            }.filter { TextMatch.matches(q, it.coffee.name) }
            CoffeesUiState(false, q, list.size, items, sh, list.map { TextMatch.normalize(it.name) }.toSet(), sv)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CoffeesUiState())

    fun onQueryChange(value: String) {
        query.value = value
    }

    fun openNew() {
        sheet.value = CoffeeSheetState.New
    }

    fun openEdit(coffee: Coffee) {
        viewModelScope.launch { sheet.value = CoffeeSheetState.Edit(coffee, coffees.calibrationCount(coffee.id)) }
    }

    fun dismissSheet() {
        sheet.value = null
    }

    fun save(name: String) {
        if (saving.value || name.isBlank()) return
        saving.value = true
        viewModelScope.launch {
            try {
                when (val s = sheet.value) {
                    is CoffeeSheetState.Edit -> coffees.rename(s.coffee.id, name)
                    else -> coffees.add(name)
                }
                sheet.value = null
            } finally {
                saving.value = false
            }
        }
    }

    fun delete() {
        val s = sheet.value as? CoffeeSheetState.Edit ?: return
        viewModelScope.launch {
            coffees.delete(s.coffee.id)
            sheet.value = null
        }
    }

    fun showOnHome(coffeeId: Long) = homeFilter.showCoffee(coffeeId)

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val c = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as TaccaApplication).container
                CoffeesViewModel(c.coffees, c.calibrations, c.homeFilter)
            }
        }
    }
}
