package com.example.ui.auth

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.network.PhoneAuthManager
import com.example.ui.theme.PitchDark
import com.example.ui.theme.CricketGreen
import com.example.ui.theme.StadiumGold

@Composable
fun OtpVerificationScreen(
    onSuccess: () -> Unit,
    onDismiss: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val authManager = remember(activity) { activity?.let { PhoneAuthManager(it) } }

    var phone by remember { mutableStateOf("") }
    var otp by remember { mutableStateOf("") }
    var isOtpSent by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PitchDark)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (!isOtpSent) "Mobile Number Dalein" else "SMS OTP Enter Karein",
            style = MaterialTheme.typography.titleLarge,
            color = StadiumGold,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (!isOtpSent) {
            OutlinedTextField(
                value = phone,
                onValueChange = { if (it.length <= 10) phone = it },
                label = { Text("10 Digit Mobile Number") },
                prefix = { Text("+91 ") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (phone.length == 10) {
                        if (authManager == null) {
                            statusText = "Activity context not found"
                            return@Button
                        }
                        isLoading = true
                        statusText = "SMS bhej rahe hain..."
                        authManager.sendOtp(
                            phoneNumber = "+91$phone",
                            onCodeSent = {
                                isLoading = false
                                isOtpSent = true
                                statusText = "OTP bhej diya gaya!"
                            },
                            onError = { err ->
                                isLoading = false
                                statusText = err
                            }
                        )
                    } else {
                        statusText = "10 digit ka number enter karein"
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = CricketGreen),
                shape = RoundedCornerShape(12.dp),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Get Real SMS OTP", fontWeight = FontWeight.Bold)
                }
            }
        } else {
            OutlinedTextField(
                value = otp,
                onValueChange = { if (it.length <= 6) otp = it },
                label = { Text("6 Digit OTP") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
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
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = StadiumGold),
                shape = RoundedCornerShape(12.dp),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Verify OTP", fontWeight = FontWeight.Bold)
                }
            }
        }

        if (statusText.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = statusText, color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
        }

        if (onDismiss != null) {
            Spacer(modifier = Modifier.height(16.dp))
            TextButton(onClick = onDismiss) {
                Text("Cancel / Back", color = MaterialTheme.colorScheme.outline)
            }
        }
    }
}
