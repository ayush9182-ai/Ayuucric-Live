package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.network.NetworkStatus
import com.example.ui.theme.CricketGreen
import com.example.ui.theme.HawkEyeCyan
import com.example.ui.theme.PitchDark
import com.example.ui.theme.PitchSurface
import com.example.ui.theme.StadiumGold
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun NetworkStatusDialog(
    currentStatus: NetworkStatus,
    isForceLiteMode: Boolean,
    onToggleForceLiteMode: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            Brush.linearGradient(listOf(CricketGreen, HawkEyeCyan))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        tint = PitchDark,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Hybrid Connectivity Engine",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        color = TextPrimary
                    )
                    Text(
                        text = "Internet • Slow Net • Offline Mode",
                        fontSize = 10.sp,
                        color = CricketGreen
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Current Active Network Status Banner
                val (statusColor, statusTitle, statusSubtitle) = when (currentStatus) {
                    NetworkStatus.ONLINE_HIGH_SPEED -> Triple(
                        CricketGreen,
                        "🟢 ONLINE (High-Speed Internet / Cloud)",
                        "Full HD Live Video Streaming & Instant Cloud Sync active."
                    )
                    NetworkStatus.ONLINE_LOW_DATA -> Triple(
                        StadiumGold,
                        "🟡 SLOW INTERNET (Lite Mode Active)",
                        "Using < 1 KB per ball! Scorecard and commentary update smoothly without lag."
                    )
                    NetworkStatus.OFFLINE_LOCAL -> Triple(
                        HawkEyeCyan,
                        "⚪ ZERO-INTERNET / OFFLINE (Ground Hotspot)",
                        "Internet off / no signal. App runs 100% locally via Room Database and Ground Wi-Fi."
                    )
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, statusColor)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = statusTitle,
                            color = statusColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = statusSubtitle,
                            color = TextSecondary,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                }

                // 3 Modes Explanation
                Text(
                    text = "Aapka App Teeno Halat Mein Kaam Karta Hai:",
                    color = TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )

                NetworkFeatureRow(
                    icon = Icons.Default.CloudDone,
                    iconTint = CricketGreen,
                    title = "1. Normal Internet (4G / 5G / Wi-Fi)",
                    desc = "Ghar pe baithe parents instant live video aur live scorecard real-time dekh sakte hain."
                )

                NetworkFeatureRow(
                    icon = Icons.Default.NetworkCheck,
                    iconTint = StadiumGold,
                    title = "2. Slow Internet (2G / 3G / Kam Range)",
                    desc = "App automatically data-saver mode me chali jaati hai. Scorecard 1 second bhi freeze nahi hota."
                )

                NetworkFeatureRow(
                    icon = Icons.Default.CloudOff,
                    iconTint = HawkEyeCyan,
                    title = "3. Zero Internet / Offline (Net Off)",
                    desc = "Agar ground par mobile network bilkul na ho, toh ground ke local Wi-Fi hotspot se 100% offline match chalta hai."
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Force Low Data Lite Mode Switch
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Force 2G / Low-Data Lite Mode",
                                color = TextPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Save 95% internet data on weak connections",
                                color = TextSecondary,
                                fontSize = 9.sp
                            )
                        }
                        Switch(
                            checked = isForceLiteMode,
                            onCheckedChange = onToggleForceLiteMode,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = CricketGreen,
                                checkedTrackColor = CricketGreen.copy(alpha = 0.4f)
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = CricketGreen),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Got It!", color = PitchDark, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = PitchSurface,
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
private fun NetworkFeatureRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    title: String,
    desc: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier
                .size(16.dp)
                .padding(top = 2.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(text = title, color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(text = desc, color = TextSecondary, fontSize = 10.sp, lineHeight = 13.sp)
        }
    }
}
