package sk.rallysupport.otackomer.bike

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MotorcycleCatalogTest {

    private val models = MotorcycleCatalog.models

    @Test
    fun `id modelov su unikatne a v tvare znacka-model`() {
        assertEquals(models.size, models.map { it.id }.toSet().size)
        models.forEach { assertTrue(it.id, it.id.matches(Regex("[a-z0-9]+(-[a-z0-9]+)+"))) }
    }

    @Test
    fun `rocniky jedneho modelu sa neprekryvaju`() {
        models.forEach { model ->
            val all = model.variants.flatMap { it.yearFrom..it.yearTo }
            assertEquals("${model.id} má prekrývajúce sa roky", all.size, all.toSet().size)
        }
    }

    @Test
    fun `hodnoty su v rozumnom rozsahu`() {
        models.flatMap { m -> m.variants.map { m to it } }.forEach { (model, v) ->
            assertTrue(model.id, v.yearFrom in 1990..2035 && v.yearTo in 1990..2035)
            v.idle?.let { assertTrue("${model.id} voľnobeh ${it.low}", it.low in 800..3_000 && it.high <= 3_000) }
            v.runInLimit?.let { assertTrue("${model.id} zábeh $it", it in 3_000..12_000) }
            v.displacementCc?.let { assertTrue("${model.id} objem $it", it in 49.0..800.0) }
            assertTrue(model.id, v.source.isNotBlank())
        }
    }

    @Test
    fun `kazdy rocnik ma prave jeden variant`() {
        models.forEach { model ->
            model.years.forEach { year -> assertNotNull("${model.id} $year", model.variantFor(year)) }
            assertEquals(model.years.sortedDescending(), model.years)
        }
    }

    @Test
    fun `predvoleny motocykel je v katalogu`() {
        val ktm = MotorcycleCatalog.find("ktm-500-exc-f")
        assertNotNull(ktm)
        val v = ktm!!.variantFor(2018)!!
        assertEquals(IdleSpec(1_800, 1_900), v.idle)
        assertEquals(7_000, v.runInLimit)
        assertTrue(v.verified)
    }

    @Test
    fun `jedna hodnota bez tolerancie dostane pasmo plus minus 50`() {
        val single = IdleSpec(1_700, 1_700)
        assertTrue(single.isSingleValue)
        assertEquals(1_650..1_750, single.acceptBand)
        assertFalse(IdleSpec(1_800, 1_900).isSingleValue)
        assertEquals(1_800..1_900, IdleSpec(1_800, 1_900).acceptBand)
    }
}
