package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.BallEventEntity
import com.example.data.model.MatchEntity
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun WagonWheelAndPitchMapDialog(
    match: MatchEntity,
    deliveries: List<BallEventEntity>,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Wagon Wheel, 1: Pitch Map

    val displayDeliveries = deliveries

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
                .shadow(24.dp, RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF090E1A)),
            border = BorderStroke(1.5.dp, Brush.linearGradient(listOf(Color(0xFF00E676), Color(0xFF38BDF8), Color(0xFFFFD700))))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
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
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF00E676).copy(alpha = 0.2f))
                                .border(1.dp, Color(0xFF00E676), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.TrackChanges,
                                contentDescription = null,
                                tint = Color(0xFF00E676),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Shot Visualizer & Radar",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                            Text(
                                text = "${match.strikerName} vs ${match.bowlerName}",
                                fontSize = 11.sp,
                                color = Color(0xFF38BDF8),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Tab Switcher (Wagon Wheel vs Pitch Map)
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color(0xFF131D31),
                    contentColor = Color.White,
                    divider = {},
                    indicator = { tabPositions ->
                        Box(
                            Modifier
                                .tabIndicatorOffset(tabPositions[selectedTab])
                                .height(3.dp)
                                .background(Color(0xFF00E676), RoundedCornerShape(2.dp))
                        )
                    }
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Adjust, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Wagon Wheel", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Straighten, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Pitch Map", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (selectedTab == 0) {
                    // WAGON WHEEL VISUALIZER
                    WagonWheelView(deliveries = displayDeliveries)
                } else {
                    // PITCH MAP VISUALIZER
                    PitchMapView(deliveries = displayDeliveries)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Legend
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    WagonLegendItem(color = Color(0xFFFFD700), label = "6s (Six)")
                    WagonLegendItem(color = Color(0xFF00E5FF), label = "4s (Four)")
                    WagonLegendItem(color = Color(0xFF00E676), label = "1s & 2s")
                    WagonLegendItem(color = Color(0xFFEF4444), label = "Wicket")
                }
            }
        }
    }
}

