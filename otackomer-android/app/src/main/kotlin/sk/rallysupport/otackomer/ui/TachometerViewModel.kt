package sk.rallysupport.otackomer.ui

import android.Manifest
import android.app.Application
import androidx.annotation.RequiresPermission
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import sk.rallysupport.otackomer.R
import sk.rallysupport.otackomer.audio.AudioCapture
import sk.rallysupport.otackomer.data.Settings
import sk.rallysupport.otackomer.data.SettingsRepository
import sk.rallysupport.otackomer.dsp.Reading
import sk.rallysupport.otackomer.dsp.SignalStatus
import sk.rallysupport.otackomer.dsp.StrokeType
import sk.rallysupport.otackomer.dsp.TachometerEngine

/** Stav obrazovky otáčkomera. */
data class TachometerUiState(
    val running: Boolean = false,
    /** Posledná zobrazená hodnota (zaokrúhlená na desiatky) alebo `null`. */
    val rpm: Int? = null,
    /** `true`, keď je hodnota zamrazená (slabý zvuk / hluk). */
    val frozen: Boolean = false,
    val status: SignalStatus = SignalStatus.NO_DATA,
    /** Zhoda signálu 0–1 (hodnota NSDF) alebo `null`. */
    val clarity: Float? = null,
    /** Vstupná úroveň 0–1. */
    val level: Float = 0f,
    val peakRpm: Int? = null,
    /** Posledných ~60 s po 200 ms; NaN = bez platnej hodnoty. */
    val history: List<Float> = emptyList(),
    val strokeType: StrokeType = StrokeType.FOUR_STROKE,
    val workBandLow: Int = Settings.DEFAULT_WORK_LOW,
    val workBandHigh: Int = Settings.DEFAULT_WORK_HIGH,
    /** Názov použitého zdroja zvuku, keď meranie beží. */
    val audioSource: String? = null,
    val audioUnprocessed: Boolean = false,
    /** Chyba pri otváraní mikrofónu (resource id) alebo `null`. */
    val errorRes: Int? = null,
) {
    val inIdleBand: Boolean
        get() = rpm != null && !frozen && rpm in IDLE_BAND_LOW..IDLE_BAND_HIGH

    companion object {
        /** Predpísaný voľnobeh 500 EXC-F podľa manuálu KTM. */
        const val IDLE_BAND_LOW = 1_800
        const val IDLE_BAND_HIGH = 1_900

        /** Strop pre prvú motohodinu zábehu podľa manuálu. */
        const val RUN_IN_LIMIT = 7_000
        const val SCALE_MAX = 10_000

        /** 60 s pri analýze každých 200 ms. */
        const val HISTORY_LENGTH = 300
    }
}

class TachometerViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepository = SettingsRepository(application)
    private val engine = TachometerEngine(AudioCapture.SAMPLE_RATE)
    private val capture = AudioCapture(application)
    private var captureJob: Job? = null

    private val _state = MutableStateFlow(TachometerUiState())
    val state: StateFlow<TachometerUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                engine.setStrokeType(settings.strokeType)
                _state.update {
                    it.copy(
                        strokeType = settings.strokeType,
                        workBandLow = settings.workBandLow,
                        workBandHigh = settings.workBandHigh,
                    )
                }
            }
        }
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun start() {
        if (captureJob?.isActive == true) return
        engine.reset()
        _state.update {
            it.copy(
                running = true,
                rpm = null,
                frozen = false,
                status = SignalStatus.NO_DATA,
                clarity = null,
                level = 0f,
                peakRpm = null,
                history = emptyList(),
                audioSource = null,
                errorRes = null,
            )
        }
        captureJob = viewModelScope.launch {
            try {
                capture.run(
                    engine,
                    onOpened = { session ->
                        _state.update { it.copy(audioSource = session.sourceLabel, audioUnprocessed = session.unprocessed) }
                    },
                    onReading = ::onReading,
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update {
                    it.copy(running = false, audioSource = null, errorRes = R.string.error_microphone)
                }
            }
        }
    }

    fun stop() {
        captureJob?.cancel()
        captureJob = null
        _state.update { it.copy(running = false, frozen = false, level = 0f, audioSource = null) }
    }

    fun toggle() {
        // Volajúci (UI) je zodpovedný za to, že oprávnenie je udelené.
        if (_state.value.running) stop() else startChecked()
    }

    @Suppress("MissingPermission")
    private fun startChecked() = start()

    fun setStrokeType(type: StrokeType) {
        engine.setStrokeType(type)
        _state.update { it.copy(strokeType = type, rpm = null, frozen = false, peakRpm = null) }
        viewModelScope.launch { settingsRepository.setStrokeType(type) }
    }

    fun setWorkBand(low: Int, high: Int) {
        val lo = low.coerceIn(Settings.WORK_BAND_MIN, Settings.WORK_BAND_MAX)
        val hi = high.coerceIn(Settings.WORK_BAND_MIN, Settings.WORK_BAND_MAX)
        if (lo >= hi) return
        _state.update { it.copy(workBandLow = lo, workBandHigh = hi) }
        viewModelScope.launch { settingsRepository.setWorkBand(lo, hi) }
    }

    private fun onReading(reading: Reading) {
        _state.update { current ->
            val shown = reading.rpm
            val history = (current.history + (if (reading.fresh && shown != null) shown.toFloat() else Float.NaN))
                .takeLast(TachometerUiState.HISTORY_LENGTH)
            current.copy(
                rpm = shown,
                frozen = !reading.fresh,
                status = reading.status,
                clarity = reading.clarity,
                level = reading.level,
                peakRpm = if (reading.fresh && shown != null) maxOf(current.peakRpm ?: 0, shown) else current.peakRpm,
                history = history,
            )
        }
    }

    override fun onCleared() {
        stop()
    }
}
