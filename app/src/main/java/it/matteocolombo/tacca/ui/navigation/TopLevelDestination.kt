package it.matteocolombo.tacca.ui.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import it.matteocolombo.tacca.R

enum class TopLevelDestination(@StringRes val label: Int, @DrawableRes val icon: Int, val route: Any) {
    CALIBRATIONS(R.string.nav_calibrations, R.drawable.ic_tab_calibrations, HomeRoute),
    COFFEES(R.string.nav_coffees, R.drawable.ic_bean, CoffeesRoute),
    METHODS(R.string.nav_methods, R.drawable.ic_drop, MethodsRoute),
    GRINDERS(R.string.nav_grinders, R.drawable.ic_grinder, GrindersRoute),
}
