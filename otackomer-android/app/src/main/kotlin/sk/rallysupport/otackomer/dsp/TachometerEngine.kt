package sk.rallysupport.otackomer.dsp

import kotlin.math.roundToInt

/** Prečo sa hodnota otáčok neaktualizovala. */
enum class SignalStatus {
    /** Ešte nie je dosť dát na odhad. */
    NO_DATA,

    /** Vstupná úroveň je príliš nízka – slabý zvuk. */
    WEAK_SIGNAL,

    /** Zhoda signálu (NSDF) je pod prahom – priveľa hluku. */
    TOO_NOISY,

    /** Otáčky boli úspešne aktualizované. */
    OK,
}

/**
 * Výsledok jedného analytického kroku.
 *
 * @property rpm posledná známa hodnota otáčok zaokrúhlená na desiatky, alebo
 *   `null`, ak ešte žiadna nebola
 * @property fresh `true`, ak [rpm] pochádza z tohto kroku; `false`, ak je
 *   zamrazená z minulosti
 * @property clarity hodnota NSDF vo vrchole (0–1), `null` ak sa nenašiel
 * @property level špičková vstupná úroveň 0–1
 */
data class Reading(
    val status: SignalStatus,
    val rpm: Int?,
    val fresh: Boolean,
    val clarity: Float?,
    val level: Float,
)

/**
 * Spája extraktor obálky, NSDF odhad a vyhladzovanie do jedného celku.
 * Vstup sú surové PCM vzorky; každých [analysisIntervalSeconds] zvuku vráti
 * [Reading].
 *
 * Všetky verejné metódy sú synchronizované – zapisovať môže vlákno so zvukom,
 * čítať UI.
 */
class TachometerEngine(
    val sampleRate: Int = 48_000,
    strokeType: StrokeType = StrokeType.FOUR_STROKE,
    analysisIntervalSeconds: Double = 0.2,
    /** Minimálna hodnota NSDF vo vrchole, aby sa hodnota prijala. */
    val minClarity: Float = 0.40f,
    /** Minimálna špičková úroveň vstupu (0–1), aby sa hodnota prijala. */
    val minLevel: Float = 0.004f,
) {
    val envelope = EnvelopeExtractor(sampleRate)
    val estimator = RpmEstimator(envelope.envRate)
    private val smoother = RpmSmoother()
    private val window = FloatArray(estimator.windowSize)
    private val analysisIntervalSamples = (sampleRate * analysisIntervalSeconds).roundToInt()
    private var samplesSinceAnalysis = 0
    private var lastRpm: Int? = null

    @Volatile
    var strokeType: StrokeType = strokeType
        private set

    /** Zmení typ motora a zahodí vyhladzovanie (perióda sa zmení dvojnásobne). */
    @Synchronized
    fun setStrokeType(type: StrokeType) {
        if (type == strokeType) return
        strokeType = type
        smoother.reset()
        lastRpm = null
    }

    /** Vymaže všetok stav – volá sa pri štarte merania. */
    @Synchronized
    fun reset() {
        envelope.reset()
        smoother.reset()
        samplesSinceAnalysis = 0
        lastRpm = null
    }

    /**
     * Spracuje blok PCM vzoriek. Ak od poslednej analýzy prešlo aspoň
     * [analysisIntervalSeconds] zvuku, spustí analýzu a vráti jej výsledok.
     */
    @Synchronized
    fun feed(samples: ShortArray, count: Int = samples.size): Reading? {
        envelope.process(samples, count)
        return maybeAnalyze(count)
    }

    /** Ako [feed], ale pre vzorky -1..1 (testy). */
    @Synchronized
    fun feed(samples: FloatArray, count: Int = samples.size): Reading? {
        envelope.process(samples, count)
        return maybeAnalyze(count)
    }

    /** Vykoná analýzu nad aktuálnym obsahom obálky bez ohľadu na interval. */
    @Synchronized
    fun analyze(): Reading {
        samplesSinceAnalysis = 0
        val level = envelope.peakLevel
        val count = envelope.latest(window.size, window)
        val estimate = estimator.estimate(window, count, strokeType)

        val status = when {
            level <= minLevel -> SignalStatus.WEAK_SIGNAL
            estimate == null -> if (count < estimator.minSamples) SignalStatus.NO_DATA else SignalStatus.TOO_NOISY
            estimate.clarity < minClarity -> SignalStatus.TOO_NOISY
            else -> SignalStatus.OK
        }

        if (status == SignalStatus.OK && estimate != null) {
            val smoothed = smoother.push(estimate.rpm)
            lastRpm = ((smoothed / 10.0).roundToInt() * 10)
        }

        return Reading(
            status = status,
            rpm = lastRpm,
            fresh = status == SignalStatus.OK,
            clarity = estimate?.clarity,
            level = level,
        )
    }

    private fun maybeAnalyze(count: Int): Reading? {
        samplesSinceAnalysis += count
        return if (samplesSinceAnalysis >= analysisIntervalSamples) analyze() else null
    }
}
