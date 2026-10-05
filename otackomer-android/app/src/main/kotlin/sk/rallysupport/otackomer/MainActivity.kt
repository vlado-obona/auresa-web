package sk.rallysupport.otackomer

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import sk.rallysupport.otackomer.ui.TachometerApp
import sk.rallysupport.otackomer.ui.TachometerViewModel
import sk.rallysupport.otackomer.ui.theme.OtackomerTheme

class MainActivity : ComponentActivity() {

    private val viewModel: TachometerViewModel by viewModels()

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLanguage.wrap(newBase))
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // Android do 12: zmena tmavého režimu prinesie konfiguráciu s jazykom
        // systému – aktivitu vytvoríme nanovo, aby sa vrátil zvolený jazyk.
        val tag = AppLanguage.storedTag(this)
        if (tag != null && newConfig.locales[0].language != tag) recreate()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OtackomerTheme {
                TachometerApp(
                    viewModel = viewModel,
                    onKeepScreenOn = ::setKeepScreenOn,
                    onLanguage = { tag -> AppLanguage.set(this, tag) },
                )
            }
        }
    }

    override fun onStop() {
        // Na pozadí nemá zmysel merať – uvoľníme mikrofón.
        viewModel.stop()
        super.onStop()
    }

    private fun setKeepScreenOn(on: Boolean) {
        if (on) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
}
