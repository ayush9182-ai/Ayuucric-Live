package com.example.ui.auth

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.network.PhoneAuthManager
import com.example.ui.theme.CricketGreen
import com.example.ui.theme.PitchDark
import com.example.ui.theme.StadiumGold

/**
 * 100% Responsive, Auto-Fitting OTP Verification Screen.
 * Adapts across 5-inch phones and tall 6.7+ displays using:
 * - Dynamic width percentage (92% max 500dp)
 * - Window safe area handling (.systemBarsPadding(), .imePadding())
 * - Scrollable container for keyboard popup resilience
 */
@Composable
fun OtpVerificationScreen(
    onSuccess: () -> Unit,
    onDismiss: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val focusManager = LocalFocusManager.current
    val authManager = remember(activity) { activity?.let { PhoneAuthManager(it) } }
    val scrollState = rememberScrollState()

    var phone by remember { mutableStateOf("") }
    var otp by remember { mutableStateOf("") }
    var isOtpSent by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(PitchDark)
            .systemBarsPadding()
            .imePadding(),
        contentAlignment = Alignment.Center
    ) {
        val screenWidth = maxWidth
        val isTablet = screenWidth > 600.dp
        val contentFraction = if (isTablet) 0.60f else 0.92f

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = if (isTablet) 24.dp else 16.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(contentFraction)
                    .widthIn(max = 500.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (!isOtpSent) "Mobile OTP Verification" else "Enter 6-Digit Code",
                    style = MaterialTheme.typography.titleLarge,
                    color = StadiumGold,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = if (!isOtpSent)
                        "Enter your 10-digit mobile number to receive an instant verification code"
                    else
                        "Enter the 6-digit OTP code sent to +91 $phone",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 6.dp, bottom = 20.dp)
                )

                if (!isOtpSent) {
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { if (it.length <= 10) phone = it },
                        label = { Text("10-Digit Mobile Number", fontSize = 14.sp) },
                        prefix = { Text("+91 ", fontWeight = FontWeight.Bold) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 52.dp),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            if (phone.length == 10) {
                                if (authManager == null) {
                                    statusText = "Activity context not found"
                                    return@Button
                                }
                                isLoading = true
                                statusText = "Sending verification code..."
                                authManager.sendOtp(
                                    phoneNumber = "+91$phone",
                                    onCodeSent = {
                                        isLoading = false
                                        isOtpSent = true
                                        if (authManager.isEmulatorFallback) {
                                            otp = "123456"
                                            statusText = "Virtual device: Test OTP (123456) ready. Tap Verify to continue."
                                        } else {
                                            statusText = "Code ready! Enter OTP to continue."
                                        }
                                    },
                                    onError = { err ->
                                        isLoading = false
                                        statusText = err
                                    }
                                )
                            } else {
                                statusText = "Please enter a valid 10-digit number."
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CricketGreen),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Get Verification Code", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = otp,
                        onValueChange = { if (it.length <= 6) otp = it },
                        label = { Text("6-Digit OTP Code", fontSize = 14.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 52.dp),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            if (authManager == null) {
                                statusText = "Activity context not found"
                                return@Button
                            }
                            isLoading = true
                            authManager.verifyOtp(
                                otpCode = otp,
                                onSuccess = {
                                    isLoading = false
                                    onSuccess()
                                },
                                onError = { err ->
                                    isLoading = false
                                    statusText = err
                                }
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = StadiumGold),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Verify & Continue", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }
                }

                if (statusText.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = statusText,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (onDismiss != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    TextButton(
                        onClick = {
                            focusManager.clearFocus()
                            onDismiss()
                        },
                        modifier = Modifier.defaultMinSize(minHeight = 44.dp)
                    ) {
                        Text("Cancel / Back", color = MaterialTheme.colorScheme.outline, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}
