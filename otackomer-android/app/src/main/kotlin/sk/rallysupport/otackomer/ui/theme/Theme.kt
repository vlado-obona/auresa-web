package sk.rallysupport.otackomer.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Farby otáčkomera. Tmavá paleta je primárna (dielňa, slnko, čitateľnosť na
 * diaľku); svetlá je jej vysokokontrastný ekvivalent pre systémový svetlý režim.
 */
data class OtackomerPalette(
    val background: Color,
    val panel: Color,
    val line: Color,
    val ink: Color,
    val muted: Color,
    val live: Color,
    val good: Color,
    val weak: Color,
    val limit: Color,
    val onLive: Color,
)

val DarkPalette = OtackomerPalette(
    background = Color(0xFF1A2330),
    panel = Color(0xFF22303F),
    line = Color(0xFF33465A),
    ink = Color(0xFFF0EAE0),
    muted = Color(0xFF93A4B5),
    live = Color(0xFFE8A33D),
    good = Color(0xFF5FB98A),
    weak = Color(0xFF6B7A8A),
    limit = Color(0xFFC9564B),
    onLive = Color(0xFF1A2330),
)

val LightPalette = OtackomerPalette(
    background = Color(0xFFF4F1EA),
    panel = Color(0xFFE4E0D6),
    line = Color(0xFFC4BEB0),
    ink = Color(0xFF1A2330),
    muted = Color(0xFF5B6A79),
    live = Color(0xFFB8721A),
    good = Color(0xFF2E8B5B),
    weak = Color(0xFF8C97A3),
    limit = Color(0xFFB8463B),
    onLive = Color(0xFFFFFFFF),
)

val LocalPalette = staticCompositionLocalOf { DarkPalette }

/** Skratka k aktuálnej palete. */
object OtackomerColors {
    val current: OtackomerPalette
        @Composable get() = LocalPalette.current
}

@Composable
fun OtackomerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val palette = if (darkTheme) DarkPalette else LightPalette
    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = palette.live,
            onPrimary = palette.onLive,
            secondary = palette.good,
            background = palette.background,
            onBackground = palette.ink,
            surface = palette.background,
            onSurface = palette.ink,
            surfaceVariant = palette.panel,
            onSurfaceVariant = palette.muted,
            outline = palette.line,
            error = palette.limit,
        )
    } else {
        lightColorScheme(
            primary = palette.live,
            onPrimary = palette.onLive,
            secondary = palette.good,
            background = palette.background,
            onBackground = palette.ink,
            surface = palette.background,
            onSurface = palette.ink,
            surfaceVariant = palette.panel,
            onSurfaceVariant = palette.muted,
            outline = palette.line,
            error = palette.limit,
        )
    }

    CompositionLocalProvider(LocalPalette provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography(),
            content = content,
        )
    }
}
