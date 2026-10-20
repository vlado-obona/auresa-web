package sk.rallysupport.otackomer.data

import sk.rallysupport.otackomer.dsp.StrokeType

/** Používateľské nastavenia uložené cez DataStore. */
data class Settings(
    val workBandLow: Int = DEFAULT_WORK_LOW,
    val workBandHigh: Int = DEFAULT_WORK_HIGH,
    /** Typ motora pri vlastnom nastavení; pri modeli z katalógu ho určuje model. */
    val strokeType: StrokeType = StrokeType.FOUR_STROKE,
    /** Id modelu z katalógu, [CUSTOM_BIKE] alebo `null` (= predvolený motocykel). */
    val bikeModelId: String? = null,
    val bikeYear: Int? = null,
    val customIdleLow: Int = DEFAULT_CUSTOM_IDLE_LOW,
    val customIdleHigh: Int = DEFAULT_CUSTOM_IDLE_HIGH,
) {
    companion object {
        const val DEFAULT_WORK_LOW = 3_500
        const val DEFAULT_WORK_HIGH = 7_000
        const val WORK_BAND_MIN = 1_000
        const val WORK_BAND_MAX = 11_000

        /** Pôvodný cieľ aplikácie – KTM 500 EXC‑F 2018. */
        const val DEFAULT_BIKE_ID = "ktm-500-exc-f"
        const val DEFAULT_BIKE_YEAR = 2018

        const val CUSTOM_BIKE = "custom"
        const val DEFAULT_CUSTOM_IDLE_LOW = 1_700
        const val DEFAULT_CUSTOM_IDLE_HIGH = 1_900
        const val IDLE_MIN = 800
        const val IDLE_MAX = 3_000
    }
}
