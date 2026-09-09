package sk.rallysupport.otackomer.ui

import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Ekvivalent `prefers-reduced-motion`: ak má používateľ v systéme vypnuté
 * animácie (mierka trvania animácií = 0), nič neanimujeme.
 */
@Composable
fun rememberReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        val resolver = context.contentResolver
        val animator = Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
        val transition = Settings.Global.getFloat(resolver, Settings.Global.TRANSITION_ANIMATION_SCALE, 1f)
        animator == 0f || transition == 0f
    }
}
