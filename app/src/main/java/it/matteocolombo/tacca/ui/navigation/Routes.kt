package it.matteocolombo.tacca.ui.navigation

import kotlinx.serialization.Serializable

/** Sentinel for "no id" in route arguments (nullable Long arguments are avoided on purpose). */
const val NEW_ID = -1L

/** SavedStateHandle key through which the grinder editor returns a newly created grinder id. */
const val NEW_GRINDER_RESULT_KEY = "newGrinderId"

@Serializable data object HomeRoute
@Serializable data object CoffeesRoute
@Serializable data object MethodsRoute
@Serializable data object GrindersRoute
@Serializable data object SettingsRoute
@Serializable data class DetailRoute(val calibrationId: Long)
@Serializable data class EditorRoute(val calibrationId: Long = NEW_ID, val coffeeId: Long = NEW_ID, val methodId: Long = NEW_ID)
@Serializable data class GrinderEditorRoute(val grinderId: Long = NEW_ID)

/** Reads a Long route argument, mapping [NEW_ID] and absence to null. */
fun androidx.lifecycle.SavedStateHandle.idArg(key: String): Long? = get<Long>(key)?.takeIf { it != NEW_ID }
