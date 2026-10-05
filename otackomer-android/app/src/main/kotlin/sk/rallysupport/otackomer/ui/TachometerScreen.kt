package sk.rallysupport.otackomer.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import sk.rallysupport.otackomer.AppLanguage
import sk.rallysupport.otackomer.R
import sk.rallysupport.otackomer.bike.BikeProfile
import sk.rallysupport.otackomer.bike.IdleSpec
import sk.rallysupport.otackomer.data.Settings
import sk.rallysupport.otackomer.dsp.SignalStatus
import sk.rallysupport.otackomer.dsp.StrokeType
import sk.rallysupport.otackomer.ui.theme.OtackomerColors
import sk.rallysupport.otackomer.ui.theme.OtackomerPalette
import java.text.NumberFormat
import java.util.Locale

/** Formát podľa jazyka aplikácie: „1 850“ po slovensky, „1,850“ po anglicky. */
fun formatRpm(value: Int): String = NumberFormat.getIntegerInstance(Locale.getDefault()).format(value.toLong())

/** Tabulárne číslice – hodnota neskáče do šírky pri zmene číslic. */
private val TabularNumbers = TextStyle(fontFeatureSettings = "tnum")

@Composable
fun TachometerScreen(
    state: TachometerUiState,
    hasPermission: Boolean,
    permissionPermanentlyDenied: Boolean,
    onToggle: () -> Unit,
    onRequestPermission: () -> Unit,
    onStrokeType: (StrokeType) -> Unit,
    onWorkBand: (Int, Int) -> Unit,
    onOpenBikePicker: () -> Unit,
    onLanguage: (String) -> Unit,
) {
    val palette = OtackomerColors.current
    val insets = WindowInsets.safeDrawing.asPaddingValues()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .verticalScroll(rememberScrollState())
            .padding(insets)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        BikeCard(state.bike, onOpenBikePicker)

        Spacer(Modifier.height(14.dp))
        StatusRow(state)

        Spacer(Modifier.height(18.dp))
        RpmReadout(state)

        Spacer(Modifier.height(16.dp))
        RpmScale(state)

        Spacer(Modifier.height(16.dp))
        HistoryChart(state)

        Spacer(Modifier.height(12.dp))
        StatsRow(state)

        Spacer(Modifier.height(18.dp))
        Controls(state, onToggle, onStrokeType)

        if (!hasPermission) {
            Spacer(Modifier.height(16.dp))
            PermissionCard(permissionPermanentlyDenied, onRequestPermission)
        }

        Spacer(Modifier.height(18.dp))
        WorkBandSettings(state.workBandLow, state.workBandHigh, onWorkBand)

        Spacer(Modifier.height(18.dp))
        InfoText(state.bike)

        Spacer(Modifier.height(18.dp))
        LanguageSelector(onLanguage)
        Spacer(Modifier.height(8.dp))
    }
}

// ---------------------------------------------------------------- status

