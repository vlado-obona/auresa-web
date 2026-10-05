package sk.rallysupport.otackomer.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import sk.rallysupport.otackomer.data.Settings as AppSettings

/**
 * Koreň UI: rieši oprávnenie na mikrofón a držanie obrazovky zapnutej,
 * samotné meranie zobrazuje [TachometerScreen].
 */
@Composable
fun TachometerApp(
    viewModel: TachometerViewModel,
    onKeepScreenOn: (Boolean) -> Unit,
    onLanguage: (String) -> Unit,
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()

    var hasPermission by remember { mutableStateOf(context.hasRecordPermission()) }
    var permanentlyDenied by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasPermission = granted
        if (granted) {
            permanentlyDenied = false
            viewModel.toggle()
        } else {
            val activity = context.findActivity()
            permanentlyDenied = activity != null &&
                !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.RECORD_AUDIO)
        }
    }

    // Po návrate z nastavení systému oprávnenie znova overíme.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasPermission = context.hasRecordPermission()
                if (hasPermission) permanentlyDenied = false
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(state.running) { onKeepScreenOn(state.running) }
    DisposableEffect(Unit) { onDispose { onKeepScreenOn(false) } }

    var showBikePicker by rememberSaveable { mutableStateOf(false) }
    if (showBikePicker) {
        val bike = state.bike
        BikePickerScreen(
            current = bike,
            customIdleLow = if (bike.isCustom) bike.idle?.low ?: AppSettings.DEFAULT_CUSTOM_IDLE_LOW else AppSettings.DEFAULT_CUSTOM_IDLE_LOW,
            customIdleHigh = if (bike.isCustom) bike.idle?.high ?: AppSettings.DEFAULT_CUSTOM_IDLE_HIGH else AppSettings.DEFAULT_CUSTOM_IDLE_HIGH,
            customStroke = bike.strokeType,
            onSelect = viewModel::selectBike,
            onSelectCustom = viewModel::selectCustomBike,
            onClose = { showBikePicker = false },
        )
        return
    }

    TachometerScreen(
        state = state,
        hasPermission = hasPermission,
        permissionPermanentlyDenied = permanentlyDenied,
        onToggle = {
            if (state.running || hasPermission) {
                viewModel.toggle()
            } else {
                launcher.launch(Manifest.permission.RECORD_AUDIO)
            }
        },
        onRequestPermission = {
            if (permanentlyDenied) {
                context.openAppSettings()
            } else {
                launcher.launch(Manifest.permission.RECORD_AUDIO)
            }
        },
        onStrokeType = viewModel::setStrokeType,
        onWorkBand = viewModel::setWorkBand,
        onOpenBikePicker = { showBikePicker = true },
        onLanguage = onLanguage,
    )
}

private fun Context.hasRecordPermission(): Boolean =
    ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

private fun Context.findActivity(): Activity? {
    var current: Context = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}

private fun Context.openAppSettings() {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { startActivity(intent) }
}
