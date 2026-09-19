package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.CricHeroesProfile
import com.example.ui.theme.*

@Composable
fun PlayerProfileCardDialog(
    profile: CricHeroesProfile,
    onDismiss: () -> Unit,
    onOpenDmWithPlayer: ((CricHeroesProfile) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .padding(vertical = 16.dp)
                .testTag("player_profile_card_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = PitchDark),
            border = BorderStroke(
                1.5.dp,
                Brush.linearGradient(
                    listOf(StadiumGold, CricketGreen, HawkEyeCyan, StadiumGold)
                )
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                // Header with Close
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "PLAYER IDENTITY CARD",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.2.sp,
                            color = StadiumGold
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Premium Holographic Player ID Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(12.dp, RoundedCornerShape(18.dp)),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                    border = BorderStroke(
                        1.dp,
                        Brush.linearGradient(listOf(Color(0xFF38BDF8), Color(0xFF00E676)))
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color(0xFF0F172A), Color(0xFF0A192F))
                                )
                            )
                            .padding(16.dp)
                    ) {
                        // Top row: Avatar + Name + Online badge
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(Color(0xFF1E293B), Color(0xFF0F172A))
                                        )
                                    )
                                    .border(2.dp, StadiumGold, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = profile.avatarEmoji, fontSize = 32.sp)
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = profile.fullName.ifBlank { profile.jerseyName },
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = Icons.Default.Verified,
                                        contentDescription = "Verified",
                                        tint = HawkEyeCyan,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                Text(
                                    text = "@${profile.username.removePrefix("@")}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = StadiumGold
                                )

                                Text(
                                    text = "${profile.jerseyName} #${profile.jerseyNumber}",
                                    fontSize = 11.sp,
                                    color = TextSecondary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            // Status badge (Online / Fallback)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (profile.isOnline) CricketGreen.copy(alpha = 0.15f)
                                        else Color(0xFF334155).copy(alpha = 0.4f)
                                    )
                                    .border(
                                        1.dp,
                                        if (profile.isOnline) CricketGreen else Color(0xFF475569),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(if (profile.isOnline) CricketGreen else Color.Gray)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (profile.isOnline) "LIVE" else "OFFLINE",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (profile.isOnline) CricketGreen else Color.Gray
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Role & Team metadata row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF1E293B).copy(alpha = 0.7f))
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("ROLE", fontSize = 9.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                                Text(profile.primaryRole, fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("TEAM", fontSize = 9.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                                Text(profile.teamName.ifBlank { "Free Agent" }, fontSize = 11.sp, color = HawkEyeCyan, fontWeight = FontWeight.Bold)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("CITY", fontSize = 9.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                                Text(profile.city.ifBlank { "India" }, fontSize = 11.sp, color = StadiumGold, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Cricket Career Stats Grid
                Text(
                    text = "CAREER STATS & PLAYING STYLE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    letterSpacing = 0.8.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatCard(
                        title = "Runs",
                        value = "${profile.runs}",
                        color = StadiumGold,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Wickets",
                        value = "${profile.wickets}",
                        color = CricketGreen,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Matches",
                        value = "${profile.matchesPlayed}",
                        color = HawkEyeCyan,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Strike Rate",
                        value = "${profile.strikeRate}",
                        color = Color(0xFFA78BFA),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Batting & Bowling Styles
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    border = BorderStroke(1.dp, Color(0xFF334155))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("🏏 Batting Style:", fontSize = 11.sp, color = TextSecondary)
                            Text(profile.battingStyle, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("🎯 Bowling Style:", fontSize = 11.sp, color = TextSecondary)
                            Text(profile.bowlingStyle, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        if (profile.mobileNumber.isNotBlank()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("📱 Registered Mobile:", fontSize = 11.sp, color = TextSecondary)
                                Text(
                                    // Mask middle digits for privacy
                                    if (profile.mobileNumber.length > 6) {
                                        profile.mobileNumber.take(4) + " **** " + profile.mobileNumber.takeLast(2)
                                    } else profile.mobileNumber,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = HawkEyeCyan
                                )
                            }
                        }
                    }
                }

                // Action Buttons
                Spacer(modifier = Modifier.height(16.dp))

                if (onOpenDmWithPlayer != null) {
                    Button(
                        onClick = {
                            onOpenDmWithPlayer(profile)
                            onDismiss()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CricketGreen),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Chat,
                            contentDescription = null,
                            tint = PitchDark,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Send Direct Message (@${profile.username.removePrefix("@")})",
                            color = PitchDark,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFF334155))
                ) {
                    Text("Close Card", color = TextSecondary, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF111827))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
            .padding(vertical = 8.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = value,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                color = color
            )
            Text(
                text = title,
                fontSize = 9.sp,
                color = TextSecondary,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
