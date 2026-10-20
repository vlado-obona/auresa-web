package sk.rallysupport.otackomer.dsp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class TachometerEngineTest {

    private val sampleRate = 48_000
    private val chunk = 2_400 // 50 ms, podobne ako reálne čítanie z AudioRecord

    /** Nakŕmi motor signálom po blokoch a vráti všetky výsledky analýz. */
    private fun run(engine: TachometerEngine, signal: FloatArray): List<Reading> {
        val readings = mutableListOf<Reading>()
        var offset = 0
        val buffer = FloatArray(chunk)
        while (offset < signal.size) {
            val n = minOf(chunk, signal.size - offset)
            System.arraycopy(signal, offset, buffer, 0, n)
            engine.feed(buffer, n)?.let(readings::add)
            offset += n
        }
        return readings
    }

    private fun assertDetects(rpm: Double, strokeType: StrokeType, tolerance: Double = 25.0) {
        val engine = TachometerEngine(sampleRate, strokeType)
        val signal = SyntheticEngine.pulseTrain(rpm, strokeType, seconds = 6.0, sampleRate = sampleRate)
        val readings = run(engine, signal)

        // Po ustálení (posledné 2 s) musí byť každá analýza úspešná…
        val settled = readings.takeLast(10)
        assertTrue("žiadne výsledky", settled.isNotEmpty())
        settled.forEach { reading ->
            assertEquals("stav pri $rpm ot./min: $reading", SignalStatus.OK, reading.status)
            assertTrue("čerstvá hodnota", reading.fresh)
            assertTrue("zhoda signálu ${reading.clarity}", (reading.clarity ?: 0f) >= 0.40f)
        }
        // …a výsledná hodnota musí sedieť do ±tolerance.
        val last = settled.last().rpm
        assertNotNull(last)
        val error = abs(last!! - rpm)
        assertTrue("očakávané $rpm, dostal som $last (chyba $error)", error <= tolerance)
    }

    @Test
    fun `4T volnobeh 1850`() = assertDetects(1_850.0, StrokeType.FOUR_STROKE)

    @Test
    fun `4T 3000`() = assertDetects(3_000.0, StrokeType.FOUR_STROKE)

    @Test
    fun `4T 6500`() = assertDetects(6_500.0, StrokeType.FOUR_STROKE)

    @Test
    fun `2T 3000`() = assertDetects(3_000.0, StrokeType.TWO_STROKE)

    @Test
    fun `4T 1850 s vyraznym sumom`() {
        val strokeType = StrokeType.FOUR_STROKE
        val engine = TachometerEngine(sampleRate, strokeType)
        val signal = SyntheticEngine.pulseTrain(
            1_850.0, strokeType, seconds = 6.0, sampleRate = sampleRate,
            pulseAmplitude = 0.4f, noiseAmplitude = 0.15f, seed = 7,
        )
        val last = run(engine, signal).last()
        assertEquals(SignalStatus.OK, last.status)
        assertTrue("dostal som ${last.rpm}", abs(last.rpm!! - 1_850.0) <= 25.0)
    }

    @Test
    fun `cisty sum nic nehlasi`() {
        val engine = TachometerEngine(sampleRate, StrokeType.FOUR_STROKE)
        val noise = SyntheticEngine.whiteNoise(seconds = 6.0, sampleRate = sampleRate, amplitude = 0.2f, seed = 3)
        val readings = run(engine, noise)
        assertTrue(readings.isNotEmpty())
        readings.forEach { reading ->
            assertTrue("šum nesmie dať OK: $reading", reading.status != SignalStatus.OK)
            assertNull("žiadna hodnota otáčok", reading.rpm)
        }
        // Šum je hlasný, takže dôvod je „priveľa hluku“, nie „slabý zvuk“.
        assertEquals(SignalStatus.TOO_NOISY, readings.last().status)
    }

    @Test
    fun `ticho hlasi slaby zvuk`() {
        val engine = TachometerEngine(sampleRate, StrokeType.FOUR_STROKE)
        val readings = run(engine, SyntheticEngine.silence(seconds = 3.0, sampleRate = sampleRate))
        assertEquals(SignalStatus.WEAK_SIGNAL, readings.last().status)
        assertNull(readings.last().rpm)
    }

    @Test
    fun `po strate signalu zostane posledna hodnota zamrazena`() {
        val strokeType = StrokeType.FOUR_STROKE
        val engine = TachometerEngine(sampleRate, strokeType)
        val running = SyntheticEngine.pulseTrain(3_000.0, strokeType, seconds = 5.0, sampleRate = sampleRate)
        val frozen = run(engine, running).last().rpm
        assertNotNull(frozen)

        val noise = SyntheticEngine.whiteNoise(seconds = 5.0, sampleRate = sampleRate, amplitude = 0.2f, seed = 5)
        val after = run(engine, noise).last()
        assertTrue(after.status != SignalStatus.OK)
        assertEquals(false, after.fresh)
        assertEquals("zamrazená hodnota sa nemení", frozen, after.rpm)
    }

    @Test
    fun `prepnutie typu motora zdvojnasobi hodnotu`() {
        // Ten istý zvuk: 4T číta 3 000, 2T musí čítať 1 500.
        val signal = SyntheticEngine.pulseTrain(3_000.0, StrokeType.FOUR_STROKE, seconds = 6.0, sampleRate = sampleRate)
        val engine = TachometerEngine(sampleRate, StrokeType.TWO_STROKE)
        val last = run(engine, signal).last()
        assertEquals(SignalStatus.OK, last.status)
        assertTrue("dostal som ${last.rpm}", abs(last.rpm!! - 1_500.0) <= 25.0)
    }
}

class RpmSmootherTest {
    @Test
    fun `median potlaci osamely vystrelok`() {
        val smoother = RpmSmoother()
        repeat(7) { smoother.push(2_000.0) }
        val afterOutlier = smoother.push(9_000.0)
        assertEquals(2_000.0, afterOutlier, 1e-9)
    }

    @Test
    fun `exponencialne vyhladenie ide k novej hodnote`() {
        val smoother = RpmSmoother(medianWindow = 1, alpha = 0.4)
        smoother.push(1_000.0)
        assertEquals(1_400.0, smoother.push(2_000.0), 1e-9)
        assertEquals(1_640.0, smoother.push(2_000.0), 1e-9)
    }
}
