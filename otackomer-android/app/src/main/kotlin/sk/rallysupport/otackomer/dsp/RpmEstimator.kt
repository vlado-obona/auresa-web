package sk.rallysupport.otackomer.dsp

import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/**
 * Odhad otáčok z obálky pomocou normalizovanej autokorelácie
 * (NSDF – normalized square difference function, McLeod pitch method).
 *
 * Pre každé oneskorenie τ v rozsahu zodpovedajúcom [rpmMin]–[rpmMax] sa spočíta
 * `nsdf[τ] = 2·Σ x[i]·x[i+τ] / Σ (x[i]² + x[i+τ]²)`. Hľadá sa **prvý** lokálny
 * vrchol nad `0,85 × globálne maximum`, nie globálne maximum – tým sa bráni
 * tomu, aby detektor vybral dvojnásobnú periódu (polovičné otáčky).
 */
class RpmEstimator(
    /** Vzorkovacia frekvencia obálky v Hz. */
    val envRate: Double,
    val rpmMin: Int = 600,
    val rpmMax: Int = 12_000,
    /** Koľko posledných vzoriek obálky sa analyzuje. */
    val windowSize: Int = 2_600,
    /** Minimálny počet vzoriek, pod ktorým sa odhad nerobí. */
    val minSamples: Int = 800,
    /** Prah pre výber prvého vrcholu ako podiel globálneho maxima. */
    val peakThreshold: Float = 0.85f,
) {
    /**
     * @property rpm odhadnuté otáčky za minútu (nevyhladené)
     * @property clarity hodnota NSDF vo vybranom vrchole (0–1), „zhoda signálu“
     * @property period perióda zážihov vo vzorkách obálky (s interpoláciou)
     */
    data class Estimate(val rpm: Double, val clarity: Float, val period: Double)

    private val work = FloatArray(windowSize)
    private val nsdf = FloatArray(windowSize)

    /**
     * Odhadne otáčky z obálky [envelope] (prvých [count] hodnôt, od najstaršej
     * po najnovšiu). Vracia `null`, ak je vzoriek málo alebo sa nenašiel žiadny
     * použiteľný vrchol.
     */
    fun estimate(envelope: FloatArray, count: Int, strokeType: StrokeType): Estimate? {
        val n = min(count, windowSize)
        if (n < minSamples) return null

        // Posledných n vzoriek bez jednosmernej zložky.
        val offset = count - n
        var mean = 0.0
        for (i in 0 until n) {
            val v = envelope[offset + i]
            work[i] = v
            mean += v
        }
        val meanF = (mean / n).toFloat()
        for (i in 0 until n) work[i] -= meanF

        val revsPerFiring = strokeType.revsPerFiring
        val lagMin = max(4, floor(envRate * 60.0 * revsPerFiring / rpmMax).toInt())
        val lagMax = min(n - 100, ceil(envRate * 60.0 * revsPerFiring / rpmMin).toInt())
        if (lagMax <= lagMin + 2) return null

        var best = 0f
        for (lag in lagMin..lagMax) {
            var r = 0.0
            var m = 0.0
            val limit = n - lag
            for (j in 0 until limit) {
                val a = work[j]
                val b = work[j + lag]
                r += a * b
                m += a * a + b * b
            }
            val v = if (m > 0.0) (2.0 * r / m).toFloat() else 0f
            nsdf[lag] = v
            if (v > best) best = v
        }
        if (best <= 0f) return null

        // Prvý lokálny vrchol nad prahom = základná perióda.
        val threshold = peakThreshold * best
        var chosen = -1
        for (lag in (lagMin + 1) until lagMax) {
            val v = nsdf[lag]
            if (v > nsdf[lag - 1] && v >= nsdf[lag + 1] && v >= threshold) {
                chosen = lag
                break
            }
        }
        if (chosen < 0) return null

        // Parabolická interpolácia cez tri body okolo vrcholu.
        val y0 = nsdf[chosen - 1]
        val y1 = nsdf[chosen]
        val y2 = nsdf[chosen + 1]
        val denominator = y0 - 2f * y1 + y2
        var shift = if (denominator != 0f) 0.5f * (y0 - y2) / denominator else 0f
        if (shift < -1f || shift > 1f) shift = 0f
        val period = chosen + shift.toDouble()

        return Estimate(
            rpm = envRate * 60.0 * revsPerFiring / period,
            clarity = y1,
            period = period,
        )
    }
}
