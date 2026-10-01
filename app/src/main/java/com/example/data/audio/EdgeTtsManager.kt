package com.example.data.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.TimeUnit

class EdgeTtsManager(private val context: Context) {

    companion object {
        private const val TRUSTED_CLIENT_TOKEN = "6A5AA1D4EAFF4E9FB37E23D68491D6F4"
        private const val CHROMIUM_FULL_VERSION = "130.0.2849.68"
        private const val SEC_MS_GEC_VERSION = "1-$CHROMIUM_FULL_VERSION"

        fun generateSecMsGec(): String {
            return try {
                val winEpoch = 11644473600L
                val nowSeconds = System.currentTimeMillis() / 1000L
                var ticks = nowSeconds + winEpoch
                ticks -= (ticks % 300L)
                val filetime = ticks * 10000000L
                val strToHash = "$filetime$TRUSTED_CLIENT_TOKEN"
                val md = MessageDigest.getInstance("SHA-256")
                val digest = md.digest(strToHash.toByteArray(Charsets.UTF_8))
                digest.joinToString("") { "%02X".format(it) }
            } catch (e: Exception) {
                ""
            }
        }
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private var mediaPlayer: MediaPlayer? = null
    private var activeWebSocket: WebSocket? = null
    private var currentJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    private var isPlayingAudio = false

    fun isPlaying(): Boolean = isPlayingAudio

    fun buildExpressiveSsml(
        text: String,
        voice: String,
        basePitch: String = "+0Hz",
        baseRate: String = "+0%",
        autoExpressivePitch: Boolean = true
    ): String {
        val lang = if (voice.startsWith("hi-")) "hi-IN" else "en-IN"
        if (!autoExpressivePitch) {
            val escapedText = escapeXml(text)
            return "<speak version='1.0' xmlns='http://www.w3.org/2001/10/synthesis' xml:lang='$lang'>" +
                    "<voice name='$voice'>" +
                    "<prosody pitch='$basePitch' rate='$baseRate'>$escapedText</prosody>" +
                    "</voice></speak>"
        }

        val rawPhrases = text.split(Regex("(?<=[!?।.\n])\\s+")).filter { it.isNotBlank() }
        val sb = StringBuilder()
        sb.append("<speak version='1.0' xmlns='http://www.w3.org/2001/10/synthesis' xml:lang='$lang'>")
        sb.append("<voice name='$voice'>")

        if (rawPhrases.isEmpty()) {
            val escaped = escapeXml(text)
            sb.append("<prosody pitch='$basePitch' rate='$baseRate'>$escaped</prosody>")
        } else {
            for ((idx, phrase) in rawPhrases.withIndex()) {
                val lower = phrase.lowercase()
                val escapedPhrase = escapeXml(phrase.trim())
                if (escapedPhrase.isBlank()) continue

                val isShout = lower.contains("ओए गुरु") || lower.contains("oye guru") ||
                        lower.contains("हैट्रिक") || lower.contains("hattrick") ||
                        lower.contains("out") || lower.contains("आउट") ||
                        lower.contains("बोल्ड") || lower.contains("bowled") ||
                        lower.contains("गया गया") || lower.contains("चक दे") ||
                        lower.contains("छक्का") || lower.contains("six") ||
                        lower.contains("बाप रे") || lower.contains("धमाका") ||
                        lower.contains("खटाक") || lower.contains("पछाड़") ||
                        phrase.contains("!")

                val isTurningPoint = lower.contains("टर्निंग पॉइंट") || lower.contains("turning point") ||
                        lower.contains("सांसें") || lower.contains("रोमांचक") ||
                        lower.contains("अंतिम ओवर") || lower.contains("पासा पलट") ||
                        lower.contains("इतिहास") || lower.contains("दबाव")

                val isPunchline = lower.contains("ठोको ताली") || lower.contains("thoko taali") ||
                        lower.contains("नप दे किल्ली") || lower.contains("ताली") ||
                        lower.contains("शायरी") || lower.contains("कहावत") ||
                        (idx == rawPhrases.lastIndex && rawPhrases.size > 1)

                val (segPitch, segRate) = when {
                    isShout -> Pair("+28Hz", "+12%")
                    isTurningPoint -> Pair("+15Hz", "+6%")
                    isPunchline -> Pair("-7Hz", "-2%")
                    idx == 0 -> Pair("+16Hz", "+8%")
                    else -> Pair("+3Hz", "+1%")
                }

                sb.append("<prosody pitch='$segPitch' rate='$segRate'>$escapedPhrase </prosody>")
            }
        }

        sb.append("</voice></speak>")
        return sb.toString()
    }

    private fun escapeXml(text: String): String {
        return text.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }

    fun synthesizeAndPlay(
        text: String,
        voice: String = "hi-IN-MadhurNeural",
        pitch: String = "+0Hz",
        rate: String = "+0%",
        autoPitchExpression: Boolean = true,
        onStart: () -> Unit,
        onDone: () -> Unit,
        onError: (String) -> Unit
    ) {
        stop()

        currentJob = scope.launch(Dispatchers.IO) {
            val audioBuffer = ByteArrayOutputStream()
            var isSynthesisComplete = false
            var hasReceivedAudio = false

            val requestId = UUID.randomUUID().toString().replace("-", "")
            val connectionId = UUID.randomUUID().toString().replace("-", "")
            val secMsGec = generateSecMsGec()

            val wsUrl = "wss://speech.platform.bing.com/consumer/speech/synthesize/readaloud/edge/v1" +
                    "?TrustedClientToken=$TRUSTED_CLIENT_TOKEN" +
                    "&Sec-MS-GEC=$secMsGec" +
                    "&Sec-MS-GEC-Version=$SEC_MS_GEC_VERSION" +
                    "&ConnectionId=$connectionId"

            val request = Request.Builder()
                .url(wsUrl)
                .addHeader("Pragma", "no-cache")
                .addHeader("Cache-Control", "no-cache")
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36 Edg/$CHROMIUM_FULL_VERSION")
                .addHeader("Origin", "chrome-extension://jdiccldimpdaibmpdkjnbmckianbfold")
                .addHeader("Accept-Encoding", "gzip, deflate, br")
                .addHeader("Accept-Language", "en-US,en;q=0.9,hi;q=0.8")
                .build()

            // Safety timeout: if Edge TTS doesn't deliver audio within 7 seconds, fallback
            val timeoutJob = launch {
                delay(7000)
                if (!hasReceivedAudio && !isSynthesisComplete) {
                    Log.w("EdgeTtsManager", "Edge TTS timeout, falling back to local TTS")
                    activeWebSocket?.cancel()
                    withContext(Dispatchers.Main) {
                        onError("Edge TTS connection timeout")
                    }
                }
            }

            activeWebSocket = client.newWebSocket(request, object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    // Send speech config
                    val configMsg = "Content-Type:application/json; charset=utf-8\r\nPath:speech.config\r\n\r\n" +
                            "{\"context\":{\"synthesis\":{\"audio\":{\"metadataoptions\":{\"sentenceBoundaryEnabled\":\"false\",\"wordBoundaryEnabled\":\"false\"},\"outputFormat\":\"audio-24khz-48kbitrate-mono-mp3\"}}}}"
                    webSocket.send(configMsg)

                    // Send multi-pitch expressive SSML with Microsoft Edge Neural voice
                    val ssml = buildExpressiveSsml(text, voice, pitch, rate, autoPitchExpression)
                    val ssmlMsg = "X-RequestId:$requestId\r\nContent-Type:application/ssml+xml\r\nPath:ssml\r\n\r\n$ssml"
                    webSocket.send(ssmlMsg)
                }

                override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                    try {
                        val data = bytes.toByteArray()
                        if (data.size > 2) {
                            // First 2 bytes are UInt16 big endian header length
                            val headerLen = ((data[0].toInt() and 0xFF) shl 8) or (data[1].toInt() and 0xFF)
                            val audioStartIndex = 2 + headerLen
                            if (audioStartIndex < data.size) {
                                audioBuffer.write(data, audioStartIndex, data.size - audioStartIndex)
                                hasReceivedAudio = true
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("EdgeTtsManager", "Error parsing audio bytes: ${e.message}")
                    }
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    if (text.contains("Path:turn.end")) {
                        isSynthesisComplete = true
                        timeoutJob.cancel()
                        webSocket.close(1000, "Done")

                        val audioBytes = audioBuffer.toByteArray()
                        if (audioBytes.isNotEmpty()) {
                            scope.launch(Dispatchers.IO) {
                                playAudioBytes(audioBytes, onStart, onDone, onError)
                            }
                        } else {
                            scope.launch(Dispatchers.Main) {
                                onError("Empty audio payload received")
                            }
                        }
                    }
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    timeoutJob.cancel()
                    Log.w("EdgeTtsManager", "WebSocket failed: ${t.message}")
                    scope.launch(Dispatchers.Main) {
                        onError(t.message ?: "Edge TTS WebSocket failure")
                    }
                }
            })
        }
    }

