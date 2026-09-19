package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SlowMotionVideo
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.HighlightClip
import com.example.ui.theme.CricketGreen
import com.example.ui.theme.DrsOutRed
import com.example.ui.theme.HawkEyeCyan
import com.example.ui.theme.PitchCard
import com.example.ui.theme.PitchCardBorder
import com.example.ui.theme.PitchDark
import com.example.ui.theme.StadiumGold
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun HighlightsReelScreen(
    clips: List<HighlightClip>,
    selectedClip: HighlightClip?,
    isPlaying: Boolean,
    progress: Float,
    videoSpeed: String,
    cameraAngle: String,
    onSelectClip: (HighlightClip) -> Unit,
    onTogglePlay: () -> Unit,
    onSetSpeed: (String) -> Unit,
    onSetCameraAngle: (String) -> Unit,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var filterCategory by remember { mutableStateOf("ALL") }
    val scrollState = rememberScrollState()

    val currentClip = selectedClip ?: clips.firstOrNull()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
            .testTag("highlights_reel_screen")
    ) {
        // Video Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Videocam,
                    contentDescription = "Highlights",
                    tint = StadiumGold,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "MATCH HIGHLIGHTS VIDEO",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        color = TextPrimary
                    )
                    Text(
                        text = "Instant 60FPS Video Replay • Broadcast Clips",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF1E293B))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "HD 1080p",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = HawkEyeCyan
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Native High-FPS Animated Video Player
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF030712)),
            border = CardDefaults.outlinedCardBorder().copy(
                brush = Brush.linearGradient(listOf(StadiumGold.copy(alpha = 0.5f), HawkEyeCyan.copy(alpha = 0.3f))),
                width = 1.2.dp
            )
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Video Screen Canvas
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                        .background(Color(0xFF070B14))
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawHighlightVideoScene(
                            animType = currentClip?.animationType ?: "MONSTER_SIX",
                            progress = progress,
                            cameraAngle = cameraAngle
                        )
                    }

                    // Watermark / Match Info Tag
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(10.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xCC000000))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${currentClip?.overText} • ${currentClip?.category}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = StadiumGold
                        )
                    }

                    // Cam Angle Badge
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(10.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0x990F172A))
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "$cameraAngle View",
                            fontSize = 9.sp,
                            color = HawkEyeCyan,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Video Progress Scrubber Bar
                Slider(
                    value = progress,
                    onValueChange = onSeek,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .height(28.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = StadiumGold,
                        activeTrackColor = StadiumGold,
                        inactiveTrackColor = Color(0xFF334155)
                    )
                )

                // Player Controls (Play/Pause, Slow-Mo 0.5x, Camera Angle)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onTogglePlay,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(StadiumGold)
                                .testTag("highlight_play_pause_btn")
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Play",
                                tint = PitchDark,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        val currentSec = (progress * 38).toInt()
                        Text(
                            text = "0:${currentSec.toString().padStart(2, '0')} / ${currentClip?.durationText ?: "0:38"}",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        // Slow motion button
                        FilterChip(
                            selected = videoSpeed == "0.5x",
                            onClick = { onSetSpeed(if (videoSpeed == "0.5x") "1.0x" else "0.5x") },
                            label = { Text("0.5x Slow-Mo", fontSize = 10.sp) },
                            modifier = Modifier.height(26.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = HawkEyeCyan.copy(alpha = 0.25f),
                                selectedLabelColor = HawkEyeCyan
                            )
                        )

                        // Camera angle switch
                        FilterChip(
                            selected = cameraAngle == "Pitch Cam",
                            onClick = { onSetCameraAngle(if (cameraAngle == "Pitch Cam") "Broadcast" else "Pitch Cam") },
                            label = { Text("Angle", fontSize = 10.sp) },
                            modifier = Modifier.height(26.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CricketGreen.copy(alpha = 0.25f),
                                selectedLabelColor = CricketGreen
                            )
                        )
                    }
                }

                // Title & Description
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
                    Text(
                        text = currentClip?.title ?: "",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = currentClip?.description ?: "",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        lineHeight = 16.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Clip Categories
        Text(
            text = "MATCH KEY MOMENTS & REELS",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = TextMuted,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf("ALL", "SIX", "WICKET", "DRS", "CLUTCH").forEach { cat ->
                FilterChip(
                    selected = filterCategory == cat,
                    onClick = { filterCategory = cat },
                    label = { Text(cat, fontSize = 11.sp) },
                    modifier = Modifier.height(28.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = StadiumGold.copy(alpha = 0.2f),
                        selectedLabelColor = StadiumGold,
                        labelColor = TextSecondary
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Filtered Video Clips List
        val filteredClips = if (filterCategory == "ALL") clips else clips.filter { it.category == filterCategory }
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            filteredClips.forEach { clip ->
                val isSelected = clip.id == currentClip?.id
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectClip(clip) }
                        .testTag("highlight_clip_${clip.id}"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) Color(0xFF1E293B) else PitchCard
                    ),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(
                            if (isSelected) StadiumGold else PitchCardBorder
                        ),
                        width = if (isSelected) 1.2.dp else 0.8.dp
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Thumbnail Box
                        Box(
                            modifier = Modifier
                                .size(58.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    when (clip.category) {
                                        "SIX" -> StadiumGold.copy(alpha = 0.2f)
                                        "WICKET" -> DrsOutRed.copy(alpha = 0.2f)
                                        "DRS" -> HawkEyeCyan.copy(alpha = 0.2f)
                                        else -> CricketGreen.copy(alpha = 0.2f)
                                    }
                                )
                                .border(
                                    1.dp,
                                    when (clip.category) {
                                        "SIX" -> StadiumGold.copy(alpha = 0.5f)
                                        "WICKET" -> DrsOutRed.copy(alpha = 0.5f)
                                        "DRS" -> HawkEyeCyan.copy(alpha = 0.5f)
                                        else -> CricketGreen.copy(alpha = 0.5f)
                                    },
                                    RoundedCornerShape(10.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play Clip",
                                tint = when (clip.category) {
                                    "SIX" -> StadiumGold
                                    "WICKET" -> DrsOutRed
                                    "DRS" -> HawkEyeCyan
                                    else -> CricketGreen
                                },
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = clip.title,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    maxLines = 1
                                )
                                Text(
                                    text = clip.durationText,
                                    fontSize = 11.sp,
                                    color = StadiumGold,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = clip.bowlerVsBatter,
                                fontSize = 11.sp,
                                color = TextSecondary,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

// Canvas Helper for Highlights scene animation
private fun DrawScope.drawHighlightVideoScene(
    animType: String,
    progress: Float,
    cameraAngle: String
) {
    val w = size.width
    val h = size.height

    // Stadium night backdrop with floodlights
    drawRect(
        brush = Brush.verticalGradient(
            listOf(Color(0xFF030712), Color(0xFF0B1726), Color(0xFF06331C))
        )
    )

    // Floodlight beams
    val beamColor = Color.White.copy(alpha = 0.08f)
    drawCircle(color = beamColor, radius = 40.dp.toPx(), center = Offset(w * 0.15f, h * 0.15f))
    drawCircle(color = beamColor, radius = 40.dp.toPx(), center = Offset(w * 0.85f, h * 0.15f))

    // Pitch Green strip
    drawRect(
        color = Color(0xFFD4A373),
        topLeft = Offset(w * 0.35f, h * 0.60f),
        size = Size(w * 0.30f, h * 0.35f)
    )

    // Stumps at batting end
    val stumpX = w * 0.50f
    val stumpY = h * 0.70f
    val stumpHeight = 24.dp.toPx()

    val isWicketBroken = animType == "YORKER_WICKET" && progress > 0.45f

    for (i in -1..1) {
        val x = stumpX + (i * 6.dp.toPx())
        val angleOffset = if (isWicketBroken && i == 0) (progress - 0.45f) * 20.dp.toPx() else 0f
        drawLine(
            color = if (isWicketBroken) Color(0xFFFF5252) else Color(0xFFFBBF24),
            start = Offset(x, stumpY),
            end = Offset(x + angleOffset, stumpY - stumpHeight),
            strokeWidth = 3.dp.toPx()
        )
    }

    // Ball Animation based on Type
    when (animType) {
        "MONSTER_SIX" -> {
            // Ball launched high into upper deck
            val startX = w * 0.50f
            val startY = h * 0.70f
            val apexX = w * 0.70f
            val apexY = h * 0.15f
            val endX = w * 0.95f
            val endY = h * 0.30f

            val ballX = when {
                progress < 0.5f -> startX + (apexX - startX) * (progress / 0.5f)
                else -> apexX + (endX - apexX) * ((progress - 0.5f) / 0.5f)
            }
            val ballY = when {
                progress < 0.5f -> startY - (startY - apexY) * (progress / 0.5f)
                else -> apexY + (endY - apexY) * ((progress - 0.5f) / 0.5f)
            }

            // Glow trail
            drawCircle(color = StadiumGold.copy(alpha = 0.4f), radius = 12.dp.toPx(), center = Offset(ballX, ballY))
            drawCircle(color = Color(0xFFFF1744), radius = 6.dp.toPx(), center = Offset(ballX, ballY))
        }

        "YORKER_WICKET" -> {
            val startX = w * 0.20f
            val startY = h * 0.50f
            val ballX = if (progress < 0.45f) startX + (stumpX - startX) * (progress / 0.45f) else stumpX + (progress - 0.45f) * 15.dp.toPx()
            val ballY = if (progress < 0.45f) startY + (stumpY - startY) * (progress / 0.45f) else stumpY

            drawCircle(color = Color(0xFFD50000), radius = 6.dp.toPx(), center = Offset(ballX, ballY))

            // Shattered bails flying in air!
            if (isWicketBroken) {
                val bailY = stumpY - stumpHeight - ((progress - 0.45f) * 35.dp.toPx())
                drawLine(
                    color = Color(0xFFFEF3C7),
                    start = Offset(stumpX - 10.dp.toPx(), bailY),
                    end = Offset(stumpX + 10.dp.toPx(), bailY - 8.dp.toPx()),
                    strokeWidth = 2.dp.toPx()
                )
            }
        }

        else -> {
            // General drive / DRS trajectory
            val ballX = w * 0.30f + (progress * w * 0.5f)
            val ballY = h * 0.65f + sin(progress * 3.14f) * 20.dp.toPx()
            drawCircle(color = HawkEyeCyan, radius = 8.dp.toPx(), center = Offset(ballX, ballY))
            drawCircle(color = Color(0xFFFF1744), radius = 5.dp.toPx(), center = Offset(ballX, ballY))
        }
    }
}
