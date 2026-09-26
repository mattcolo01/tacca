package it.matteocolombo.tacca.domain

enum class MethodIcon { MOKA, ESPRESSO, V60, FRENCH_PRESS, AEROPRESS, DROP, CUP }

enum class Tint {
    PEACH, SAND, SKY, SAGE, LILAC;

    companion object {
        fun rotating(index: Int): Tint = entries[Math.floorMod(index, entries.size)]
    }
}

/** Methods seeded on first launch. The key is stable and stored in the database. */
enum class BuiltInMethod(val key: String, val icon: MethodIcon, val tint: Tint) {
    MOKA("moka", MethodIcon.MOKA, Tint.PEACH),
    ESPRESSO("espresso", MethodIcon.ESPRESSO, Tint.SAND),
    V60("v60", MethodIcon.V60, Tint.SKY),
    FRENCH_PRESS("french", MethodIcon.FRENCH_PRESS, Tint.SAGE),
    AEROPRESS("aero", MethodIcon.AEROPRESS, Tint.LILAC);

    companion object {
        fun fromKey(key: String?): BuiltInMethod? = entries.firstOrNull { it.key == key }
    }
}
