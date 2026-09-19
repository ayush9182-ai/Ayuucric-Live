package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
fun ZeroDelayLiveRadar(
    onTriggerDelivery: () -> Unit,
    modifier: Modifier = Modifier,
    animPhase: Float? = null
) {
    var viewMode by remember { mutableStateOf("3D Pitch") } // "3D Pitch" or "Field Radar"

    val infiniteTransition = rememberInfiniteTransition(label = "pitchRadarAnim")
    val internalPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "internalAnimPhase"
    )
    val effectivePhase = animPhase ?: internalPhase

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("zero_delay_radar_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = PitchCard),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.linearGradient(
                listOf(PitchCardBorder, HawkEyeCyan.copy(alpha = 0.4f), Color(0xFF0D9488))
            ),
            width = 1.2.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Live Stream Header Banner
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Sensors,
                        contentDescription = "Live Signal",
                        tint = CricketGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "ZERO-DELAY LIVE STREAM",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary,
                        letterSpacing = 0.8.sp
                    )
                }

                // Zero Buffering Pill
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF064E3B))
                        .border(0.6.dp, CricketGreen, RoundedCornerShape(10.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = "Zero Lag",
                        tint = CricketGreen,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "0ms Lag • 60 FPS",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = CricketGreen
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Canvas Display: 3D Pitch or Field Radar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF0B1726))
                    .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(14.dp))
            ) {
                if (viewMode == "3D Pitch") {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        draw3DPitchSimulation(effectivePhase)
                    }
                } else {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawFieldRadarSimulation(effectivePhase)
                    }
                }

                // Speed & Delivery Telemetry Overlay
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xCC000000))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = "Speed",
                            tint = HawkEyeCyan,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "137.4 km/h • Good Length",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = HawkEyeCyan
                        )
                    }
                }

                // Delivery Status Pill (e.g., Bowler Running / Pitched / Shot Played)
                val statusText = when {
                    effectivePhase < 0.25f -> "Bowler Run-up & Release"
                    effectivePhase < 0.55f -> "Pitched (Good Length)"
                    effectivePhase < 0.85f -> "Cover Drive Placed!"
                    else -> "Fielder Intercepting..."
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xCC0A192F))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = statusText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Camera Angle and Action Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = viewMode == "3D Pitch",
                        onClick = { viewMode = "3D Pitch" },
                        label = { Text("Pitch Cam", fontSize = 11.sp) },
                        modifier = Modifier.height(28.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CricketGreen.copy(alpha = 0.2f),
                            selectedLabelColor = CricketGreen
                        )
                    )

                    FilterChip(
                        selected = viewMode == "Field Radar",
                        onClick = { viewMode = "Field Radar" },
                        label = { Text("Ground Radar", fontSize = 11.sp) },
                        modifier = Modifier.height(28.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = HawkEyeCyan.copy(alpha = 0.2f),
                            selectedLabelColor = HawkEyeCyan
                        )
                    )
                }

                // Replay ball button
                IconButton(
                    onClick = onTriggerDelivery,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E293B))
                        .testTag("simulate_ball_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Simulate Delivery",
                        tint = CricketGreen,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

// 3D Pitch Perspective Drawing
private fun DrawScope.draw3DPitchSimulation(phase: Float) {
    val w = size.width
    val h = size.height

    // Stadium Turf Background
    drawRect(
        brush = Brush.verticalGradient(
            listOf(Color(0xFF06331C), Color(0xFF042012))
        )
    )

    // Pitch Trapezoid (Perspective)
    val pitchTopLeft = Offset(w * 0.40f, h * 0.18f)
    val pitchTopRight = Offset(w * 0.60f, h * 0.18f)
    val pitchBottomRight = Offset(w * 0.72f, h * 0.88f)
    val pitchBottomLeft = Offset(w * 0.28f, h * 0.88f)

    val pitchPath = Path().apply {
        moveTo(pitchTopLeft.x, pitchTopLeft.y)
        lineTo(pitchTopRight.x, pitchTopRight.y)
        lineTo(pitchBottomRight.x, pitchBottomRight.y)
        lineTo(pitchBottomLeft.x, pitchBottomLeft.y)
        close()
    }

    // Pitch turf clay color
    drawPath(
        path = pitchPath,
        brush = Brush.verticalGradient(
            listOf(Color(0xFFD4A373), Color(0xFF996633)),
            startY = h * 0.18f,
            endY = h * 0.88f
        )
    )

    // Crease Lines (White)
    // Bowling Crease (Top)
    drawLine(
        color = Color.White.copy(alpha = 0.8f),
        start = Offset(w * 0.38f, h * 0.24f),
        end = Offset(w * 0.62f, h * 0.24f),
        strokeWidth = 2.dp.toPx()
    )

    // Popping Crease (Bottom)
    drawLine(
        color = Color.White.copy(alpha = 0.9f),
        start = Offset(w * 0.24f, h * 0.82f),
        end = Offset(w * 0.76f, h * 0.82f),
        strokeWidth = 2.5.dp.toPx()
    )

    // Stumps at Batting End
    val stumpBaseY = h * 0.84f
    val stumpHeight = 22.dp.toPx()
    for (i in -1..1) {
        val stumpX = (w * 0.50f) + (i * 7.dp.toPx())
        drawLine(
            color = Color(0xFFFDE68A),
            start = Offset(stumpX, stumpBaseY),
            end = Offset(stumpX, stumpBaseY - stumpHeight),
            strokeWidth = 3.dp.toPx()
        )
    }
    // Bail
    drawLine(
        color = Color(0xFFFEF3C7),
        start = Offset(w * 0.50f - 9.dp.toPx(), stumpBaseY - stumpHeight),
        end = Offset(w * 0.50f + 9.dp.toPx(), stumpBaseY - stumpHeight),
        strokeWidth = 2.dp.toPx()
    )

    // Good Length Zone rectangle marker (semi-transparent cyan)
    drawRect(
        color = HawkEyeCyan.copy(alpha = 0.25f),
        topLeft = Offset(w * 0.43f, h * 0.48f),
        size = Size(w * 0.14f, h * 0.14f)
    )

    // Ball Animation Trajectory
    // Phase 0.0 to 0.5: Bowler releasing to Bounce
    // Phase 0.5 to 0.75: Bounce to Bat Impact
    // Phase 0.75 to 1.0: Shot off the bat into the field!

    val startX = w * 0.50f
    val startY = h * 0.19f
    val bounceX = w * 0.51f
    val bounceY = h * 0.55f
    val batX = w * 0.49f
    val batY = h * 0.82f
    val shotEndX = w * 0.85f
    val shotEndY = h * 0.40f

    // Draw ball trajectory trail (dotted cyan arc)
    val trajectoryPath = Path().apply {
        moveTo(startX, startY)
        quadraticTo(startX + 6.dp.toPx(), (startY + bounceY) / 2, bounceX, bounceY)
        quadraticTo(bounceX, (bounceY + batY) / 2, batX, batY)
        if (phase > 0.75f) {
            lineTo(shotEndX, shotEndY)
        }
    }

    drawPath(
        path = trajectoryPath,
        color = HawkEyeCyan.copy(alpha = 0.6f),
        style = Stroke(
            width = 2.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
        )
    )

    // Current Ball Position calculation
    val currentBallPos = when {
        phase < 0.50f -> {
            val t = phase / 0.50f
            Offset(
                startX + (bounceX - startX) * t,
                startY + (bounceY - startY) * t
            )
        }
        phase < 0.75f -> {
            val t = (phase - 0.50f) / 0.25f
            Offset(
                bounceX + (batX - bounceX) * t,
                bounceY + (batY - bounceY) * t
            )
        }
        else -> {
            val t = (phase - 0.75f) / 0.25f
            Offset(
                batX + (shotEndX - batX) * t,
                batY + (shotEndY - batY) * t
            )
        }
    }

    // Bounce shockwave circle
    if (phase >= 0.50f) {
        val shockRadius = ((phase - 0.50f) * 40.dp.toPx()).coerceAtMost(16.dp.toPx())
        drawCircle(
            color = HawkEyeCyan.copy(alpha = (1f - (phase - 0.50f) * 3).coerceIn(0f, 0.8f)),
            radius = shockRadius,
            center = Offset(bounceX, bounceY),
            style = Stroke(width = 2.dp.toPx())
        )
    }

    // Ball Glowing Render
    val ballRadius = 6.5.dp.toPx()
    // Glow
    drawCircle(
        color = Color(0x66FF1744),
        radius = ballRadius * 1.8f,
        center = currentBallPos
    )
    // Ball Body
    drawCircle(
        color = Color(0xFFD50000),
        radius = ballRadius,
        center = currentBallPos
    )
    // Ball Highlight
    drawCircle(
        color = Color(0xFFFF8A80),
        radius = ballRadius * 0.4f,
        center = Offset(currentBallPos.x - 1.5.dp.toPx(), currentBallPos.y - 1.5.dp.toPx())
    )
}

// 2D Full Ground Radar
private fun DrawScope.drawFieldRadarSimulation(phase: Float) {
    val w = size.width
    val h = size.height
    val cx = w / 2f
    val cy = h / 2f
    val fieldRadius = (h * 0.44f).coerceAtMost(w * 0.44f)

    // Field Outfield
    drawCircle(
        brush = Brush.radialGradient(
            listOf(Color(0xFF047857), Color(0xFF064E3B), Color(0xFF022C22)),
            center = Offset(cx, cy),
            radius = fieldRadius
        ),
        radius = fieldRadius,
        center = Offset(cx, cy)
    )

    // Boundary Rope
    drawCircle(
        color = Color.White.copy(alpha = 0.7f),
        radius = fieldRadius,
        center = Offset(cx, cy),
        style = Stroke(width = 2.dp.toPx())
    )

    // 30-yard circle (inner circle)
    drawCircle(
        color = Color.White.copy(alpha = 0.35f),
        radius = fieldRadius * 0.52f,
        center = Offset(cx, cy),
        style = Stroke(
            width = 1.2.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
        )
    )

    // Pitch in center
    drawRect(
        color = Color(0xFFD4A373),
        topLeft = Offset(cx - 5.dp.toPx(), cy - 20.dp.toPx()),
        size = Size(10.dp.toPx(), 40.dp.toPx())
    )

    // 9 Fielders Positions (Yellow Dots)
    val fielderAngles = listOf(25f, 65f, 120f, 170f, 210f, 250f, 300f, 340f)
    fielderAngles.forEach { angle ->
        val rad = Math.toRadians(angle.toDouble())
        val dist = fieldRadius * 0.75f
        val fx = cx + (dist * cos(rad)).toFloat()
        val fy = cy + (dist * sin(rad)).toFloat()

        drawCircle(
            color = StadiumGold,
            radius = 3.5.dp.toPx(),
            center = Offset(fx, fy)
        )
    }

    // Shot trace in radar
    val shotRad = Math.toRadians(45.0)
    val maxShotDist = fieldRadius * 0.85f
    val currentDist = if (phase > 0.6f) ((phase - 0.6f) / 0.4f) * maxShotDist else 0f

    val ballX = cx + (currentDist * cos(shotRad)).toFloat()
    val ballY = cy + (currentDist * sin(shotRad)).toFloat()

    if (currentDist > 0f) {
        drawLine(
            color = CricketGreen,
            start = Offset(cx, cy),
            end = Offset(ballX, ballY),
            strokeWidth = 2.5.dp.toPx()
        )
        drawCircle(
            color = Color(0xFFFF1744),
            radius = 5.dp.toPx(),
            center = Offset(ballX, ballY)
        )
    }
}
