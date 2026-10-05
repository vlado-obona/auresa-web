package sk.rallysupport.otackomer.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import sk.rallysupport.otackomer.R
import sk.rallysupport.otackomer.bike.BikeModel
import sk.rallysupport.otackomer.bike.BikeProfile
import sk.rallysupport.otackomer.bike.BikeVariant
import sk.rallysupport.otackomer.bike.IdleSpec
import sk.rallysupport.otackomer.bike.MotorcycleCatalog
import sk.rallysupport.otackomer.data.Settings
import sk.rallysupport.otackomer.dsp.StrokeType
import sk.rallysupport.otackomer.ui.theme.OtackomerColors
import java.text.NumberFormat
import java.util.Locale

// ---------------------------------------------------------------- formátovanie

/** „1 800 – 1 900“ alebo „1 700“ pri jednej hodnote. */
fun formatIdle(idle: IdleSpec): String =
    if (idle.isSingleValue) formatRpm(idle.low) else "${formatRpm(idle.low)} – ${formatRpm(idle.high)}"

fun formatCc(cc: Double): String =
    NumberFormat.getNumberInstance(Locale.getDefault()).apply { maximumFractionDigits = 1 }.format(cc)

/** „KTM 500 EXC‑F, ročník 2018“ alebo „Vlastný motocykel“ (podľa jazyka). */
@Composable
fun bikeDisplayName(bike: BikeProfile): String =
    if (bike.isCustom || bike.name == null || bike.year == null) {
        stringResource(R.string.bike_custom_name)
    } else {
        stringResource(R.string.bike_name_year, bike.name, bike.year)
    }

@Composable
private fun idleText(idle: IdleSpec?): String =
    if (idle == null) stringResource(R.string.bike_idle_unknown) else stringResource(R.string.bike_idle, formatIdle(idle))

@Composable
private fun strokeLabel(type: StrokeType): String = when (type) {
    StrokeType.FOUR_STROKE -> stringResource(R.string.stroke_four)
    StrokeType.TWO_STROKE -> stringResource(R.string.stroke_two)
}

// ---------------------------------------------------------------- karta na hlavnej obrazovke

/** Vybraný motocykel s predpísaným voľnobehom; ťuknutím sa otvorí výber. */
@Composable
fun BikeCard(bike: BikeProfile, onClick: () -> Unit) {
    val palette = OtackomerColors.current
    val shape = RoundedCornerShape(6.dp)
    val detail = buildList {
        add(idleText(bike.idle))
        add(strokeLabel(bike.strokeType))
        if (!bike.verified) add(stringResource(R.string.bike_unverified))
    }.joinToString(" · ")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clip(shape)
            .background(palette.panel)
            .border(1.dp, palette.line, shape)
            .clickable(onClickLabel = stringResource(R.string.bike_change), role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                bikeDisplayName(bike),
                color = palette.ink,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(detail, color = palette.muted, style = MaterialTheme.typography.bodyMedium)
        }
        Text(
            stringResource(R.string.bike_change),
            color = palette.live,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

// ---------------------------------------------------------------- výber motocykla

private sealed interface PickerStep {
    data object Brand : PickerStep
    data class Model(val brand: String) : PickerStep
    data class Year(val model: BikeModel) : PickerStep
    data object Custom : PickerStep
}

/**
 * Celoobrazovkový výber: značka → model → ročník, alebo vlastné nastavenie.
 * Systémové „späť“ vráti o krok, z prvého kroku výber zavrie.
 */
@Composable
fun BikePickerScreen(
    current: BikeProfile,
    customIdleLow: Int,
    customIdleHigh: Int,
    customStroke: StrokeType,
    onSelect: (modelId: String, year: Int) -> Unit,
    onSelectCustom: (idleLow: Int, idleHigh: Int, stroke: StrokeType) -> Unit,
    onClose: () -> Unit,
    catalog: List<BikeModel> = MotorcycleCatalog.models,
) {
    val palette = OtackomerColors.current
    var step by remember { mutableStateOf<PickerStep>(PickerStep.Brand) }

    fun back() {
        step = when (val s = step) {
            PickerStep.Brand -> {
                onClose()
                PickerStep.Brand
            }
            is PickerStep.Model -> PickerStep.Brand
            is PickerStep.Year -> PickerStep.Model(s.model.brand)
            PickerStep.Custom -> PickerStep.Brand
        }
    }
    BackHandler(onBack = ::back)

    val insets = WindowInsets.safeDrawing.asPaddingValues()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .padding(insets)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = ::back, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(
                    if (step == PickerStep.Brand) stringResource(R.string.picker_close) else stringResource(R.string.picker_back),
                    color = palette.live,
                    fontSize = 16.sp,
                )
            }
        }
        val title = when (val s = step) {
            PickerStep.Brand -> stringResource(R.string.picker_title_brand)
            is PickerStep.Model -> stringResource(R.string.picker_title_model, s.brand)
            is PickerStep.Year -> stringResource(R.string.picker_title_year, s.model.fullName)
            PickerStep.Custom -> stringResource(R.string.picker_title_custom)
        }
        Text(
            title,
            color = palette.ink,
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 12.dp),
        )

        when (val s = step) {
            PickerStep.Brand -> BrandList(catalog, current, onBrand = { step = PickerStep.Model(it) }, onCustom = { step = PickerStep.Custom })
            is PickerStep.Model -> ModelList(catalog.filter { it.brand == s.brand }, current, onModel = { step = PickerStep.Year(it) })
            is PickerStep.Year -> YearList(s.model, current, onYear = { year -> onSelect(s.model.id, year); onClose() })
            PickerStep.Custom -> CustomBikeForm(
                initialLow = customIdleLow,
                initialHigh = customIdleHigh,
                initialStroke = customStroke,
                onSave = { lo, hi, stroke -> onSelectCustom(lo, hi, stroke); onClose() },
            )
        }
    }
}

