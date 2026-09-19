package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.MatchEntity
import com.example.ui.theme.*

/**
 * Compact Match Switcher & Unified Management Sheet.
 * Replaces the overcrowded top chip strip and provides clean access to
 * match selection, Toss, Squad, Reset, and Ground Tools.
 */
@Composable
fun MatchSwitcherBottomSheet(
    matches: List<MatchEntity>,
    selectedMatchId: String,
    onSelectMatch: (String) -> Unit,
    onOpenToss: () -> Unit,
    onOpenSquad: () -> Unit,
    onOpenResetDialog: () -> Unit,
    onOpenNetworkHub: () -> Unit,
    onOpenGuide: () -> Unit,
    onOpenCreateMatch: () -> Unit,
    onOpenLogin: () -> Unit,
    onOpenAiSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.85f)
                .testTag("match_switcher_modal"),
            shape = RoundedCornerShape(24.dp),
            color = PitchDark,
            border = BorderStroke(1.dp, Color(0xFF1E293B)),
            tonalElevation = 16.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
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
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(CricketGreen.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SportsCricket,
                                contentDescription = null,
                                tint = CricketGreen,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Match Center & Tools",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = TextPrimary
                            )
                            Text(
                                text = "Switch fixtures or manage ground controls",
                                fontSize = 11.sp,
                                color = TextSecondary
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
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Section 1: Quick Ground Management Action Cards (Clean 3-column Grid)
                Text(
                    text = "GROUND CONTROLS",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickActionCard(
                        icon = Icons.Default.Casino,
                        title = "Toss",
                        subtitle = "Coin Flipper",
                        tint = StadiumGold,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            onDismiss()
                            onOpenToss()
                        }
                    )
                    QuickActionCard(
                        icon = Icons.Default.Groups,
                        title = "Squad",
                        subtitle = "Playing 11",
                        tint = CricketGreen,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            onDismiss()
                            onOpenSquad()
                        }
                    )
                    QuickActionCard(
                        icon = Icons.Default.Refresh,
                        title = "Reset 0-0",
                        subtitle = "New Match",
                        tint = DrsOutRed,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            onDismiss()
                            onOpenResetDialog()
                        }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickActionCard(
                        icon = Icons.Default.FlashOn,
                        title = "Network",
                        subtitle = "Offline/Sync",
                        tint = HawkEyeCyan,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            onDismiss()
                            onOpenNetworkHub()
                        }
                    )
                    QuickActionCard(
                        icon = Icons.Default.AutoAwesome,
                        title = "Sidhu Paaji",
                        subtitle = "AI Voice",
                        tint = Color(0xFFA78BFA),
                        modifier = Modifier.weight(1f),
                        onClick = {
                            onDismiss()
                            onOpenAiSettings()
                        }
                    )
                    QuickActionCard(
                        icon = Icons.Default.MenuBook,
                        title = "Guide",
                        subtitle = "App Tutorial",
                        tint = Color(0xFF60A5FA),
                        modifier = Modifier.weight(1f),
                        onClick = {
                            onDismiss()
                            onOpenGuide()
                        }
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Section 2: Switch Match List
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "AVAILABLE FIXTURES (${matches.size})",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        letterSpacing = 1.sp
                    )

                    TextButton(
                        onClick = {
                            onDismiss()
                            onOpenCreateMatch()
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = CricketGreen,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "New Match",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = CricketGreen
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(matches, key = { it.id }) { match ->
                        val isSelected = match.id == selectedMatchId
                        val isLive = match.status == "LIVE"

                        Surface(
                            color = if (isSelected) Color(0xFF131B2A) else Color(0xFF0F172A),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(
                                if (isSelected) 1.5.dp else 0.5.dp,
                                if (isSelected) CricketGreen else Color(0xFF1E293B)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelectMatch(match.id)
                                    onDismiss()
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (isLive) {
                                            Box(
                                                modifier = Modifier
                                                    .size(7.dp)
                                                    .clip(CircleShape)
                                                    .background(DrsOutRed)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                        }

                                        Text(
                                            text = "${match.teamA} vs ${match.teamB}",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )

                                        Spacer(modifier = Modifier.width(8.dp))

                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(
                                                    if (isLive) DrsOutRed.copy(alpha = 0.2f)
                                                    else Color(0xFF334155).copy(alpha = 0.4f)
                                                )
                                                .padding(horizontal = 5.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = match.status,
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Black,
                                                color = if (isLive) DrsOutRed else TextSecondary
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    val scoreText = "${match.score}/${match.wickets} (${match.legalBalls / 6}.${match.legalBalls % 6} ov)"
                                    Text(
                                        text = "$scoreText • ${match.statusDetail}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (isLive) StadiumGold else TextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Text(
                                        text = "${match.venue} • ${match.totalOvers} Overs",
                                        fontSize = 10.sp,
                                        color = TextMuted
                                    )
                                }

                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(CricketGreen),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = PitchDark,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Bottom Account Login Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF0F172A))
                        .clickable {
                            onDismiss()
                            onOpenLogin()
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = StadiumGold,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Login / Switch CricHeroes Account",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                    }
                    Text(
                        text = "Switch ›",
                        fontSize = 11.sp,
                        color = StadiumGold,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickActionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    tint: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        color = Color(0xFF0F172A),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(0.5.dp, Color(0xFF1E293B)),
        modifier = modifier.clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(tint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(15.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                maxLines = 1
            )
            Text(
                text = subtitle,
                fontSize = 9.sp,
                color = TextSecondary,
                maxLines = 1
            )
        }
    }
}
