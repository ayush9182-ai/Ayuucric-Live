package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.model.BallEventEntity
import com.example.data.model.DeviceRole
import com.example.data.model.MatchEntity
import com.example.ui.theme.CricketGreen
import com.example.ui.theme.DrsOutRed
import com.example.ui.theme.HawkEyeCyan
import com.example.ui.theme.PitchDark
import com.example.ui.theme.StadiumGold
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WicketRed

@Composable
fun LiveMatchBroadcastPlayer(
    match: MatchEntity,
    recentBalls: List<BallEventEntity>,
    currentRole: DeviceRole,
    spectatorCamAngle: String,
    onSelectSpectatorAngle: (String) -> Unit,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val hasCameraPermission = remember {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    // Blinking Live Badge Animation
    val infiniteTransition = rememberInfiniteTransition(label = "livePulse")
    val liveAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "liveAlpha"
    )

    // Ball Pitch Animation Phase for Visualizer
    val pitchBallProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pitchProgress"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.Black),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0F172A))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Pulsing Red Live Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(DrsOutRed.copy(alpha = liveAlpha))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "LIVE VIDEO",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "Ground Broadcast Stream",
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Angle Selector Pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF1E293B))
                            .clickable {
                                val nextAngle = if (spectatorCamAngle == "PITCH_CAM") "SIDE_CAM" else "PITCH_CAM"
                                onSelectSpectatorAngle(nextAngle)
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (spectatorCamAngle == "PITCH_CAM") "🎥 Pitch Cam" else "📐 Crease Cam",
                            color = if (spectatorCamAngle == "PITCH_CAM") CricketGreen else HawkEyeCyan,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    IconButton(
                        onClick = onToggleExpand,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = if (isExpanded) "Collapse Video" else "Expand Video",
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .background(Color.Black)
                ) {
                    // Video Layer: Real Camera if permission granted & role is camera; else live ground pitch visualizer
                    val showRealCamera = hasCameraPermission && (
                        currentRole == DeviceRole.BOWLER_END_UMPIRE ||
                        currentRole == DeviceRole.SQUARE_LEG_UMPIRE
                    )

                    if (showRealCamera) {
                        AndroidView(
                            factory = { ctx ->
                                val previewView = PreviewView(ctx).apply {
                                    layoutParams = ViewGroup.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT
                                    )
                                    scaleType = PreviewView.ScaleType.FILL_CENTER
                                }

                                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                                cameraProviderFuture.addListener({
                                    val cameraProvider = cameraProviderFuture.get()
                                    val preview = Preview.Builder().build().also {
                                        it.setSurfaceProvider(previewView.surfaceProvider)
                                    }
                                    try {
                                        cameraProvider.unbindAll()
                                        cameraProvider.bindToLifecycle(
                                            lifecycleOwner,
                                            CameraSelector.DEFAULT_BACK_CAMERA,
                                            preview
                                        )
                                    } catch (_: Exception) {}
                                }, ContextCompat.getMainExecutor(ctx))

                                previewView
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        // Ground Match Pitch Visualizer Stream (Spectator / Home Parents View)
                        GroundMatchPitchSimulation(
                            camAngle = spectatorCamAngle,
                            ballProgress = pitchBallProgress,
                            striker = match.strikerName,
                            bowler = match.bowlerName
                        )
                    }

                    // TV BROADCAST SCOREBOARD OVERLAY (CricHeroes / Star Sports Style)
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(10.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Top Badges
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            // Match Venue Tag
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color.Black.copy(alpha = 0.7f))
                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = match.tournamentName,
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            // Stream Quality & Audio Badge
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color.Black.copy(alpha = 0.7f))
                                        .padding(horizontal = 6.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = "HD 1080p • 60fps",
                                        color = CricketGreen,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Bottom TV Broadcast Score Ribbon
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            Color(0xFF0F172A).copy(alpha = 0.85f),
                                            Color(0xFF020617).copy(alpha = 0.95f)
                                        )
                                    )
                                )
                                .border(0.5.dp, Color(0xFF334155), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            // Row 1: Score & Equation
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f, fill = false)
                                ) {
                                    Text(
                                        text = match.teamBShort,
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Black,
                                        maxLines = 1
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "${match.score}/${match.wickets}",
                                        color = CricketGreen,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Black,
                                        maxLines = 1
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "(${match.legalBalls / 6}.${match.legalBalls % 6}/${match.totalOvers} ov)",
                                        color = TextSecondary,
                                        fontSize = 11.sp,
                                        maxLines = 1
                                    )
                                }

                                Spacer(modifier = Modifier.width(6.dp))

                                Text(
                                    text = match.statusDetail,
                                    color = StadiumGold,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }

                            Spacer(modifier = Modifier.height(3.dp))

                            // Row 2: Batters & Bowlers
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1.2f, fill = false)
                                ) {
                                    Text(
                                        text = "${match.strikerName}*",
                                        color = StadiumGold,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = " ${match.strikerRuns}(${match.strikerBalls})",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = " • ${match.nonStrikerName} ${match.nonStrikerRuns}",
                                        color = TextSecondary,
                                        fontSize = 9.5.sp,
                                        maxLines = 1
                                    )
                                }

                                Spacer(modifier = Modifier.width(6.dp))

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f, fill = false),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Text(
                                        text = match.bowlerName,
                                        color = HawkEyeCyan,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = " ${match.bowlerWickets}/${match.bowlerRuns} (${match.bowlerBalls / 6}.${match.bowlerBalls % 6})",
                                        color = Color.White,
                                        fontSize = 9.5.sp,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GroundMatchPitchSimulation(
    camAngle: String,
    ballProgress: Float,
    striker: String,
    bowler: String
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Turf Grass Canvas
            drawRect(
                brush = Brush.verticalGradient(
                    listOf(
                        Color(0xFF0F3D24),
                        Color(0xFF0B2E1B),
                        Color(0xFF051B0F)
                    )
                )
            )

            // Pitch 22 Yards Strip
            val pitchTop = h * 0.15f
            val pitchBottom = h * 0.88f
            val pitchWidthTop = w * 0.22f
            val pitchWidthBottom = w * 0.48f

            val pitchPath = androidx.compose.ui.graphics.Path().apply {
                moveTo(w * 0.5f - pitchWidthTop / 2, pitchTop)
                lineTo(w * 0.5f + pitchWidthTop / 2, pitchTop)
                lineTo(w * 0.5f + pitchWidthBottom / 2, pitchBottom)
                lineTo(w * 0.5f - pitchWidthBottom / 2, pitchBottom)
                close()
            }

            drawPath(
                path = pitchPath,
                brush = Brush.verticalGradient(
                    listOf(
                        Color(0xFF927042),
                        Color(0xFF805E34),
                        Color(0xFF6B4D27)
                    )
                )
            )

            // Crease Lines (Pop and Bowling crease)
            val bowlingCreaseY = pitchTop + 10f
            val battingCreaseY = pitchBottom - 25f

            drawLine(
                color = Color.White.copy(alpha = 0.9f),
                start = Offset(w * 0.5f - pitchWidthTop * 0.6f, bowlingCreaseY),
                end = Offset(w * 0.5f + pitchWidthTop * 0.6f, bowlingCreaseY),
                strokeWidth = 3f
            )

            drawLine(
                color = Color.White.copy(alpha = 0.9f),
                start = Offset(w * 0.5f - pitchWidthBottom * 0.6f, battingCreaseY),
                end = Offset(w * 0.5f + pitchWidthBottom * 0.6f, battingCreaseY),
                strokeWidth = 4f
            )

            // Stumps at batting end
            val stumpX = w * 0.5f
            for (i in -1..1) {
                drawLine(
                    color = StadiumGold,
                    start = Offset(stumpX + (i * 7f), battingCreaseY + 5f),
                    end = Offset(stumpX + (i * 7f), battingCreaseY - 24f),
                    strokeWidth = 3f
                )
            }

            // Stumps at bowling end
            for (i in -1..1) {
                drawLine(
                    color = StadiumGold.copy(alpha = 0.8f),
                    start = Offset(stumpX + (i * 4f), bowlingCreaseY - 2f),
                    end = Offset(stumpX + (i * 4f), bowlingCreaseY - 14f),
                    strokeWidth = 2f
                )
            }

            // Ball trajectory in flight
            val startY = bowlingCreaseY
            val endY = battingCreaseY - 10f
            val currentY = startY + (endY - startY) * ballProgress
            val pitchBounceY = startY + (endY - startY) * 0.65f

            // Ball arc deviation
            val arcOffset = if (currentY < pitchBounceY) {
                kotlin.math.sin(ballProgress * kotlin.math.PI.toFloat()) * 15f
            } else {
                -kotlin.math.sin((ballProgress - 0.65f) / 0.35f * kotlin.math.PI.toFloat()) * 12f
            }

            val currentX = w * 0.5f + (ballProgress - 0.5f) * 18f + arcOffset

            // Ball trail line
            drawLine(
                color = HawkEyeCyan.copy(alpha = 0.5f),
                start = Offset(w * 0.5f, bowlingCreaseY),
                end = Offset(currentX, currentY),
                strokeWidth = 2f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))
            )

            // The Leather Cricket Ball
            drawCircle(
                color = DrsOutRed,
                radius = 6f + (ballProgress * 4f),
                center = Offset(currentX, currentY)
            )

            // Ball seam reflection
            drawCircle(
                color = Color.White.copy(alpha = 0.8f),
                radius = 2f,
                center = Offset(currentX - 2f, currentY - 2f)
            )
        }

        // Live Umpire Angle Label
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(10.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color.Black.copy(alpha = 0.6f))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = if (camAngle == "PITCH_CAM") "⚡ Bowler End Pitch Feed (Auto Track)" else "⚡ Square Leg Laser Crease Feed",
                color = TextSecondary,
                fontSize = 9.sp
            )
        }
    }
}