@Composable
private fun PickerRow(
    title: String,
    subtitle: String?,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val palette = OtackomerColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, color = if (selected) palette.live else palette.ink, style = MaterialTheme.typography.titleMedium)
            subtitle?.let { Text(it, color = palette.muted, style = MaterialTheme.typography.bodyMedium) }
        }
        Text(if (selected) "✓" else "›", color = if (selected) palette.live else palette.muted, fontSize = 20.sp)
    }
    Spacer(Modifier.fillMaxWidth().height(1.dp).background(palette.line))
}

@Composable
private fun BrandList(
    catalog: List<BikeModel>,
    current: BikeProfile,
    onBrand: (String) -> Unit,
    onCustom: () -> Unit,
) {
    val palette = OtackomerColors.current
    val currentBrand = catalog.firstOrNull { it.id == current.modelId }?.brand
    val brands = catalog.groupBy { it.brand }.toSortedMap(String.CASE_INSENSITIVE_ORDER)
    LazyColumn(Modifier.fillMaxSize()) {
        items(brands.keys.toList(), key = { it }) { brand ->
            val count = brands.getValue(brand).size
            PickerRow(
                title = brand,
                subtitle = pluralStringResource(R.plurals.picker_model_count, count, count),
                selected = brand == currentBrand,
                onClick = { onBrand(brand) },
            )
        }
        item(key = "custom") {
            PickerRow(
                title = stringResource(R.string.picker_custom),
                subtitle = stringResource(R.string.picker_custom_hint),
                selected = current.isCustom,
                onClick = onCustom,
            )
        }
        item(key = "note") {
            Text(
                stringResource(R.string.picker_note),
                color = palette.muted,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 16.dp, start = 4.dp, end = 4.dp, bottom = 16.dp),
            )
        }
    }
}

@Composable
private fun ModelList(models: List<BikeModel>, current: BikeProfile, onModel: (BikeModel) -> Unit) {
    LazyColumn(Modifier.fillMaxSize()) {
        items(models.sortedBy { it.name }, key = { it.id }) { model ->
            val latest: BikeVariant = model.variants.maxBy { it.yearTo }
            val years = model.years
            val yearsText = if (years.size == 1) years.first().toString() else "${years.last()} – ${years.first()}"
            val meta = listOfNotNull(
                strokeLabel(latest.strokeType),
                latest.displacementCc?.let { stringResource(R.string.picker_cc, formatCc(it)) },
                yearsText,
            ).joinToString(" · ")
            PickerRow(
                title = model.name,
                subtitle = meta,
                selected = model.id == current.modelId,
                onClick = { onModel(model) },
            )
        }
    }
}

@Composable
private fun YearList(model: BikeModel, current: BikeProfile, onYear: (Int) -> Unit) {
    LazyColumn(Modifier.fillMaxSize()) {
        items(model.years, key = { it }) { year ->
            val variant = model.variantFor(year)
            val subtitle = buildList {
                add(idleText(variant?.idle))
                if (variant != null && !variant.verified) add(stringResource(R.string.bike_unverified))
            }.joinToString(" · ")
            PickerRow(
                title = year.toString(),
                subtitle = subtitle,
                selected = model.id == current.modelId && year == current.year,
                onClick = { onYear(year) },
            )
        }
    }
}

@Composable
private fun CustomBikeForm(
    initialLow: Int,
    initialHigh: Int,
    initialStroke: StrokeType,
    onSave: (Int, Int, StrokeType) -> Unit,
) {
    val palette = OtackomerColors.current
    var lowText by remember { mutableStateOf(initialLow.toString()) }
    var highText by remember { mutableStateOf(initialHigh.toString()) }
    var stroke by remember { mutableStateOf(initialStroke) }

    val low = lowText.toIntOrNull()
    val high = highText.toIntOrNull()
    val valid = low != null && high != null &&
        low in Settings.IDLE_MIN..Settings.IDLE_MAX &&
        high in Settings.IDLE_MIN..Settings.IDLE_MAX &&
        low <= high

    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(stringResource(R.string.picker_custom_explain), color = palette.muted, style = MaterialTheme.typography.bodyMedium)
        Text(stringResource(R.string.picker_custom_idle), color = palette.muted, style = MaterialTheme.typography.bodyMedium)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val colors = rpmFieldColors()
            RpmField(lowText, stringResource(R.string.work_band_low), colors) { lowText = it }
            Text("–", color = palette.muted)
            RpmField(highText, stringResource(R.string.work_band_high), colors) { highText = it }
        }
        if (!valid) {
            Text(
                stringResource(R.string.picker_custom_invalid, formatRpm(Settings.IDLE_MIN), formatRpm(Settings.IDLE_MAX)),
                color = palette.limit,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Text(stringResource(R.string.picker_custom_stroke), color = palette.muted, style = MaterialTheme.typography.bodyMedium)
        StrokeSelector(stroke, enabled = true, onSelect = { stroke = it })
        Button(
            onClick = { if (valid) onSave(low!!, high!!, stroke) },
            enabled = valid,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(6.dp),
            colors = ButtonDefaults.buttonColors(containerColor = palette.live, contentColor = palette.onLive),
        ) {
            Text(stringResource(R.string.picker_save), fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
