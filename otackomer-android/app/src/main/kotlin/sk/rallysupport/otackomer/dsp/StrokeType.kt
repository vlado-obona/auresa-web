package sk.rallysupport.otackomer.dsp

/**
 * Typ motora. Určuje, koľko otáčok kľukového hriadeľa pripadá na jeden zážih –
 * štvortakt zapaľuje raz za dve otáčky, dvojtakt pri každej.
 */
enum class StrokeType(val revsPerFiring: Int) {
    FOUR_STROKE(2),
    TWO_STROKE(1),
}
