package it.matteocolombo.tacca.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import it.matteocolombo.tacca.R

private fun variable(res: Int, weight: Int) =
    Font(res, FontWeight(weight), variationSettings = FontVariation.Settings(FontVariation.weight(weight)))

val Bricolage = FontFamily(
    variable(R.font.bricolage_grotesque, 500),
    variable(R.font.bricolage_grotesque, 700),
)

val Figtree = FontFamily(
    variable(R.font.figtree, 400),
    variable(R.font.figtree, 500),
    variable(R.font.figtree, 600),
    variable(R.font.figtree, 700),
)

/** Text styles measured from the mockup. Colors are applied at the call site. */
object TaccaType {
    val brand = TextStyle(fontFamily = Bricolage, fontWeight = FontWeight.Bold, fontSize = 15.sp)
    val h1 = TextStyle(fontFamily = Bricolage, fontWeight = FontWeight.Bold, fontSize = 34.sp, lineHeight = 36.sp, letterSpacing = (-0.02).em)
    val h1Small = TextStyle(fontFamily = Bricolage, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 31.sp, letterSpacing = (-0.02).em)
    val cardTitle = TextStyle(fontFamily = Bricolage, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 24.sp)
    val numberXL = TextStyle(fontFamily = Bricolage, fontWeight = FontWeight.Bold, fontSize = 76.sp, lineHeight = 72.sp, letterSpacing = (-0.03).em)
    val numberL = TextStyle(fontFamily = Bricolage, fontWeight = FontWeight.Bold, fontSize = 34.sp, lineHeight = 34.sp)
    val numberM = TextStyle(fontFamily = Bricolage, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 24.sp)
    val numberS = TextStyle(fontFamily = Bricolage, fontWeight = FontWeight.Bold, fontSize = 17.sp, lineHeight = 20.sp)
    val title = TextStyle(fontFamily = Figtree, fontWeight = FontWeight.Bold, fontSize = 15.sp, lineHeight = 19.sp)
    val rowTitle = TextStyle(fontFamily = Figtree, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 19.sp)
    val body = TextStyle(fontFamily = Figtree, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 21.sp)
    val bodySmall = TextStyle(fontFamily = Figtree, fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 17.sp)
    val label = TextStyle(fontFamily = Figtree, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 18.sp)
    val legend = TextStyle(fontFamily = Figtree, fontWeight = FontWeight.Bold, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.07.em)
    val unit = TextStyle(fontFamily = Figtree, fontWeight = FontWeight.Bold, fontSize = 10.sp, lineHeight = 12.sp, letterSpacing = 0.06.em)
    val nav = TextStyle(fontFamily = Figtree, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, lineHeight = 14.sp)
    val button = TextStyle(fontFamily = Figtree, fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 20.sp)
}

internal val MaterialTypography = Typography().let { base ->
    Typography(
        displayLarge = base.displayLarge.copy(fontFamily = Bricolage),
        headlineLarge = base.headlineLarge.copy(fontFamily = Bricolage),
        headlineMedium = base.headlineMedium.copy(fontFamily = Bricolage),
        headlineSmall = base.headlineSmall.copy(fontFamily = Bricolage, fontWeight = FontWeight.Bold),
        titleLarge = base.titleLarge.copy(fontFamily = Figtree, fontWeight = FontWeight.Bold),
        titleMedium = base.titleMedium.copy(fontFamily = Figtree, fontWeight = FontWeight.SemiBold),
        titleSmall = base.titleSmall.copy(fontFamily = Figtree, fontWeight = FontWeight.SemiBold),
        bodyLarge = base.bodyLarge.copy(fontFamily = Figtree),
        bodyMedium = base.bodyMedium.copy(fontFamily = Figtree),
        bodySmall = base.bodySmall.copy(fontFamily = Figtree),
        labelLarge = base.labelLarge.copy(fontFamily = Figtree, fontWeight = FontWeight.Bold),
        labelMedium = base.labelMedium.copy(fontFamily = Figtree),
        labelSmall = base.labelSmall.copy(fontFamily = Figtree),
    )
}
