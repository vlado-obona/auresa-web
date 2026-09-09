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
import sk.rallysupport.otackomer.R
import sk.rallysupport.otackomer.data.Settings
import sk.rallysupport.otackomer.dsp.SignalStatus
import sk.rallysupport.otackomer.dsp.StrokeType
import sk.rallysupport.otackomer.ui.theme.OtackomerColors
import sk.rallysupport.otackomer.ui.theme.OtackomerPalette
import java.text.NumberFormat
import java.util.Locale

private val SlovakNumbers: NumberFormat = NumberFormat.getIntegerInstance(Locale.forLanguageTag("sk"))

/** Formát „1 850“. */
fun formatRpm(value: Int): String = SlovakNumbers.format(value.toLong())

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
        InfoText()
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
        val idleLeft = x(TachometerUiState.IDLE_BAND_LOW)
        val idleRight = x(TachometerUiState.IDLE_BAND_HIGH)
        drawRect(palette.good.copy(alpha = 0.32f), Offset(idleLeft, trackTop), Size(idleRight - idleLeft, trackHeight))
        drawLine(palette.good, Offset(idleLeft, trackTop), Offset(idleLeft, trackTop + trackHeight), 1.dp.toPx())
        drawLine(palette.good, Offset(idleRight, trackTop), Offset(idleRight, trackTop + trackHeight), 1.dp.toPx())

        // strop zábehu
        val limitX = x(TachometerUiState.RUN_IN_LIMIT)
        drawLine(palette.limit, Offset(limitX, trackTop), Offset(limitX, trackTop + trackHeight), 2.dp.toPx())

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
        val yHi = y(TachometerUiState.IDLE_BAND_HIGH.toFloat())
        val yLo = y(TachometerUiState.IDLE_BAND_LOW.toFloat())
        drawRect(palette.good.copy(alpha = 0.20f), Offset(0f, yHi), Size(w, yLo - yHi))

        // strop zábehu
        val yLimit = y(TachometerUiState.RUN_IN_LIMIT.toFloat())
        drawLine(palette.limit.copy(alpha = 0.6f), Offset(0f, yLimit), Offset(w, yLimit), 1.dp.toPx())

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
            value = state.clarity?.let { "${(it * 100).toInt()} %" } ?: "–",
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

        StrokeSelector(state.strokeType, onStrokeType)
    }
}

@Composable
private fun StrokeSelector(selected: StrokeType, onSelect: (StrokeType) -> Unit) {
    val palette = OtackomerColors.current
    val options = listOf(
        StrokeType.FOUR_STROKE to stringResource(R.string.stroke_four),
        StrokeType.TWO_STROKE to stringResource(R.string.stroke_two),
    )
    val groupDescription = stringResource(R.string.a11y_stroke_group)
    SingleChoiceSegmentedButtonRow(
        modifier = Modifier.height(56.dp).semantics { contentDescription = groupDescription },
    ) {
        options.forEachIndexed { index, (type, label) ->
            SegmentedButton(
                selected = type == selected,
                onClick = { onSelect(type) },
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
                modifier = Modifier.widthIn(min = 64.dp),
            ) {
                Text(label, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
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

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = palette.ink,
        unfocusedTextColor = palette.ink,
        focusedBorderColor = palette.live,
        unfocusedBorderColor = palette.line,
        cursorColor = palette.live,
        focusedContainerColor = palette.panel,
        unfocusedContainerColor = palette.panel,
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(stringResource(R.string.work_band_label), color = palette.muted, style = MaterialTheme.typography.bodyMedium)
        OutlinedTextField(
            value = lowText,
            onValueChange = { lowText = it.filter(Char::isDigit).take(5); commit(lowText, highText) },
            modifier = Modifier.width(96.dp),
            singleLine = true,
            textStyle = TabularNumbers.copy(textAlign = TextAlign.Center),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = fieldColors,
            shape = RoundedCornerShape(4.dp),
            label = { Text(stringResource(R.string.work_band_low), color = palette.muted) },
        )
        Text("–", color = palette.muted)
        OutlinedTextField(
            value = highText,
            onValueChange = { highText = it.filter(Char::isDigit).take(5); commit(lowText, highText) },
            modifier = Modifier.width(96.dp),
            singleLine = true,
            textStyle = TabularNumbers.copy(textAlign = TextAlign.Center),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = fieldColors,
            shape = RoundedCornerShape(4.dp),
            label = { Text(stringResource(R.string.work_band_high), color = palette.muted) },
        )
        Text(stringResource(R.string.unit_rpm), color = palette.muted, style = MaterialTheme.typography.bodyMedium)
    }
}

// ---------------------------------------------------------------- info

@Composable
private fun InfoText() {
    val palette = OtackomerColors.current
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        listOf(
            R.string.info_idle_band,
            R.string.info_work_band,
            R.string.info_placement,
            R.string.info_freeze,
        ).forEach { res ->
            Text(
                stringResource(res),
                color = palette.muted,
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = 20.sp,
            )
        }
    }
}
