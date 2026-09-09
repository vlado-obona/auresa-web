package sk.rallysupport.otackomer.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import sk.rallysupport.otackomer.dsp.StrokeType

/** Používateľské nastavenia uložené cez DataStore. */
data class Settings(
    val workBandLow: Int = DEFAULT_WORK_LOW,
    val workBandHigh: Int = DEFAULT_WORK_HIGH,
    val strokeType: StrokeType = StrokeType.FOUR_STROKE,
) {
    companion object {
        const val DEFAULT_WORK_LOW = 3_500
        const val DEFAULT_WORK_HIGH = 7_000
        const val WORK_BAND_MIN = 1_000
        const val WORK_BAND_MAX = 11_000
    }
}

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "otackomer_settings")

class SettingsRepository(context: Context) {
    private val store = context.applicationContext.dataStore

    val settings: Flow<Settings> = store.data.map { prefs ->
        Settings(
            workBandLow = prefs[KEY_WORK_LOW] ?: Settings.DEFAULT_WORK_LOW,
            workBandHigh = prefs[KEY_WORK_HIGH] ?: Settings.DEFAULT_WORK_HIGH,
            strokeType = prefs[KEY_STROKE]?.let { runCatching { StrokeType.valueOf(it) }.getOrNull() }
                ?: StrokeType.FOUR_STROKE,
        )
    }

    suspend fun setWorkBand(low: Int, high: Int) {
        store.edit { prefs ->
            prefs[KEY_WORK_LOW] = low
            prefs[KEY_WORK_HIGH] = high
        }
    }

    suspend fun setStrokeType(type: StrokeType) {
        store.edit { prefs -> prefs[KEY_STROKE] = type.name }
    }

    private companion object {
        val KEY_WORK_LOW = intPreferencesKey("work_band_low")
        val KEY_WORK_HIGH = intPreferencesKey("work_band_high")
        val KEY_STROKE = stringPreferencesKey("stroke_type")
    }
}
