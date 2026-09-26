package it.matteocolombo.tacca.domain

enum class ThemeMode { SYSTEM, LIGHT, DARK }

enum class AccentOption(val argb: Long) {
    TERRACOTTA(0xFFB4552E),
    PINE(0xFF2F6B5A),
    INDIGO(0xFF3D5A8A),
    PLUM(0xFF8A4B6E),
}

data class AppSettings(
    val accent: AccentOption = AccentOption.TERRACOTTA,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
)
