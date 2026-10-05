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

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "otackomer_settings")

class SettingsRepository(context: Context) {
    private val store = context.applicationContext.dataStore

    val settings: Flow<Settings> = store.data.map { prefs ->
        Settings(
            workBandLow = prefs[KEY_WORK_LOW] ?: Settings.DEFAULT_WORK_LOW,
            workBandHigh = prefs[KEY_WORK_HIGH] ?: Settings.DEFAULT_WORK_HIGH,
            strokeType = prefs[KEY_STROKE]?.let { runCatching { StrokeType.valueOf(it) }.getOrNull() }
                ?: StrokeType.FOUR_STROKE,
            bikeModelId = prefs[KEY_BIKE_MODEL],
            bikeYear = prefs[KEY_BIKE_YEAR],
            customIdleLow = prefs[KEY_CUSTOM_IDLE_LOW] ?: Settings.DEFAULT_CUSTOM_IDLE_LOW,
            customIdleHigh = prefs[KEY_CUSTOM_IDLE_HIGH] ?: Settings.DEFAULT_CUSTOM_IDLE_HIGH,
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

    /** Model z katalógu a jeho ročník. */
    suspend fun setBike(modelId: String, year: Int) {
        store.edit { prefs ->
            prefs[KEY_BIKE_MODEL] = modelId
            prefs[KEY_BIKE_YEAR] = year
        }
    }

    /** Vlastný motocykel: voľnobeh a typ motora zadané používateľom. */
    suspend fun setCustomBike(idleLow: Int, idleHigh: Int, strokeType: StrokeType) {
        store.edit { prefs ->
            prefs[KEY_BIKE_MODEL] = Settings.CUSTOM_BIKE
            prefs.remove(KEY_BIKE_YEAR)
            prefs[KEY_CUSTOM_IDLE_LOW] = idleLow
            prefs[KEY_CUSTOM_IDLE_HIGH] = idleHigh
            prefs[KEY_STROKE] = strokeType.name
        }
    }

    private companion object {
        val KEY_WORK_LOW = intPreferencesKey("work_band_low")
        val KEY_WORK_HIGH = intPreferencesKey("work_band_high")
        val KEY_STROKE = stringPreferencesKey("stroke_type")
        val KEY_BIKE_MODEL = stringPreferencesKey("bike_model")
        val KEY_BIKE_YEAR = intPreferencesKey("bike_year")
        val KEY_CUSTOM_IDLE_LOW = intPreferencesKey("custom_idle_low")
        val KEY_CUSTOM_IDLE_HIGH = intPreferencesKey("custom_idle_high")
    }
}
