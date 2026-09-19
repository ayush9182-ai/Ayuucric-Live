package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.SignalWifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.ai.AiEngineMode
import com.example.ui.theme.CricketGreen
import com.example.ui.theme.PitchDark
import com.example.ui.theme.PitchSurface
import com.example.ui.theme.StadiumGold
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun AiSettingsDialog(
    currentMode: AiEngineMode,
    savedApiKey: String,
    sidhuPitch: Float = 0.82f,
    sidhuSpeed: Float = 0.88f,
    sidhuVoiceGender: String = "MALE",
    onSelectMode: (AiEngineMode) -> Unit,
    onSaveApiKey: (String) -> Unit,
    onClearApiKey: () -> Unit,
    onTestApiKey: (String, (Boolean, String) -> Unit) -> Unit,
    onPitchChange: (Float) -> Unit = {},
    onSpeedChange: (Float) -> Unit = {},
    onGenderChange: (String) -> Unit = {},
    onResetVoice: () -> Unit = {},
    onTestVoice: () -> Unit = {},
    onDismiss: () -> Unit
) {
    var keyInput by remember { mutableStateOf(savedApiKey) }
    var isTesting by remember { mutableStateOf(false) }
    var testFeedback by remember { mutableStateOf<Pair<Boolean, String>?>(null) }
    var showPassword by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .clip(RoundedCornerShape(22.dp))
                .background(PitchDark)
                .border(1.5.dp, StadiumGold.copy(alpha = 0.5f), RoundedCornerShape(22.dp))
                .padding(18.dp)
                .testTag("ai_settings_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(StadiumGold.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = StadiumGold,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "🤖 AI & Gemini Key Settings",
                                color = TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = "Viewers aur Organizer dono ke liye options",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // OPTION 1: AUTO HYBRID (Viewers ke liye 100% Free & Automatic)
                ModeCardItem(
                    title = "⚡ Option 1: Auto Mode (Sabhi Viewers Ke Liye)",
                    subtitle = "Recommended: Viewers ko koi key daalne ki zaroorat nahi. Sabke phone par 100% automatic chalta hai! Key ho to Gemini cloud, warna built-in Sidhu Paaji engine.",
                    badge = "ZERO-SETUP FOR VIEWERS",
                    badgeColor = CricketGreen,
                    isSelected = currentMode == AiEngineMode.AUTO_HYBRID,
                    onClick = { onSelectMode(AiEngineMode.AUTO_HYBRID) }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // OPTION 2: CUSTOM KEY (Organizer / Custom key)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .border(
                            1.dp,
                            if (currentMode == AiEngineMode.CUSTOM_KEY) StadiumGold else Color(0xFF1E293B),
                            RoundedCornerShape(14.dp)
                        )
                        .clickable { onSelectMode(AiEngineMode.CUSTOM_KEY) },
                    colors = CardDefaults.cardColors(containerColor = PitchSurface)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = currentMode == AiEngineMode.CUSTOM_KEY,
                                    onClick = { onSelectMode(AiEngineMode.CUSTOM_KEY) },
                                    colors = RadioButtonDefaults.colors(selectedColor = StadiumGold)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = "🔑 Option 2: Custom Gemini API Key",
                                        color = TextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Organizer apni personal Gemini key yahan direct paste kar sakte hain.",
                                        color = TextSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }

                        // Input Box for API Key
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = keyInput,
                            onValueChange = {
                                keyInput = it
                                testFeedback = null
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("custom_api_key_input"),
                            placeholder = {
                                Text("Paste Gemini Key (AIzaSy...)", color = TextSecondary.copy(alpha = 0.6f), fontSize = 12.sp)
                            },
                            singleLine = true,
                            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = StadiumGold,
                                unfocusedBorderColor = Color(0xFF334155),
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedContainerColor = PitchDark,
                                unfocusedContainerColor = PitchDark
                            ),
                            trailingIcon = {
                                Row {
                                    IconButton(onClick = { showPassword = !showPassword }) {
                                        Icon(
                                            imageVector = if (showPassword) Icons.Default.Key else Icons.Default.Lock,
                                            contentDescription = null,
                                            tint = TextSecondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    if (keyInput.isNotBlank()) {
                                        IconButton(onClick = {
                                            keyInput = ""
                                            onClearApiKey()
                                            testFeedback = null
                                        }) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Clear",
                                                tint = Color(0xFFEF4444),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Test & Save Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Test Key Button
                            Button(
                                onClick = {
                                    focusManager.clearFocus()
                                    isTesting = true
                                    onTestApiKey(keyInput) { success, msg ->
                                        isTesting = false
                                        testFeedback = Pair(success, msg)
                                    }
                                },
                                enabled = !isTesting && keyInput.isNotBlank(),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF1E293B),
                                    contentColor = StadiumGold
                                )
                            ) {
                                if (isTesting) {
                                    CircularProgressIndicator(
                                        color = StadiumGold,
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(16.dp)
                                    )
                                } else {
                                    Text("🧪 Test Key", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            // Save Key Button
                            Button(
                                onClick = {
                                    focusManager.clearFocus()
                                    onSaveApiKey(keyInput)
                                },
                                enabled = keyInput.isNotBlank(),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = StadiumGold,
                                    contentColor = PitchDark
                                )
                            ) {
                                Text("💾 Save Key", fontSize = 11.sp, fontWeight = FontWeight.Black)
                            }
                        }

                        // Test Feedback Message
                        if (testFeedback != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            val (success, msg) = testFeedback!!
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (success) CricketGreen.copy(alpha = 0.15f) else Color(0xFFEF4444).copy(alpha = 0.15f))
                                    .border(1.dp, if (success) CricketGreen else Color(0xFFEF4444), RoundedCornerShape(8.dp))
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = msg,
                                    color = if (success) CricketGreen else Color(0xFFEF4444),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // OPTION 3: 100% OFFLINE GROUND MODE
                ModeCardItem(
                    title = "📴 Option 3: 100% Offline Ground Mode",
                    subtitle = "Bina kisi internet ke scorecard aur match data se instant Sidhu Paaji commentary aur summary suniye (0 KB Mobile Data).",
                    badge = "OFFLINE GUARANTEED",
                    badgeColor = Color(0xFF38BDF8),
                    isSelected = currentMode == AiEngineMode.OFFLINE_LOCAL,
                    onClick = { onSelectMode(AiEngineMode.OFFLINE_LOCAL) }
                )

                Spacer(modifier = Modifier.height(14.dp))

                // VOICE CONTROLS: PITCH, SPEED & GENDER
                SidhuVoiceTuningCard(
                    pitch = sidhuPitch,
                    speed = sidhuSpeed,
                    gender = sidhuVoiceGender,
                    onPitchChange = onPitchChange,
                    onSpeedChange = onSpeedChange,
                    onGenderChange = onGenderChange,
                    onResetVoice = onResetVoice,
                    onTestVoice = onTestVoice
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Close / Done button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CricketGreen,
                        contentColor = PitchDark
                    )
                ) {
                    Text("Theek Hai (Done)", fontWeight = FontWeight.Black, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun ModeCardItem(
    title: String,
    subtitle: String,
    badge: String,
    badgeColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(
                1.dp,
                if (isSelected) badgeColor else Color(0xFF1E293B),
                RoundedCornerShape(14.dp)
            )
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = PitchSurface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = isSelected,
                        onClick = onClick,
                        colors = RadioButtonDefaults.colors(selectedColor = badgeColor)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = title,
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(badgeColor.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = badge,
                        color = badgeColor,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                color = TextSecondary,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                modifier = Modifier.padding(start = 38.dp)
            )
        }
    }
}
