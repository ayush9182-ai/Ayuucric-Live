package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BroadcastOverlayEvent
import kotlinx.coroutines.delay

@Composable
fun BroadcastGraphicOverlay(
    event: BroadcastOverlayEvent,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Auto dismiss after 4.5 seconds
    LaunchedEffect(event.id) {
        delay(4500)
        onDismiss()
    }

    val infiniteTransition = rememberInfiniteTransition(label = "broadcast_glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    val accentColor = Color(event.accentColorHex)

    val bgGradient = when (event.type) {
        BroadcastOverlayEvent.OverlayType.WICKET_DISMISSAL -> Brush.horizontalGradient(
            listOf(Color(0xFF450A0A), Color(0xFF1F0404), Color(0xFF0F172A))
        )
        BroadcastOverlayEvent.OverlayType.MILESTONE_50,
        BroadcastOverlayEvent.OverlayType.MILESTONE_100 -> Brush.horizontalGradient(
            listOf(Color(0xFF422006), Color(0xFF1E1B08), Color(0xFF0F172A))
        )
        BroadcastOverlayEvent.OverlayType.MAXIMUM_SIX -> Brush.horizontalGradient(
            listOf(Color(0xFF14532D), Color(0xFF0A2E1A), Color(0xFF0F172A))
        )
        BroadcastOverlayEvent.OverlayType.BOUNDARY_FOUR -> Brush.horizontalGradient(
            listOf(Color(0xFF082F49), Color(0xFF031C2D), Color(0xFF0F172A))
        )
        else -> Brush.horizontalGradient(
            listOf(Color(0xFF1E293B), Color(0xFF0F172A))
        )
    }

    val iconVector = when (event.type) {
        BroadcastOverlayEvent.OverlayType.WICKET_DISMISSAL -> Icons.Default.Warning
        BroadcastOverlayEvent.OverlayType.MILESTONE_50,
        BroadcastOverlayEvent.OverlayType.MILESTONE_100 -> Icons.Default.EmojiEvents
        BroadcastOverlayEvent.OverlayType.MAXIMUM_SIX -> Icons.Default.Bolt
        BroadcastOverlayEvent.OverlayType.BOUNDARY_FOUR -> Icons.Default.FlashOn
        else -> Icons.Default.SportsCricket
    }

    val badgeLabel = when (event.type) {
        BroadcastOverlayEvent.OverlayType.WICKET_DISMISSAL -> "⚡ WICKET REPLAY"
        BroadcastOverlayEvent.OverlayType.MILESTONE_100 -> "👑 CENTURY CELEBRATION"
        BroadcastOverlayEvent.OverlayType.MILESTONE_50 -> "⭐ HALF CENTURY 50"
        BroadcastOverlayEvent.OverlayType.MAXIMUM_SIX -> "💥 MAXIMUM SIX"
        BroadcastOverlayEvent.OverlayType.BOUNDARY_FOUR -> "⚡ BOUNDARY FOUR"
        else -> "LIVE BROADCAST GRAPHIC"
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .clickable { onDismiss() }
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(16.dp, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            border = BorderStroke(1.5.dp, accentColor.copy(alpha = glowAlpha))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(bgGradient)
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left Glowing Emblem
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.2f))
                            .border(1.5.dp, accentColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = iconVector,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Text Content
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(accentColor)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = badgeLabel,
                                    color = Color.Black,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.5.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "AyuuCric TV",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.height(3.dp))

                        Text(
                            text = event.headline,
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            maxLines = 1
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = event.subheadline,
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (event.statDetail.isNotBlank()) {
                                Text(
                                    text = " • ${event.statDetail}",
                                    color = accentColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Dismiss icon
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss",
                            tint = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
