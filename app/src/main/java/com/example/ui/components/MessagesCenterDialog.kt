package com.example.ui.components

import android.Manifest
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
import com.example.util.MediaAttachmentHelper
import java.text.SimpleDateFormat
import java.util.*

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
    onDeleteMatchMessage: ((String) -> Unit)? = null,
    onDeleteDirectMessage: ((String) -> Unit)? = null,
    onRefreshUsers: () -> Unit = {},
    isDmMuted: Boolean = false,
    onToggleDmMute: (() -> Unit)? = null,
    onSendDirectMessageWithMedia: ((recipientUsername: String, text: String, mediaUrl: String, mediaType: String, isSnap: Boolean) -> Unit)? = null,
    onMarkSnapOpened: ((ChatMessage) -> Unit)? = null,
    unreadDmCounts: Map<String, Int> = emptyMap(),
    lastDmMessageBySender: Map<String, String> = emptyMap(),
    lastDmTimestampBySender: Map<String, Long> = emptyMap(),
    onMarkAllDmsRead: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    var selectedTab by remember(initialTab) { mutableIntStateOf(initialTab) }
    var viewingPlayerBiodata by remember { mutableStateOf<CricHeroesProfile?>(null) }
    var viewingSnapMessage by remember { mutableStateOf<ChatMessage?>(null) }
    val isOwner = currentUser.username.trim().removePrefix("@").equals("ayush_7", ignoreCase = true) ||
                  currentUser.fullName.lowercase().contains("ayush")
    val totalUnreadDmCount = remember(unreadDmCounts) { unreadDmCounts.values.sum() }

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
                MessagesHeaderBar(
                    selectedTab = selectedTab,
                    onSelectTab = {
                        selectedTab = it
                        if (it == 0 && activeRecipient != null) {
                            onCloseDirectChat()
                        }
                    },
                    matchChatCount = matchMessages.size,
                    unreadDmCount = totalUnreadDmCount,
                    onDismiss = onDismiss
                )

                HorizontalDivider(color = Color(0xFF1E293B), thickness = 1.dp)

                Box(modifier = Modifier.weight(1f)) {
                    if (selectedTab == 0) {
                        MatchLiveRoomChatPane(
                            matchTitle = matchTitle,
                            messages = matchMessages,
                            isOwner = isOwner,
                            onSendMessage = onSendMatchMessage,
                            onSendReaction = onSendMatchReaction,
                            onDeleteMessage = onDeleteMatchMessage
                        )
                    } else {
                        PersonalDmsPane(
                            currentUser = currentUser,
                            communityPlayers = communityPlayers,
                            personalMessages = personalMessages,
                            activeRecipient = activeRecipient,
                            isOwner = isOwner,
                            isDmMuted = isDmMuted,
                            onToggleDmMute = onToggleDmMute,
                            onSelectRecipient = onSelectRecipient,
                            onCloseChat = onCloseDirectChat,
                            onSendMessage = onSendDirectMessage,
                            onSendWithMedia = onSendDirectMessageWithMedia,
                            onDeleteMessage = onDeleteDirectMessage,
                            onRefreshUsers = onRefreshUsers,
                            onOpenBiodata = { viewingPlayerBiodata = it },
                            onOpenSnap = { viewingSnapMessage = it },
                            unreadDmCounts = unreadDmCounts,
                            lastDmMessageBySender = lastDmMessageBySender,
                            lastDmTimestampBySender = lastDmTimestampBySender,
                            onMarkAllDmsRead = onMarkAllDmsRead
                        )
                    }
                }
            }

            // Player Biodata Full Identity Card Dialog
            if (viewingPlayerBiodata != null) {
                PlayerProfileCardDialog(
                    profile = viewingPlayerBiodata!!,
                    onDismiss = { viewingPlayerBiodata = null },
                    onOpenDmWithPlayer = {
                        viewingPlayerBiodata = null
                        onSelectRecipient(it)
                        selectedTab = 1
                    }
                )
            }

            // Snapchat-Style Disappearing Snap Fullscreen Viewer with Countdown
            if (viewingSnapMessage != null) {
                ViewingSnapDialog(
                    msg = viewingSnapMessage!!,
                    onDismiss = {
                        val snapToMark = viewingSnapMessage
                        viewingSnapMessage = null
                        if (snapToMark != null && !snapToMark.isFromMe) {
                            onMarkSnapOpened?.invoke(snapToMark)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun MessagesHeaderBar(
    selectedTab: Int,
    onSelectTab: (Int) -> Unit,
    matchChatCount: Int,
    unreadDmCount: Int = 0,
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
        Row(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF1E293B))
                .border(0.5.dp, Color(0xFF334155), RoundedCornerShape(12.dp))
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(9.dp))
                    .background(
                        if (selectedTab == 0) Brush.linearGradient(listOf(Color(0xFFE1306C), Color(0xFFFD1D1D)))
                        else androidx.compose.ui.graphics.SolidColor(Color.Transparent)
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
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(9.dp))
                    .background(
                        if (selectedTab == 1) Brush.linearGradient(listOf(CricketGreen, Color(0xFF007E33)))
                        else androidx.compose.ui.graphics.SolidColor(Color.Transparent)
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
                    if (unreadDmCount > 0) {
                        Spacer(modifier = Modifier.width(5.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(DrsOutRed)
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "$unreadDmCount",
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

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
    isOwner: Boolean = false,
    onSendMessage: (String) -> Unit,
    onSendReaction: (String) -> Unit,
    onDeleteMessage: ((String) -> Unit)? = null
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

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Surface(
                    color = Color(0xFF1E293B).copy(alpha = 0.6f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
                ) {
                    Text(
                        text = "⏱️ Messages 1 mahine tak safe rehte hain, uske baad auto-delete hote hain taaki storage light rahe.",
                        color = TextMuted,
                        fontSize = 10.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            items(messages, key = { it.id }) { msg ->
                LiveChatMessageBubble(
                    msg = msg,
                    isOwner = isOwner,
                    onDelete = { onDeleteMessage?.invoke(msg.id) }
                )
            }
        }

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
                modifier = Modifier.weight(1f),
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
                        onSendMessage(textInput.trim())
                        textInput = ""
                    }
                })
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = {
                    if (textInput.isNotBlank()) {
                        onSendMessage(textInput.trim())
                        textInput = ""
                    }
                },
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(
                        if (textInput.isNotBlank()) Brush.linearGradient(listOf(Color(0xFFE1306C), Color(0xFFFD1D1D)))
                        else Brush.linearGradient(listOf(Color(0xFF334155), Color(0xFF1E293B)))
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
private fun LiveChatMessageBubble(
    msg: ChatMessage,
    isOwner: Boolean = false,
    onDelete: (() -> Unit)? = null
) {
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val timeStr = timeFormat.format(Date(msg.timestamp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (msg.isFromMe) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
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

        if (msg.isFromMe || isOwner) {
            IconButton(
                onClick = { onDelete?.invoke() },
                modifier = Modifier.size(24.dp).padding(bottom = 2.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Delete",
                    tint = Color.Red.copy(alpha = 0.6f),
                    modifier = Modifier.size(13.dp)
                )
            }
            Spacer(modifier = Modifier.width(3.dp))
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
                        if (msg.isFromMe) Brush.linearGradient(listOf(Color(0xFF007E33), CricketGreen))
                        else Brush.linearGradient(listOf(Color(0xFF1E293B), Color(0xFF0F172A)))
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
    isOwner: Boolean = false,
    isDmMuted: Boolean = false,
    onToggleDmMute: (() -> Unit)? = null,
    onSelectRecipient: (CricHeroesProfile) -> Unit,
    onCloseChat: () -> Unit,
    onSendMessage: (recipientUsername: String, text: String) -> Unit,
    onSendWithMedia: ((recipientUsername: String, text: String, mediaUrl: String, mediaType: String, isSnap: Boolean) -> Unit)? = null,
    onDeleteMessage: ((String) -> Unit)? = null,
    onRefreshUsers: () -> Unit = {},
    onOpenBiodata: (CricHeroesProfile) -> Unit = {},
    onOpenSnap: (ChatMessage) -> Unit = {},
    unreadDmCounts: Map<String, Int> = emptyMap(),
    lastDmMessageBySender: Map<String, String> = emptyMap(),
    lastDmTimestampBySender: Map<String, Long> = emptyMap(),
    onMarkAllDmsRead: (() -> Unit)? = null
) {
    if (activeRecipient == null) {
        DmRosterInbox(
            currentUser = currentUser,
            players = communityPlayers,
            onSelect = onSelectRecipient,
            onRefresh = onRefreshUsers,
            unreadDmCounts = unreadDmCounts,
            lastDmMessageBySender = lastDmMessageBySender,
            lastDmTimestampBySender = lastDmTimestampBySender,
            onMarkAllDmsRead = onMarkAllDmsRead
        )
    } else {
        DmChatConversation(
            currentUser = currentUser,
            recipient = activeRecipient,
            messages = personalMessages,
            isOwner = isOwner,
            isDmMuted = isDmMuted,
            onToggleDmMute = onToggleDmMute,
            onBack = onCloseChat,
            onSendMessage = onSendMessage,
            onSendWithMedia = onSendWithMedia,
            onDeleteMessage = onDeleteMessage,
            onOpenBiodata = { onOpenBiodata(activeRecipient) },
            onOpenSnap = onOpenSnap
        )
    }
}

@Composable
private fun DmChatConversation(
    currentUser: CricHeroesProfile,
    recipient: CricHeroesProfile,
    messages: List<ChatMessage>,
    isOwner: Boolean = false,
    isDmMuted: Boolean = false,
    onToggleDmMute: (() -> Unit)? = null,
    onBack: () -> Unit,
    onSendMessage: (recipientUsername: String, text: String) -> Unit,
    onSendWithMedia: ((recipientUsername: String, text: String, mediaUrl: String, mediaType: String, isSnap: Boolean) -> Unit)? = null,
    onDeleteMessage: ((String) -> Unit)? = null,
    onOpenBiodata: () -> Unit = {},
    onOpenSnap: (ChatMessage) -> Unit = {}
) {
    var textInput by remember { mutableStateOf("") }
    val context = LocalContext.current

    // Real media capture and attachment states
    var pendingBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var pendingIsVideo by remember { mutableStateOf(false) }
    var showMediaPreviewDialog by remember { mutableStateOf(false) }
    var isSnapMode by remember { mutableStateOf(true) } // true for disappearing snap, false for chat photo/video
    var mediaCaptionInput by remember { mutableStateOf("") }
    var viewingFullScreenMedia by remember { mutableStateOf<ChatMessage?>(null) }

    // Real Camera photo click launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            pendingBitmap = bitmap
            pendingIsVideo = false
            showMediaPreviewDialog = true
        }
    }

    // Camera permission request launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            cameraLauncher.launch(null)
        } else {
            Toast.makeText(context, "Camera permission granted nahi hui! Please allow camera in settings.", Toast.LENGTH_SHORT).show()
        }
    }

    // Real Gallery picker launcher (Photo & Video Picker)
    val galleryPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val mimeType = context.contentResolver.getType(uri) ?: ""
            val isVid = mimeType.startsWith("video")
            pendingIsVideo = isVid
            val dataUrl = MediaAttachmentHelper.uriToDataUrl(context, uri, isVideo = isVid)
            val bmp = dataUrl?.let { MediaAttachmentHelper.decodeDataUrlToBitmap(it) }
            if (bmp != null) {
                pendingBitmap = bmp
                showMediaPreviewDialog = true
            } else {
                Toast.makeText(context, "Selected photo/video load nahi ho saki", Toast.LENGTH_SHORT).show()
            }
        }
    }

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
                    contentDescription = "Back",
                    tint = TextPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Clickable Recipient Identity Card Trigger (Opens full Biodata)
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onOpenBiodata() }
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E293B)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(recipient.avatarEmoji.ifBlank { "🏏" }, fontSize = 16.sp)
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = recipient.fullName.ifBlank { recipient.jerseyName },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "ℹ️ Biodata",
                            fontSize = 9.sp,
                            color = HawkEyeCyan,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Text(
                        text = "@${recipient.username.removePrefix("@")} • Tap for details",
                        fontSize = 9.sp,
                        color = CricketGreen
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // High Priority DND Bypass System Settings Button
                IconButton(
                    onClick = {
                        com.example.notification.MatchNotificationHelper.openChannelNotificationSettings(context)
                        Toast.makeText(
                            context,
                            "DND Override: Settings me 'Bypass Do Not Disturb' toggle on rakhein taaki DND me bhi alert aaye!",
                            Toast.LENGTH_LONG
                        ).show()
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PriorityHigh,
                        contentDescription = "Bypass Do Not Disturb Settings",
                        tint = StadiumGold,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Mute / Unmute DM Notification toggle button
                IconButton(
                    onClick = { onToggleDmMute?.invoke() },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (isDmMuted) Icons.Default.NotificationsOff else Icons.Default.NotificationsActive,
                        contentDescription = if (isDmMuted) "DM Muted" else "DM Active",
                        tint = if (isDmMuted) Color(0xFFEF4444) else CricketGreen,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Messages list
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Surface(
                    color = Color(0xFF0F172A),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(0.8.dp, Color(0xFF334155)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp)
                        .clickable {
                            com.example.notification.MatchNotificationHelper.openChannelNotificationSettings(context)
                            Toast.makeText(
                                context,
                                "DND Override: Channel settings khul gayi hain.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🚨", fontSize = 12.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "High Priority Alert • Phone ke Do Not Disturb (DND) ko bhi bypass karke pop-up aur sound aayega. Tap for settings.",
                            color = StadiumGold,
                            fontSize = 10.sp,
                            lineHeight = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            item {
                Surface(
                    color = Color(0xFF1E293B).copy(alpha = 0.6f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
                ) {
                    Text(
                        text = "⚡ Real-time Quick Snaps & Camera Photos enabled. Snaps vanish after opening!",
                        color = TextMuted,
                        fontSize = 10.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            items(messages, key = { it.id }) { msg ->
                if (msg.isSnap) {
                    SnapMessageBubble(
                        msg = msg,
                        isOwner = isOwner,
                        onOpenSnap = { onOpenSnap(msg) },
                        onDelete = { onDeleteMessage?.invoke(msg.id) }
                    )
                } else if (msg.mediaUrl.isNotBlank()) {
                    MediaMessageBubble(
                        msg = msg,
                        isOwner = isOwner,
                        onOpenMedia = { viewingFullScreenMedia = msg },
                        onDelete = { onDeleteMessage?.invoke(msg.id) }
                    )
                } else {
                    LiveChatMessageBubble(
                        msg = msg,
                        isOwner = isOwner,
                        onDelete = { onDeleteMessage?.invoke(msg.id) }
                    )
                }
            }
        }

        // Quick input row with Camera click, Gallery pick, and Snap options
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0F172A))
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Real Camera Button: 1-tap opens device camera to click a photo (Snap/Photo)
            IconButton(
                onClick = {
                    isSnapMode = true
                    if (MediaAttachmentHelper.hasCameraPermission(context)) {
                        cameraLauncher.launch(null)
                    } else {
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                },
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFFD600).copy(alpha = 0.2f))
            ) {
                Icon(
                    imageVector = Icons.Default.PhotoCamera,
                    contentDescription = "Camera Photo / Snap",
                    tint = Color(0xFFFFD600),
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Real Gallery Button: Opens device gallery to pick photo or video
            IconButton(
                onClick = {
                    isSnapMode = false
                    galleryPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                    )
                },
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(HawkEyeCyan.copy(alpha = 0.15f))
            ) {
                Icon(
                    imageVector = Icons.Default.PhotoLibrary,
                    contentDescription = "Gallery Photo or Video",
                    tint = HawkEyeCyan,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            OutlinedTextField(
                value = textInput,
                onValueChange = { textInput = it },
                placeholder = { Text("Message @${recipient.username.removePrefix("@")}...", fontSize = 12.sp, color = TextMuted) },
                modifier = Modifier.weight(1f),
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
                        onSendMessage(recipient.username.removePrefix("@").trim(), textInput.trim())
                        textInput = ""
                    }
                })
            )

            Spacer(modifier = Modifier.width(6.dp))

            IconButton(
                onClick = {
                    if (textInput.isNotBlank()) {
                        onSendMessage(recipient.username.removePrefix("@").trim(), textInput.trim())
                        textInput = ""
                    }
                },
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (textInput.isNotBlank()) Brush.linearGradient(listOf(CricketGreen, Color(0xFF007E33)))
                        else Brush.linearGradient(listOf(Color(0xFF334155), Color(0xFF1E293B)))
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

    // Real Media & Snap Send Live Preview Dialog
    if (showMediaPreviewDialog && pendingBitmap != null) {
        Dialog(
            onDismissRequest = {
                showMediaPreviewDialog = false
                pendingBitmap = null
                mediaCaptionInput = ""
            },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.94f)
                    .wrapContentHeight()
                    .clip(RoundedCornerShape(22.dp)),
                color = SurfaceDark,
                border = BorderStroke(1.dp, if (isSnapMode) Color(0xFFFFD600).copy(alpha = 0.5f) else Color(0xFF334155))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (isSnapMode) "🔥 Disappearing Snap" else if (pendingIsVideo) "🎬 Video Clip" else "📷 Photo Preview",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = if (isSnapMode) Color(0xFFFFD600) else Color.White
                            )
                        }
                        IconButton(onClick = {
                            showMediaPreviewDialog = false
                            pendingBitmap = null
                            mediaCaptionInput = ""
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Mode selector tabs: Snap vs Permanent Photo/Video
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSnapMode) Color(0xFFFFD600).copy(alpha = 0.2f) else PitchDark,
                            border = BorderStroke(1.dp, if (isSnapMode) Color(0xFFFFD600) else Color(0xFF334155)),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { isSnapMode = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("🔥", fontSize = 14.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Snap (Disappears)",
                                    color = if (isSnapMode) Color(0xFFFFD600) else TextSecondary,
                                    fontWeight = if (isSnapMode) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (!isSnapMode) CricketGreen.copy(alpha = 0.2f) else PitchDark,
                            border = BorderStroke(1.dp, if (!isSnapMode) CricketGreen else Color(0xFF334155)),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { isSnapMode = false }
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(if (pendingIsVideo) "🎬" else "📷", fontSize = 14.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (pendingIsVideo) "Video Clip" else "Normal Photo",
                                    color = if (!isSnapMode) CricketGreen else TextSecondary,
                                    fontWeight = if (!isSnapMode) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // REAL CAPTURED PHOTO / MEDIA PREVIEW CARD
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = pendingBitmap!!.asImageBitmap(),
                            contentDescription = "Captured Photo",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )

                        if (isSnapMode) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color.Black.copy(alpha = 0.7f),
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("🔥 Disappears in 8s", fontSize = 10.sp, color = Color(0xFFFFD600), fontWeight = FontWeight.Bold)
                                }
                            }
                        } else if (pendingIsVideo) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.6f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = Color.White, modifier = Modifier.size(32.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Caption Input
                    OutlinedTextField(
                        value = mediaCaptionInput,
                        onValueChange = { mediaCaptionInput = it },
                        placeholder = { Text(if (isSnapMode) "Snap text / caption..." else "Caption (optional)...", fontSize = 12.sp, color = TextMuted) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (isSnapMode) Color(0xFFFFD600) else CricketGreen,
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedContainerColor = PitchDark,
                            unfocusedContainerColor = PitchDark,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Action buttons: Retake & Send
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                if (MediaAttachmentHelper.hasCameraPermission(context)) {
                                    cameraLauncher.launch(null)
                                } else {
                                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                            border = BorderStroke(1.dp, Color(0xFF475569))
                        ) {
                            Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Retake", fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                val currentBmp = pendingBitmap ?: return@Button
                                val dataUrl = MediaAttachmentHelper.bitmapToDataUrl(currentBmp)
                                val finalCaption = mediaCaptionInput.ifBlank {
                                    if (isSnapMode) "🔥 [Disappearing Snap]" else if (pendingIsVideo) "🎬 Video" else "📷 Photo"
                                }
                                val mediaType = if (isSnapMode) "SNAP" else if (pendingIsVideo) "VIDEO" else "PHOTO"

                                if (onSendWithMedia != null) {
                                    onSendWithMedia(
                                        recipient.username.removePrefix("@").trim(),
                                        finalCaption,
                                        dataUrl,
                                        mediaType,
                                        isSnapMode
                                    )
                                } else {
                                    onSendMessage(recipient.username.removePrefix("@").trim(), finalCaption)
                                }

                                showMediaPreviewDialog = false
                                pendingBitmap = null
                                mediaCaptionInput = ""
                            },
                            modifier = Modifier.weight(1.5f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSnapMode) Color(0xFFFFD600) else CricketGreen
                            )
                        ) {
                            Text(
                                text = if (isSnapMode) "🔥 Send Snap" else "Send Photo",
                                color = PitchDark,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }

    // Full screen viewer for regular media photos in chat
    if (viewingFullScreenMedia != null) {
        FullScreenMediaViewerDialog(
            msg = viewingFullScreenMedia!!,
            onDismiss = { viewingFullScreenMedia = null }
        )
    }
}

@Composable
private fun SnapMessageBubble(
    msg: ChatMessage,
    isOwner: Boolean,
    onOpenSnap: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (msg.isFromMe) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!msg.isFromMe) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1E293B)),
                contentAlignment = Alignment.Center
            ) {
                Text(msg.avatarEmoji.ifBlank { "🔥" }, fontSize = 14.sp)
            }
            Spacer(modifier = Modifier.width(6.dp))
        }

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = if (msg.isSnapOpened) Color(0xFF1E293B).copy(alpha = 0.7f) else Color(0xFF2E1A05),
            border = BorderStroke(
                1.dp,
                if (msg.isSnapOpened) Color(0xFF475569) else Color(0xFFFFD600)
            ),
            modifier = Modifier
                .widthIn(max = 260.dp)
                .clickable { onOpenSnap() }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LocalFireDepartment,
                    contentDescription = "Snap",
                    tint = if (msg.isSnapOpened) Color(0xFF94A3B8) else Color(0xFFFFD600),
                    modifier = Modifier.size(22.dp)
                )

                Column {
                    Text(
                        text = if (msg.isSnapOpened) "Snap Opened" else "🔥 New Snap",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (msg.isSnapOpened) TextMuted else Color(0xFFFFD600)
                    )
                    Text(
                        text = if (msg.isSnapOpened) "Disappeared" else "Tap to view (Disappears)",
                        fontSize = 10.sp,
                        color = if (msg.isSnapOpened) TextMuted else Color.White
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
                    .background(Color(0xFFFFD600).copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(msg.avatarEmoji.ifBlank { "🔥" }, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun MediaMessageBubble(
    msg: ChatMessage,
    isOwner: Boolean,
    onOpenMedia: () -> Unit,
    onDelete: () -> Unit
) {
    val bitmap = remember(msg.mediaUrl) {
        MediaAttachmentHelper.decodeDataUrlToBitmap(msg.mediaUrl)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (msg.isFromMe) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = if (msg.isFromMe) Color(0xFF007E33).copy(alpha = 0.25f) else Color(0xFF1E293B),
            border = BorderStroke(1.dp, if (msg.isFromMe) CricketGreen else Color(0xFF334155)),
            modifier = Modifier.widthIn(max = 260.dp)
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                // Real Image / Video thumbnail preview
                if (bitmap != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.Black)
                            .clickable { onOpenMedia() },
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Photo",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        if (msg.mediaType == "VIDEO") {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.6f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = Color.White, modifier = Modifier.size(24.dp))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = if (msg.mediaType == "VIDEO") Icons.Default.Videocam else Icons.Default.Image,
                        contentDescription = msg.mediaType,
                        tint = if (msg.mediaType == "VIDEO") HawkEyeCyan else CricketGreen,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = if (msg.mediaType == "VIDEO") "Video" else "Photo",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                if (msg.message.isNotBlank() && msg.message != "Sent attachment" && msg.message != "📷 Photo") {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = msg.message, fontSize = 12.sp, color = TextPrimary)
                }
            }
        }
    }
}

@Composable
fun ViewingSnapDialog(
    msg: ChatMessage,
    onDismiss: () -> Unit
) {
    var timeLeft by remember { mutableIntStateOf(8) }
    val progress by remember { derivedStateOf { timeLeft / 8f } }

    val snapBitmap = remember(msg.mediaUrl) {
        MediaAttachmentHelper.decodeDataUrlToBitmap(msg.mediaUrl)
    }

    LaunchedEffect(Unit) {
        while (timeLeft > 0) {
            kotlinx.coroutines.delay(1000)
            timeLeft -= 1
        }
        onDismiss()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable { onDismiss() }
        ) {
            // Full Screen Real Photo
            if (snapBitmap != null) {
                Image(
                    bitmap = snapBitmap.asImageBitmap(),
                    contentDescription = "Snap Photo",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            } else {
                // Elegant Snapchat Yellow gradient if text snap
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFF2E1A05), Color.Black, Color(0xFF1E293B))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Text("🔥", fontSize = 72.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = msg.message.ifBlank { "🔥 Disappearing Snap" },
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }

            // Top Bar with Countdown and Progress
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Black.copy(alpha = 0.85f), Color.Transparent)
                        )
                    )
                    .padding(16.dp)
            ) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = Color(0xFFFFD600),
                    trackColor = Color.White.copy(alpha = 0.25f),
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFFD600).copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(msg.avatarEmoji.ifBlank { "🔥" }, fontSize = 20.sp)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = msg.senderName.ifBlank { "@${msg.senderUsername}" },
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Disappearing Snap • ${timeLeft}s",
                                color = Color(0xFFFFD600),
                                fontSize = 11.sp
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }
            }

            // Bottom Caption Overlay
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f))
                        )
                    )
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (snapBitmap != null && msg.message.isNotBlank() && msg.message != "🔥 [Disappearing Snap]") {
                    Surface(
                        color = Color.Black.copy(alpha = 0.75f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFFFD600).copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                    ) {
                        Text(
                            text = msg.message,
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }

                Text(
                    text = "Tap anywhere to close • Disappears in ${timeLeft}s",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
fun FullScreenMediaViewerDialog(
    msg: ChatMessage,
    onDismiss: () -> Unit
) {
    val bitmap = remember(msg.mediaUrl) {
        MediaAttachmentHelper.decodeDataUrlToBitmap(msg.mediaUrl)
    }

    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // Interactive Zoomable and Pannable Image Container
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(1f, 5f)
                            if (scale > 1f) {
                                val maxOffsetX = (size.width * (scale - 1f)) / 2f
                                val maxOffsetY = (size.height * (scale - 1f)) / 2f
                                offset = Offset(
                                    x = (offset.x + pan.x * scale).coerceIn(-maxOffsetX, maxOffsetX),
                                    y = (offset.y + pan.y * scale).coerceIn(-maxOffsetY, maxOffsetY)
                                )
                            } else {
                                offset = Offset.Zero
                            }
                        }
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = { tapOffset ->
                                if (scale > 1.2f) {
                                    scale = 1f
                                    offset = Offset.Zero
                                } else {
                                    scale = 2.5f
                                    val centerX = size.width / 2f
                                    val centerY = size.height / 2f
                                    offset = Offset(
                                        x = (centerX - tapOffset.x) * 1.5f,
                                        y = (centerY - tapOffset.y) * 1.5f
                                    )
                                }
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Full Screen Photo",
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                translationX = offset.x
                                translationY = offset.y
                            },
                        contentScale = ContentScale.Fit,
                        filterQuality = androidx.compose.ui.graphics.FilterQuality.High
                    )
                } else {
                    Text(
                        text = "Unable to load image",
                        color = TextMuted,
                        fontSize = 14.sp
                    )
                }
            }

            // Top Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.85f), Color.Transparent)))
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = msg.senderName.ifBlank { "@${msg.senderUsername}" },
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = if (msg.mediaType == "VIDEO") "Video Clip" else "HD Photo • Double-tap or pinch to zoom",
                        color = HawkEyeCyan,
                        fontSize = 11.sp
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Reset zoom button if zoomed in
                    if (scale > 1f) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF1E293B).copy(alpha = 0.8f),
                            modifier = Modifier.clickable {
                                scale = 1f
                                offset = Offset.Zero
                            }
                        ) {
                            Text(
                                text = "1x Reset",
                                color = StadiumGold,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.6f))
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
            }

            // Bottom Caption and Zoom Controller Bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))))
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Caption if present
                if (msg.message.isNotBlank() && msg.message != "Sent attachment" && msg.message != "📷 Photo") {
                    Surface(
                        color = Color.Black.copy(alpha = 0.75f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(0.5.dp, Color(0xFF334155)),
                        modifier = Modifier.padding(bottom = 10.dp)
                    ) {
                        Text(
                            text = msg.message,
                            color = Color.White,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }

                // Interactive Quick Zoom Toolbar (+ / - / percentage)
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color(0xFF1E293B).copy(alpha = 0.85f),
                    border = BorderStroke(1.dp, Color(0xFF334155))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Zoom Out (-)
                        IconButton(
                            onClick = {
                                scale = (scale - 0.5f).coerceAtLeast(1f)
                                if (scale == 1f) offset = Offset.Zero
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Zoom Out", tint = Color.White, modifier = Modifier.size(16.dp))
                        }

                        Text(
                            text = "${(scale * 100).toInt()}%",
                            color = StadiumGold,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )

                        // Zoom In (+)
                        IconButton(
                            onClick = {
                                scale = (scale + 0.5f).coerceAtMost(5f)
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Zoom In", tint = Color.White, modifier = Modifier.size(16.dp))
                        }

                        // Fit/Reset
                        IconButton(
                            onClick = {
                                scale = 1f
                                offset = Offset.Zero
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.FullscreenExit, contentDescription = "Reset Zoom", tint = HawkEyeCyan, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DmRosterInbox(
    currentUser: CricHeroesProfile,
    players: List<CricHeroesProfile>,
    onSelect: (CricHeroesProfile) -> Unit,
    onRefresh: () -> Unit = {},
    unreadDmCounts: Map<String, Int> = emptyMap(),
    lastDmMessageBySender: Map<String, String> = emptyMap(),
    lastDmTimestampBySender: Map<String, Long> = emptyMap(),
    onMarkAllDmsRead: (() -> Unit)? = null
) {
    var query by remember { mutableStateOf("") }

    // Instagram-style sorting:
    // 1. Unread chats at top
    // 2. Most recent active chat (last timestamp) descending
    // 3. Alphabetical / roster fallback
    val filtered = remember(players, query, unreadDmCounts, lastDmTimestampBySender) {
        val q = query.trim().lowercase()
        val cleanQ = q.removePrefix("@")
        players.filter {
            it.username.removePrefix("@").trim().lowercase() != currentUser.username.removePrefix("@").trim().lowercase() &&
            (cleanQ.isEmpty() ||
             it.fullName.lowercase().contains(q) ||
             it.username.lowercase().contains(cleanQ) ||
             it.id.lowercase().contains(cleanQ) ||
             it.jerseyName.lowercase().contains(q))
        }.sortedWith(
            compareByDescending<CricHeroesProfile> { player ->
                val cleanUser = player.username.trim().removePrefix("@").lowercase()
                unreadDmCounts[cleanUser] ?: 0
            }.thenByDescending { player ->
                val cleanUser = player.username.trim().removePrefix("@").lowercase()
                lastDmTimestampBySender[cleanUser] ?: 0L
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Search by ID, @username, or name...", fontSize = 12.sp, color = TextMuted) },
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

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "MESSAGES (${filtered.size})",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = TextMuted,
                letterSpacing = 1.sp
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (unreadDmCounts.values.sum() > 0 && onMarkAllDmsRead != null) {
                    TextButton(
                        onClick = onMarkAllDmsRead,
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Icon(Icons.Default.DoneAll, contentDescription = null, tint = StadiumGold, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("Mark all read", fontSize = 10.sp, color = StadiumGold, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }
                TextButton(onClick = onRefresh, contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)) {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = CricketGreen, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("Refresh", fontSize = 10.sp, color = CricketGreen)
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(filtered, key = { it.username }) { player ->
                val cleanUser = player.username.trim().removePrefix("@").lowercase()
                val unreadCount = unreadDmCounts[cleanUser] ?: 0
                val lastMsg = lastDmMessageBySender[cleanUser]
                val lastTimestamp = lastDmTimestampBySender[cleanUser] ?: 0L
                val timeAgo = formatDmTimeAgo(lastTimestamp)

                Surface(
                    color = if (unreadCount > 0) Color(0xFF1E293B) else Color(0xFF0F172A),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(
                        if (unreadCount > 0) 1.dp else 0.5.dp,
                        if (unreadCount > 0) Color(0xFFEF4444).copy(alpha = 0.8f) else Color(0xFF1E293B)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(player) }
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Avatar with Red Dot Badge ONLY if unread
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1E293B)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(player.avatarEmoji.ifBlank { "🏏" }, fontSize = 19.sp)
                            if (unreadCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .size(11.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFEF4444))
                                        .border(1.5.dp, PitchDark, CircleShape)
                                        .align(Alignment.TopEnd)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        // Player details & Last message preview
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = player.fullName.ifBlank { player.jerseyName },
                                    fontSize = 13.sp,
                                    fontWeight = if (unreadCount > 0) FontWeight.Black else FontWeight.Bold,
                                    color = Color.White
                                )
                                if (unreadCount > 0) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFEF4444))
                                    )
                                }
                            }

                            if (!lastMsg.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = lastMsg,
                                        fontSize = 11.sp,
                                        color = if (unreadCount > 0) Color(0xFFFCA5A5) else TextMuted,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        fontWeight = if (unreadCount > 0) FontWeight.Bold else FontWeight.Normal,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    if (timeAgo.isNotBlank()) {
                                        Text(
                                            text = " • $timeAgo",
                                            fontSize = 9.5.sp,
                                            color = if (unreadCount > 0) StadiumGold else TextMuted.copy(alpha = 0.7f),
                                            fontWeight = if (unreadCount > 0) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            } else {
                                Text(
                                    text = "@${player.username.removePrefix("@")} • ${player.primaryRole}",
                                    fontSize = 10.sp,
                                    color = HawkEyeCyan
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Action / Unread Count Badge
                        if (unreadCount > 0) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFFEF4444))
                                    .padding(horizontal = 7.dp, vertical = 2.5.dp)
                            ) {
                                Text(
                                    text = if (unreadCount == 1) "New" else "$unreadCount",
                                    color = Color.White,
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        } else {
                            Icon(
                                imageVector = Icons.Default.Chat,
                                contentDescription = "Message",
                                tint = if (lastTimestamp > 0L) CricketGreen else Color(0xFF475569),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatDmTimeAgo(timestamp: Long): String {
    if (timestamp <= 0L) return ""
    val diff = System.currentTimeMillis() - timestamp
    return when {
        diff < 60_000L -> "now"
        diff < 3600_000L -> "${(diff / 60_000L).coerceAtLeast(1)}m"
        diff < 86400_000L -> "${diff / 3600_000L}h"
        diff < 7 * 86400_000L -> "${diff / 86400_000L}d"
        else -> SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(timestamp))
    }
}

@Composable
private fun DmChatConversation(
    currentUser: CricHeroesProfile,
    recipient: CricHeroesProfile,
    messages: List<ChatMessage>,
    isOwner: Boolean = false,
    onBack: () -> Unit,
    onSendMessage: (recipientUsername: String, text: String) -> Unit,
    onDeleteMessage: ((String) -> Unit)? = null
) {
    var textInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
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
                    contentDescription = "Back",
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
                    text = "@${recipient.username.removePrefix("@")} • Direct Message",
                    fontSize = 9.sp,
                    color = CricketGreen
                )
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Surface(
                    color = Color(0xFF1E293B).copy(alpha = 0.6f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
                ) {
                    Text(
                        text = "⏱️ Messages 1 mahine tak safe rehte hain, uske baad auto-delete hote hain taaki storage light rahe.",
                        color = TextMuted,
                        fontSize = 10.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            items(messages, key = { it.id }) { msg ->
                LiveChatMessageBubble(
                    msg = msg,
                    isOwner = isOwner,
                    onDelete = { onDeleteMessage?.invoke(msg.id) }
                )
            }
        }

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
                placeholder = { Text("Message @${recipient.username.removePrefix("@")}...", fontSize = 12.sp, color = TextMuted) },
                modifier = Modifier.weight(1f),
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
                        onSendMessage(recipient.username.removePrefix("@").trim(), textInput.trim())
                        textInput = ""
                    }
                })
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = {
                    if (textInput.isNotBlank()) {
                        onSendMessage(recipient.username.removePrefix("@").trim(), textInput.trim())
                        textInput = ""
                    }
                },
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(
                        if (textInput.isNotBlank()) Brush.linearGradient(listOf(CricketGreen, Color(0xFF007E33)))
                        else Brush.linearGradient(listOf(Color(0xFF334155), Color(0xFF1E293B)))
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