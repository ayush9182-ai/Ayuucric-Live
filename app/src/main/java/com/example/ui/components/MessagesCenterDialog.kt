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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.ChatMessage
import com.example.data.model.CricHeroesProfile
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * Unified commercial-grade Messages Hub consolidating:
 *  1. Live Match Room Chat (Fans & Spectators)
 *  2. 1-on-1 Personal DMs (Cricketers, Umpires & Scorers)
 * Driven 100% by real Firebase Firestore snapshots.
 */
@Composable
fun MessagesCenterDialog(
    initialTab: Int = 0,
    matchTitle: String,
    matchMessages: List<ChatMessage>,
    onSendMatchMessage: (String) -> Unit,
    onSendMatchReaction: (String) -> Unit,
    currentUser: CricHeroesProfile,
    communityPlayers: List<CricHeroesProfile>,
    personalMessages: List<ChatMessage>,
    activeRecipient: CricHeroesProfile?,
    onSelectRecipient: (CricHeroesProfile) -> Unit,
    onCloseDirectChat: () -> Unit,
    onSendDirectMessage: (recipientUsername: String, text: String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTab by remember(initialTab) { mutableIntStateOf(initialTab) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .height(620.dp)
                .testTag("messages_center_dialog"),
            shape = RoundedCornerShape(24.dp),
            color = PitchDark,
            border = BorderStroke(1.dp, Color(0xFF1E293B)),
            tonalElevation = 12.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Header Bar with Close & Segmented 2-Tab Switch
                MessagesHeaderBar(
                    selectedTab = selectedTab,
                    onSelectTab = {
                        selectedTab = it
                        if (it == 0 && activeRecipient != null) {
                            onCloseDirectChat()
                        }
                    },
                    matchChatCount = matchMessages.size,
                    onDismiss = onDismiss
                )

                HorizontalDivider(color = Color(0xFF1E293B), thickness = 1.dp)

                // Tab Content Switcher
                Box(modifier = Modifier.weight(1f)) {
                    if (selectedTab == 0) {
                        MatchLiveRoomChatPane(
                            matchTitle = matchTitle,
                            messages = matchMessages,
                            onSendMessage = onSendMatchMessage,
                            onSendReaction = onSendMatchReaction
                        )
                    } else {
                        PersonalDmsPane(
                            currentUser = currentUser,
                            communityPlayers = communityPlayers,
                            personalMessages = personalMessages,
                            activeRecipient = activeRecipient,
                            onSelectRecipient = onSelectRecipient,
                            onCloseChat = onCloseDirectChat,
                            onSendMessage = onSendDirectMessage
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MessagesHeaderBar(
    selectedTab: Int,
    onSelectTab: (Int) -> Unit,
    matchChatCount: Int,
    onDismiss: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0F172A))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Segmented Tab Switcher (Match Live vs Personal DMs)
        Row(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF1E293B))
                .border(0.5.dp, Color(0xFF334155), RoundedCornerShape(12.dp))
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Tab 0: Match Live Room
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(9.dp))
                    .background(
                        if (selectedTab == 0) {
                            Brush.linearGradient(listOf(Color(0xFFE1306C), Color(0xFFFD1D1D)))
                        } else androidx.compose.ui.graphics.SolidColor(Color.Transparent)
                    )
                    .clickable { onSelectTab(0) }
                    .padding(vertical = 7.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "🔥", fontSize = 11.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Match Live",
                        fontSize = 11.sp,
                        fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium,
                        color = if (selectedTab == 0) Color.White else TextSecondary
                    )
                    if (matchChatCount > 0 && selectedTab != 0) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE1306C))
                        )
                    }
                }
            }

            // Tab 1: Personal DMs
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(9.dp))
                    .background(
                        if (selectedTab == 1) {
                            Brush.linearGradient(listOf(CricketGreen, Color(0xFF007E33)))
                        } else androidx.compose.ui.graphics.SolidColor(Color.Transparent)
                    )
                    .clickable { onSelectTab(1) }
                    .padding(vertical = 7.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Chat,
                        contentDescription = null,
                        tint = if (selectedTab == 1) PitchDark else TextSecondary,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Personal DMs",
                        fontSize = 11.sp,
                        fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium,
                        color = if (selectedTab == 1) PitchDark else TextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Close Button
        IconButton(
            onClick = onDismiss,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close Messages",
                tint = TextSecondary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun MatchLiveRoomChatPane(
    matchTitle: String,
    messages: List<ChatMessage>,
    onSendMessage: (String) -> Unit,
    onSendReaction: (String) -> Unit
) {
    var textInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val quickReactions = listOf("🔥", "🏏", "👏", "😱", "❤️", "🚀", "🏆")

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Room Subheader Banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF131B2A))
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF22C55E))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = matchTitle.ifBlank { "Live Match Ground Chat" },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = "Cloud Synced",
                    fontSize = 9.sp,
                    color = CricketGreen
                )
            }
        }

        // Messages List
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (messages.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🏏", fontSize = 32.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Live Ground Fan Chat",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Be the first to cheer for your team!",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }

            items(messages, key = { it.id }) { msg ->
                LiveChatMessageBubble(msg = msg)
            }
        }

        // Quick Reactions Strip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0F172A))
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            quickReactions.forEach { emoji ->
                Text(
                    text = emoji,
                    fontSize = 17.sp,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable { onSendReaction(emoji) }
                        .padding(4.dp)
                )
            }
        }

        // Input Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0F172A))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = textInput,
                onValueChange = { textInput = it },
                placeholder = { Text("Comment on the live match...", fontSize = 12.sp, color = TextMuted) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("match_chat_input"),
                shape = RoundedCornerShape(20.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFE1306C),
                    unfocusedBorderColor = Color(0xFF334155),
                    focusedContainerColor = PitchDark,
                    unfocusedContainerColor = PitchDark,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = {
                    if (textInput.isNotBlank()) {
                        onSendMessage(textInput)
                        textInput = ""
                    }
                })
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = {
                    if (textInput.isNotBlank()) {
                        onSendMessage(textInput)
                        textInput = ""
                    }
                },
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(
                        if (textInput.isNotBlank()) {
                            Brush.linearGradient(listOf(Color(0xFFE1306C), Color(0xFFFD1D1D)))
                        } else Brush.linearGradient(listOf(Color(0xFF334155), Color(0xFF1E293B)))
                    )
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun LiveChatMessageBubble(msg: ChatMessage) {
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val timeStr = timeFormat.format(Date(msg.timestamp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (msg.isFromMe) Arrangement.End else Arrangement.Start
    ) {
        if (!msg.isFromMe) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1E293B)),
                contentAlignment = Alignment.Center
            ) {
                Text(msg.avatarEmoji.ifBlank { "🏏" }, fontSize = 14.sp)
            }
            Spacer(modifier = Modifier.width(6.dp))
        }

        Column(
            modifier = Modifier.widthIn(max = 280.dp),
            horizontalAlignment = if (msg.isFromMe) Alignment.End else Alignment.Start
        ) {
            if (!msg.isFromMe) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = msg.senderName,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = StadiumGold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "• ${msg.senderRole}",
                        fontSize = 9.sp,
                        color = TextMuted
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
            }

            Box(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = 14.dp,
                            topEnd = 14.dp,
                            bottomStart = if (msg.isFromMe) 14.dp else 2.dp,
                            bottomEnd = if (msg.isFromMe) 2.dp else 14.dp
                        )
                    )
                    .background(
                        if (msg.isFromMe) {
                            Brush.linearGradient(listOf(Color(0xFF007E33), CricketGreen))
                        } else {
                            Brush.linearGradient(listOf(Color(0xFF1E293B), Color(0xFF0F172A)))
                        }
                    )
                    .border(
                        0.5.dp,
                        if (msg.isFromMe) CricketGreen else Color(0xFF334155),
                        RoundedCornerShape(14.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 7.dp)
            ) {
                Column {
                    Text(
                        text = msg.message,
                        fontSize = 12.sp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = timeStr,
                        fontSize = 8.sp,
                        color = if (msg.isFromMe) Color(0xFFD1FAE5) else TextMuted,
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            }
        }

        if (msg.isFromMe) {
            Spacer(modifier = Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(CricketGreen.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(msg.avatarEmoji.ifBlank { "🏏" }, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun PersonalDmsPane(
    currentUser: CricHeroesProfile,
    communityPlayers: List<CricHeroesProfile>,
    personalMessages: List<ChatMessage>,
    activeRecipient: CricHeroesProfile?,
    onSelectRecipient: (CricHeroesProfile) -> Unit,
    onCloseChat: () -> Unit,
    onSendMessage: (recipientUsername: String, text: String) -> Unit
) {
    if (activeRecipient == null) {
        // Inbox Directory
        DmRosterInbox(
            currentUser = currentUser,
            players = communityPlayers,
            onSelect = onSelectRecipient
        )
    } else {
        // 1-on-1 Direct Chat Thread
        DmChatConversation(
            currentUser = currentUser,
            recipient = activeRecipient,
            messages = personalMessages,
            onBack = onCloseChat,
            onSendMessage = onSendMessage
        )
    }
}

@Composable
private fun DmRosterInbox(
    currentUser: CricHeroesProfile,
    players: List<CricHeroesProfile>,
    onSelect: (CricHeroesProfile) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(players, query) {
        val q = query.trim().lowercase()
        players.filter {
            it.username != currentUser.username &&
            (q.isEmpty() || it.fullName.lowercase().contains(q) || it.username.lowercase().contains(q) || it.teamName.lowercase().contains(q))
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        // Search Input
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Search cricketers, scorers, umpires...", fontSize = 12.sp, color = TextMuted) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted, modifier = Modifier.size(16.dp)) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CricketGreen,
                unfocusedBorderColor = Color(0xFF334155),
                focusedContainerColor = PitchDark,
                unfocusedContainerColor = PitchDark,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "MATCH PLAYERS & NETWORK (${filtered.size})",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = TextMuted,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(6.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(filtered, key = { it.id.ifBlank { it.username } }) { player ->
                Surface(
                    color = Color(0xFF0F172A),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(0.5.dp, Color(0xFF1E293B)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(player) }
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1E293B)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(player.avatarEmoji.ifBlank { "🏏" }, fontSize = 18.sp)
                            if (player.isOnline) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF22C55E))
                                        .align(Alignment.BottomEnd)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = player.fullName.ifBlank { player.jerseyName },
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "@${player.username}",
                                    fontSize = 10.sp,
                                    color = HawkEyeCyan
                                )
                            }
                            Text(
                                text = "${player.primaryRole} • ${player.teamName.ifBlank { "Local Team" }}",
                                fontSize = 10.sp,
                                color = TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.Chat,
                            contentDescription = "Message",
                            tint = CricketGreen,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DmChatConversation(
    currentUser: CricHeroesProfile,
    recipient: CricHeroesProfile,
    messages: List<ChatMessage>,
    onBack: () -> Unit,
    onSendMessage: (recipientUsername: String, text: String) -> Unit
) {
    var textInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Conversation Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF131B2A))
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back to Inbox",
                    tint = TextPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }

            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1E293B)),
                contentAlignment = Alignment.Center
            ) {
                Text(recipient.avatarEmoji.ifBlank { "🏏" }, fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column {
                Text(
                    text = recipient.fullName.ifBlank { recipient.jerseyName },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "@${recipient.username} • Direct Message",
                    fontSize = 9.sp,
                    color = CricketGreen
                )
            }
        }

        // Messages List
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (messages.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 50.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("💬", fontSize = 32.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Start Direct Conversation",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Real-time, private, cloud-persisted chat.",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }

            items(messages, key = { it.id }) { msg ->
                LiveChatMessageBubble(msg = msg)
            }
        }

        // Input Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0F172A))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = textInput,
                onValueChange = { textInput = it },
                placeholder = { Text("Message @${recipient.username}...", fontSize = 12.sp, color = TextMuted) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("dm_chat_input"),
                shape = RoundedCornerShape(20.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CricketGreen,
                    unfocusedBorderColor = Color(0xFF334155),
                    focusedContainerColor = PitchDark,
                    unfocusedContainerColor = PitchDark,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = {
                    if (textInput.isNotBlank()) {
                        onSendMessage(recipient.username, textInput)
                        textInput = ""
                    }
                })
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = {
                    if (textInput.isNotBlank()) {
                        onSendMessage(recipient.username, textInput)
                        textInput = ""
                    }
                },
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(
                        if (textInput.isNotBlank()) {
                            Brush.linearGradient(listOf(CricketGreen, Color(0xFF007E33)))
                        } else Brush.linearGradient(listOf(Color(0xFF334155), Color(0xFF1E293B)))
                    )
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send DM",
                    tint = if (textInput.isNotBlank()) PitchDark else Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
