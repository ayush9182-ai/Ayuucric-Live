package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.CricHeroesProfile
import com.example.data.model.MatchEntity
import com.example.ui.theme.*

@Composable
fun PlayingSquadDialog(
    match: MatchEntity,
    registeredPlayers: List<CricHeroesProfile> = emptyList(),
    onDismiss: () -> Unit,
    onSaveSquad: (striker: String, nonStriker: String, bowler: String, teamAPlayers: String, teamBPlayers: String) -> Unit,
    onViewPlayerProfile: ((CricHeroesProfile) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var selectedTeamTab by remember { mutableIntStateOf(if (match.currentInnings == 1) 0 else 1) } // 0: Team A, 1: Team B

    var currentStriker by remember { mutableStateOf(match.strikerName) }
    var currentNonStriker by remember { mutableStateOf(match.nonStrikerName) }
    var currentBowler by remember { mutableStateOf(match.bowlerName) }

    var teamAPlayersList by remember {
        mutableStateOf(
            match.teamAPlayers
                .split(",")
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .toMutableList()
        )
    }

    var teamBPlayersList by remember {
        mutableStateOf(
            match.teamBPlayers
                .split(",")
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .toMutableList()
        )
    }

    var addMode by remember { mutableIntStateOf(0) } // 0: Registered User, 1: Random Player
    var selectedUsernameToAdd by remember { mutableStateOf("") }
    var randomPlayerName by remember { mutableStateOf("") }
    var editingPlayerIndex by remember { mutableStateOf<Int?>(null) }
    var editingPlayerText by remember { mutableStateOf("") }

    val scrollState = rememberScrollState()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.88f)
                .padding(vertical = 16.dp)
                .testTag("playing_squad_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = PitchDark),
            border = CardDefaults.outlinedCardBorder().copy(
                brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF1E293B)),
                width = 1.5.dp
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
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
                                .background(CricketGreen.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Groups,
                                contentDescription = null,
                                tint = CricketGreen,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Playing XI & Squad",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "प्लेइंग 11 और एक्टिव खिलाड़ी",
                                fontSize = 11.sp,
                                color = TextSecondary
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
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Active Match Live Roles Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(PitchCard)
                        .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "CURRENT ON PITCH (मैदान पर वर्तमान खिलाड़ी):",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = StadiumGold,
                            letterSpacing = 0.5.sp
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("🏏 Striker: ", fontSize = 11.sp, color = TextMuted)
                                Text(currentStriker, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CricketGreen)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("🏃 Non-Striker: ", fontSize = 11.sp, color = TextMuted)
                                Text(currentNonStriker, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = HawkEyeCyan)
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🎯 Bowler: ", fontSize = 11.sp, color = TextMuted)
                            Text(currentBowler, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = StadiumGold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Team Tab Switcher
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF1E293B))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selectedTeamTab == 0) CricketGreen else Color.Transparent)
                            .clickable { selectedTeamTab = 0 }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${match.teamA} (Team A)",
                            fontSize = 12.sp,
                            fontWeight = if (selectedTeamTab == 0) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTeamTab == 0) PitchDark else TextSecondary
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selectedTeamTab == 1) HawkEyeCyan else Color.Transparent)
                            .clickable { selectedTeamTab = 1 }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${match.teamB} (Team B)",
                            fontSize = 12.sp,
                            fontWeight = if (selectedTeamTab == 1) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTeamTab == 1) PitchDark else TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Spacer(modifier = Modifier.height(10.dp))

                // Mode toggle: Registered App User vs Random Player
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF0F172A))
                        .border(1.dp, Color(0xFF334155), RoundedCornerShape(8.dp))
                        .padding(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (addMode == 0) CricketGreen.copy(alpha = 0.2f) else Color.Transparent)
                            .border(if (addMode == 0) 1.dp else 0.dp, if (addMode == 0) CricketGreen else Color.Transparent, RoundedCornerShape(6.dp))
                            .clickable { addMode = 0 }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "📱 App User (@username)",
                            fontSize = 11.sp,
                            fontWeight = if (addMode == 0) FontWeight.Bold else FontWeight.Normal,
                            color = if (addMode == 0) CricketGreen else TextSecondary
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (addMode == 1) StadiumGold.copy(alpha = 0.2f) else Color.Transparent)
                            .border(if (addMode == 1) 1.dp else 0.dp, if (addMode == 1) StadiumGold else Color.Transparent, RoundedCornerShape(6.dp))
                            .clickable { addMode = 1 }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "⚡ Random Player (नाम से)",
                            fontSize = 11.sp,
                            fontWeight = if (addMode == 1) FontWeight.Bold else FontWeight.Normal,
                            color = if (addMode == 1) StadiumGold else TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (addMode == 0) {
                    // Registered Users Dropdown / Quick Pick
                    var isDropdownExpanded by remember { mutableStateOf(false) }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedButton(
                                onClick = { isDropdownExpanded = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, if (selectedUsernameToAdd.isNotBlank()) CricketGreen else Color(0xFF334155)),
                                colors = ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFF1E293B))
                            ) {
                                Text(
                                    text = if (selectedUsernameToAdd.isNotBlank()) selectedUsernameToAdd else "Select Registered User (@)",
                                    fontSize = 12.sp,
                                    color = if (selectedUsernameToAdd.isNotBlank()) CricketGreen else TextSecondary,
                                    maxLines = 1
                                )
                            }

                            DropdownMenu(
                                expanded = isDropdownExpanded,
                                onDismissRequest = { isDropdownExpanded = false },
                                modifier = Modifier.background(PitchDark)
                            ) {
                                if (registeredPlayers.isEmpty()) {
                                    DropdownMenuItem(
                                        text = { Text("No other users logged in yet", color = TextSecondary, fontSize = 12.sp) },
                                        onClick = { isDropdownExpanded = false }
                                    )
                                } else {
                                    registeredPlayers.forEach { user ->
                                        DropdownMenuItem(
                                            text = {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(user.avatarEmoji, fontSize = 16.sp)
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Column {
                                                        Text(user.fullName, fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                                        Text("@${user.username.removePrefix("@")}", fontSize = 10.sp, color = StadiumGold)
                                                    }
                                                }
                                            },
                                            onClick = {
                                                selectedUsernameToAdd = "${user.fullName} (@${user.username.removePrefix("@")})"
                                                isDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Button(
                            onClick = {
                                if (selectedUsernameToAdd.isNotBlank()) {
                                    if (selectedTeamTab == 0) {
                                        teamAPlayersList = (teamAPlayersList + selectedUsernameToAdd.trim()).toMutableList()
                                    } else {
                                        teamBPlayersList = (teamBPlayersList + selectedUsernameToAdd.trim()).toMutableList()
                                    }
                                    selectedUsernameToAdd = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CricketGreen),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.PersonAdd, contentDescription = "Add", tint = PitchDark, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add", color = PitchDark, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                } else {
                    // Random Player Name Manual Input
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = randomPlayerName,
                            onValueChange = { randomPlayerName = it },
                            placeholder = { Text("Random player name (e.g. Bunty)", fontSize = 12.sp) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = StadiumGold,
                                unfocusedBorderColor = Color(0xFF334155),
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            modifier = Modifier.weight(1f)
                        )

                        Button(
                            onClick = {
                                if (randomPlayerName.isNotBlank()) {
                                    val entry = "${randomPlayerName.trim()} (Guest)"
                                    if (selectedTeamTab == 0) {
                                        teamAPlayersList = (teamAPlayersList + entry).toMutableList()
                                    } else {
                                        teamBPlayersList = (teamBPlayersList + entry).toMutableList()
                                    }
                                    randomPlayerName = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = StadiumGold),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = "Add", tint = PitchDark, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add", color = PitchDark, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Players List Scrollable Area
                val activeList = if (selectedTeamTab == 0) teamAPlayersList else teamBPlayersList

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    activeList.forEachIndexed { index, player ->
                        val isStriker = player == currentStriker
                        val isNonStriker = player == currentNonStriker
                        val isBowler = player == currentBowler

                        // Match with registered community player for clickable profile card
                        val matchedRegisteredProfile = registeredPlayers.firstOrNull { reg ->
                            player.contains("@${reg.username.removePrefix("@")}", ignoreCase = true) ||
                            player.equals(reg.fullName, ignoreCase = true) ||
                            player.equals(reg.jerseyName, ignoreCase = true)
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = when {
                                    isStriker -> CricketGreen.copy(alpha = 0.12f)
                                    isNonStriker -> HawkEyeCyan.copy(alpha = 0.12f)
                                    isBowler -> StadiumGold.copy(alpha = 0.12f)
                                    else -> PitchCard
                                }
                            ),
                            border = CardDefaults.outlinedCardBorder().copy(
                                brush = androidx.compose.ui.graphics.SolidColor(
                                    when {
                                        isStriker -> CricketGreen
                                        isNonStriker -> HawkEyeCyan
                                        isBowler -> StadiumGold
                                        else -> Color(0xFF1E293B)
                                    }
                                )
                            )
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .weight(1f, fill = false)
                                            .clickable {
                                                if (matchedRegisteredProfile != null && onViewPlayerProfile != null) {
                                                    onViewPlayerProfile(matchedRegisteredProfile)
                                                } else if (onViewPlayerProfile != null) {
                                                    // Generate dynamic profile card for player
                                                    val cardProfile = CricHeroesProfile(
                                                        id = "p_$index",
                                                        username = player.filter { it.isLetterOrDigit() }.lowercase().take(10).ifBlank { "player_$index" },
                                                        fullName = player.replace("(Guest)", "").trim(),
                                                        jerseyName = player.replace("(Guest)", "").trim().take(8).uppercase(),
                                                        jerseyNumber = index + 1,
                                                        primaryRole = if (isBowler) "Bowler" else "Batter",
                                                        isOnline = matchedRegisteredProfile?.isOnline ?: false
                                                    )
                                                    onViewPlayerProfile(cardProfile)
                                                }
                                            }
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(26.dp)
                                                .clip(CircleShape)
                                                .background(if (matchedRegisteredProfile != null) CricketGreen.copy(alpha = 0.25f) else Color(0xFF1E293B))
                                                .border(1.dp, if (matchedRegisteredProfile != null) CricketGreen else Color.Transparent, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = matchedRegisteredProfile?.avatarEmoji ?: "${index + 1}",
                                                fontSize = if (matchedRegisteredProfile != null) 13.sp else 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (matchedRegisteredProfile != null) Color.White else TextSecondary
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))

                                        if (editingPlayerIndex == index) {
                                            OutlinedTextField(
                                                value = editingPlayerText,
                                                onValueChange = { editingPlayerText = it },
                                                singleLine = true,
                                                modifier = Modifier.width(160.dp),
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedTextColor = TextPrimary,
                                                    unfocusedTextColor = TextPrimary
                                                )
                                            )
                                            IconButton(
                                                onClick = {
                                                    if (editingPlayerText.isNotBlank()) {
                                                        if (selectedTeamTab == 0) {
                                                            teamAPlayersList[index] = editingPlayerText.trim()
                                                        } else {
                                                            teamBPlayersList[index] = editingPlayerText.trim()
                                                        }
                                                    }
                                                    editingPlayerIndex = null
                                                }
                                            ) {
                                                Icon(imageVector = Icons.Default.Check, contentDescription = "Save", tint = CricketGreen)
                                            }
                                        } else {
                                            Column {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = player,
                                                        fontSize = 13.5.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = TextPrimary
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Icon(
                                                        imageVector = Icons.Default.AccountBox,
                                                        contentDescription = "View Profile Card",
                                                        tint = StadiumGold,
                                                        modifier = Modifier.size(15.dp)
                                                    )
                                                }
                                                if (matchedRegisteredProfile != null) {
                                                    Text(
                                                        text = "Verified App User • Tap to view ID Card",
                                                        fontSize = 9.sp,
                                                        color = CricketGreen
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // Role Status Badges
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        if (isStriker) {
                                            BadgePill(text = "STRIKER 🏏", color = CricketGreen)
                                        }
                                        if (isNonStriker) {
                                            BadgePill(text = "NON-STRIKER 🏃", color = HawkEyeCyan)
                                        }
                                        if (isBowler) {
                                            BadgePill(text = "BOWLER 🎯", color = StadiumGold)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                // Quick Actions: Set as Striker, Non-Striker, Bowler
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    AssistChip(
                                        onClick = { currentStriker = player },
                                        label = { Text("Striker 🏏", fontSize = 10.sp) },
                                        colors = AssistChipDefaults.assistChipColors(
                                            labelColor = if (isStriker) CricketGreen else TextSecondary
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )

                                    AssistChip(
                                        onClick = { currentNonStriker = player },
                                        label = { Text("Non-Striker 🏃", fontSize = 10.sp) },
                                        colors = AssistChipDefaults.assistChipColors(
                                            labelColor = if (isNonStriker) HawkEyeCyan else TextSecondary
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )

                                    AssistChip(
                                        onClick = { currentBowler = player },
                                        label = { Text("Bowler 🎯", fontSize = 10.sp) },
                                        colors = AssistChipDefaults.assistChipColors(
                                            labelColor = if (isBowler) StadiumGold else TextSecondary
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Bottom Save Button
                Button(
                    onClick = {
                        onSaveSquad(
                            currentStriker,
                            currentNonStriker,
                            currentBowler,
                            teamAPlayersList.joinToString(", "),
                            teamBPlayersList.joinToString(", ")
                        )
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CricketGreen),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = PitchDark)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Apply Squad & Active Players (स्क्वाड लागू करें)",
                        color = PitchDark,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun BadgePill(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.2f))
            .border(0.5.dp, color, RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            color = color
        )
    }
}
