package sk.rallysupport.otackomer

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OtackomerTheme {
                TachometerApp(
                    viewModel = viewModel,
                    onKeepScreenOn = ::setKeepScreenOn,
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
