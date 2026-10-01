package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.ChatViewModel

@Composable
fun ChatScreen(
    currentUserId: String,
    threadId: String,
    viewModel: ChatViewModel,
    recipientUid: String = "",
    recipientUsername: String = "",
    senderUsername: String = "",
    senderName: String = ""
) {
    val messages by viewModel.messages.collectAsState()
    var inputMessage by remember { mutableStateOf("") }

    LaunchedEffect(threadId) {
        viewModel.listenToMessages(threadId)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("chat_screen")
    ) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages, key = { it.id.ifBlank { "${it.senderUid}_${it.timestamp}" } }) { msg ->
                val isCurrentUser = msg.senderUid == currentUserId || msg.isFromMe

                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = if (isCurrentUser) Alignment.CenterEnd else Alignment.CenterStart
                ) {
                    Column(
                        modifier = Modifier
                            .background(
                                color = if (isCurrentUser) Color(0xFF1E3A8A) else Color(0xFF27272A),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        if (!isCurrentUser && msg.senderName.isNotBlank()) {
                            Text(
                                text = msg.senderName,
                                color = Color(0xFF93C5FD),
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                        }
                        Text(
                            text = msg.message,
                            color = Color.White,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }

        // Optional bottom send bar for interactive standalone use
        if (recipientUid.isNotBlank() || threadId.isNotBlank()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF18181B))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputMessage,
                    onValueChange = { inputMessage = it },
                    placeholder = { Text("Write a message...", color = Color.Gray, fontSize = 14.sp) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_input_field"),
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF3B82F6),
                        unfocusedBorderColor = Color(0xFF3F3F46),
                        focusedContainerColor = Color(0xFF27272A),
                        unfocusedContainerColor = Color(0xFF27272A)
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = {
                        if (inputMessage.isNotBlank()) {
                            viewModel.sendMessage(
                                threadId = threadId,
                                text = inputMessage,
                                recipientUid = recipientUid,
                                recipientUsername = recipientUsername,
                                senderUsername = senderUsername,
                                senderName = senderName
                            )
                            inputMessage = ""
                        }
                    })
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (inputMessage.isNotBlank()) {
                            viewModel.sendMessage(
                                threadId = threadId,
                                text = inputMessage,
                                recipientUid = recipientUid,
                                recipientUsername = recipientUsername,
                                senderUsername = senderUsername,
                                senderName = senderName
                            )
                            inputMessage = ""
                        }
                    },
                    modifier = Modifier.testTag("chat_send_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send Message",
                        tint = Color(0xFF3B82F6)
                    )
                }
            }
        }
    }
}