@Composable
private fun StatusRow(state: TachometerUiState) {
    val palette = OtackomerColors.current
    val message = when {
        state.errorRes != null -> stringResource(state.errorRes)
        !state.running -> if (state.rpm != null) stringResource(R.string.status_stopped) else stringResource(R.string.status_idle)
        else -> when (state.status) {
            SignalStatus.NO_DATA -> stringResource(R.string.status_listening)
            SignalStatus.WEAK_SIGNAL -> stringResource(R.string.status_weak)
            SignalStatus.TOO_NOISY -> stringResource(R.string.status_noisy)
            SignalStatus.OK ->
                if (state.inIdleBand) stringResource(R.string.status_idle_band) else stringResource(R.string.status_measuring)
        }
    }
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = message,
            color = if (state.errorRes != null) palette.limit else palette.muted,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f, fill = false),
        )
        LevelMeter(level = if (state.running) state.level else 0f, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun LevelMeter(level: Float, modifier: Modifier = Modifier) {
    val palette = OtackomerColors.current
    val fraction = (level * 6f).coerceIn(0f, 1f)
    val description = stringResource(R.string.a11y_level, (fraction * 100).toInt())
    Box(
        modifier = modifier
            .height(6.dp)
            .semantics { contentDescription = description }
            .background(palette.line, RoundedCornerShape(3.dp)),
    ) {
        Box(
            Modifier
                .fillMaxWidth(fraction)
                .height(6.dp)
                .background(if (fraction > 0.02f) palette.muted else Color.Transparent, RoundedCornerShape(3.dp)),
        )
    }
}

// ---------------------------------------------------------------- readout

@Composable
private fun RpmReadout(state: TachometerUiState) {
    val palette = OtackomerColors.current
    val color = when {
        !state.running || state.rpm == null || state.frozen -> palette.weak
        state.inIdleBand -> palette.good
        else -> palette.live
    }
    val text = state.rpm?.let(::formatRpm) ?: "––––"
    val description = state.rpm?.let { stringResource(R.string.a11y_rpm, it) } ?: stringResource(R.string.a11y_rpm_none)

    BoxWithConstraints(Modifier.fillMaxWidth()) {
        // Šírka „12 000“ pri Roboto ≈ 3,4 em → veľkosť písma z dostupnej šírky.
        val fontSize = with(LocalDensity.current) { (maxWidth * 0.27f).toSp() }
        Row(
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = description },
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                text = text,
                color = color,
                fontSize = fontSize,
                lineHeight = fontSize,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.04).em,
                style = TabularNumbers,
                maxLines = 1,
                softWrap = false,
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = stringResource(R.string.unit_rpm),
                color = palette.muted,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
    }
}

// ---------------------------------------------------------------- scale

@Composable
private fun RpmScale(state: TachometerUiState) {
    val palette = OtackomerColors.current
    val reducedMotion = rememberReducedMotion()
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = TextStyle(color = palette.muted, fontSize = 11.sp, fontFeatureSettings = "tnum")
    val scaleMax = TachometerUiState.SCALE_MAX.toFloat()

    val idleBand = state.bike.idle?.acceptBand
    val runInLimit = state.bike.runInLimit
    val showNeedle = state.running && state.rpm != null
    val target = ((state.rpm ?: 0).toFloat() / scaleMax).coerceIn(0f, 1f)
    val needlePos = if (reducedMotion) target else animateFloatAsState(target, tween(120), label = "needle").value

    val description = stringResource(R.string.a11y_scale)
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .semantics { contentDescription = description },
    ) {
        val w = size.width
        val trackTop = 16.dp.toPx()
        val trackHeight = 14.dp.toPx()
        fun x(rpm: Int) = (rpm / scaleMax).coerceIn(0f, 1f) * w

        // podklad stupnice
        drawRect(palette.panel, Offset(0f, trackTop), Size(w, trackHeight))

        // pracovné pásmo (svetlé)
        drawRect(
            palette.good.copy(alpha = 0.18f),
            Offset(x(state.workBandLow), trackTop),
            Size(x(state.workBandHigh) - x(state.workBandLow), trackHeight),
        )
        // predpísaný voľnobeh (sýte)
        idleBand?.let { band ->
            val idleLeft = x(band.first)
            val idleRight = x(band.last)
            drawRect(palette.good.copy(alpha = 0.32f), Offset(idleLeft, trackTop), Size(idleRight - idleLeft, trackHeight))
            drawLine(palette.good, Offset(idleLeft, trackTop), Offset(idleLeft, trackTop + trackHeight), 1.dp.toPx())
            drawLine(palette.good, Offset(idleRight, trackTop), Offset(idleRight, trackTop + trackHeight), 1.dp.toPx())
        }

        // strop zábehu, ak ho manuál uvádza
        runInLimit?.let { limit ->
            val limitX = x(limit)
            drawLine(palette.limit, Offset(limitX, trackTop), Offset(limitX, trackTop + trackHeight), 2.dp.toPx())
        }

        // dieliky
        val labelTop = trackTop + trackHeight + 6.dp.toPx()
        for (tick in 0..TachometerUiState.SCALE_MAX step 1_000) {
            val label = if (tick == 0) "0" else "${tick / 1_000}k"
            val measured = textMeasurer.measure(label, labelStyle)
            val tx = (x(tick) - measured.size.width / 2f).coerceIn(0f, w - measured.size.width)
            drawText(measured, topLeft = Offset(tx, labelTop))
        }

        // ukazovateľ
        if (showNeedle) {
            val nx = needlePos * w
            drawLine(
                color = palette.live,
                start = Offset(nx, trackTop - 8.dp.toPx()),
                end = Offset(nx, trackTop + trackHeight + 8.dp.toPx()),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }
    }
}

