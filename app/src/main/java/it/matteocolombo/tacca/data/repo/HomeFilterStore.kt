package it.matteocolombo.tacca.data.repo

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class HomeFilter(val methodId: Long? = null, val coffeeId: Long? = null, val query: String = "")

/** Home screen filter, shared so that the Coffees and Methods tabs can open a filtered home. */
class HomeFilterStore {
    private val state = MutableStateFlow(HomeFilter())
    val filter: StateFlow<HomeFilter> = state.asStateFlow()

    fun update(transform: (HomeFilter) -> HomeFilter) = state.update(transform)
    fun showCoffee(id: Long) = state.update { HomeFilter(coffeeId = id) }
    fun showMethod(id: Long) = state.update { HomeFilter(methodId = id) }
}
