package it.matteocolombo.tacca.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import it.matteocolombo.tacca.R

/** Per-app language via AppCompat (persisted on API < 33 by AppLocalesMetadataHolderService). */
object AppLanguage {
    val supported = listOf("en", "it", "es", "fr", "de")

    /** Language tag chosen for the app, or null when following the system. */
    fun current(): String? = AppCompatDelegate.getApplicationLocales().takeUnless { it.isEmpty }?.get(0)?.language

    fun set(tag: String?) = AppCompatDelegate.setApplicationLocales(
        if (tag == null) LocaleListCompat.getEmptyLocaleList() else LocaleListCompat.forLanguageTags(tag),
    )

    @StringRes
    fun labelRes(tag: String?): Int = when (tag) {
        "en" -> R.string.language_en
        "it" -> R.string.language_it
        "es" -> R.string.language_es
        "fr" -> R.string.language_fr
        "de" -> R.string.language_de
        else -> R.string.language_system
    }

    /** System per-app language screen (API 33+), or null to use the in-app picker. */
    fun systemSettingsIntent(context: Context): Intent? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Intent(Settings.ACTION_APP_LOCALE_SETTINGS, Uri.fromParts("package", context.packageName, null))
        } else {
            null
        }
}
