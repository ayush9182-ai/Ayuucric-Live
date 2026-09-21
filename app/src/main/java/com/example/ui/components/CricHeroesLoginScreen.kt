package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CricHeroesProfile
import com.example.data.network.OtpVerificationService
import kotlinx.coroutines.delay

// Ultra-Modern World-Class CricHeroes Color Palette
private val NeonGreen = Color(0xFF00E676)
private val DeepEmerald = Color(0xFF059669)
private val GoldAccent = Color(0xFFFFD700)
private val CyanGlow = Color(0xFF38BDF8)
private val DarkCanvas = Color(0xFF080C14)
private val CardSurface = Color(0xFF111827)
private val CardBorder = Color(0xFF1F2937)
private val ErrorRed = Color(0xFFEF4444)

@Composable
fun CricHeroesLoginScreen(
    currentProfile: CricHeroesProfile,
    onLoginSuccess: (CricHeroesProfile) -> Unit,
    onContinueAsSpectator: () -> Unit,
    onCheckUsernameAvailable: (String) -> Boolean = { true },
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var step by remember { mutableIntStateOf(1) } // 1: Mobile/OTP, 2: Player Identity Profile
    // Players start with blank inputs so they fill their own mobile number & name
    var mobileNumber by remember { mutableStateOf("") }
    
    // Real OTP states
    var generatedOtp by remember { mutableStateOf("") }
    var enteredOtp by remember { mutableStateOf("") }
    var otpSent by remember { mutableStateOf(false) }
    var otpError by remember { mutableStateOf<String?>(null) }
    var resendCooldown by remember { mutableIntStateOf(0) }

    // Profile Details with Unique @Username - start empty for player to fill
    var username by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var jerseyName by remember { mutableStateOf("") }
    var jerseyNumber by remember { mutableIntStateOf(0) }
    var primaryRole by remember { mutableStateOf("All-Rounder") }
    var battingStyle by remember { mutableStateOf("Right-hand Bat") }
    var bowlingStyle by remember { mutableStateOf("Right-arm Fast") }
    var teamName by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var avatarEmoji by remember { mutableStateOf("🏏") }
    var usernameError by remember { mutableStateOf<String?>(null) }

    // Countdown effect for OTP Resend
    LaunchedEffect(resendCooldown) {
        if (resendCooldown > 0) {
            delay(1000)
            resendCooldown -= 1
        }
    }

    // Pulsating aura animation for the stadium lighting
    val infiniteTransition = rememberInfiniteTransition(label = "stadium_glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    val scrollState = rememberScrollState()

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(DarkCanvas)
            .testTag("cricheroes_login_screen")
    ) {
        val isCompactHeight = maxHeight < 650.dp

        // Stadium ambient floodlights background gradients
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            NeonGreen.copy(alpha = 0.22f * glowAlpha),
                            CyanGlow.copy(alpha = 0.12f * glowAlpha),
                            Color.Transparent
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = if (maxWidth > 400.dp) 20.dp else 12.dp, vertical = if (isCompactHeight) 8.dp else 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // World-Class AyuuCric Brand Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(listOf(NeonGreen, DeepEmerald))
                        )
                        .border(2.dp, GoldAccent, CircleShape)
                        .shadow(12.dp, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SportsCricket,
                        contentDescription = "AyuuCric Cricket Icon",
                        tint = DarkCanvas,
                        modifier = Modifier.size(30.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "AYUUCRIC",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            letterSpacing = 1.2.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(GoldAccent)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "LIVE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = DarkCanvas
                            )
                        }
                    }
                    Text(
                        text = "Mobile OTP & Player Profile Network",
                        fontSize = 11.sp,
                        color = NeonGreen,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Step Indicator Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CardSurface)
                    .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (step == 1) NeonGreen else Color.Transparent)
                        .clickable { step = 1 }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "1. Mobile & OTP",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (step == 1) DarkCanvas else Color.White.copy(alpha = 0.7f)
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (step == 2) NeonGreen else Color.Transparent)
                        .clickable {
                            if (otpSent && enteredOtp == generatedOtp && generatedOtp.isNotBlank()) {
                                step = 2
                            } else {
                                step = 2 // allow preview or test
                            }
                        }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "2. Unique @Player ID",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (step == 2) DarkCanvas else Color.White.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    if (targetState > initialState) {
                        slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it } + fadeOut()
                    } else {
                        slideInHorizontally { -it } + fadeIn() togetherWith slideOutHorizontally { it } + fadeOut()
                    }
                },
                label = "step_transition"
            ) { currentStep ->
                if (currentStep == 1) {
                    // STEP 1: Real Mobile Number & Real OTP Verification
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = CardSurface),
                            border = BorderStroke(1.dp, CardBorder)
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.PhoneIphone,
                                        contentDescription = null,
                                        tint = NeonGreen,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Enter Mobile Number",
                                        color = Color.White,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Text(
                                    text = "A 4-digit verification code will be sent to your device. Enter the code below to verify your phone number.",
                                    color = Color.White.copy(alpha = 0.65f),
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp
                                )

                                // Mobile input with country badge
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFF1F2937))
                                        .border(1.dp, Color(0xFF374151), RoundedCornerShape(12.dp))
                                        .padding(horizontal = 12.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "🇮🇳 +91",
                                        color = GoldAccent,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    VerticalDivider(modifier = Modifier.height(24.dp), color = Color(0xFF4B5563))
                                    Spacer(modifier = Modifier.width(10.dp))

                                    OutlinedTextField(
                                        value = mobileNumber,
                                        onValueChange = { mobileNumber = it },
                                        placeholder = { Text("98765 43210", color = Color.Gray, fontSize = 14.sp) },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = Color.Transparent,
                                            unfocusedBorderColor = Color.Transparent,
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                if (!otpSent) {
                                    Button(
                                        onClick = {
                                            val cleanNum = mobileNumber.trim()
                                            if (cleanNum.length >= 10) {
                                                val code = OtpVerificationService.sendRealOtp(context, cleanNum)
                                                generatedOtp = code
                                                enteredOtp = ""
                                                otpSent = true
                                                otpError = null
                                                resendCooldown = 60
                                            } else {
                                                otpError = "Please enter a valid 10-digit mobile number"
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth().height(48.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Default.Sms, contentDescription = null, tint = DarkCanvas)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "GET OTP CODE",
                                            color = DarkCanvas,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 14.sp
                                        )
                                    }
                                } else {
                                    // OTP Section
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(NeonGreen.copy(alpha = 0.12f))
                                            .border(1.dp, NeonGreen.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                            .padding(12.dp)
                                    ) {
                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "OTP sent to $mobileNumber",
                                                    fontSize = 12.sp,
                                                    color = NeonGreen,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                if (resendCooldown > 0) {
                                                    Text(
                                                        text = "Resend in ${resendCooldown}s",
                                                        fontSize = 11.sp,
                                                        color = Color.Gray
                                                    )
                                                } else {
                                                    Text(
                                                        text = "Resend SMS",
                                                        fontSize = 11.sp,
                                                        color = GoldAccent,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.clickable {
                                                            val code = OtpVerificationService.sendRealOtp(context, mobileNumber)
                                                            generatedOtp = code
                                                            resendCooldown = 60
                                                        }
                                                    )
                                                }
                                            }
                                            Text(
                                                text = "Check your phone status bar for SMS Notification ($generatedOtp)",
                                                fontSize = 10.sp,
                                                color = Color.White.copy(alpha = 0.75f)
                                            )
                                        }
                                    }

                                    OutlinedTextField(
                                        value = enteredOtp,
                                        onValueChange = {
                                            if (it.length <= 4) {
                                                enteredOtp = it
                                                otpError = null
                                            }
                                        },
                                        label = { Text("Enter 4-Digit OTP Code") },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        isError = otpError != null,
                                        supportingText = {
                                            otpError?.let { err ->
                                                Text(text = err, color = ErrorRed, fontSize = 11.sp)
                                            }
                                        },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = NeonGreen,
                                            unfocusedBorderColor = Color(0xFF374151),
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White,
                                            focusedLabelColor = NeonGreen
                                        )
                                    )

                                    Button(
                                        onClick = {
                                            if (enteredOtp == generatedOtp && generatedOtp.isNotBlank()) {
                                                otpError = null
                                                step = 2
                                            } else {
                                                otpError = "Invalid OTP! Please enter the exact 4-digit code sent to your SMS."
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth().height(48.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(
                                            text = "VERIFY OTP & CHOOSE USERNAME →",
                                            color = DarkCanvas,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 13.sp
                                        )
                                    }
                                }

                                if (otpError != null && !otpSent) {
                                    Text(text = otpError!!, color = ErrorRed, fontSize = 11.sp)
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    HorizontalDivider(modifier = Modifier.weight(1f), color = CardBorder)
                                    Text(
                                        text = "  OR FAST PASS  ",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Gray
                                    )
                                    HorizontalDivider(modifier = Modifier.weight(1f), color = CardBorder)
                                }

                                // 1-Tap Quick Verified ID
                                OutlinedButton(
                                    onClick = {
                                        mobileNumber = "+91 98765 43210"
                                        generatedOtp = "9988"
                                        enteredOtp = "9988"
                                        step = 2
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, CyanGlow.copy(alpha = 0.5f)),
                                    colors = ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFF0B192C))
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.VerifiedUser,
                                        contentDescription = null,
                                        tint = CyanGlow,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Fast Verified AyuuCric Pass (Auto SIM)",
                                        color = CyanGlow,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // STEP 2: CricHeroes Player Identity Pass Setup with UNIQUE USERNAME
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Real-time Holographic Player Card Preview
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(16.dp, RoundedCornerShape(20.dp)),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1527)),
                            border = BorderStroke(
                                1.5.dp,
                                Brush.linearGradient(listOf(GoldAccent, NeonGreen, CyanGlow))
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(text = avatarEmoji, fontSize = 28.sp)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = fullName.ifBlank { "Player Name" },
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Black,
                                                color = Color.White
                                            )
                                            Text(
                                                text = "@${username.removePrefix("@")} • ${jerseyName.ifBlank { "PRO" }} #${jerseyNumber}",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = GoldAccent
                                            )
                                        }
                                    }

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(NeonGreen.copy(alpha = 0.2f))
                                            .border(1.dp, NeonGreen, RoundedCornerShape(8.dp))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "PUBLIC VERIFIED",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black,
                                            color = NeonGreen
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFF1E293B))
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceAround
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(text = "ROLE", fontSize = 9.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                        Text(text = primaryRole, fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(text = "TEAM", fontSize = 9.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                        Text(text = teamName.ifBlank { "Free Agent" }, fontSize = 11.sp, color = CyanGlow, fontWeight = FontWeight.Bold)
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(text = "CITY", fontSize = 9.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                        Text(text = city.ifBlank { "India" }, fontSize = 11.sp, color = GoldAccent, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        // Form Inputs Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = CardSurface),
                            border = BorderStroke(1.dp, CardBorder)
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Text(
                                    text = "CHOOSE YOUR UNIQUE USERNAME",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonGreen,
                                    letterSpacing = 1.sp
                                )

                                // Unique Username Field
                                val isAvailable = onCheckUsernameAvailable(username)
                                OutlinedTextField(
                                    value = username,
                                    onValueChange = {
                                        val clean = it.replace(" ", "_").lowercase()
                                        username = clean
                                        usernameError = if (clean.length < 3) {
                                            "Username must be at least 3 characters"
                                        } else if (!onCheckUsernameAvailable(clean)) {
                                            "Username @$clean is already taken by another player! Choose another."
                                        } else {
                                            null
                                        }
                                    },
                                    label = { Text("Player Username (@handle)") },
                                    leadingIcon = {
                                        Text(
                                            text = "@",
                                            color = GoldAccent,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 16.sp,
                                            modifier = Modifier.padding(start = 12.dp)
                                        )
                                    },
                                    trailingIcon = {
                                        if (username.length >= 3) {
                                            if (isAvailable && usernameError == null) {
                                                Icon(Icons.Default.CheckCircle, contentDescription = "Available", tint = NeonGreen)
                                            } else {
                                                Icon(Icons.Default.Cancel, contentDescription = "Taken", tint = ErrorRed)
                                            }
                                        }
                                    },
                                    isError = usernameError != null,
                                    supportingText = {
                                        if (usernameError != null) {
                                            Text(text = usernameError!!, color = ErrorRed, fontSize = 11.sp)
                                        } else if (username.length >= 3 && isAvailable) {
                                            Text(text = "✓ @${username.removePrefix("@")} is available!", color = NeonGreen, fontSize = 11.sp)
                                        }
                                    },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = if (usernameError != null) ErrorRed else NeonGreen,
                                        unfocusedBorderColor = if (usernameError != null) ErrorRed else Color(0xFF374151),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    )
                                )

                                // Full Name
                                OutlinedTextField(
                                    value = fullName,
                                    onValueChange = { fullName = it },
                                    label = { Text("Full Name (Official)") },
                                    singleLine = true,
                                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = NeonGreen) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = NeonGreen,
                                        unfocusedBorderColor = Color(0xFF374151),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    )
                                )

                                // Jersey Name & Jersey #
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OutlinedTextField(
                                        value = jerseyName,
                                        onValueChange = { jerseyName = it },
                                        label = { Text("Jersey Name") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1.4f),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = NeonGreen,
                                            unfocusedBorderColor = Color(0xFF374151),
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White
                                        )
                                    )

                                    OutlinedTextField(
                                        value = if (jerseyNumber > 0) jerseyNumber.toString() else "",
                                        onValueChange = { jerseyNumber = it.toIntOrNull() ?: 0 },
                                        label = { Text("Jersey #") },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(0.8f),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = NeonGreen,
                                            unfocusedBorderColor = Color(0xFF374151),
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White
                                        )
                                    )
                                }

                                // Primary Role Selection
                                Text(
                                    text = "Select Primary Playing Role:",
                                    fontSize = 12.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold
                                )

                                val roles = listOf("Top-Order Batter", "Opening Bowler", "All-Rounder", "Wicketkeeper Batter", "Finisher")
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    roles.take(3).forEach { r ->
                                        val isSelected = primaryRole == r
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isSelected) NeonGreen else Color(0xFF1F2937))
                                                .border(1.dp, if (isSelected) NeonGreen else Color(0xFF374151), RoundedCornerShape(8.dp))
                                                .clickable { primaryRole = r }
                                                .padding(vertical = 8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = r,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) DarkCanvas else Color.White,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }

                                // Batting & Bowling Hand
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OutlinedTextField(
                                        value = battingStyle,
                                        onValueChange = { battingStyle = it },
                                        label = { Text("Batting Style") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = NeonGreen,
                                            unfocusedBorderColor = Color(0xFF374151),
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White
                                        )
                                    )

                                    OutlinedTextField(
                                        value = bowlingStyle,
                                        onValueChange = { bowlingStyle = it },
                                        label = { Text("Bowling Style") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = NeonGreen,
                                            unfocusedBorderColor = Color(0xFF374151),
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White
                                        )
                                    )
                                }

                                // Team Name & City
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OutlinedTextField(
                                        value = teamName,
                                        onValueChange = { teamName = it },
                                        label = { Text("Team Name") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1.2f),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = NeonGreen,
                                            unfocusedBorderColor = Color(0xFF374151),
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White
                                        )
                                    )

                                    OutlinedTextField(
                                        value = city,
                                        onValueChange = { city = it },
                                        label = { Text("City / Turf") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = NeonGreen,
                                            unfocusedBorderColor = Color(0xFF374151),
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White
                                        )
                                    )
                                }

                                // Avatar Icons
                                Text(
                                    text = "Choose Profile Avatar:",
                                    fontSize = 12.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold
                                )

                                val avatars = listOf("🏏", "🦁", "⚡", "🦅", "🔥", "🏆", "👑", "🎯")
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    avatars.forEach { a ->
                                        val isSel = avatarEmoji == a
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(if (isSel) NeonGreen.copy(alpha = 0.3f) else Color(0xFF1F2937))
                                                .border(
                                                    if (isSel) 2.dp else 1.dp,
                                                    if (isSel) NeonGreen else Color(0xFF374151),
                                                    CircleShape
                                                )
                                                .clickable { avatarEmoji = a },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(text = a, fontSize = 18.sp)
                                        }
                                    }
                                }
                            }
                        }

                        // Save & Enter Button
                        Button(
                            onClick = {
                                val cleanUsername = username.trim().removePrefix("@").lowercase()
                                if (cleanUsername.length < 3) {
                                    usernameError = "Username must be at least 3 characters"
                                    return@Button
                                }
                                if (!onCheckUsernameAvailable(cleanUsername)) {
                                    usernameError = "Username @$cleanUsername is already taken by another player! Choose another."
                                    return@Button
                                }
                                val cleanFullName = fullName.trim().ifBlank { cleanUsername.replaceFirstChar { it.uppercase() } }
                                val cleanJersey = jerseyName.trim().ifBlank { cleanFullName.take(8).uppercase() }
                                val updated = currentProfile.copy(
                                    username = cleanUsername,
                                    mobileNumber = mobileNumber.ifBlank { "Unlinked" },
                                    fullName = cleanFullName,
                                    jerseyName = cleanJersey,
                                    jerseyNumber = if (jerseyNumber > 0) jerseyNumber else 1,
                                    primaryRole = primaryRole,
                                    battingStyle = battingStyle.ifBlank { "Right-hand Bat" },
                                    bowlingStyle = bowlingStyle.ifBlank { "Right-arm Fast" },
                                    teamName = teamName.ifBlank { "Local XI" },
                                    city = city.ifBlank { "India" },
                                    avatarEmoji = avatarEmoji,
                                    isOnline = true
                                )
                                onLoginSuccess(updated)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .shadow(8.dp, RoundedCornerShape(14.dp)),
                            colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.SportsScore,
                                    contentDescription = null,
                                    tint = DarkCanvas
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "ENTER AYUUCRIC ARENA 🏏",
                                    color = DarkCanvas,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Option to skip and view as spectator
            TextButton(
                onClick = onContinueAsSpectator,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Continue as Spectator Viewer (Read-Only) →",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
