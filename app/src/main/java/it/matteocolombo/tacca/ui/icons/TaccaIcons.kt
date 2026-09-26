package it.matteocolombo.tacca.ui.icons

import androidx.annotation.DrawableRes
import it.matteocolombo.tacca.R
import it.matteocolombo.tacca.domain.MethodIcon

@DrawableRes
fun MethodIcon.drawableRes(): Int = when (this) {
    MethodIcon.MOKA -> R.drawable.ic_method_moka
    MethodIcon.ESPRESSO -> R.drawable.ic_method_espresso
    MethodIcon.V60 -> R.drawable.ic_method_v60
    MethodIcon.FRENCH_PRESS -> R.drawable.ic_method_french_press
    MethodIcon.AEROPRESS -> R.drawable.ic_method_aeropress
    MethodIcon.DROP -> R.drawable.ic_drop
    MethodIcon.CUP -> R.drawable.ic_method_cup
}
