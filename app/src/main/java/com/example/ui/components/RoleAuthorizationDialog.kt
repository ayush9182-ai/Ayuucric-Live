package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DeviceRole
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
fun RoleAuthorizationDialog(
    currentRole: DeviceRole,
    officialPin: String,
    pendingRequestsCount: Int = 0,
    onDismiss: () -> Unit,
    onSelectRole: (DeviceRole, String) -> Boolean,
    onRequestRole: (DeviceRole, String) -> Unit = { _, _ -> },
    onOpenAdminRequests: () -> Unit = {}
) {
    var selectedRole by remember { mutableStateOf(currentRole) }
    var enteredPin by remember { mutableStateOf("") }
    var requestReason by remember { mutableStateOf("") }
    var showRequestForm by remember { mutableStateOf(false) }
    var requestSentSuccess by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = PitchDark,
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(CricketGreen.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = CricketGreen,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = "Match Officials & Devices",
                        color = TextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "4 Official Phones + Spectator Mode",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF1E293B))
                        .padding(10.dp)
                ) {
                    Text(
                        text = "Rules: Exactly 4 phones get official roles to record video, count runs/wickets, and declare DRS. All other phones connect as live spectators.",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }

                Text(
                    text = "SELECT ROLE FOR THIS PHONE:",
                    color = StadiumGold,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )

                // 4 Official Roles
                RoleItemCard(
                    role = DeviceRole.BOWLER_END_UMPIRE,
                    isSelected = selectedRole == DeviceRole.BOWLER_END_UMPIRE,
                    icon = Icons.Default.Videocam,
                    iconColor = CricketGreen,
                    onSelect = {
                        selectedRole = DeviceRole.BOWLER_END_UMPIRE
                        errorMessage = null
                    }
                )

                RoleItemCard(
                    role = DeviceRole.SQUARE_LEG_UMPIRE,
                    isSelected = selectedRole == DeviceRole.SQUARE_LEG_UMPIRE,
                    icon = Icons.Default.Straighten,
                    iconColor = HawkEyeCyan,
                    onSelect = {
                        selectedRole = DeviceRole.SQUARE_LEG_UMPIRE
                        errorMessage = null
                    }
                )

                RoleItemCard(
                    role = DeviceRole.OFFICIAL_SCORER,
                    isSelected = selectedRole == DeviceRole.OFFICIAL_SCORER,
                    icon = Icons.Default.EditNote,
                    iconColor = StadiumGold,
                    onSelect = {
                        selectedRole = DeviceRole.OFFICIAL_SCORER
                        errorMessage = null
                    }
                )

                RoleItemCard(
                    role = DeviceRole.THIRD_UMPIRE_DRS,
                    isSelected = selectedRole == DeviceRole.THIRD_UMPIRE_DRS,
                    icon = Icons.Default.Policy,
                    iconColor = Color(0xFFA78BFA),
                    onSelect = {
                        selectedRole = DeviceRole.THIRD_UMPIRE_DRS
                        errorMessage = null
                    }
                )

                // Spectator Role
                RoleItemCard(
                    role = DeviceRole.SPECTATOR_VIEWER,
                    isSelected = selectedRole == DeviceRole.SPECTATOR_VIEWER,
                    icon = Icons.Default.RemoveRedEye,
                    iconColor = TextSecondary,
                    onSelect = {
                        selectedRole = DeviceRole.SPECTATOR_VIEWER
                        errorMessage = null
                    }
                )

                // Official role authorization section
                if (selectedRole.isOfficial) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(SurfaceDark)
                            .border(1.dp, Color(0xFF334155), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = StadiumGold,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Admin Approval Required (सुरक्षित अनुमति)",
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "Cheating aur unauthenticated scoring se bachne ke liye naye users Viewer role mein rehte hain. Official banne ke liye Admin Ayush se permission request bhejiye.",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )

                        // Direct Request Form to Admin Ayush
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF0F172A))
                                .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(10.dp))
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Send Role Request to Admin Ayush:",
                                color = StadiumGold,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            OutlinedTextField(
                                value = requestReason,
                                onValueChange = { requestReason = it },
                                placeholder = { Text("Aapka Naam & Role badalne ka kaaran", fontSize = 11.sp) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = StadiumGold,
                                    unfocusedBorderColor = Color(0xFF334155),
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Button(
                                onClick = {
                                    onRequestRole(selectedRole, if (requestReason.isNotBlank()) requestReason else "Requesting official access")
                                    requestSentSuccess = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = StadiumGold),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = PitchDark, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Send Request to Admin (एडमिन को रिक्वेस्ट भेजें)", color = PitchDark, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                            if (requestSentSuccess) {
                                Text(
                                    text = "✓ Request sent! Admin Ayush ke pass notification chali gayi hai. Approval milte hi aapka role activate ho jayega.",
                                    color = CricketGreen,
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp
                                )
                            }
                        }

                        // Server-Side Role Claim Verification
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B4B).copy(alpha = 0.5f)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF4338CA).copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = null,
                                    tint = HawkEyeCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "SERVER AUTHORIZATION REQUIRED",
                                        color = StadiumGold,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Official match roles (Scorer, Umpires, DRS) are validated via Firebase Auth claims & Admin approval. Spectators enjoy read-only live broadcast.",
                                        color = TextSecondary,
                                        fontSize = 9.sp,
                                        lineHeight = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }

                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = WicketRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedRole == DeviceRole.SPECTATOR_VIEWER) {
                        onSelectRole(selectedRole, "")
                        onDismiss()
                    } else {
                        val success = onSelectRole(selectedRole, enteredPin)
                        if (success) {
                            onDismiss()
                        } else {
                            errorMessage = "Authorization required from Admin or Match Official. Please submit a request above."
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = CricketGreen),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Confirm Role", color = PitchDark, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}

@Composable
private fun RoleItemCard(
    role: DeviceRole,
    isSelected: Boolean,
    icon: ImageVector,
    iconColor: Color,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) SurfaceDark else Color(0xFF131D2E)
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.horizontalGradient(
                if (isSelected) listOf(iconColor, iconColor.copy(alpha = 0.5f))
                else listOf(Color(0xFF263345), Color(0xFF1E2837))
            )
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = role.phoneLabel,
                        color = iconColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "• ${role.title}",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Text(
                    text = role.hindiTitle,
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = role.description,
                    color = TextMuted,
                    fontSize = 10.sp,
                    lineHeight = 13.sp
                )
            }

            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