@Composable
private fun WagonWheelView(deliveries: List<BallEventEntity>) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(260.dp)
                .clip(CircleShape)
                .background(Color(0xFF0A2E1A))
                .border(2.dp, Color(0xFF15803D), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                val center = Offset(size.width / 2, size.height / 2)
                val radius = size.width / 2

                // Draw Boundary Rope
                drawCircle(
                    color = Color.White.copy(alpha = 0.8f),
                    radius = radius,
                    center = center,
                    style = Stroke(width = 2f)
                )

                // Draw 30-Yard Circle (dashed)
                drawCircle(
                    color = Color.White.copy(alpha = 0.35f),
                    radius = radius * 0.52f,
                    center = center,
                    style = Stroke(
                        width = 1.5f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    )
                )

                // Draw Central Pitch
                val pitchW = 16f
                val pitchH = 34f
                drawRect(
                    color = Color(0xFFD4A373),
                    topLeft = Offset(center.x - pitchW / 2, center.y - pitchH / 2),
                    size = Size(pitchW, pitchH)
                )

                // Draw Deliveries / Shots
                deliveries.forEach { ball ->
                    val angleRad = Math.toRadians((ball.shotAngle - 90.0))
                    val distFraction = when {
                        ball.isWicket -> 0.15f
                        ball.runs >= 6 -> 0.98f
                        ball.runs >= 4 -> 0.92f
                        ball.runs in 2..3 -> 0.65f
                        ball.runs == 1 -> 0.45f
                        else -> 0.25f
                    }
                    val shotDist = radius * distFraction
                    val endX = center.x + (shotDist * cos(angleRad)).toFloat()
                    val endY = center.y + (shotDist * sin(angleRad)).toFloat()

                    val shotColor = when {
                        ball.isWicket -> Color(0xFFEF4444)
                        ball.runs >= 6 -> Color(0xFFFFD700)
                        ball.runs >= 4 -> Color(0xFF00E5FF)
                        ball.runs > 0 -> Color(0xFF00E676)
                        else -> Color.White.copy(alpha = 0.4f)
                    }

                    // Line from pitch to shot landing
                    drawLine(
                        color = shotColor,
                        start = center,
                        end = Offset(endX, endY),
                        strokeWidth = if (ball.runs >= 4) 3f else 1.5f
                    )

                    // Dot at end
                    drawCircle(
                        color = shotColor,
                        radius = if (ball.runs >= 4) 5f else 3.5f,
                        center = Offset(endX, endY)
                    )
                }
            }

            // Ground Orientation Text
            Text(
                text = "Off Side ◄",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.CenterStart).padding(start = 6.dp)
            )
            Text(
                text = "► Leg Side",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 6.dp)
            )
            Text(
                text = "Straight (Long-on / Long-off)",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 4.dp)
            )

            if (deliveries.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "No records yet",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Abhi tak koi shot nahi khela gaya hai",
                            color = Color.White.copy(alpha = 0.65f),
                            fontSize = 10.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Off-side vs Leg-side distribution
        if (deliveries.isNotEmpty()) {
            val offRuns = deliveries.filter { it.shotAngle in 0f..180f }.sumOf { it.runs }
            val legRuns = deliveries.filter { it.shotAngle > 180f }.sumOf { it.runs }
            val total = (offRuns + legRuns).coerceAtLeast(1)

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Off-Side: $offRuns runs (${(offRuns * 100) / total}%)", fontSize = 11.sp, color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold)
                Text("Leg-Side: $legRuns runs (${(legRuns * 100) / total}%)", fontSize = 11.sp, color = Color(0xFF00E676), fontWeight = FontWeight.Bold)
            }
        } else {
            Text(
                text = "No records yet (0 shots)",
                fontSize = 11.sp,
                color = Color.Gray,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Composable
private fun PitchMapView(deliveries: List<BallEventEntity>) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier
                .width(220.dp)
                .height(260.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF261D15)),
            border = BorderStroke(1.5.dp, Color(0xFF8B5E3C))
        ) {
            Box(modifier = Modifier.fillMaxSize().padding(10.dp)) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height

                    // Stumps at top (Bowling end)
                    drawLine(Color(0xFFFFD700), Offset(w * 0.35f, 15f), Offset(w * 0.65f, 15f), strokeWidth = 5f)

                    // Stumps at bottom (Batting end)
                    drawLine(Color(0xFFFFD700), Offset(w * 0.35f, h - 15f), Offset(w * 0.65f, h - 15f), strokeWidth = 5f)

                    // Crease markings
                    drawLine(Color.White, Offset(0f, 35f), Offset(w, 35f), strokeWidth = 2f)
                    drawLine(Color.White, Offset(0f, h - 35f), Offset(w, h - 35f), strokeWidth = 2f)

                    // Length Zone Dividers
                    val yorkerLine = h - 60f
                    val fullLine = h - 105f
                    val goodLine = h - 165f
                    val shortLine = h - 215f

                    drawLine(Color.White.copy(alpha = 0.25f), Offset(0f, yorkerLine), Offset(w, yorkerLine), strokeWidth = 1f)
                    drawLine(Color.White.copy(alpha = 0.25f), Offset(0f, fullLine), Offset(w, fullLine), strokeWidth = 1f)
                    drawLine(Color.White.copy(alpha = 0.25f), Offset(0f, goodLine), Offset(w, goodLine), strokeWidth = 1f)
                    drawLine(Color.White.copy(alpha = 0.25f), Offset(0f, shortLine), Offset(w, shortLine), strokeWidth = 1f)

                    // Plot balls onto pitch
                    deliveries.forEachIndexed { idx, ball ->
                        val yPos = when (ball.pitchZone) {
                            "Yorker" -> yorkerLine + 12f
                            "Full" -> (yorkerLine + fullLine) / 2
                            "Good Length" -> (fullLine + goodLine) / 2
                            "Short" -> (goodLine + shortLine) / 2
                            else -> shortLine - 15f
                        }
                        val xPos = w * 0.3f + ((idx * 37) % (w * 0.4f).toInt())

                        val ballColor = when {
                            ball.isWicket -> Color(0xFFEF4444)
                            ball.runs >= 6 -> Color(0xFFFFD700)
                            ball.runs >= 4 -> Color(0xFF00E5FF)
                            ball.runs > 0 -> Color(0xFF00E676)
                            else -> Color.White
                        }

                        drawCircle(
                            color = ballColor,
                            radius = 6f,
                            center = Offset(xPos, yPos)
                        )
                    }
                }

                // Zone Labels on left side
                Column(
                    modifier = Modifier.fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceAround
                ) {
                    Text("Bouncer", fontSize = 8.sp, color = Color.White.copy(alpha = 0.6f))
                    Text("Short", fontSize = 8.sp, color = Color.White.copy(alpha = 0.6f))
                    Text("Good Length", fontSize = 8.sp, color = Color(0xFF00E676), fontWeight = FontWeight.Bold)
                    Text("Full", fontSize = 8.sp, color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold)
                    Text("Yorker", fontSize = 8.sp, color = Color(0xFFFFD700), fontWeight = FontWeight.Bold)
                }

                if (deliveries.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.55f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Text(
                                text = "No records yet",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "Abhi tak koi delivery nahi dali gayi",
                                color = Color.White.copy(alpha = 0.65f),
                                fontSize = 9.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WagonLegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(9.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, fontSize = 10.sp, color = Color.White.copy(alpha = 0.8f), fontWeight = FontWeight.SemiBold)
    }
}
