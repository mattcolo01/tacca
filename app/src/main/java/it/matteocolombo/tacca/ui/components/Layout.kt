package it.matteocolombo.tacca.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

/** Content padding for the four tab screens: mockup side gutters plus the status bar inset. */
@Composable
fun topLevelContentPadding(): PaddingValues {
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    return PaddingValues(start = 20.dp, end = 20.dp, top = top + 20.dp, bottom = 24.dp)
}
