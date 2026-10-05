package sk.rallysupport.otackomer.bike

import sk.rallysupport.otackomer.dsp.StrokeType.FOUR_STROKE
import sk.rallysupport.otackomer.dsp.StrokeType.TWO_STROKE

/**
 * Katalóg jednovalcových motocyklov s predpísaným voľnobehom.
 *
 * Pravidlá pre údaje:
 *  - hodnota sa sem dostane len s menom manuálu, z ktorého pochádza;
 *  - `verified = true` iba vtedy, keď ju niekto videl priamo v manuáli;
 *  - ročníky sa nedopĺňajú odhadom – ak je overený len jeden rok, je tu len ten rok;
 *  - `source` je názov manuálu po anglicky, aplikácia ho ukazuje v oboch jazykoch.
 *
 * Nový model = nový [BikeModel] s unikátnym [BikeModel.id] (ukladá sa do
 * nastavení, preto sa id existujúcich modelov nemenia).
 */
object MotorcycleCatalog {

    val models: List<BikeModel> = listOf(
        // ---------------------------------------------------------------- KTM
        BikeModel(
            id = "ktm-350-exc-f",
            brand = "KTM",
            name = "350 EXC‑F",
            variants = listOf(
                BikeVariant(
                    yearFrom = 2018, yearTo = 2022,
                    strokeType = FOUR_STROKE,
                    displacementCc = 349.7,
                    idle = IdleSpec(1_950, 2_050),
                    source = "KTM Owner's manual 350 EXC‑F 2018 (3213648en) & 2022",
                    verified = false,
                ),
            ),
        ),
        BikeModel(
            id = "ktm-450-exc-f",
            brand = "KTM",
            name = "450 EXC‑F",
            variants = listOf(
                BikeVariant(
                    yearFrom = 2018, yearTo = 2022,
                    strokeType = FOUR_STROKE,
                    displacementCc = null,
                    idle = IdleSpec(1_800, 1_900),
                    runInLimit = 7_000,
                    source = "KTM Owner's manual 450/500 EXC‑F 2018 (3213650en) – 2022",
                    verified = false,
                ),
            ),
        ),
        BikeModel(
            id = "ktm-500-exc-f",
            brand = "KTM",
            name = "500 EXC‑F",
            variants = listOf(
                BikeVariant(
                    yearFrom = 2018, yearTo = 2018,
                    strokeType = FOUR_STROKE,
                    displacementCc = null,
                    idle = IdleSpec(1_800, 1_900),
                    runInLimit = 7_000,
                    source = "KTM Owner's manual 2018 450/500 EXC‑F (3213650en)",
                ),
                BikeVariant(
                    yearFrom = 2019, yearTo = 2024,
                    strokeType = FOUR_STROKE,
                    displacementCc = null,
                    idle = IdleSpec(1_800, 1_900),
                    runInLimit = 7_000,
                    source = "KTM Owner's manual 500 EXC‑F 2019 (3213883) – 2024",
                    verified = false,
                ),
            ),
        ),
        BikeModel(
            id = "ktm-450-sx-f",
            brand = "KTM",
            name = "450 SX‑F",
            variants = listOf(
                BikeVariant(
                    yearFrom = 2020, yearTo = 2021,
                    strokeType = FOUR_STROKE,
                    displacementCc = null,
                    idle = IdleSpec(2_250, 2_350),
                    source = "KTM Owner's manual 450 SX‑F 2020 (3214005en) & 2021",
                    verified = false,
                ),
            ),
        ),
        BikeModel(
            id = "ktm-250-exc-tpi",
            brand = "KTM",
            name = "250 EXC TPI",
            variants = listOf(
                BikeVariant(
                    yearFrom = 2019, yearTo = 2022,
                    strokeType = TWO_STROKE,
                    displacementCc = null,
                    idle = IdleSpec(1_400, 1_500),
                    source = "KTM Owner's manual 250/300 EXC TPI 2019 (3213853) – 2022",
                    verified = false,
                ),
            ),
        ),
        BikeModel(
            id = "ktm-300-exc-tpi",
            brand = "KTM",
            name = "300 EXC TPI",
            variants = listOf(
                BikeVariant(
                    yearFrom = 2019, yearTo = 2022,
                    strokeType = TWO_STROKE,
                    displacementCc = 293.2,
                    idle = IdleSpec(1_400, 1_500),
                    source = "KTM Owner's manual 250/300 EXC TPI 2019 (3213853) – 2022",
                    verified = false,
                ),
            ),
        ),

        // ---------------------------------------------------------- Husqvarna
        BikeModel(
            id = "husqvarna-701-enduro",
            brand = "Husqvarna",
            name = "701 Enduro",
            variants = listOf(
                BikeVariant(
                    yearFrom = 2017, yearTo = 2017,
                    strokeType = FOUR_STROKE,
                    displacementCc = 692.7,
                    idle = IdleSpec(1_600, 1_700),
                    runInLimit = 6_000,
                    source = "Husqvarna 701 Enduro 2017 Owner's manual",
                    verified = false,
                ),
            ),
        ),
        BikeModel(
            id = "husqvarna-fc-250",
            brand = "Husqvarna",
            name = "FC 250",
            variants = listOf(
                BikeVariant(
                    yearFrom = 2022, yearTo = 2022,
                    strokeType = FOUR_STROKE,
                    displacementCc = 249.91,
                    idle = IdleSpec(2_250, 2_350),
                    source = "Husqvarna FC 250 2022 Owner's manual",
                    verified = false,
                ),
            ),
        ),

        // -------------------------------------------------------------- GASGAS
        BikeModel(
            id = "gasgas-ec-350f",
            brand = "GASGAS",
            name = "EC 350F",
            variants = listOf(
                BikeVariant(
                    yearFrom = 2021, yearTo = 2021,
                    strokeType = FOUR_STROKE,
                    displacementCc = 349.7,
                    idle = IdleSpec(1_950, 2_050),
                    source = "GASGAS EC 350F Owner's manual (2021)",
                    verified = false,
                ),
            ),
        ),
        BikeModel(
            id = "gasgas-ec-250",
            brand = "GASGAS",
            name = "EC 250",
            variants = listOf(
                BikeVariant(
                    yearFrom = 2021, yearTo = 2021,
                    strokeType = TWO_STROKE,
                    displacementCc = null,
                    idle = IdleSpec(1_400, 1_500),
                    source = "GASGAS Owner's manual 2021 EC 250 / EC 300 (3215011en)",
                    verified = false,
                ),
            ),
        ),
        BikeModel(
            id = "gasgas-ec-300",
            brand = "GASGAS",
            name = "EC 300",
            variants = listOf(
                BikeVariant(
                    yearFrom = 2021, yearTo = 2021,
                    strokeType = TWO_STROKE,
                    displacementCc = 293.15,
                    idle = IdleSpec(1_400, 1_500),
                    source = "GASGAS Owner's manual 2021 EC 250 / EC 300 (3215011en)",
                    verified = false,
                ),
            ),
        ),

        // ---------------------------------------------------------------- Beta
        BikeModel(
            id = "beta-rr-250-2t",
            brand = "Beta",
            name = "RR 2T 250",
            variants = listOf(
                BikeVariant(
                    yearFrom = 2019, yearTo = 2019,
                    strokeType = TWO_STROKE,
                    displacementCc = 249.0,
                    idle = IdleSpec(1_700, 1_800),
                    source = "Beta Owner's manual 2019 RR 250/300 2T (Standard & Race)",
                    verified = false,
                ),
            ),
        ),
        BikeModel(
            id = "beta-rr-300-2t",
            brand = "Beta",
            name = "RR 2T 300",
            variants = listOf(
                BikeVariant(
                    yearFrom = 2019, yearTo = 2019,
                    strokeType = TWO_STROKE,
                    displacementCc = 293.1,
                    idle = IdleSpec(1_700, 1_800),
                    source = "Beta Owner's manual 2019 RR 250/300 2T (Standard & Race)",
                    verified = false,
                ),
            ),
        ),

        // -------------------------------------------------------------- Sherco
        BikeModel(
            id = "sherco-se-250",
            brand = "Sherco",
            name = "SE 250",
            variants = listOf(
                BikeVariant(
                    yearFrom = 2021, yearTo = 2021,
                    strokeType = TWO_STROKE,
                    displacementCc = 249.32,
                    idle = IdleSpec(2_100, 2_300),
                    source = "Sherco Owner's manual 2021 250/300 SE Racing/Factory (2200 ± 100)",
                    verified = false,
                ),
            ),
        ),
        BikeModel(
            id = "sherco-se-300",
            brand = "Sherco",
            name = "SE 300",
            variants = listOf(
                BikeVariant(
                    yearFrom = 2021, yearTo = 2021,
                    strokeType = TWO_STROKE,
                    displacementCc = 293.14,
                    idle = IdleSpec(2_100, 2_300),
                    source = "Sherco Owner's manual 2021 250/300 SE Racing/Factory (2200 ± 100)",
                    verified = false,
                ),
            ),
        ),

        // ---------------------------------------------------------- TM Racing
        BikeModel(
            id = "tm-en-250-fi",
            brand = "TM Racing",
            name = "EN 250 Fi 4T",
            variants = listOf(
                BikeVariant(
                    yearFrom = 2020, yearTo = 2020,
                    strokeType = FOUR_STROKE,
                    displacementCc = 249.5,
                    idle = IdleSpec(2_500, 2_800),
                    source = "TM Racing 4-stroke Use and maintenance manual ed. 02/2020",
                    verified = false,
                ),
            ),
        ),
        BikeModel(
            id = "tm-en-300-fi",
            brand = "TM Racing",
            name = "EN 300 Fi 4T",
            variants = listOf(
                BikeVariant(
                    yearFrom = 2020, yearTo = 2020,
                    strokeType = FOUR_STROKE,
                    displacementCc = 292.3,
                    idle = IdleSpec(2_500, 2_800),
                    source = "TM Racing 4-stroke Use and maintenance manual ed. 02/2020",
                    verified = false,
                ),
            ),
        ),

        // -------------------------------------------------------------- Fantic
        BikeModel(
            id = "fantic-xef-250",
            brand = "Fantic",
            name = "XEF 250",
            variants = listOf(
                BikeVariant(
                    yearFrom = 2023, yearTo = 2024,
                    strokeType = FOUR_STROKE,
                    displacementCc = null,
                    idle = IdleSpec(1_900, 2_100),
                    source = "Fantic XEF 250/450 Use and maintenance manual 2023/2024",
                    verified = false,
                ),
            ),
        ),
        BikeModel(
            id = "fantic-xef-450",
            brand = "Fantic",
            name = "XEF 450",
            variants = listOf(
                BikeVariant(
                    yearFrom = 2023, yearTo = 2024,
                    strokeType = FOUR_STROKE,
                    displacementCc = null,
                    idle = IdleSpec(1_900, 2_100),
                    source = "Fantic XEF 250/450 Use and maintenance manual 2023/2024",
                    verified = false,
                ),
            ),
        ),

        // --------------------------------------------------------------- Honda
        BikeModel(
            id = "honda-crf250r",
            brand = "Honda",
            name = "CRF250R",
            variants = listOf(
                BikeVariant(
                    yearFrom = 2018, yearTo = 2022,
                    strokeType = FOUR_STROKE,
                    displacementCc = null,
                    idle = IdleSpec(1_900, 2_100),
                    source = "Honda CRF250R Owner's manual 2018 & 2022 CRF250R/RX (2000 ± 100)",
                    verified = false,
                ),
            ),
        ),
        BikeModel(
            id = "honda-crf450l",
            brand = "Honda",
            name = "CRF450L",
            variants = listOf(
                BikeVariant(
                    yearFrom = 2019, yearTo = 2019,
                    strokeType = FOUR_STROKE,
                    displacementCc = null,
                    idle = IdleSpec(1_700, 1_900),
                    source = "Honda CRF450L 2019 Owner's manual (1800 ± 100)",
                    verified = false,
                ),
            ),
        ),
        BikeModel(
            id = "honda-crf450rl",
            brand = "Honda",
            name = "CRF450RL",
            variants = listOf(
                BikeVariant(
                    yearFrom = 2021, yearTo = 2024,
                    strokeType = FOUR_STROKE,
                    displacementCc = null,
                    idle = IdleSpec(1_700, 1_900),
                    source = "Honda CRF450RL Owner's manual 2021 & 2024 (1800 ± 100)",
                    verified = false,
                ),
            ),
        ),
        BikeModel(
            id = "honda-xr650l",
            brand = "Honda",
            name = "XR650L",
            variants = listOf(
                BikeVariant(
                    yearFrom = 2018, yearTo = 2019,
                    strokeType = FOUR_STROKE,
                    displacementCc = null,
                    idle = IdleSpec(1_200, 1_400),
                    source = "Honda XR650L Owner's manual 2018 & 2019 (1300 ± 100)",
                    verified = false,
                ),
            ),
        ),

        // -------------------------------------------------------------- Yamaha
        BikeModel(
            id = "yamaha-yz250f",
            brand = "Yamaha",
            name = "YZ250F",
            variants = listOf(
                BikeVariant(
                    yearFrom = 2024, yearTo = 2024,
                    strokeType = FOUR_STROKE,
                    displacementCc = null,
                    idle = IdleSpec(1_900, 2_100),
                    source = "Yamaha YZ250F 2024 Owner's service manual (engine warm, coolant 70–80 °C)",
                    verified = false,
                ),
            ),
        ),
        BikeModel(
            id = "yamaha-yz450f",
            brand = "Yamaha",
            name = "YZ450F",
            variants = listOf(
                BikeVariant(
                    yearFrom = 2020, yearTo = 2020,
                    strokeType = FOUR_STROKE,
                    displacementCc = null,
                    idle = IdleSpec(1_900, 2_100),
                    source = "Yamaha YZ450F 2020 Owner's service manual",
                    verified = false,
                ),
            ),
        ),
        BikeModel(
            id = "yamaha-yz450fx",
            brand = "Yamaha",
            name = "YZ450FX",
            variants = listOf(
                BikeVariant(
                    yearFrom = 2019, yearTo = 2019,
                    strokeType = FOUR_STROKE,
                    displacementCc = null,
                    idle = IdleSpec(1_900, 2_100),
                    source = "Yamaha YZ450FX 2019 Owner's service manual",
                    verified = false,
                ),
            ),
        ),
        BikeModel(
            id = "yamaha-wr250r",
            brand = "Yamaha",
            name = "WR250R",
            variants = listOf(
                BikeVariant(
                    yearFrom = 2020, yearTo = 2020,
                    strokeType = FOUR_STROKE,
                    displacementCc = null,
                    idle = IdleSpec(1_450, 1_650),
                    source = "Yamaha WR250R 2020 Owner's manual",
                    verified = false,
                ),
            ),
        ),

        // ------------------------------------------------------------ Kawasaki
        BikeModel(
            id = "kawasaki-kx450f",
            brand = "Kawasaki",
            name = "KX450F",
            variants = listOf(
                BikeVariant(
                    yearFrom = 2017, yearTo = 2017,
                    strokeType = FOUR_STROKE,
                    displacementCc = null,
                    idle = IdleSpec(1_950, 2_050),
                    source = "Kawasaki KX450F 2017 Owner's manual",
                    verified = false,
                ),
            ),
        ),
    )

    fun find(id: String?): BikeModel? = models.firstOrNull { it.id == id }
}