// ---------------------------------------------------------------- chart

@Composable
private fun HistoryChart(state: TachometerUiState) {
    val palette = OtackomerColors.current
    val scaleMax = TachometerUiState.SCALE_MAX.toFloat()
    val description = stringResource(R.string.a11y_chart)
    val idleBand = state.bike.idle?.acceptBand
    val runInLimit = state.bike.runInLimit

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .background(palette.panel, RoundedCornerShape(4.dp))
            .semantics { contentDescription = description },
    ) {
        val w = size.width
        val h = size.height
        fun y(rpm: Float) = h - (rpm.coerceIn(0f, scaleMax) / scaleMax) * h

        // pásmo voľnobehu
        idleBand?.let { band ->
            val yHi = y(band.last.toFloat())
            val yLo = y(band.first.toFloat())
            drawRect(palette.good.copy(alpha = 0.20f), Offset(0f, yHi), Size(w, yLo - yHi))
        }

        // strop zábehu
        runInLimit?.let { limit ->
            val yLimit = y(limit.toFloat())
            drawLine(palette.limit.copy(alpha = 0.6f), Offset(0f, yLimit), Offset(w, yLimit), 1.dp.toPx())
        }

        val history = state.history
        if (history.size < 2) return@Canvas
        val n = TachometerUiState.HISTORY_LENGTH
        val startIndex = n - history.size // najnovšia hodnota je vpravo
        val path = Path()
        var open = false
        history.forEachIndexed { i, value ->
            val px = ((startIndex + i).toFloat() / (n - 1)) * w
            if (value.isNaN()) {
                open = false
            } else {
                val py = y(value)
                if (open) path.lineTo(px, py) else path.moveTo(px, py)
                open = true
            }
        }
        drawPath(
            path = path,
            color = palette.live,
            style = Stroke(width = 2.dp.toPx(), join = StrokeJoin.Round, cap = StrokeCap.Round),
        )
    }
}

// ---------------------------------------------------------------- stats

@Composable
private fun StatsRow(state: TachometerUiState) {
    val palette = OtackomerColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Stat(
            label = stringResource(R.string.stat_clarity),
            value = state.clarity?.let { stringResource(R.string.stat_percent, (it * 100).toInt()) } ?: "–",
            palette = palette,
        )
        Stat(
            label = stringResource(R.string.stat_peak),
            value = state.peakRpm?.let(::formatRpm) ?: "–",
            palette = palette,
        )
        state.audioSource?.let { source ->
            Stat(
                label = stringResource(R.string.stat_source),
                value = if (state.audioUnprocessed) source else stringResource(R.string.stat_source_fallback, source),
                palette = palette,
            )
        }
    }
}

@Composable
private fun Stat(label: String, value: String, palette: OtackomerPalette) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, color = palette.muted, style = MaterialTheme.typography.bodyMedium)
        Text(
            value,
            color = palette.ink,
            style = MaterialTheme.typography.bodyMedium.merge(TabularNumbers),
            fontWeight = FontWeight.SemiBold,
        )
    }
}

// ---------------------------------------------------------------- controls

