package com.example.data.audio

import android.content.Context
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

class AiSidhuCommentaryManager(private val context: Context) : TextToSpeech.OnInitListener {

    private val scope = CoroutineScope(Dispatchers.Main)
    private var tts: TextToSpeech? = null
    private var isTtsReady = false

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val prefs: SharedPreferences = context.getSharedPreferences("ayuu_sidhu_voice_prefs", Context.MODE_PRIVATE)

    private val edgeTtsManager = EdgeTtsManager(context)

    private val _isCommentaryEnabled = MutableStateFlow(true)
    val isCommentaryEnabled = _isCommentaryEnabled.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking = _isSpeaking.asStateFlow()

    private val _currentDialogue = MutableStateFlow(
        "🎙️ Sidhu Paaji AI (Microsoft Edge Madhur): Thoko taali guru! Live match scorecard shuru ho chuka hai!"
    )
    val currentDialogue = _currentDialogue.asStateFlow()

    private val _voiceEngineTitle = MutableStateFlow("Microsoft Edge Madhur Neural (Male Live)")
    val voiceEngineTitle = _voiceEngineTitle.asStateFlow()

    private val _voiceStyle = MutableStateFlow(SidhuVoiceStyle.ENERGETIC_JOSH)
    val voiceStyle = _voiceStyle.asStateFlow()

    // Interactive Pitch & Speed controls (Persisted)
    // Edge Madhur Neural natural base pitch is 1.0f, speed 1.0f
    private val _currentPitch = MutableStateFlow(prefs.getFloat(KEY_PITCH, 0.88f))
    val currentPitch = _currentPitch.asStateFlow()

    private val _currentSpeed = MutableStateFlow(prefs.getFloat(KEY_SPEED, 1.0f))
    val currentSpeed = _currentSpeed.asStateFlow()

    private val _voiceGender = MutableStateFlow(prefs.getString(KEY_GENDER, "MALE") ?: "MALE")
    val voiceGender = _voiceGender.asStateFlow()

