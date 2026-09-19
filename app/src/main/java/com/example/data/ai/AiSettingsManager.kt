package com.example.data.ai

import android.content.Context
import android.content.SharedPreferences
import com.example.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AiEngineMode(val title: String, val description: String) {
    AUTO_HYBRID(
        "Auto Hybrid (Recommended for All Viewers)",
        "Sabke phone par bina kisi key ke chalta hai. Agar build key hai to Gemini Cloud, warna offline Smart Sidhu Engine!"
    ),
    CUSTOM_KEY(
        "Organizer Custom Key",
        "Apni personal Gemini API key se direct connect karein."
    ),
    OFFLINE_LOCAL(
        "100% Offline Ground Mode",
        "Zero internet data. Scorecard aur stats se turant instant summary banata hai."
    )
}

class AiSettingsManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("ayuu_cric_ai_prefs", Context.MODE_PRIVATE)

    private val _customApiKey = MutableStateFlow(prefs.getString(KEY_CUSTOM_API_KEY, "") ?: "")
    val customApiKey: StateFlow<String> = _customApiKey.asStateFlow()

    private val _currentMode = MutableStateFlow(
        try {
            AiEngineMode.valueOf(prefs.getString(KEY_AI_MODE, AiEngineMode.AUTO_HYBRID.name) ?: AiEngineMode.AUTO_HYBRID.name)
        } catch (_: Exception) {
            AiEngineMode.AUTO_HYBRID
        }
    )
    val currentMode: StateFlow<AiEngineMode> = _currentMode.asStateFlow()

    fun saveCustomApiKey(key: String) {
        val trimmed = key.trim()
        prefs.edit().putString(KEY_CUSTOM_API_KEY, trimmed).apply()
        _customApiKey.value = trimmed
        if (trimmed.isNotBlank()) {
            setAiMode(AiEngineMode.CUSTOM_KEY)
        }
    }

    fun clearCustomApiKey() {
        prefs.edit().remove(KEY_CUSTOM_API_KEY).apply()
        _customApiKey.value = ""
        setAiMode(AiEngineMode.AUTO_HYBRID)
    }

    fun setAiMode(mode: AiEngineMode) {
        prefs.edit().putString(KEY_AI_MODE, mode.name).apply()
        _currentMode.value = mode
    }

    fun getEffectiveApiKey(): String {
        return when (_currentMode.value) {
            AiEngineMode.OFFLINE_LOCAL -> ""
            AiEngineMode.CUSTOM_KEY -> _customApiKey.value
            AiEngineMode.AUTO_HYBRID -> {
                if (_customApiKey.value.isNotBlank()) {
                    _customApiKey.value
                } else {
                    try {
                        val buildKey = BuildConfig.GEMINI_API_KEY
                        if (buildKey.isNotBlank() && buildKey != "MY_GEMINI_API_KEY") buildKey else ""
                    } catch (_: Exception) {
                        ""
                    }
                }
            }
        }
    }

    companion object {
        private const val KEY_CUSTOM_API_KEY = "custom_gemini_api_key"
        private const val KEY_AI_MODE = "ai_engine_mode"
    }
}
