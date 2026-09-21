package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.SportsCricket
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CricHeroesProfile
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
fun CricHeroesProfileDialog(
    initialProfile: CricHeroesProfile,
    onDismiss: () -> Unit,
    onSaveProfile: (CricHeroesProfile) -> Unit,
    onCheckUsernameAvailable: (String) -> Boolean = { true },
    onLogout: () -> Unit = {}
) {
    var username by remember { mutableStateOf(initialProfile.username.ifBlank { "ayush_7" }) }
    var usernameError by remember { mutableStateOf<String?>(null) }
    var fullName by remember { mutableStateOf(initialProfile.fullName) }
    var mobileNumber by remember { mutableStateOf(initialProfile.mobileNumber) }
    var jerseyName by remember { mutableStateOf(initialProfile.jerseyName) }
    var jerseyNumber by remember { mutableStateOf(initialProfile.jerseyNumber.toString()) }
    var primaryRole by remember { mutableStateOf(initialProfile.primaryRole) }
    var battingStyle by remember { mutableStateOf(initialProfile.battingStyle) }
    var bowlingStyle by remember { mutableStateOf(initialProfile.bowlingStyle) }
    var teamName by remember { mutableStateOf(initialProfile.teamName) }
    var city by remember { mutableStateOf(initialProfile.city) }
    var avatarEmoji by remember { mutableStateOf(initialProfile.avatarEmoji) }

    val rolesList = listOf("Top-Order Batter", "Finisher", "All-Rounder", "Fast Bowler", "Spin Wizard", "Wicketkeeper")
    val avatarChoices = listOf("🏏", "⚡", "🔥", "👑", "🏆", "🧤", "🚀", "🎯", "🦁", "🦅")

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = PitchDark,
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(listOf(Color(0xFFE11D48), Color(0xFFFB923C)))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SportsCricket,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "AYUUCRIC",
                            color = StadiumGold,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = null,
                            tint = HawkEyeCyan,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        text = "Official Player Profile & Digital ID",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // World-Class Holographic Player ID Card Preview
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .border(
                            width = 1.5.dp,
                            brush = Brush.linearGradient(
                                listOf(StadiumGold, CricketGreen, HawkEyeCyan, StadiumGold)
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        Color(0xFF0F172A),
                                        Color(0xFF064E3B),
                                        Color(0xFF0F172A)
                                    )
                                )
                            )
                            .padding(14.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            // Top Bar of Card
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "AYUUCRIC PRO PASS",
                                        color = StadiumGold,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 1.sp
                                    )
                                }
                                Text(
                                    text = "ID: ${initialProfile.id.takeLast(6).uppercase()}",
                                    color = HawkEyeCyan,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Avatar + Name + Jersey
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(CircleShape)
                                        .background(SurfaceDark)
                                        .border(2.dp, StadiumGold, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = avatarEmoji, fontSize = 28.sp)
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = fullName.ifBlank { "Player Name" },
                                            color = TextPrimary,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "#${jerseyNumber.ifBlank { "7" }}",
                                            color = CricketGreen,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                    Text(
                                        text = "@${username.removePrefix("@")} • ${jerseyName.ifBlank { "NAME" }}",
                                        color = StadiumGold,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "${teamName.ifBlank { "Warriors CC" }} • ${city.ifBlank { "Local Turf" }}",
                                        color = TextSecondary,
                                        fontSize = 10.sp
                                    )
                                }
                            }

                            // Style tags
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFF1E293B))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = battingStyle,
                                        color = HawkEyeCyan,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFF1E293B))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = bowlingStyle,
                                        color = CricketGreen,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFF334155))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "100% Verified",
                                        color = StadiumGold,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // Choose Avatar Emoji
                Text(
                    text = "CHOOSE AVATAR BADGE:",
                    color = StadiumGold,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    avatarChoices.forEach { emoji ->
                        val isSelected = avatarEmoji == emoji
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) CricketGreen.copy(alpha = 0.3f) else SurfaceDark)
                                .border(
                                    if (isSelected) 2.dp else 1.dp,
                                    if (isSelected) StadiumGold else Color(0xFF334155),
                                    CircleShape
                                )
                                .clickable { avatarEmoji = emoji },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = emoji, fontSize = 20.sp)
                        }
                    }
                }

                // Unique Username Field
                Text(
                    text = "UNIQUE CRICKET USERNAME (@ID):",
                    color = StadiumGold,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )

                val isAvailable = onCheckUsernameAvailable(username)
                OutlinedTextField(
                    value = username,
                    onValueChange = {
                        val clean = it.replace(" ", "_").lowercase()
                        username = clean
                        usernameError = if (clean.length < 3) {
                            "Username must be at least 3 characters"
                        } else if (!onCheckUsernameAvailable(clean)) {
                            "@$clean is taken by another player"
                        } else {
                            null
                        }
                    },
                    label = { Text("Unique Username", fontSize = 12.sp) },
                    leadingIcon = {
                        Text(
                            text = "@",
                            color = StadiumGold,
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp,
                            modifier = Modifier.padding(start = 12.dp)
                        )
                    },
                    trailingIcon = {
                        if (username.length >= 3) {
                            if (isAvailable && usernameError == null) {
                                Icon(Icons.Default.CheckCircle, contentDescription = "Available", tint = CricketGreen)
                            } else {
                                Icon(Icons.Default.Cancel, contentDescription = "Taken", tint = WicketRed)
                            }
                        }
                    },
                    isError = usernameError != null,
                    supportingText = {
                        usernameError?.let {
                            Text(text = it, color = WicketRed, fontSize = 11.sp)
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (usernameError != null) WicketRed else CricketGreen,
                        unfocusedBorderColor = if (usernameError != null) WicketRed else Color(0xFF475569),
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Form Inputs
                Text(
                    text = "PLAYER DETAILS:",
                    color = StadiumGold,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("Full Player Name", fontSize = 12.sp) },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = StadiumGold)
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CricketGreen,
                        unfocusedBorderColor = Color(0xFF475569),
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = mobileNumber,
                    onValueChange = { mobileNumber = it },
                    label = { Text("Verified Mobile Number", fontSize = 12.sp) },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Phone, contentDescription = null, tint = CricketGreen)
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CricketGreen,
                        unfocusedBorderColor = Color(0xFF475569),
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Jersey Name & Number
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = jerseyName,
                        onValueChange = { jerseyName = it },
                        label = { Text("Jersey Name", fontSize = 12.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CricketGreen,
                            unfocusedBorderColor = Color(0xFF475569),
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.weight(1.3f)
                    )

                    OutlinedTextField(
                        value = jerseyNumber,
                        onValueChange = { jerseyNumber = it },
                        label = { Text("Jersey #", fontSize = 12.sp) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CricketGreen,
                            unfocusedBorderColor = Color(0xFF475569),
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.weight(0.7f)
                    )
                }

                // Playing Role selection
                Text(
                    text = "PRIMARY PLAYING ROLE:",
                    color = StadiumGold,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    rolesList.chunked(3).forEach { chunk ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            chunk.forEach { role ->
                                val isSelected = primaryRole == role
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) CricketGreen else SurfaceDark,
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSelected) CricketGreen else Color(0xFF334155)
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { primaryRole = role }
                                ) {
                                    Text(
                                        text = role,
                                        color = if (isSelected) PitchDark else TextPrimary,
                                        fontSize = 10.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }

                // Team & City
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = teamName,
                        onValueChange = { teamName = it },
                        label = { Text("Team Name", fontSize = 12.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CricketGreen,
                            unfocusedBorderColor = Color(0xFF475569),
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = city,
                        onValueChange = { city = it },
                        label = { Text("City / Turf", fontSize = 12.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CricketGreen,
                            unfocusedBorderColor = Color(0xFF475569),
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cleanUser = username.trim().removePrefix("@").lowercase()
                    if (cleanUser.length < 3) {
                        usernameError = "Username must be at least 3 characters"
                        return@Button
                    }
                    if (!onCheckUsernameAvailable(cleanUser)) {
                        usernameError = "@$cleanUser is already taken by another player"
                        return@Button
                    }
                    val parsedNum = jerseyNumber.toIntOrNull() ?: 7
                    val updated = initialProfile.copy(
                        username = cleanUser,
                        fullName = fullName.ifBlank { "Ayush Sunil" },
                        mobileNumber = mobileNumber.ifBlank { "+91 98765 43210" },
                        jerseyName = jerseyName.ifBlank { "AYUSH" },
                        jerseyNumber = parsedNum,
                        primaryRole = primaryRole,
                        battingStyle = battingStyle,
                        bowlingStyle = bowlingStyle,
                        teamName = teamName.ifBlank { "Warriors CC" },
                        city = city.ifBlank { "Delhi, India" },
                        avatarEmoji = avatarEmoji
                    )
                    onSaveProfile(updated)
                },
                colors = ButtonDefaults.buttonColors(containerColor = CricketGreen),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "SAVE & SYNC TO GROUND NETWORK 🚀",
                    color = PitchDark,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        },
        dismissButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = {
                        onDismiss()
                        onLogout()
                    }
                ) {
                    Text(
                        text = "🚪 Logout / Switch",
                        color = Color(0xFFEF4444),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                TextButton(
                    onClick = onDismiss
                ) {
                    Text(
                        text = "Close",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }
        }
    )
}
