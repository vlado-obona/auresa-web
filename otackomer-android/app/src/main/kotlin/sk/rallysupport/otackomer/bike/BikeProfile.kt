package sk.rallysupport.otackomer.bike

import sk.rallysupport.otackomer.data.Settings
import sk.rallysupport.otackomer.dsp.StrokeType

/**
 * Motocykel, podľa ktorého sa práve meria: buď konkrétny model a ročník
 * z [MotorcycleCatalog], alebo vlastné nastavenie používateľa.
 */
data class BikeProfile(
    /** `null` pri vlastnom nastavení. */
    val modelId: String?,
    val year: Int?,
    /** Napr. „KTM 500 EXC‑F“ alebo `null` pri vlastnom nastavení. */
    val name: String?,
    val strokeType: StrokeType,
    val displacementCc: Double?,
    val idle: IdleSpec?,
    val runInLimit: Int?,
    val source: String?,
    val verified: Boolean = true,
) {
    val isCustom: Boolean get() = modelId == null

    companion object {
        /**
         * Zostaví profil z uložených nastavení. Neznámy model (napr. po
         * úprave katalógu) padne na predvolený motocykel, neznámy ročník na
         * najbližší pokrytý ročník daného modelu.
         */
        fun resolve(settings: Settings, catalog: List<BikeModel> = MotorcycleCatalog.models): BikeProfile {
            if (settings.bikeModelId == Settings.CUSTOM_BIKE) {
                return BikeProfile(
                    modelId = null,
                    year = null,
                    name = null,
                    strokeType = settings.strokeType,
                    displacementCc = null,
                    idle = IdleSpec(settings.customIdleLow, settings.customIdleHigh),
                    runInLimit = null,
                    source = null,
                )
            }
            val model = catalog.firstOrNull { it.id == settings.bikeModelId }
                ?: catalog.firstOrNull { it.id == Settings.DEFAULT_BIKE_ID }
                ?: catalog.first()
            val wantedYear = settings.bikeYear
                ?.takeIf { model.id == settings.bikeModelId }
                ?: if (model.id == Settings.DEFAULT_BIKE_ID) Settings.DEFAULT_BIKE_YEAR else model.years.first()
            val year = model.years.minBy { kotlin.math.abs(it - wantedYear) * 2 + if (it < wantedYear) 1 else 0 }
            val variant = requireNotNull(model.variantFor(year))
            return BikeProfile(
                modelId = model.id,
                year = year,
                name = model.fullName,
                strokeType = variant.strokeType,
                displacementCc = variant.displacementCc,
                idle = variant.idle,
                runInLimit = variant.runInLimit,
                source = variant.source,
                verified = variant.verified,
            )
        }
    }
}
