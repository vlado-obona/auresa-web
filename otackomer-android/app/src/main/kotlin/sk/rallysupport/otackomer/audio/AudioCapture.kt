package sk.rallysupport.otackomer.audio

import android.Manifest
import android.content.Context
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.AudioEffect
import android.media.audiofx.AutomaticGainControl
import android.media.audiofx.NoiseSuppressor
import android.util.Log
import androidx.annotation.RequiresPermission
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import sk.rallysupport.otackomer.dsp.Reading
import sk.rallysupport.otackomer.dsp.TachometerEngine

/**
 * Snímanie zvuku z mikrofónu bez akéhokoľvek predspracovania.
 *
 * Poradie zdrojov:
 * 1. [MediaRecorder.AudioSource.UNPROCESSED] – ak ho zariadenie deklaruje cez
 *    [AudioManager.PROPERTY_SUPPORT_AUDIO_SOURCE_UNPROCESSED],
 * 2. [MediaRecorder.AudioSource.VOICE_RECOGNITION],
 * 3. [MediaRecorder.AudioSource.MIC].
 *
 * Pri fallbacku sa explicitne vypnú AGC, potlačenie šumu a echo canceller,
 * ak sú na zariadení dostupné – inak by zničili obálku signálu, na ktorej
 * meranie stojí.
 */
class AudioCapture(private val context: Context) {

    /** Otvorený mikrofón. Po skončení treba zavolať [release]. */
    class Session internal constructor(
        val record: AudioRecord,
        /** Ľudsky čitateľný názov použitého zdroja (pre UI). */
        val sourceLabel: String,
        /** `true`, ak beží cez UNPROCESSED bez akýchkoľvek efektov. */
        val unprocessed: Boolean,
        private val effects: List<AudioEffect>,
    ) {
        fun release() {
            runCatching { record.stop() }
            effects.forEach { runCatching { it.release() } }
            runCatching { record.release() }
        }
    }

    /** Vyhodí sa, keď sa nepodarí otvoriť žiadny zdroj. */
    class OpenException(message: String) : Exception(message)

    /** Otvorí mikrofón. Vyžaduje udelené oprávnenie RECORD_AUDIO. */
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun open(): Session {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val supportsUnprocessed =
            audioManager.getProperty(AudioManager.PROPERTY_SUPPORT_AUDIO_SOURCE_UNPROCESSED) == "true"

        val candidates = buildList<Pair<Int, String>> {
            if (supportsUnprocessed) add(MediaRecorder.AudioSource.UNPROCESSED to "UNPROCESSED")
            add(MediaRecorder.AudioSource.VOICE_RECOGNITION to "VOICE_RECOGNITION")
            add(MediaRecorder.AudioSource.MIC to "MIC")
        }

        val minBuffer = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL, ENCODING)
        if (minBuffer <= 0) throw OpenException("Zariadenie nepodporuje 48 kHz mono PCM16")
        // Aspoň 0,5 s zásoby, aby nič nevypadlo, keď je hlavné vlákno zaneprázdnené.
        val bufferBytes = maxOf(minBuffer, SAMPLE_RATE * BYTES_PER_SAMPLE / 2)

        for ((source, label) in candidates) {
            val record = try {
                AudioRecord(source, SAMPLE_RATE, CHANNEL, ENCODING, bufferBytes)
            } catch (e: IllegalArgumentException) {
                Log.w(TAG, "Zdroj $label sa nedá vytvoriť", e)
                continue
            }
            if (record.state != AudioRecord.STATE_INITIALIZED) {
                Log.w(TAG, "Zdroj $label sa neinicializoval")
                record.release()
                continue
            }

            val unprocessed = source == MediaRecorder.AudioSource.UNPROCESSED
            val effects = if (unprocessed) emptyList() else disableEffects(record.audioSessionId)

            try {
                record.startRecording()
            } catch (e: IllegalStateException) {
                Log.w(TAG, "Zdroj $label sa nedá spustiť", e)
                effects.forEach { runCatching { it.release() } }
                record.release()
                continue
            }
            if (record.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
                effects.forEach { runCatching { it.release() } }
                record.release()
                continue
            }
            Log.i(TAG, "Nahrávam zo zdroja $label (unprocessed=$unprocessed)")
            return Session(record, label, unprocessed, effects)
        }
        throw OpenException("Mikrofón sa nepodarilo otvoriť")
    }

    /**
     * Beží, kým sa coroutine nezruší: číta bloky vzoriek, posiela ich do
     * [engine] a každý výsledok analýzy odovzdá cez [onReading]. Mikrofón sa
     * uvoľní v `finally`, teda aj pri zrušení.
     */
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    suspend fun run(
        engine: TachometerEngine,
        onOpened: (Session) -> Unit,
        onReading: (Reading) -> Unit,
    ): Unit = withContext(Dispatchers.IO) {
        val session = open()
        onOpened(session)
        val buffer = ShortArray(READ_CHUNK)
        try {
            while (true) {
                ensureActive()
                val n = session.record.read(buffer, 0, buffer.size, AudioRecord.READ_BLOCKING)
                if (n < 0) throw OpenException("Chyba čítania z mikrofónu ($n)")
                if (n == 0) continue
                engine.feed(buffer, n)?.let(onReading)
            }
        } catch (e: CancellationException) {
            throw e
        } finally {
            session.release()
        }
    }

    /** Vypne efekty, ktoré by menili obálku signálu. Vráti ich, aby sa dali uvoľniť. */
    private fun disableEffects(sessionId: Int): List<AudioEffect> {
        val effects = mutableListOf<AudioEffect>()
        if (AutomaticGainControl.isAvailable()) {
            AutomaticGainControl.create(sessionId)?.let { agc ->
                agc.enabled = false
                effects += agc
            }
        }
        if (NoiseSuppressor.isAvailable()) {
            NoiseSuppressor.create(sessionId)?.let { ns ->
                ns.enabled = false
                effects += ns
            }
        }
        if (AcousticEchoCanceler.isAvailable()) {
            AcousticEchoCanceler.create(sessionId)?.let { aec ->
                aec.enabled = false
                effects += aec
            }
        }
        return effects
    }

    companion object {
        private const val TAG = "AudioCapture"
        const val SAMPLE_RATE = 48_000
        private const val CHANNEL = AudioFormat.CHANNEL_IN_MONO
        private const val ENCODING = AudioFormat.ENCODING_PCM_16BIT
        private const val BYTES_PER_SAMPLE = 2

        /** 50 ms zvuku na jedno čítanie. */
        private const val READ_CHUNK = SAMPLE_RATE / 20
    }
}
