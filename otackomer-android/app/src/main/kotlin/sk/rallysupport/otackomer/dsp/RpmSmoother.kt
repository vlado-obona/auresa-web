package sk.rallysupport.otackomer.dsp

/**
 * Vyhladenie odhadov: medián z posledných [medianWindow] hodnôt, potom
 * exponenciálne vyhladenie s koeficientom [alpha].
 */
class RpmSmoother(
    private val medianWindow: Int = 7,
    private val alpha: Double = 0.4,
) {
    private val recent = ArrayDeque<Double>(medianWindow + 1)
    private var smoothed: Double? = null

    /** Aktuálna vyhladená hodnota alebo `null`, ak ešte nič neprišlo. */
    val value: Double? get() = smoothed

    /** Pridá nový odhad a vráti vyhladenú hodnotu. */
    fun push(rpm: Double): Double {
        recent.addLast(rpm)
        while (recent.size > medianWindow) recent.removeFirst()
        val median = recent.sorted()[recent.size / 2]
        val next = smoothed?.let { it + alpha * (median - it) } ?: median
        smoothed = next
        return next
    }

    fun reset() {
        recent.clear()
        smoothed = null
    }
}
