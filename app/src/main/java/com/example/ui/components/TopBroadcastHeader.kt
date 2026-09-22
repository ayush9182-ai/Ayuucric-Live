package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CricHeroesProfile
import com.example.data.model.DeviceRole
import com.example.data.model.MatchEntity
import com.example.data.network.NetworkStatus
import com.example.ui.theme.*
import com.example.util.ApkShareHelper

/**
 * Streamlined Top Broadcast Header (Maximum 4 Focus Elements)
 *  1. Left: App Logo (AyuuCric) + Minimal Network Status Dot
 *  2. Right: Messages Hub Icon (Unread Badge), Role Badge/PIN Chip, Player Profile Avatar
 *  Followed by a single compact match selector card [Team A vs Team B • LIVE • 14.2 Ov] ▾
 */
@Composable
fun TopBroadcastHeader(
    currentMatch: MatchEntity?,
    currentRole: DeviceRole,
    networkStatus: NetworkStatus,
    userProfile: CricHeroesProfile,
    unreadMessagesCount: Int = 0,
    onOpenNetworkDialog: () -> Unit,
    onOpenMessagesHub: () -> Unit,
    onOpenRoleDialog: () -> Unit,
    onOpenProfileDialog: () -> Unit,
    onOpenMatchSwitcher: () -> Unit,
    onShareWhatsApp: () -> Unit = {},
    onOpenWagonWheel: () -> Unit = {}
) {
    val context = LocalContext.current
    Surface(
        color = PitchSurface,
        border = BorderStroke(0.5.dp, Color(0xFF1E293B)),
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .testTag("top_broadcast_header")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            // Top Row: Exactly 4 Primary Elements
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Focus Element 1 (Left): AyuuCric Branding + Minimal Network Status Dot
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onOpenNetworkDialog() }
                        .padding(vertical = 2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(CricketGreen, HawkEyeCyan))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SportsCricket,
                            contentDescription = "AyuuCric",
                            tint = PitchDark,
                            modifier = Modifier.size(17.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(7.dp))

                    Text(
                        text = "AyuuCric",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    // Minimal Network Status Dot
                    val dotColor = when (networkStatus) {
                        NetworkStatus.ONLINE_HIGH_SPEED -> Color(0xFF22C55E)
                        NetworkStatus.ONLINE_LOW_DATA -> Color(0xFFF59E0B)
                        NetworkStatus.OFFLINE_LOCAL -> Color(0xFF06B6D4)
                    }

                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(dotColor)
                    )
                }

                // Right Focus Elements (2, 3, 4)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Focus Element 2: Messages Hub Icon (Live Room Chat + DMs)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF0F172A))
                            .border(1.dp, Color(0xFF334155), RoundedCornerShape(10.dp))
                            .clickable { onOpenMessagesHub() }
                            .padding(horizontal = 9.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Chat,
                                contentDescription = "Messages Hub",
                                tint = CricketGreen,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Chat",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            if (unreadMessagesCount > 0) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFE1306C))
                                )
                            }
                        }
                    }

                    // Focus Element 3: Active Role Badge / PIN Lock Chip
                    val (roleColor, roleLabel) = when (currentRole) {
                        DeviceRole.SPECTATOR_VIEWER -> Pair(Color(0xFF64748B), "Spectator")
                        DeviceRole.OFFICIAL_SCORER -> Pair(StadiumGold, "Scorer")
                        DeviceRole.BOWLER_END_UMPIRE, DeviceRole.SQUARE_LEG_UMPIRE -> Pair(CricketGreen, "Umpire")
                        DeviceRole.THIRD_UMPIRE_DRS -> Pair(Color(0xFFA78BFA), "3rd Umpire")
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(roleColor.copy(alpha = 0.15f))
                            .border(1.dp, roleColor.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                            .clickable { onOpenRoleDialog() }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (currentRole == DeviceRole.SPECTATOR_VIEWER) Icons.Default.Shield else Icons.Default.Lock,
                                contentDescription = "Role Lock",
                                tint = roleColor,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = roleLabel,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = roleColor
                            )
                        }
                    }

                    // Focus Element 4: Player Profile Circular Avatar
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E293B))
                            .border(1.2.dp, CricketGreen, CircleShape)
                            .clickable { onOpenProfileDialog() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = userProfile.avatarEmoji.ifBlank { "🏏" },
                            fontSize = 17.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Compact Match Selector Dropdown Bar (Replaces cluttered horizontal scroll strip)
            val match = currentMatch
            val isLive = match?.status == "LIVE"

            Surface(
                color = Color(0xFF0F172A),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(0.8.dp, Color(0xFF1E293B)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenMatchSwitcher() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (isLive) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(DrsOutRed)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }

                        val fixtureText = if (match != null) {
                            val scoreStr = "${match.score}/${match.wickets} (${match.legalBalls / 6}.${match.legalBalls % 6} ov)"
                            "${match.teamAShort} vs ${match.teamBShort} • ${match.status} • $scoreStr"
                        } else {
                            "No Match Live • Tap to Select / Create 🏏"
                        }

                        Text(
                            text = fixtureText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isLive) StadiumGold else TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Quick 1-Tap Share APK Button (Direct APK Share without USB)
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF6366F1).copy(alpha = 0.25f))
                                .border(1.dp, Color(0xFF818CF8), CircleShape)
                                .clickable { ApkShareHelper.shareInstalledApk(context) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Android,
                                contentDescription = "Share App APK",
                                tint = Color(0xFFA5B4FC),
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // Quick 1-Tap WhatsApp Share Poster
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF25D366).copy(alpha = 0.25f))
                                .border(1.dp, Color(0xFF25D366), CircleShape)
                                .clickable { onShareWhatsApp() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share WhatsApp",
                                tint = Color(0xFF25D366),
                                modifier = Modifier.size(15.dp)
                            )
                        }

                        // Quick 1-Tap Wagon Wheel Radar
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(HawkEyeCyan.copy(alpha = 0.25f))
                                .border(1.dp, HawkEyeCyan, CircleShape)
                                .clickable { onOpenWagonWheel() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.TrackChanges,
                                contentDescription = "Wagon Wheel Radar",
                                tint = HawkEyeCyan,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { onOpenMatchSwitcher() }
                        ) {
                            Text(
                                text = "Switch",
                                fontSize = 10.sp,
                                color = TextSecondary,
                                fontWeight = FontWeight.Medium
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Switch Match",
                                tint = TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