    private suspend fun playAudioBytes(
        audioBytes: ByteArray,
        onStart: () -> Unit,
        onDone: () -> Unit,
        onError: (String) -> Unit
    ) = withContext(Dispatchers.Main) {
        try {
            val tempFile = File(context.cacheDir, "edge_prabhat_${System.currentTimeMillis()}.mp3")
            FileOutputStream(tempFile).use { it.write(audioBytes) }

            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                setDataSource(tempFile.absolutePath)
                setOnPreparedListener { mp ->
                    mp.start()
                    isPlayingAudio = true
                    onStart()
                }
                setOnCompletionListener { mp ->
                    isPlayingAudio = false
                    mp.release()
                    mediaPlayer = null
                    tempFile.delete()
                    onDone()
                }
                setOnErrorListener { mp, what, extra ->
                    isPlayingAudio = false
                    mp.release()
                    mediaPlayer = null
                    tempFile.delete()
                    onError("MediaPlayer error: what=$what extra=$extra")
                    true
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            isPlayingAudio = false
            Log.e("EdgeTtsManager", "Playback error: ${e.message}")
            onError(e.message ?: "Failed to play audio")
        }
    }

    fun stop() {
        try {
            currentJob?.cancel()
            activeWebSocket?.cancel()
            activeWebSocket = null
            mediaPlayer?.let {
                if (it.isPlaying) it.stop()
                it.release()
            }
            mediaPlayer = null
            isPlayingAudio = false
        } catch (e: Exception) {
            Log.w("EdgeTtsManager", "Error stopping audio: ${e.message}")
        }
    }
}
