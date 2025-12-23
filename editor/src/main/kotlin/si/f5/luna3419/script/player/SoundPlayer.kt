package si.f5.luna3419.script.player

import java.io.ByteArrayOutputStream
import java.net.URL
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip
import javax.sound.sampled.FloatControl
import kotlin.math.log10

object SoundPlayer {
    private var currentClip: Clip? = null

    fun play(url: URL, volume: Int) {
        stop()
        try {
            val originalStream = AudioSystem.getAudioInputStream(url)
            val baseFormat = originalStream.format
            val decodedFormat = AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                baseFormat.sampleRate,
                16,
                baseFormat.channels,
                baseFormat.channels * 2,
                baseFormat.sampleRate,
                false
            )
            val audioInputStream = AudioSystem.getAudioInputStream(decodedFormat, originalStream)

            val outBuffer = ByteArrayOutputStream()
            val data = ByteArray(4096)
            var nBytesRead: Int
            while (audioInputStream.read(data).also { nBytesRead = it } != -1) {
                outBuffer.write(data, 0, nBytesRead)
            }
            val audioBytes = outBuffer.toByteArray()

            val clip = AudioSystem.getClip()
            currentClip = clip
            clip.open(decodedFormat, audioBytes, 0, audioBytes.size)

            if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                val gainControl = clip.getControl(FloatControl.Type.MASTER_GAIN) as FloatControl
                val vol = volume.coerceIn(0, 100)
                val db = if (vol > 0) 20f * log10(vol / 100f) else -80f
                gainControl.value = db.coerceIn(gainControl.minimum, gainControl.maximum)
            }

            clip.start()
        } catch (e: Exception) {
            println("Audio Error: ${e.message} (Format might not be supported)")
            e.printStackTrace()
        }
    }

    fun stop() {
        try {
            currentClip?.stop()
            currentClip?.close()
        } catch (_: Exception) {
        }
        currentClip = null
    }

    fun loadClipPool(url: URL, count: Int = 15): List<Clip> {
        return try {
            val originalStream = AudioSystem.getAudioInputStream(url)
            val baseFormat = originalStream.format
            println("SoundDebug: Loading Pool from $url ($count instances)")

            val decodedFormat = AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                baseFormat.sampleRate,
                16,
                baseFormat.channels,
                baseFormat.channels * 2,
                baseFormat.sampleRate,
                false
            )
            val audioInputStream = AudioSystem.getAudioInputStream(decodedFormat, originalStream)

            val outBuffer = ByteArrayOutputStream()
            val data = ByteArray(4096)
            var nBytesRead: Int
            while (audioInputStream.read(data).also { nBytesRead = it } != -1) {
                outBuffer.write(data, 0, nBytesRead)
            }
            val audioBytes = outBuffer.toByteArray()
            if (audioBytes.isEmpty()) return emptyList()


            val pool = mutableListOf<Clip>()
            repeat(count) {
                val clip = AudioSystem.getClip()
                clip.open(decodedFormat, audioBytes, 0, audioBytes.size)
                pool.add(clip)
            }
            pool
        } catch (e: Exception) {
            println("SoundError (Pool): ${e.message}")
            e.printStackTrace()
            emptyList()
        }
    }

    fun playFromPool(pool: List<Clip>, volume: Int = 100) {
        try {
            val clip = pool.firstOrNull { !it.isRunning }

            if (clip != null) {
                clip.framePosition = 0
                if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                    val gainControl = clip.getControl(FloatControl.Type.MASTER_GAIN) as FloatControl
                    val vol = volume.coerceIn(0, 100)
                    val db = if (vol > 0) 20f * log10(vol / 100f) else -80f
                    gainControl.value = db.coerceIn(gainControl.minimum, gainControl.maximum)
                }
                clip.start()
            }
        } catch (e: Exception) {
            println("SoundPlaybackError: ${e.message}")
            e.printStackTrace()
        }
    }
}
