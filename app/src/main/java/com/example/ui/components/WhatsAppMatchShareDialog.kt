package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.MatchEntity

@Composable
fun WhatsAppMatchShareDialog(
    match: MatchEntity,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    // Prepare formatted WhatsApp share scorecard text with rich emojis
    val oversFormatted = "${match.legalBalls / 6}.${match.legalBalls % 6}"
    val runRate = if (match.legalBalls > 0) {
        String.format("%.2f", (match.score.toFloat() / match.legalBalls) * 6f)
    } else "0.00"

    val whatsappText = buildString {
        appendLine("🏆 *${match.tournamentName.ifBlank { "GULLY CRICKET PREMIER LEAGUE" }}*")
        appendLine("📍 Venue: ${match.venue}")
        appendLine("━━━━━━━━━━━━━━━━━━━")
        appendLine("🏏 *${match.teamA}* vs *${match.teamB}*")
        appendLine("📊 *Score:* ${match.score}/${match.wickets} (${oversFormatted}/${match.totalOvers} ov)")
        appendLine("⚡ *Run Rate (CRR):* $runRate")
        appendLine("━━━━━━━━━━━━━━━━━━━")
        appendLine("🔥 *Striker:* ${match.strikerName} (${match.strikerRuns} off ${match.strikerBalls}b)")
        appendLine("🎯 *Non-Striker:* ${match.nonStrikerName} (${match.nonStrikerRuns} off ${match.nonStrikerBalls}b)")
        appendLine("🎳 *Current Bowler:* ${match.bowlerName} (${match.bowlerWickets}/${match.bowlerRuns} in ${match.bowlerBalls / 6}.${match.bowlerBalls % 6} ov)")
        appendLine("━━━━━━━━━━━━━━━━━━━")
        appendLine("🌟 *Match Status:* ${if (match.status == "LIVE") "🔴 LIVE ON AYUUCRIC APP" else "🏁 COMPLETED"}")
        appendLine("🎙️ *AI Sidhu Paaji Commentary & DRS Hawk-Eye Active!*")
        appendLine("📲 Follow live ball-by-ball updates on *AyuuCric Scorecard*!")
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
                .shadow(24.dp, RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0F1D)),
            border = BorderStroke(1.5.dp, Brush.linearGradient(listOf(Color(0xFF25D366), Color(0xFFFFD700), Color(0xFF38BDF8))))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Bar
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
                                .background(Color(0xFF25D366).copy(alpha = 0.2f))
                                .border(1.dp, Color(0xFF25D366), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                tint = Color(0xFF25D366),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "WhatsApp Match Card",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                            Text(
                                text = "CricHeroes 1-Tap Status & Poster",
                                fontSize = 11.sp,
                                color = Color(0xFF25D366),
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

                Spacer(modifier = Modifier.height(16.dp))

                // High-Impact Poster Graphic Card (Preview)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF131E33)),
                    border = BorderStroke(1.dp, Color(0xFF1E293B))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color(0xFF1E293B), Color(0xFF0F172A), Color(0xFF060913))
                                )
                            )
                            .padding(16.dp)
                    ) {
                        // Tournament badge & Venue
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = match.tournamentName.ifBlank { "GULLY PREMIER LEAGUE 2026" },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFFFFD700),
                                letterSpacing = 0.5.sp
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (match.status == "LIVE") Color(0xFFEF4444) else Color(0xFF38BDF8))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (match.status == "LIVE") "● LIVE" else "COMPLETED",
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }

                        Text(
                            text = "📍 ${match.venue}",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.6f)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Scoreboard Matchup
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Team A
                            Column {
                                Text(
                                    text = match.teamA,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                                Text(
                                    text = "${match.score}/${match.wickets}",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF00E676)
                                )
                                Text(
                                    text = "(${oversFormatted}/${match.totalOvers} ov)",
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Text(
                                text = "VS",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFFFFD700).copy(alpha = 0.8f)
                            )

                            // Team B
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = match.teamB,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                                Text(
                                    text = "Target: ${if (match.target > 0) match.target else (match.score + 1)}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF38BDF8)
                                )
                                Text(
                                    text = "CRR: $runRate",
                                    fontSize = 12.sp,
                                    color = Color(0xFFFFD700),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = Color(0xFF1E293B))
                        Spacer(modifier = Modifier.height(10.dp))

                        // Key Performers
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("🏏 Top Batter", fontSize = 10.sp, color = Color.White.copy(alpha = 0.6f), fontWeight = FontWeight.Bold)
                                Text(
                                    text = "${match.strikerName} ${match.strikerRuns}* (${match.strikerBalls})",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                            }
                            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                                Text("🎳 Key Bowler", fontSize = 10.sp, color = Color.White.copy(alpha = 0.6f), fontWeight = FontWeight.Bold)
                                Text(
                                    text = "${match.bowlerName} ${match.bowlerWickets}/${match.bowlerRuns}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // App Branding Footer
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "AyuuCric • Real Ground Scoring & DRS",
                                fontSize = 10.sp,
                                color = Color(0xFF25D366),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Primary Share on WhatsApp Button
                Button(
                    onClick = {
                        try {
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, whatsappText)
                                type = "text/plain"
                                setPackage("com.whatsapp")
                            }
                            context.startActivity(sendIntent)
                        } catch (_: Exception) {
                            // If WhatsApp package is not installed directly, fallback to system chooser
                            val fallbackIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, whatsappText)
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(fallbackIntent, "Share Match via"))
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF25D366),
                        contentColor = Color.Black
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        tint = Color.Black,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "SHARE ON WHATSAPP",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.Black,
                        letterSpacing = 0.5.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Secondary Copy Scorecard Button
                OutlinedButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("AyuuCric Match Scorecard", whatsappText)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Scorecard copied to clipboard! 📋", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color(0xFF334155)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy",
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Copy Scorecard Summary",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}
