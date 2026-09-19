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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CricketGreen
import com.example.ui.theme.PitchDark
import com.example.ui.theme.PitchSurface
import com.example.ui.theme.StadiumGold
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun SidhuVoiceTuningCard(
    pitch: Float,
    speed: Float,
    gender: String,
    onPitchChange: (Float) -> Unit,
    onSpeedChange: (Float) -> Unit,
    onGenderChange: (String) -> Unit,
    onResetVoice: () -> Unit,
    onTestVoice: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, StadiumGold.copy(alpha = 0.45f), RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = PitchSurface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(StadiumGold.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = StadiumGold,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "🎙️ Sidhu Paaji Voice Controls",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Awaaz, Pitch aur Raftaar adjust karein",
                            color = TextSecondary,
                            fontSize = 10.sp
                        )
                    }
                }

                // Reset Button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF1E293B))
                        .clickable { onResetVoice() }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset",
                            tint = StadiumGold,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "Reset Default",
                            fontSize = 10.sp,
                            color = StadiumGold,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Gender Selection (Male / Female / Auto)
            Text(
                text = "Voice Gender / Type:",
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GenderChip(
                    label = "👨 Sidhu (Male)",
                    isSelected = gender == "MALE",
                    onClick = { onGenderChange("MALE") },
                    modifier = Modifier.weight(1f)
                )
                GenderChip(
                    label = "👩 Female Voice",
                    isSelected = gender == "FEMALE",
                    onClick = { onGenderChange("FEMALE") },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Pitch Control Slider (Bhaari Awaz vs Patli Awaz)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "🔊 Voice Pitch (Awaaz Ka Bhaari-pan):",
                        color = TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                val pitchDescription = if (pitch <= 0.85f) {
                    "Deep Male Sidhu (Bhaari)"
                } else if (pitch <= 1.0f) {
                    "Natural"
                } else {
                    "High Pitch"
                }
                Text(
                    text = String.format("%.2fx - %s", pitch, pitchDescription),
                    color = StadiumGold,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Slider(
                value = pitch,
                onValueChange = { onPitchChange(it) },
                valueRange = 0.60f..1.35f,
                steps = 15,
                colors = SliderDefaults.colors(
                    thumbColor = StadiumGold,
                    activeTrackColor = StadiumGold,
                    inactiveTrackColor = Color(0xFF334155)
                ),
                modifier = Modifier.height(28.dp)
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Speed Control Slider (Raftaar)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Speech Speed (Khabar Ki Raftaar):",
                        color = TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                val speedDescription = if (speed <= 0.85f) {
                    "Dheemi & Saaf"
                } else if (speed <= 0.95f) {
                    "Perfect Pace"
                } else {
                    "Fast Josh"
                }
                Text(
                    text = String.format("%.2fx - %s", speed, speedDescription),
                    color = CricketGreen,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Slider(
                value = speed,
                onValueChange = { onSpeedChange(it) },
                valueRange = 0.60f..1.30f,
                steps = 14,
                colors = SliderDefaults.colors(
                    thumbColor = CricketGreen,
                    activeTrackColor = CricketGreen,
                    inactiveTrackColor = Color(0xFF334155)
                ),
                modifier = Modifier.height(28.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Test Voice Button
            Button(
                onClick = onTestVoice,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1E293B),
                    contentColor = StadiumGold
                )
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = null,
                        tint = StadiumGold,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "🔊 Bolke Suno (Test Voice Sample)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun GenderChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) StadiumGold.copy(alpha = 0.2f) else PitchDark)
            .border(
                1.dp,
                if (isSelected) StadiumGold else Color(0xFF334155),
                RoundedCornerShape(8.dp)
            )
            .clickable { onClick() }
            .padding(vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isSelected) StadiumGold else TextSecondary,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}