@Composable
private fun Controls(
    state: TachometerUiState,
    onToggle: () -> Unit,
    onStrokeType: (StrokeType) -> Unit,
) {
    val palette = OtackomerColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (state.running) {
            OutlinedButton(
                onClick = onToggle,
                modifier = Modifier.weight(1f).height(64.dp),
                shape = RoundedCornerShape(6.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = palette.ink),
            ) {
                Text(stringResource(R.string.button_stop), fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            }
        } else {
            Button(
                onClick = onToggle,
                modifier = Modifier.weight(1f).height(64.dp),
                shape = RoundedCornerShape(6.dp),
                colors = ButtonDefaults.buttonColors(containerColor = palette.live, contentColor = palette.onLive),
            ) {
                Text(stringResource(R.string.button_start), fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        StrokeSelector(state.strokeType, enabled = state.bike.isCustom, onSelect = onStrokeType)
    }
}

@Composable
internal fun StrokeSelector(selected: StrokeType, enabled: Boolean, onSelect: (StrokeType) -> Unit) {
    val palette = OtackomerColors.current
    val options = listOf(
        StrokeType.FOUR_STROKE to stringResource(R.string.stroke_four),
        StrokeType.TWO_STROKE to stringResource(R.string.stroke_two),
    )
    val groupDescription = if (enabled) {
        stringResource(R.string.a11y_stroke_group)
    } else {
        stringResource(R.string.a11y_stroke_locked)
    }
    SingleChoiceSegmentedButtonRow(
        modifier = Modifier.height(56.dp).semantics { contentDescription = groupDescription },
    ) {
        options.forEachIndexed { index, (type, label) ->
            SegmentedButton(
                selected = type == selected,
                onClick = { onSelect(type) },
                enabled = enabled || type == selected,
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size, baseShape = RoundedCornerShape(6.dp)),
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = palette.line,
                    activeContentColor = palette.ink,
                    inactiveContainerColor = palette.panel,
                    inactiveContentColor = palette.muted,
                    activeBorderColor = palette.line,
                    inactiveBorderColor = palette.line,
                    disabledActiveContainerColor = palette.line,
                    disabledActiveContentColor = palette.ink,
                    disabledActiveBorderColor = palette.line,
                    disabledInactiveContainerColor = palette.panel,
                    disabledInactiveContentColor = palette.weak,
                    disabledInactiveBorderColor = palette.line,
                ),
                icon = {},
                modifier = Modifier.widthIn(min = 64.dp),
            ) {
                Text(label, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ---------------------------------------------------------------- jazyk

/** Prepínač jazyka aplikácie; názvy jazykov sú vždy v ich vlastnom jazyku. */
@Composable
private fun LanguageSelector(onSelect: (String) -> Unit) {
    val palette = OtackomerColors.current
    val current = AppLanguage.current(LocalConfiguration.current)
    val options = listOf("sk" to "Slovenčina", "en" to "English")
    Column {
        Text(
            stringResource(R.string.language_label),
            color = palette.muted,
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(6.dp))
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth().height(48.dp)) {
            options.forEachIndexed { index, (tag, label) ->
                SegmentedButton(
                    selected = tag == current,
                    onClick = { if (tag != current) onSelect(tag) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size, baseShape = RoundedCornerShape(6.dp)),
                    colors = SegmentedButtonDefaults.colors(
                        activeContainerColor = palette.line,
                        activeContentColor = palette.ink,
                        inactiveContainerColor = palette.panel,
                        inactiveContentColor = palette.muted,
                        activeBorderColor = palette.line,
                        inactiveBorderColor = palette.line,
                    ),
                    icon = {},
                ) {
                    Text(label, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

// ---------------------------------------------------------------- permission

@Composable
private fun PermissionCard(permanentlyDenied: Boolean, onRequest: () -> Unit) {
    val palette = OtackomerColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(palette.panel, RoundedCornerShape(6.dp))
            .border(1.dp, palette.line, RoundedCornerShape(6.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            stringResource(R.string.permission_title),
            color = palette.ink,
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            stringResource(R.string.permission_rationale),
            color = palette.muted,
            style = MaterialTheme.typography.bodyMedium,
        )
        OutlinedButton(
            onClick = onRequest,
            modifier = Modifier.height(56.dp),
            shape = RoundedCornerShape(6.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = palette.ink),
        ) {
            Text(
                if (permanentlyDenied) stringResource(R.string.permission_open_settings)
                else stringResource(R.string.permission_grant),
            )
        }
    }
}

// ---------------------------------------------------------------- settings

@Composable
private fun WorkBandSettings(low: Int, high: Int, onWorkBand: (Int, Int) -> Unit) {
    val palette = OtackomerColors.current
    var lowText by remember(low) { mutableStateOf(low.toString()) }
    var highText by remember(high) { mutableStateOf(high.toString()) }

    fun commit(lo: String, hi: String) {
        val l = lo.toIntOrNull() ?: return
        val h = hi.toIntOrNull() ?: return
        val valid = l in Settings.WORK_BAND_MIN..Settings.WORK_BAND_MAX &&
            h in Settings.WORK_BAND_MIN..Settings.WORK_BAND_MAX && l < h
        if (valid) onWorkBand(l, h)
    }

    val fieldColors = rpmFieldColors()

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(stringResource(R.string.work_band_label), color = palette.muted, style = MaterialTheme.typography.bodyMedium)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            RpmField(
                value = lowText,
                label = stringResource(R.string.work_band_low),
                colors = fieldColors,
                onValueChange = { lowText = it; commit(lowText, highText) },
            )
            Text("–", color = palette.muted)
            RpmField(
                value = highText,
                label = stringResource(R.string.work_band_high),
                colors = fieldColors,
                onValueChange = { highText = it; commit(lowText, highText) },
            )
        }
    }
}

/** Číselné pole pre otáčky (len číslice, max. 5 znakov). */
@Composable
internal fun RpmField(
    value: String,
    label: String,
    colors: androidx.compose.material3.TextFieldColors,
    onValueChange: (String) -> Unit,
) {
    val palette = OtackomerColors.current
    OutlinedTextField(
        value = value,
        onValueChange = { onValueChange(it.filter(Char::isDigit).take(5)) },
        modifier = Modifier.width(112.dp),
        singleLine = true,
        textStyle = TabularNumbers.copy(textAlign = TextAlign.Center),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        colors = colors,
        shape = RoundedCornerShape(4.dp),
        label = { Text(label, color = palette.muted) },
    )
}

/** Farby číselných polí v palete aplikácie. */
@Composable
internal fun rpmFieldColors(): androidx.compose.material3.TextFieldColors {
    val palette = OtackomerColors.current
    return OutlinedTextFieldDefaults.colors(
        focusedTextColor = palette.ink,
        unfocusedTextColor = palette.ink,
        focusedBorderColor = palette.live,
        unfocusedBorderColor = palette.line,
        cursorColor = palette.live,
        focusedContainerColor = palette.panel,
        unfocusedContainerColor = palette.panel,
    )
}

// ---------------------------------------------------------------- info

@Composable
private fun InfoText(bike: BikeProfile) {
    val palette = OtackomerColors.current
    val paragraphs = buildList {
        val idle = bike.idle
        val bikeName = bikeDisplayName(bike)
        when {
            idle == null -> add(stringResource(R.string.info_idle_unknown, bikeName))
            bike.isCustom -> add(stringResource(R.string.info_idle_custom, formatIdle(idle)))
            else -> add(stringResource(R.string.info_idle_model, formatIdle(idle), bikeName))
        }
        if (idle != null && idle.isSingleValue) {
            add(stringResource(R.string.info_idle_single, IdleSpec.SINGLE_VALUE_TOLERANCE))
        }
        bike.runInLimit?.let { add(stringResource(R.string.info_run_in, formatRpm(it))) }
        add(stringResource(R.string.info_work_band))
        add(stringResource(R.string.info_placement))
        add(stringResource(R.string.info_freeze))
        bike.source?.let { source ->
            add(
                if (bike.verified) stringResource(R.string.info_source, source)
                else stringResource(R.string.info_source_unverified, source),
            )
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        paragraphs.forEach { text ->
            Text(
                text,
                color = palette.muted,
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = 20.sp,
            )
        }
    }
}