    init {
        try {
            tts = TextToSpeech(context.applicationContext, this)
        } catch (e: Exception) {
            Log.e("AiSidhuCommentary", "TTS init error: ${e.message}")
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.let { engine ->
                // Try Hindi first for authentic Sidhu Paaji voice
                val hindiLocale = Locale("hi", "IN")
                val result = engine.setLanguage(hindiLocale)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    engine.setLanguage(Locale("en", "IN"))
                }

                // Select male voice if available in Android TTS engine
                selectAppropriateVoice(engine)

                // Audio attributes for media playback so it plays in background
                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
                engine.setAudioAttributes(audioAttributes)

                // Apply deep male baritone pitch & articulate speed
                engine.setPitch(_currentPitch.value)
                engine.setSpeechRate(_currentSpeed.value)

                engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        scope.launch { _isSpeaking.value = true }
                    }

                    override fun onDone(utteranceId: String?) {
                        scope.launch { _isSpeaking.value = false }
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        scope.launch { _isSpeaking.value = false }
                    }

                    override fun onError(utteranceId: String?, errorCode: Int) {
                        scope.launch { _isSpeaking.value = false }
                    }
                })

                isTtsReady = true
            }
        }
    }

    private fun selectAppropriateVoice(engine: TextToSpeech) {
        try {
            val allVoices: Set<Voice>? = engine.voices
            if (!allVoices.isNullOrEmpty()) {
                val targetGender = _voiceGender.value
                val hindiVoices = allVoices.filter {
                    it.locale.language.equals("hi", ignoreCase = true) ||
                            (it.locale.language.equals("en", ignoreCase = true) && it.locale.country.equals("IN", ignoreCase = true))
                }

                // Strictly select male voices and reject female identifiers (-hia, -hie, -hic, female)
                val chosen = when (targetGender) {
                    "MALE" -> {
                        hindiVoices.firstOrNull { v ->
                            val n = v.name.lowercase()
                            (n.contains("cfc") || n.contains("-hid") || n.contains("cfl") || n.contains("male") || n.contains("man")) &&
                                    !n.contains("female") && !n.contains("woman") && !n.contains("-hia") && !n.contains("-hie") && !n.contains("-hic")
                        } ?: hindiVoices.firstOrNull { v ->
                            val n = v.name.lowercase()
                            !n.contains("female") && !n.contains("woman") && !n.contains("-hia") && !n.contains("-hie") && !n.contains("-hic") && !n.contains("girl")
                        }
                    }
                    "FEMALE" -> {
                        hindiVoices.firstOrNull { v ->
                            val n = v.name.lowercase()
                            n.contains("female") || n.contains("-hia") || n.contains("-hie") || n.contains("-hic")
                        }
                    }
                    else -> null
                }

                if (chosen != null) {
                    engine.voice = chosen
                    Log.d("AiSidhuCommentary", "Selected local voice: ${chosen.name}")
                }
                // When male is selected, ensure a resonant, masculine baritone pitch
                if (targetGender == "MALE") {
                    engine.setPitch(_currentPitch.value.coerceAtMost(0.88f))
                }
            }
        } catch (e: Exception) {
            Log.w("AiSidhuCommentary", "Voice selection exception: ${e.message}")
        }
    }

    fun setCustomPitch(pitch: Float) {
        val clamped = pitch.coerceIn(0.55f, 1.45f)
        _currentPitch.value = clamped
        prefs.edit().putFloat(KEY_PITCH, clamped).apply()
        tts?.setPitch(clamped)
    }

    fun setCustomSpeed(speed: Float) {
        val clamped = speed.coerceIn(0.55f, 1.45f)
        _currentSpeed.value = clamped
        prefs.edit().putFloat(KEY_SPEED, clamped).apply()
        tts?.setSpeechRate(clamped)
    }

    fun setVoiceGender(gender: String) {
        _voiceGender.value = gender
        prefs.edit().putString(KEY_GENDER, gender).apply()
        tts?.let { selectAppropriateVoice(it) }
    }

    fun resetToSidhuDefaults() {
        setCustomPitch(0.88f)
        setCustomSpeed(1.0f)
        setVoiceGender("MALE")
        testVoiceSample()
    }

    fun testVoiceSample() {
        speak("ओए गुरु! ठोको ताली गुरु! मैं हूँ सिद्धू पाजी, माइक्रोसॉफ्ट एज की मधुर आवाज़ में! चक दे फट्टे!")
    }

    fun setVoiceStyle(style: SidhuVoiceStyle) {
        _voiceStyle.value = style
        setCustomPitch(style.pitchMultiplier)
        setCustomSpeed(style.speedMultiplier)
        speak("ओए गुरु, अब सिद्धू पाजी ${style.label} में कमेंट्री करेंगे! ठोको ताली!")
    }

    fun toggleCommentary(enabled: Boolean? = null) {
        val newState = enabled ?: !_isCommentaryEnabled.value
        _isCommentaryEnabled.value = newState
        if (!newState) {
            stop()
        } else {
            speak("ओए गुरु! सिद्धू पाजी की लाइव कमेंट्री चालू हो गई है! बैकग्राउंड में भी मस्त चलेगी!")
        }
    }

    fun speak(text: String, flush: Boolean = true) {
        if (!_isCommentaryEnabled.value) return

        _currentDialogue.value = text

        try {
            requestAudioFocus()
            val speechReadyText = HindiSpeechFormatter.formatForHindiTts(text)

            // Primary: Microsoft Edge Madhur Neural (Male Hindi) online voice
            val edgeVoice = if (_voiceGender.value == "FEMALE") "hi-IN-SwaraNeural" else "hi-IN-MadhurNeural"
            val (prosodyPitch, prosodyRate) = when (_voiceStyle.value) {
                SidhuVoiceStyle.SHAYARI_PUNCH -> Pair("-2Hz", "-4%")
                SidhuVoiceStyle.ENERGETIC_JOSH -> Pair("+2Hz", "+5%")
                SidhuVoiceStyle.TV_BROADCAST -> Pair("+0Hz", "+0%")
            }

            edgeTtsManager.synthesizeAndPlay(
                text = speechReadyText,
                voice = edgeVoice,
                pitch = prosodyPitch,
                rate = prosodyRate,
                onStart = {
                    scope.launch {
                        _isSpeaking.value = true
                        _voiceEngineTitle.value = "Microsoft Edge Madhur Neural (Male Live)"
                    }
                },
                onDone = {
                    scope.launch { _isSpeaking.value = false }
                },
                onError = { errorMsg ->
                    Log.w("AiSidhuCommentary", "Edge TTS error: $errorMsg. Falling back to local TTS engine.")
                    scope.launch {
                        _voiceEngineTitle.value = "Local Android TTS (Male Fallback)"
                        speakViaLocalTts(speechReadyText, flush)
                    }
                }
            )
        } catch (e: Exception) {
            Log.e("AiSidhuCommentary", "Speak dispatch error: ${e.message}")
            val speechReadyText = HindiSpeechFormatter.formatForHindiTts(text)
            speakViaLocalTts(speechReadyText, flush)
        }
    }

    private fun speakViaLocalTts(speechReadyText: String, flush: Boolean) {
        if (!isTtsReady || tts == null) {
            Log.w("AiSidhuCommentary", "Local TTS not ready")
            return
        }
        try {
            val queueMode = if (flush) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
            val utteranceId = "sidhu_${System.currentTimeMillis()}"
            tts?.speak(speechReadyText, queueMode, null, utteranceId)
        } catch (e: Exception) {
            Log.e("AiSidhuCommentary", "Local TTS speak error: ${e.message}")
        }
    }

    fun stop() {
        try {
            edgeTtsManager.stop()
            tts?.stop()
            _isSpeaking.value = false
        } catch (e: Exception) {
            Log.e("AiSidhuCommentary", "Stop error: ${e.message}")
        }
    }

    private fun requestAudioFocus() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build()
                    )
                    .build()
                audioManager.requestAudioFocus(focusRequest)
            } else {
                @Suppress("DEPRECATION")
                audioManager.requestAudioFocus(
                    null,
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
                )
            }
        } catch (e: Exception) {
            Log.w("AiSidhuCommentary", "Audio focus error: ${e.message}")
        }
    }

    fun triggerTestDialogue() {
        val randomRuns = listOf(4, 6, 0, 1, 2).random()
        val isWicket = randomRuns == 0 && kotlin.random.Random.nextBoolean()
        val dummyBall = com.example.data.model.BallEventEntity(
            matchId = "test",
            overNumber = (1..19).random(),
            ballInOver = (1..6).random(),
            runs = randomRuns,
            isWicket = isWicket,
            batsman = "Batter",
            bowler = "Bowler",
            commentary = ""
        )
        val freshDialogue = SidhuCommentaryGenerator.generateBallCommentary(
            ball = dummyBall,
            strikerName = dummyBall.batsman,
            bowlerName = dummyBall.bowler,
            currentScore = "${(100..220).random()}/${(1..8).random()}",
            style = _voiceStyle.value
        )
        speak(freshDialogue)
    }

    fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (_: Exception) {}
    }

    companion object {
        private const val KEY_PITCH = "sidhu_custom_pitch"
        private const val KEY_SPEED = "sidhu_custom_speed"
        private const val KEY_GENDER = "sidhu_voice_gender"
    }
}

