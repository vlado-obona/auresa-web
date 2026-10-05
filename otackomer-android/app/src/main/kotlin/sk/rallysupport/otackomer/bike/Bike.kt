package sk.rallysupport.otackomer.bike

import sk.rallysupport.otackomer.dsp.StrokeType

/**
 * Predpísaný voľnobeh tak, ako ho uvádza manuál výrobcu.
 *
 * Ak manuál dáva jedno číslo bez tolerancie, [low] == [high]. Aplikácia potom
 * pri vyhodnotení „v pásme“ pridá ±[SINGLE_VALUE_TOLERANCE] – to je rozlíšenie
 * merania, nie údaj výrobcu, a UI to tak aj píše.
 */
data class IdleSpec(val low: Int, val high: Int) {
    init {
        require(low in 500..4_000 && high in low..4_000) { "Nezmyselný voľnobeh $low–$high" }
    }

    val isSingleValue: Boolean get() = low == high

    /** Pásmo, v ktorom aplikácia zobrazí voľnobeh ako „v poriadku“. */
    val acceptBand: IntRange
        get() = if (isSingleValue) (low - SINGLE_VALUE_TOLERANCE)..(high + SINGLE_VALUE_TOLERANCE) else low..high

    companion object {
        const val SINGLE_VALUE_TOLERANCE = 50
    }
}

/** Údaje platné pre rozsah modelových rokov [yearFrom]..[yearTo]. */
data class BikeVariant(
    val yearFrom: Int,
    val yearTo: Int,
    val strokeType: StrokeType,
    /** Skutočný zdvihový objem v cm³ podľa technických údajov, ak je známy. */
    val displacementCc: Double?,
    /** `null` = manuál číselný voľnobeh neuvádza (typické pri dvojtaktoch). */
    val idle: IdleSpec?,
    /** Najvyššie otáčky počas zábehu, ak ich manuál uvádza. */
    val runInLimit: Int? = null,
    /** Odkiaľ údaj pochádza, napr. „KTM Owner's manual 2018 450/500 EXC‑F“. */
    val source: String,
    /**
     * `true` = hodnota overená priamo v manuáli. `false` = prevzatá zo súhrnu
     * vyhľadávania, ktorý na manuál odkazuje; UI ju označí ako orientačnú.
     */
    val verified: Boolean = true,
) {
    init {
        require(yearFrom <= yearTo) { "Rozsah rokov $yearFrom–$yearTo" }
        require(source.isNotBlank()) { "Chýba zdroj údajov" }
    }

    operator fun contains(year: Int): Boolean = year in yearFrom..yearTo
}

/** Jeden model v katalógu, napr. KTM 500 EXC‑F. */
data class BikeModel(
    /** Stabilný identifikátor ukladaný do nastavení, napr. `ktm-500-exc-f`. */
    val id: String,
    val brand: String,
    val name: String,
    val variants: List<BikeVariant>,
) {
    init {
        require(variants.isNotEmpty()) { "$id nemá žiadne roky" }
    }

    /** Všetky pokryté ročníky od najnovšieho. */
    val years: List<Int> get() = variants.flatMap { it.yearFrom..it.yearTo }.distinct().sortedDescending()

    fun variantFor(year: Int): BikeVariant? = variants.firstOrNull { year in it }

    val fullName: String get() = "$brand $name"
}
