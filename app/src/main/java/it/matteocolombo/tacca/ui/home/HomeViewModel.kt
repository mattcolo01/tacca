package it.matteocolombo.tacca.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import it.matteocolombo.tacca.TaccaApplication
import it.matteocolombo.tacca.data.repo.CalibrationRepository
import it.matteocolombo.tacca.data.repo.CoffeeRepository
import it.matteocolombo.tacca.data.repo.GrinderRepository
import it.matteocolombo.tacca.data.repo.HomeFilter
import it.matteocolombo.tacca.data.repo.HomeFilterStore
import it.matteocolombo.tacca.data.repo.MethodRepository
import it.matteocolombo.tacca.domain.Calibration
import it.matteocolombo.tacca.domain.Coffee
import it.matteocolombo.tacca.domain.Method
import it.matteocolombo.tacca.domain.TextMatch
import it.matteocolombo.tacca.ui.format.methodName
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class HomeGroup(val coffee: Coffee, val rows: List<Calibration>)

data class HomeUiState(
    val loading: Boolean = true,
    val hasCalibrations: Boolean = false,
    /** No coffee or no grinder exists yet. */
    val needsSetup: Boolean = false,
    val chips: List<Method> = emptyList(),
    val selectedMethodId: Long? = null,
    val coffeeFilter: Coffee? = null,
    val query: String = "",
    val searchOpen: Boolean = false,
    val groups: List<HomeGroup> = emptyList(),
)

class HomeViewModel(
    calibrations: CalibrationRepository,
    methods: MethodRepository,
    coffees: CoffeeRepository,
    grinders: GrinderRepository,
    private val homeFilter: HomeFilterStore,
    private val methodLabel: (Method) -> String,
) : ViewModel() {
    private val searchOpen = MutableStateFlow(false)
    private val filterAndSearch = combine(homeFilter.filter, searchOpen) { f, open -> f to open }

    val uiState: StateFlow<HomeUiState> =
        combine(calibrations.observeAll(), methods.observeMethods(), coffees.observeCoffees(), grinders.observeGrinders(), filterAndSearch) { cals, ms, cs, gs, (f, open) ->
            // Filters pointing at deleted data are ignored (e.g. after a delete or an import).
            val methodId = f.methodId?.takeIf { id -> ms.any { it.id == id } }
            val coffee = f.coffeeId?.let { id -> cs.firstOrNull { it.id == id } }
            val used = cals.map { it.method.id }.toSet()
            val filtered = cals.filter {
                (methodId == null || it.method.id == methodId) &&
                    (coffee == null || it.coffee.id == coffee.id) &&
                    TextMatch.matches(f.query, it.coffee.name, methodLabel(it.method), it.grinder.name)
            }
            val groups = filtered.groupBy { it.coffee.id }.values
                .map { rows -> HomeGroup(rows.first().coffee, rows.sortedWith(compareBy({ it.method.sortOrder }, { it.grinder.name }))) }
                .sortedByDescending { g -> g.rows.maxOf { it.updatedAt } }
            HomeUiState(
                loading = false,
                hasCalibrations = cals.isNotEmpty(),
                needsSetup = cs.isEmpty() || gs.isEmpty(),
                chips = ms.filter { it.id in used || it.id == methodId },
                selectedMethodId = methodId,
                coffeeFilter = coffee,
                query = f.query,
                searchOpen = open || f.query.isNotEmpty(),
                groups = groups,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    val currentFilter: HomeFilter get() = homeFilter.filter.value

    fun selectMethod(id: Long?) = homeFilter.update { it.copy(methodId = id) }
    fun clearCoffeeFilter() = homeFilter.update { it.copy(coffeeId = null) }
    fun onQueryChange(query: String) = homeFilter.update { it.copy(query = query) }
    fun clearFilters() = homeFilter.update { HomeFilter() }

    fun openSearch() {
        searchOpen.value = true
    }

    fun closeSearch() {
        searchOpen.value = false
        homeFilter.update { it.copy(query = "") }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as TaccaApplication
                val c = app.container
                HomeViewModel(c.calibrations, c.methods, c.coffees, c.grinders, c.homeFilter) { methodName(app.resources, it) }
            }
        }
    }
}
