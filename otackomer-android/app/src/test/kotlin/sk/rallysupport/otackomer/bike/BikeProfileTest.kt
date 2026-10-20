package sk.rallysupport.otackomer.bike

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import sk.rallysupport.otackomer.data.Settings
import sk.rallysupport.otackomer.dsp.StrokeType

class BikeProfileTest {

    private fun variant(from: Int, to: Int, idle: IdleSpec?, stroke: StrokeType = StrokeType.FOUR_STROKE) =
        BikeVariant(from, to, stroke, 449.9, idle, runInLimit = null, source = "test")

    private val catalog = listOf(
        BikeModel(
            id = Settings.DEFAULT_BIKE_ID, brand = "KTM", name = "500 EXC‑F",
            variants = listOf(variant(2017, 2019, IdleSpec(1_800, 1_900))),
        ),
        BikeModel(
            id = "test-2t", brand = "Test", name = "300",
            variants = listOf(
                variant(2016, 2017, null, StrokeType.TWO_STROKE),
                variant(2018, 2020, IdleSpec(1_400, 1_500), StrokeType.TWO_STROKE),
            ),
        ),
    )

    @Test
    fun `bez nastavenia je predvoleny KTM 500 EXC-F 2018`() {
        val p = BikeProfile.resolve(Settings(), catalog)
        assertEquals(Settings.DEFAULT_BIKE_ID, p.modelId)
        assertEquals(2018, p.year)
        assertEquals(IdleSpec(1_800, 1_900), p.idle)
        assertEquals(StrokeType.FOUR_STROKE, p.strokeType)
    }

    @Test
    fun `model z katalogu urcuje typ motora bez ohladu na ulozeny prepinac`() {
        val p = BikeProfile.resolve(
            Settings(bikeModelId = "test-2t", bikeYear = 2019, strokeType = StrokeType.FOUR_STROKE),
            catalog,
        )
        assertEquals(StrokeType.TWO_STROKE, p.strokeType)
        assertEquals(IdleSpec(1_400, 1_500), p.idle)
    }

    @Test
    fun `rocnik bez cisla v manuali vrati prazdny volnobeh`() {
        val p = BikeProfile.resolve(Settings(bikeModelId = "test-2t", bikeYear = 2016), catalog)
        assertNull(p.idle)
    }

    @Test
    fun `nepokryty rocnik spadne na najblizsi pokryty`() {
        assertEquals(2020, BikeProfile.resolve(Settings(bikeModelId = "test-2t", bikeYear = 2024), catalog).year)
        assertEquals(2016, BikeProfile.resolve(Settings(bikeModelId = "test-2t", bikeYear = 2010), catalog).year)
    }

    @Test
    fun `neznamy model spadne na predvoleny`() {
        val p = BikeProfile.resolve(Settings(bikeModelId = "zmazany-model", bikeYear = 2019), catalog)
        assertEquals(Settings.DEFAULT_BIKE_ID, p.modelId)
        assertEquals(2018, p.year)
    }

    @Test
    fun `vlastne nastavenie pouzije zadany volnobeh a prepinac`() {
        val p = BikeProfile.resolve(
            Settings(
                bikeModelId = Settings.CUSTOM_BIKE,
                customIdleLow = 1_550,
                customIdleHigh = 1_650,
                strokeType = StrokeType.TWO_STROKE,
            ),
            catalog,
        )
        assertTrue(p.isCustom)
        assertEquals(IdleSpec(1_550, 1_650), p.idle)
        assertEquals(StrokeType.TWO_STROKE, p.strokeType)
        assertNull(p.runInLimit)
    }
}
