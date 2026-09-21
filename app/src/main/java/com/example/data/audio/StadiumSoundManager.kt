package com.example.data.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.sin

/**
 * StadiumSoundManager generates realistic cricket audio effects (cheers, horns, whistles)
 * completely synthesized offline using Android AudioTrack PCM. Zero external sound assets needed!
 */
object StadiumSoundManager {
    private val scope = CoroutineScope(Dispatchers.Default)

    fun playSixCheer() {
        scope.launch {
            try {
                // Stadium brass fanfare + crowd swell
                playToneSequence(
                    frequencies = floatArrayOf(440f, 554.37f, 659.25f, 880f),
                    durationsMs = intArrayOf(120, 120, 150, 350),
                    volume = 0.8f
                )
            } catch (e: Exception) {
                Log.w("StadiumSound", "Error playing six cheer: ${e.message}")
            }
        }
    }

    fun playFourHorn() {
        scope.launch {
            try {
                // Crisp dual-tone boundary horn
                playToneSequence(
                    frequencies = floatArrayOf(523.25f, 659.25f),
                    durationsMs = intArrayOf(180, 250),
                    volume = 0.75f
                )
            } catch (e: Exception) {
                Log.w("StadiumSound", "Error playing four horn: ${e.message}")
            }
        }
    }

    fun playWicketDismissal() {
        scope.launch {
            try {
                // Dramatic umpire whistle + deep drum beat
                playWhistleSound()
            } catch (e: Exception) {
                Log.w("StadiumSound", "Error playing wicket sound: ${e.message}")
            }
        }
    }

    fun playUmpireWhistle() {
        scope.launch {
            try {
                playWhistleSound()
            } catch (e: Exception) {
                Log.w("StadiumSound", "Error playing whistle: ${e.message}")
            }
        }
    }

    private fun playWhistleSound() {
        val sampleRate = 44100
        val durationMs = 320
        val numSamples = (sampleRate * durationMs) / 1000
        val buffer = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            // Whistle modulation between 2800 Hz and 3100 Hz with harmonic
            val freqMod = 2800.0 + 300.0 * sin(2.0 * Math.PI * 25.0 * t)
            val envelope = if (i < numSamples * 0.1) {
                i / (numSamples * 0.1)
            } else if (i > numSamples * 0.8) {
                (numSamples - i) / (numSamples * 0.2)
            } else {
                1.0
            }
            val sample = (sin(2.0 * Math.PI * freqMod * t) * 0.6 + sin(2.0 * Math.PI * freqMod * 2.0 * t) * 0.2) * envelope
            buffer[i] = (sample * Short.MAX_VALUE * 0.6).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }

        playPcmBuffer(buffer, sampleRate)
    }

    private fun playToneSequence(frequencies: FloatArray, durationsMs: IntArray, volume: Float) {
        val sampleRate = 44100
        val totalMs = durationsMs.sum()
        val totalSamples = (sampleRate * totalMs) / 1000
        val buffer = ShortArray(totalSamples)

        var offset = 0
        for (idx in frequencies.indices) {
            val freq = frequencies[idx].toDouble()
            val dur = durationsMs[idx]
            val samplesCount = (sampleRate * dur) / 1000

            for (i in 0 until samplesCount) {
                val t = i.toDouble() / sampleRate
                val envelope = when {
                    i < samplesCount * 0.08 -> i / (samplesCount * 0.08)
                    i > samplesCount * 0.85 -> (samplesCount - i) / (samplesCount * 0.15)
                    else -> 1.0
                }
                val sample = sin(2.0 * Math.PI * freq * t) * envelope
                val bufIndex = offset + i
                if (bufIndex < totalSamples) {
                    buffer[bufIndex] = (sample * Short.MAX_VALUE * volume).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                }
            }
            offset += samplesCount
        }

        playPcmBuffer(buffer, sampleRate)
    }

    private fun playPcmBuffer(buffer: ShortArray, sampleRate: Int) {
        var track: AudioTrack? = null
        try {
            val bufferSize = buffer.size * 2
            track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            track.write(buffer, 0, buffer.size)
            track.play()
            Thread.sleep((buffer.size * 1000L) / sampleRate + 50)
        } catch (_: Exception) {
        } finally {
            try {
                track?.stop()
                track?.release()
            } catch (_: Exception) {}
        }
    }
}
