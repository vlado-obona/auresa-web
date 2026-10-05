package sk.rallysupport.otackomer

import android.app.Activity
import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import java.util.Locale

/**
 * Jazyk aplikácie nezávisle od jazyka systému.
 *
 * Android 13+ má jazyk pre aplikáciu priamo v systéme ([LocaleManager]) – výber
 * sa zobrazí aj v Nastavenia → Aplikácie → Otáčkomer → Jazyk a systém aktivitu
 * po zmene sám vytvorí nanovo. Na starších verziách si voľbu pamätáme sami
 * a kontext aktivity prekladáme v [wrap].
 */
object AppLanguage {

    /** Podporované jazyky v poradí, v akom ich ukazuje prepínač. */
    val supported: List<String> = listOf("sk", "en")

    private const val PREFS = "app_language"
    private const val KEY_TAG = "tag"

    /** Jazyk, v ktorom sa práve zobrazujú texty („sk“ alebo „en“). */
    fun current(configuration: Configuration): String {
        val language = configuration.locales[0].language
        return if (language in supported) language else "en"
    }

    fun set(activity: Activity, tag: String) {
        require(tag in supported) { "Nepodporovaný jazyk $tag" }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            activity.getSystemService(LocaleManager::class.java).applicationLocales = LocaleList.forLanguageTags(tag)
        } else {
            activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_TAG, tag).commit()
            activity.recreate()
        }
    }

    /** Uložený jazyk pre Android do 12, inak null (vtedy rozhoduje systém). */
    fun storedTag(context: Context): String? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            null
        } else {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_TAG, null)
        }

    /** Pre `attachBaseContext`: na Androide do 12 vráti kontext s uloženým jazykom. */
    fun wrap(base: Context): Context {
        val tag = storedTag(base) ?: return base
        val locale = Locale.forLanguageTag(tag)
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        return base.createConfigurationContext(config)
    }
}
