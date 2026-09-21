package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.model.DeviceRole
import com.example.data.model.MatchEntity
import com.example.ui.theme.CricketGreen
import com.example.ui.theme.HawkEyeCyan
import com.example.ui.theme.PitchDark
import com.example.ui.theme.StadiumGold
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WicketRed

@Composable
fun UmpireCameraScreen(
    currentRole: DeviceRole,
    match: MatchEntity?,
    isStreamingPitchCam: Boolean,
    isStreamingSideCam: Boolean,
    spectatorCamAngle: String,
    onTogglePitchCam: () -> Unit,
    onToggleSideCam: () -> Unit,
    onSelectSpectatorAngle: (String) -> Unit,
    onRequestDrsAppeal: (String) -> Unit,
    onChangeRoleClick: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> hasCameraPermission = granted }
    )

    var cameraLensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }
    var isTorchOn by remember { mutableStateOf(false) }
    var useSimulationFallback by remember { mutableStateOf(false) }

    // Calibratable crease laser line for side-on square leg umpire
    var creaseLineX by remember { mutableFloatStateOf(0.48f) }

    // Pulsing recording dot
    val infiniteTransition = rememberInfiniteTransition(label = "rec")
    val recAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "recAlpha"
    )

    val isPitchCam = when (currentRole) {
        DeviceRole.BOWLER_END_UMPIRE -> true
        DeviceRole.SQUARE_LEG_UMPIRE -> false
        else -> spectatorCamAngle == "PITCH_CAM"
    }

    val isBroadcasting = if (isPitchCam) isStreamingPitchCam else isStreamingSideCam

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Camera Viewfinder or Simulation Stream
        if (hasCameraPermission && !useSimulationFallback) {
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        try {
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().also {
                                it.surfaceProvider = previewView.surfaceProvider
                            }
                            val cameraSelector = CameraSelector.Builder()
                                .requireLensFacing(cameraLensFacing)
                                .build()
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview
                            )
                        } catch (e: Exception) {
                            // Fallback to visual stream simulation if hardware unavail
                            useSimulationFallback = true
                        }
                    }, ContextCompat.getMainExecutor(ctx))
                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // High-fidelity pitch/crease visual feed
            CameraStreamSimulation(
                isPitchCam = isPitchCam,
                creaseLineX = creaseLineX
            )
        }

        // AR Lines & Crease Calibration Overlay
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    if (!isPitchCam && currentRole == DeviceRole.SQUARE_LEG_UMPIRE) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            creaseLineX = (creaseLineX + dragAmount.x / size.width).coerceIn(0.1f, 0.9f)
                        }
                    }
                }
        ) {
            val width = size.width
            val height = size.height

            if (isPitchCam) {
                // Bowler End Overlay: Pitch Corridor & Popping Crease
                val pitchTopLeft = Offset(width * 0.38f, height * 0.32f)
                val pitchTopRight = Offset(width * 0.62f, height * 0.32f)
                val pitchBottomLeft = Offset(width * 0.15f, height * 0.88f)
                val pitchBottomRight = Offset(width * 0.85f, height * 0.88f)

                // Draw pitch corridor lines
                drawLine(
                    color = HawkEyeCyan.copy(alpha = 0.65f),
                    start = pitchTopLeft,
                    end = pitchBottomLeft,
                    strokeWidth = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 10f), 0f)
                )
                drawLine(
                    color = HawkEyeCyan.copy(alpha = 0.65f),
                    start = pitchTopRight,
                    end = pitchBottomRight,
                    strokeWidth = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 10f), 0f)
                )

                // Bowler Popping Crease line (No-ball fairness)
                val creaseY = height * 0.82f
                drawLine(
                    color = Color.White.copy(alpha = 0.85f),
                    start = Offset(width * 0.18f, creaseY),
                    end = Offset(width * 0.82f, creaseY),
                    strokeWidth = 3.dp.toPx()
                )

                // Stumps target box at batsman end
                val stumpBoxY = height * 0.32f
                drawLine(
                    color = StadiumGold.copy(alpha = 0.8f),
                    start = Offset(width * 0.46f, stumpBoxY),
                    end = Offset(width * 0.54f, stumpBoxY),
                    strokeWidth = 4.dp.toPx()
                )
            } else {
                // Square Leg Overlay: Calibratable Vertical Laser Crease Line
                val laserX = width * creaseLineX
                drawLine(
                    color = Color(0xFFFF2255),
                    start = Offset(laserX, height * 0.15f),
                    end = Offset(laserX, height * 0.85f),
                    strokeWidth = 3.dp.toPx()
                )

                // Grounded Bat zone bracket
                drawCircle(
                    color = Color(0xFFFF2255).copy(alpha = 0.25f),
                    radius = 24.dp.toPx(),
                    center = Offset(laserX, height * 0.5f)
                )
            }
        }

        // Top Broadcast Header & Controls
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Live Status Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black.copy(alpha = 0.75f))
                        .border(1.dp, Color(0xFF334155), RoundedCornerShape(16.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(
                                if (isBroadcasting) WicketRed.copy(alpha = recAlpha)
                                else Color.Gray
                            )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isBroadcasting) "LIVE 1080p 60FPS" else "PAUSED",
                        color = if (isBroadcasting) Color.White else TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Active Phone Role Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(SurfaceDark.copy(alpha = 0.9f))
                        .border(1.dp, StadiumGold.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "${currentRole.phoneLabel}: ${currentRole.hindiTitle}",
                        color = StadiumGold,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Switch Role / Setup Button
                Button(
                    onClick = onChangeRoleClick,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text("Role", color = TextPrimary, fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Spectator angle selector chips (when in viewer mode)
            if (currentRole == DeviceRole.SPECTATOR_VIEWER) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = spectatorCamAngle == "PITCH_CAM",
                        onClick = { onSelectSpectatorAngle("PITCH_CAM") },
                        label = { Text("🎥 Cam 1: Bowler End (Pitch)", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CricketGreen,
                            selectedLabelColor = PitchDark,
                            containerColor = Color.Black.copy(alpha = 0.7f),
                            labelColor = Color.White
                        )
                    )
                    FilterChip(
                        selected = spectatorCamAngle == "SIDE_CAM",
                        onClick = { onSelectSpectatorAngle("SIDE_CAM") },
                        label = { Text("📐 Cam 2: Square Leg (Side)", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = HawkEyeCyan,
                            selectedLabelColor = PitchDark,
                            containerColor = Color.Black.copy(alpha = 0.7f),
                            labelColor = Color.White
                        )
                    )
                }
            }

            // Quick Match Score Overlay
            if (match != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.Black.copy(alpha = 0.7f))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${match.teamAShort} vs ${match.teamBShort}",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${match.score}/${match.wickets} (${match.legalBalls / 6}.${match.legalBalls % 6} ov)",
                        color = CricketGreen,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "${match.strikerName}*",
                        color = StadiumGold,
                        fontSize = 11.sp
                    )
                }
            }
        }

        // Bottom Umpire HUD Controls
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.95f))
                    )
                )
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Guide instructions for Square Leg laser line
            if (!isPitchCam && currentRole == DeviceRole.SQUARE_LEG_UMPIRE) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF1E293B).copy(alpha = 0.85f))
                        .padding(8.dp)
                ) {
                    Text(
                        text = "👉 Drag red laser line on screen to align exactly with painted crease line on the ground for run-out and stumping accuracy.",
                        color = HawkEyeCyan,
                        fontSize = 11.sp
                    )
                }
            }

            // Hardware permission prompt if not yet granted
            if (!hasCameraPermission) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(SurfaceDark)
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Camera Permission Required",
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Enable device camera for real match recording",
                            color = TextSecondary,
                            fontSize = 10.sp
                        )
                    }
                    Button(
                        onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                        colors = ButtonDefaults.buttonColors(containerColor = CricketGreen),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Grant", color = PitchDark, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Quick Camera controls (Torch & Flip)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        cameraLensFacing = if (cameraLensFacing == CameraSelector.LENS_FACING_BACK)
                            CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK
                    },
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E293B).copy(alpha = 0.85f))
                ) {
                    Icon(
                        imageVector = Icons.Default.FlipCameraAndroid,
                        contentDescription = "Flip Camera",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = { isTorchOn = !isTorchOn },
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(if (isTorchOn) StadiumGold else Color(0xFF1E293B).copy(alpha = 0.85f))
                ) {
                    Icon(
                        imageVector = Icons.Default.FlashOn,
                        contentDescription = "Torch",
                        tint = if (isTorchOn) PitchDark else Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Official Action Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Broadcast Stream Toggle
                if (currentRole.isOfficial) {
                    Button(
                        onClick = {
                            if (isPitchCam) onTogglePitchCam() else onToggleSideCam()
                        },
                        modifier = Modifier.weight(1f).height(44.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isBroadcasting) WicketRed else CricketGreen
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = if (isBroadcasting) Icons.Default.VideocamOff else Icons.Default.Videocam,
                            contentDescription = null,
                            tint = PitchDark,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isBroadcasting) "Stop Stream" else "Start Live Stream",
                            color = PitchDark,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            maxLines = 1
                        )
                    }
                }

                // Call Third Umpire DRS Button
                Button(
                    onClick = {
                        onRequestDrsAppeal(if (isPitchCam) "LBW" else "RUN_OUT")
                    },
                    modifier = Modifier.widthIn(min = 115.dp).height(44.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = HawkEyeCyan),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Policy,
                        contentDescription = null,
                        tint = PitchDark,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Call DRS",
                        color = PitchDark,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun CameraStreamSimulation(
    isPitchCam: Boolean,
    creaseLineX: Float
) {
    val infiniteTransition = rememberInfiniteTransition(label = "simBall")
    val ballProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ballProgress"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        // Ground turf texture background
        drawRect(
            brush = Brush.verticalGradient(
                listOf(Color(0xFF1A3824), Color(0xFF0F2617), Color(0xFF0B1B10))
            )
        )

        if (isPitchCam) {
            // Draw 22-yard clay cricket pitch corridor
            val pitchBrush = Brush.verticalGradient(
                listOf(Color(0xFF9E8552), Color(0xFF6B5832), Color(0xFF4A3C22))
            )
            val pTopLeft = Offset(width * 0.36f, height * 0.28f)
            val pTopRight = Offset(width * 0.64f, height * 0.28f)
            val pBottomLeft = Offset(width * 0.12f, height * 0.92f)
            val pBottomRight = Offset(width * 0.88f, height * 0.92f)

            val pitchPath = androidx.compose.ui.graphics.Path().apply {
                moveTo(pTopLeft.x, pTopLeft.y)
                lineTo(pTopRight.x, pTopRight.y)
                lineTo(pBottomRight.x, pBottomRight.y)
                lineTo(pBottomLeft.x, pBottomLeft.y)
                close()
            }
            drawPath(pitchPath, pitchBrush)

            // Animated cricket ball bowling down the pitch
            val startX = width * 0.5f
            val startY = height * 0.85f
            val endX = width * 0.48f
            val endY = height * 0.32f

            val curX = startX + (endX - startX) * ballProgress
            val curY = startY + (endY - startY) * ballProgress
            val ballRadius = (16.dp.toPx() * (1f - ballProgress * 0.6f)).coerceAtLeast(6.dp.toPx())

            // Ball shadow
            drawCircle(
                color = Color.Black.copy(alpha = 0.4f),
                radius = ballRadius * 1.1f,
                center = Offset(curX + 4f, curY + 6f)
            )
            // Red cricket ball
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Color(0xFFFF3B30), Color(0xFF8B0000)),
                    center = Offset(curX - 2f, curY - 2f),
                    radius = ballRadius
                ),
                radius = ballRadius,
                center = Offset(curX, curY)
            )
        } else {
            // Square leg angle: white popping crease on green pitch
            val groundCreaseX = width * 0.48f
            drawLine(
                color = Color.White,
                start = Offset(groundCreaseX, 0f),
                end = Offset(groundCreaseX, height),
                strokeWidth = 14.dp.toPx()
            )

            // Stumps at side
            val stumpY = height * 0.45f
            drawRect(
                color = StadiumGold,
                topLeft = Offset(width * 0.45f, stumpY - 50.dp.toPx()),
                size = androidx.compose.ui.geometry.Size(12.dp.toPx(), 100.dp.toPx())
            )

            // Moving bat sliding into crease
            val batX = width * (0.8f - ballProgress * 0.45f)
            val batY = height * 0.52f
            drawRoundRect(
                color = Color(0xFFC2A066),
                topLeft = Offset(batX, batY),
                size = androidx.compose.ui.geometry.Size(80.dp.toPx(), 18.dp.toPx()),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx())
            )
        }
    }
}
