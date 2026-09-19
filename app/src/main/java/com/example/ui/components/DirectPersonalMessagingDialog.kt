package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.CricHeroesProfile
import com.example.data.model.ChatMessage
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DirectPersonalMessagingDialog(
    currentUser: CricHeroesProfile,
    communityPlayers: List<CricHeroesProfile>,
    personalMessages: List<ChatMessage>,
    activeRecipient: CricHeroesProfile?,
    onSelectRecipient: (CricHeroesProfile) -> Unit,
    onCloseChat: () -> Unit,
    onSendMessage: (recipientUsername: String, text: String) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Instagram / WhatsApp Gradient Palette
    val igGradient = Brush.linearGradient(
        listOf(
            Color(0xFF833AB4),
            Color(0xFFC13584),
            Color(0xFFE1306C),
            Color(0xFFFD1D1D),
            Color(0xFFF77737)
        )
    )

    // Filter community users for inbox
    val filteredPlayers = remember(communityPlayers, searchQuery, currentUser) {
        communityPlayers.filter { player ->
            player.username != currentUser.username &&
            (searchQuery.isBlank() ||
             player.username.contains(searchQuery, ignoreCase = true) ||
             player.fullName.contains(searchQuery, ignoreCase = true) ||
             player.teamName.contains(searchQuery, ignoreCase = true))
        }
    }

    // Messages with activeRecipient
    val activeConversation = remember(personalMessages, activeRecipient) {
        if (activeRecipient == null) emptyList()
        else personalMessages.sortedBy { it.timestamp }
    }

    LaunchedEffect(activeConversation.size) {
        if (activeConversation.isNotEmpty()) {
            listState.animateScrollToItem(activeConversation.size - 1)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .height(620.dp)
                .testTag("direct_personal_messaging_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = PitchDark),
            border = BorderStroke(1.dp, Color(0xFF1E293B))
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header (Switches between Inbox and 1-on-1 Chat)
                if (activeRecipient == null) {
                    // INBOX HEADER (WhatsApp / Instagram Direct style)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(igGradient)
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Chat,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "Direct Messages (DM)",
                                        color = Color.White,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "@${currentUser.username.removePrefix("@")} • Personal 1-on-1",
                                        color = Color.White.copy(alpha = 0.85f),
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = Color.White
                                )
                            }
                        }
                    }

                    // Search input for finding registered players
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search by @username, name, or team...", fontSize = 12.sp, color = TextMuted) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = StadiumGold, modifier = Modifier.size(18.dp)) },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextMuted, modifier = Modifier.size(16.dp))
                                    }
                                }
                            },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CricketGreen,
                                unfocusedBorderColor = Color(0xFF334155),
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Players Directory List
                    Text(
                        text = "REGISTERED CRICKETERS ON GROUND (${filteredPlayers.size})",
                        color = StadiumGold,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )

                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (filteredPlayers.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No players found matching \"$searchQuery\"",
                                        color = TextSecondary,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        } else {
                            items(filteredPlayers, key = { it.id }) { player ->
                                // Find last message between me and player
                                val lastMsg = if (player.id == activeRecipient?.id || player.phoneNumber == activeRecipient?.phoneNumber) {
                                    personalMessages.maxByOrNull { it.timestamp }
                                } else null

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onSelectRecipient(player) },
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                                    border = BorderStroke(1.dp, Color(0xFF1E293B))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Avatar with Online indicator
                                        Box {
                                            Box(
                                                modifier = Modifier
                                                    .size(44.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFF0F172A))
                                                    .border(1.5.dp, StadiumGold, CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(text = player.avatarEmoji, fontSize = 22.sp)
                                            }
                                            if (player.isOnline) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(12.dp)
                                                        .clip(CircleShape)
                                                        .background(CricketGreen)
                                                        .border(1.5.dp, PitchDark, CircleShape)
                                                        .align(Alignment.BottomEnd)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = player.fullName,
                                                    color = TextPrimary,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                if (lastMsg != null) {
                                                    val timeStr = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(lastMsg.timestamp))
                                                    Text(
                                                        text = timeStr,
                                                        color = TextMuted,
                                                        fontSize = 10.sp
                                                    )
                                                }
                                            }

                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = "@${player.username.removePrefix("@")}",
                                                    color = StadiumGold,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "• ${player.jerseyName} #${player.jerseyNumber}",
                                                    color = HawkEyeCyan,
                                                    fontSize = 10.sp
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = lastMsg?.text ?: "${player.teamName} • Tap to message",
                                                color = if (lastMsg != null) TextSecondary else TextMuted,
                                                fontSize = 11.sp,
                                                maxLines = 1
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(8.dp))
                                        Icon(
                                            imageVector = Icons.Default.ChevronRight,
                                            contentDescription = null,
                                            tint = TextMuted,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // 1-ON-1 PERSONAL CHAT SCREEN (WhatsApp / Instagram Direct style)
                    // Top Bar for active chat
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF0F172A))
                            .border(BorderStroke(1.dp, Color(0xFF1E293B)))
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                IconButton(
                                    onClick = onCloseChat,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back to inbox",
                                        tint = StadiumGold
                                    )
                                }

                                Box {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(SurfaceDark)
                                            .border(1.dp, StadiumGold, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = activeRecipient.avatarEmoji, fontSize = 20.sp)
                                    }
                                    if (activeRecipient.isOnline) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(CricketGreen)
                                                .border(1.dp, PitchDark, CircleShape)
                                                .align(Alignment.BottomEnd)
                                        )
                                    }
                                }

                                Column {
                                    Text(
                                        text = activeRecipient.fullName,
                                        color = TextPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "@${activeRecipient.username.removePrefix("@")} • ${if (activeRecipient.isOnline) "Active Now" else "Offline"}",
                                        color = if (activeRecipient.isOnline) CricketGreen else TextMuted,
                                        fontSize = 10.sp
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = onDismiss,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Close",
                                        tint = TextSecondary
                                    )
                                }
                            }
                        }
                    }

                    // Conversation message list
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (activeConversation.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(36.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(text = activeRecipient.avatarEmoji, fontSize = 40.sp)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Start chatting with @${activeRecipient.username.removePrefix("@")}",
                                            color = TextPrimary,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Send match updates, pitch tips, or strategy!",
                                            color = TextMuted,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        } else {
                            items(activeConversation, key = { it.id }) { msg ->
                                val isMe = msg.isFromMe || msg.senderUsername.removePrefix("@").equals(
                                    currentUser.username.removePrefix("@"),
                                    ignoreCase = true
                                )
                                val timeStr = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(msg.timestamp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
                                ) {
                                    Column(
                                        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start,
                                        modifier = Modifier.widthIn(max = 270.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .clip(
                                                    RoundedCornerShape(
                                                        topStart = 16.dp,
                                                        topEnd = 16.dp,
                                                        bottomStart = if (isMe) 16.dp else 2.dp,
                                                        bottomEnd = if (isMe) 2.dp else 16.dp
                                                    )
                                                )
                                                .background(
                                                    if (isMe) {
                                                        Brush.linearGradient(listOf(Color(0xFF00C853), Color(0xFF007E33)))
                                                    } else {
                                                        Brush.linearGradient(listOf(Color(0xFF1E293B), Color(0xFF0F172A)))
                                                    }
                                                )
                                                .padding(horizontal = 14.dp, vertical = 10.dp)
                                        ) {
                                            Text(
                                                text = msg.text,
                                                color = Color.White,
                                                fontSize = 13.sp,
                                                lineHeight = 17.sp
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(2.dp))
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = timeStr,
                                                color = TextMuted,
                                                fontSize = 9.sp
                                            )
                                            if (isMe) {
                                                Icon(
                                                    imageVector = Icons.Default.DoneAll,
                                                    contentDescription = "Delivered",
                                                    tint = CricketGreen,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Quick cricket preset replies
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Match start kab hai? 🏏", "Pitch kaisa hai? 🔥", "Badhiya shot! 🚀").forEach { quick ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = SurfaceDark,
                                border = BorderStroke(1.dp, Color(0xFF334155)),
                                modifier = Modifier.clickable {
                                    onSendMessage(activeRecipient.username, quick)
                                }
                            ) {
                                Text(
                                    text = quick,
                                    color = TextSecondary,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    // Message input field
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF0B111E))
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = messageText,
                            onValueChange = { messageText = it },
                            placeholder = { Text("Message @${activeRecipient.username.removePrefix("@")}...", fontSize = 12.sp, color = TextMuted) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(
                                onSend = {
                                    if (messageText.isNotBlank()) {
                                        onSendMessage(activeRecipient.username, messageText)
                                        messageText = ""
                                    }
                                }
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CricketGreen,
                                unfocusedBorderColor = Color(0xFF334155),
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.weight(1f)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            onClick = {
                                if (messageText.isNotBlank()) {
                                    onSendMessage(activeRecipient.username, messageText)
                                    messageText = ""
                                }
                            },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(CricketGreen)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send",
                                tint = PitchDark,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
