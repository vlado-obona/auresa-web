package sk.rallysupport.otackomer.dsp

import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/**
 * Generátor syntetického zvuku motora pre testy: sled krátkych tlmených
 * impulzov (zážihov) s prekryvom bieleho šumu.
 */
object SyntheticEngine {
    /**
     * @param rpm otáčky motora
     * @param strokeType typ motora – určuje, koľko zážihov pripadá na otáčku
     * @param seconds dĺžka signálu
     * @param sampleRate vzorkovacia frekvencia
     * @param pulseAmplitude amplitúda jedného zážihu (0–1)
     * @param noiseAmplitude smerodajná odchýlka bieleho šumu
     * @param pulseFrequencyHz nosná frekvencia impulzu (zvuk výfuku)
     * @param pulseDecaySeconds časová konštanta doznievania impulzu
     */
    fun pulseTrain(
        rpm: Double,
        strokeType: StrokeType,
        seconds: Double,
        sampleRate: Int = 48_000,
        pulseAmplitude: Float = 0.5f,
        noiseAmplitude: Float = 0.05f,
        pulseFrequencyHz: Double = 1_400.0,
        pulseDecaySeconds: Double = 0.004,
        seed: Int = 1,
    ): FloatArray {
        val length = (seconds * sampleRate).toInt()
        val out = whiteNoise(seconds, sampleRate, noiseAmplitude, seed)

        val firingsPerSecond = rpm / 60.0 / strokeType.revsPerFiring
        val periodSamples = sampleRate / firingsPerSecond
        val pulseLengthSamples = (6 * pulseDecaySeconds * sampleRate).toInt()

        var firing = 0
        while (true) {
            val start = firing * periodSamples
            if (start >= length) break
            val startIndex = start.toInt()
            val fraction = start - startIndex
            for (k in 0 until pulseLengthSamples) {
                val idx = startIndex + k
                if (idx >= length) break
                val t = (k - fraction) / sampleRate
                if (t < 0) continue
                val envelope = exp(-t / pulseDecaySeconds)
                out[idx] += (pulseAmplitude * envelope * sin(2 * PI * pulseFrequencyHz * t)).toFloat()
            }
            firing++
        }
        return out
    }

    fun whiteNoise(seconds: Double, sampleRate: Int = 48_000, amplitude: Float = 0.05f, seed: Int = 1): FloatArray {
        val random = Random(seed)
        val length = (seconds * sampleRate).toInt()
        return FloatArray(length) { gaussian(random) * amplitude }
    }

    fun silence(seconds: Double, sampleRate: Int = 48_000): FloatArray = FloatArray((seconds * sampleRate).toInt())

    private fun gaussian(random: Random): Float {
        // Box–Muller
        val u1 = 1.0 - random.nextDouble()
        val u2 = random.nextDouble()
        return (kotlin.math.sqrt(-2.0 * kotlin.math.ln(u1)) * kotlin.math.cos(2 * PI * u2)).toFloat()
    }
}
