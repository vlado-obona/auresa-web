package sk.rallysupport.otackomer.dsp

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Z PCM vzoriek vyrába obálku hlasitosti.
 *
 * 1. každá vzorka sa usmerní (absolútna hodnota),
 * 2. prejde jednopólovým dolnopriepustným filtrom s medznou frekvenciou [cutoffHz],
 * 3. decimuje sa na približne [targetEnvRate] Hz,
 * 4. ukladá sa do kruhového bufferu dlhého približne [bufferSeconds] sekúnd.
 *
 * Zážihy jednovalca sú v obálke prítomné aj vtedy, keď mikrofón telefónu
 * odreže samotnú základnú frekvenciu pod ~100 Hz.
 *
 * Trieda nie je thread-safe – volajúci ju musí používať z jedného vlákna
 * alebo synchronizovať sám (viď [TachometerEngine]).
 */
class EnvelopeExtractor(
    val sampleRate: Int,
    targetEnvRate: Int = 1_000,
    cutoffHz: Double = 120.0,
    bufferSeconds: Double = 4.0,
) {
    /** Každá n-tá hodnota filtra ide do obálky. */
    val decimation: Int = max(1, (sampleRate.toDouble() / targetEnvRate).roundToInt())

    /** Skutočná vzorkovacia frekvencia obálky v Hz. */
    val envRate: Double = sampleRate.toDouble() / decimation

    /** Kapacita kruhového bufferu v počte vzoriek obálky. */
    val capacity: Int = max(16, (envRate * bufferSeconds).roundToInt())

    /** Počet platných vzoriek v bufferi (max. [capacity]). */
    var filled: Int = 0
        private set

    /**
     * Špičková vstupná úroveň (0–1) s pomalým doznievaním – slúži na indikátor
     * úrovne a na rozhodnutie, či je zvuk dosť silný na meranie.
     */
    var peakLevel: Float = 0f
        private set

    private val alpha: Float = (1.0 - exp(-2.0 * PI * cutoffHz / sampleRate)).toFloat()
    private val ring = FloatArray(capacity)
    private var writePos = 0
    private var lowPass = 0f
    private var decimationCounter = 0

    /** Spracuje [count] 16-bitových PCM vzoriek z [samples]. */
    fun process(samples: ShortArray, count: Int = samples.size) {
        var blockPeak = 0f
        var lp = lowPass
        var dec = decimationCounter
        for (i in 0 until count) {
            val a = abs(samples[i].toInt()) * INV_PCM16
            if (a > blockPeak) blockPeak = a
            lp += alpha * (a - lp)
            if (++dec >= decimation) {
                dec = 0
                push(lp)
            }
        }
        lowPass = lp
        decimationCounter = dec
        updatePeak(blockPeak, count)
    }

    /** Spracuje [count] vzoriek v rozsahu -1..1 (používajú testy). */
    fun process(samples: FloatArray, count: Int = samples.size) {
        var blockPeak = 0f
        var lp = lowPass
        var dec = decimationCounter
        for (i in 0 until count) {
            val a = abs(samples[i])
            if (a > blockPeak) blockPeak = a
            lp += alpha * (a - lp)
            if (++dec >= decimation) {
                dec = 0
                push(lp)
            }
        }
        lowPass = lp
        decimationCounter = dec
        updatePeak(blockPeak, count)
    }

    /**
     * Skopíruje posledných najviac [n] vzoriek obálky do [out] (od najstaršej
     * po najnovšiu) a vráti ich skutočný počet.
     */
    fun latest(n: Int, out: FloatArray): Int {
        val count = minOf(n, filled, out.size)
        var idx = (writePos - count + capacity) % capacity
        for (i in 0 until count) {
            out[i] = ring[idx]
            idx = (idx + 1) % capacity
        }
        return count
    }

    fun reset() {
        ring.fill(0f)
        writePos = 0
        filled = 0
        lowPass = 0f
        decimationCounter = 0
        peakLevel = 0f
    }

    private fun push(value: Float) {
        ring[writePos] = value
        writePos = (writePos + 1) % capacity
        if (filled < capacity) filled++
    }

    private fun updatePeak(blockPeak: Float, count: Int) {
        // Doznievanie ~0.85 na 4096 vzoriek, nezávisle od veľkosti bloku.
        val decay = PEAK_DECAY_PER_BLOCK.pow(count.toFloat() / PEAK_DECAY_BLOCK)
        peakLevel = max(peakLevel * decay, blockPeak)
    }

    private companion object {
        const val INV_PCM16 = 1f / 32_768f
        const val PEAK_DECAY_PER_BLOCK = 0.85f
        const val PEAK_DECAY_BLOCK = 4_096f
    }
}
