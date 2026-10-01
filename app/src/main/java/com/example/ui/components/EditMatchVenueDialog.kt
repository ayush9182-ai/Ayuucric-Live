package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.maps.GoogleMapsGroundingService
import com.example.data.maps.GroundVenueInfo
import com.example.data.model.MatchEntity
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditMatchVenueDialog(
    match: MatchEntity,
    onDismiss: () -> Unit,
    onSaveVenue: (venue: String, date: String, time: String, address: String, coordinates: String) -> Unit,
    onSearchGrounds: (String) -> Unit,
    searchResults: List<GroundVenueInfo>,
    isSearching: Boolean
) {
    val context = LocalContext.current
    val todayFormatted = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date()) }
    val nowFormatted = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date()) }

    var venueName by remember { mutableStateOf(match.venue.ifBlank { "Ground Turf" }) }
    var matchDate by remember { mutableStateOf(match.matchDate.ifBlank { todayFormatted }) }
    var matchTime by remember { mutableStateOf(match.matchTime.ifBlank { nowFormatted }) }
    var venueAddress by remember { mutableStateOf(match.venueAddress) }
    var venueCoordinates by remember { mutableStateOf(match.venueCoordinates) }

    var searchQuery by remember { mutableStateOf("") }
    var showGroundSearchSection by remember { mutableStateOf(false) }

    val isWeird = remember(venueName) { GoogleMapsGroundingService.isWeirdVenueName(venueName) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .testTag("edit_match_venue_dialog"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = PitchDark),
            border = CardDefaults.outlinedCardBorder().copy(
                brush = androidx.compose.ui.graphics.SolidColor(StadiumGold.copy(alpha = 0.5f)),
                width = 1.dp
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
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
                                .background(StadiumGold.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Place,
                                contentDescription = null,
                                tint = StadiumGold,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "MATCH VENUE & TIME",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black,
                                color = TextPrimary,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "मैदान, तारीख और समय एडिट करें",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                    }
                }

                // Weird name detection banner ("agar ajeeb sa name aaye to usse edit bhi kar sake")
                if (isWeird) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A1C0E)),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, StadiumGold.copy(alpha = 0.6f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "⚠️ Ajeeb / Lamba Name Detected",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = StadiumGold
                                )
                                Text(
                                    text = "Isko aasan aur friendly banayein",
                                    fontSize = 10.sp,
                                    color = TextSecondary
                                )
                            }
                            Button(
                                onClick = {
                                    venueName = GoogleMapsGroundingService.suggestCleanFriendlyName(venueName, venueAddress)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = StadiumGold),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("Auto Clean ✨", color = PitchDark, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Venue Name Input
                OutlinedTextField(
                    value = venueName,
                    onValueChange = { venueName = it },
                    label = { Text("Ground / Stadium Name *", fontSize = 12.sp) },
                    placeholder = { Text("e.g. Shivaji Park Turf, Sector 7", fontSize = 12.sp, color = TextMuted) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = StadiumGold,
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedContainerColor = PitchSurface,
                        unfocusedContainerColor = PitchSurface,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    trailingIcon = {
                        IconButton(onClick = { showGroundSearchSection = !showGroundSearchSection }) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search on Maps",
                                tint = CricketGreen
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_venue_name_input")
                )

                // Date & Time Inputs Row ("date or time bhi")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = matchDate,
                        onValueChange = { matchDate = it },
                        label = { Text("Date (तारीख)", fontSize = 11.sp) },
                        placeholder = { Text(todayFormatted, fontSize = 11.sp, color = TextMuted) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CricketGreen,
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedContainerColor = PitchSurface,
                            unfocusedContainerColor = PitchSurface,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        leadingIcon = {
                            Icon(Icons.Default.DateRange, contentDescription = null, tint = CricketGreen, modifier = Modifier.size(16.dp))
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("edit_match_date_input")
                    )

                    OutlinedTextField(
                        value = matchTime,
                        onValueChange = { matchTime = it },
                        label = { Text("Time (समय)", fontSize = 11.sp) },
                        placeholder = { Text(nowFormatted, fontSize = 11.sp, color = TextMuted) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CricketGreen,
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedContainerColor = PitchSurface,
                            unfocusedContainerColor = PitchSurface,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        leadingIcon = {
                            Icon(Icons.Default.AccessTime, contentDescription = null, tint = StadiumGold, modifier = Modifier.size(16.dp))
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("edit_match_time_input")
                    )
                }

                // Full Address Input
                OutlinedTextField(
                    value = venueAddress,
                    onValueChange = { venueAddress = it },
                    label = { Text("Full Address / Area (वैकल्पिक)", fontSize = 11.sp) },
                    placeholder = { Text("e.g. Sector 62, Near Metro Station", fontSize = 11.sp, color = TextMuted) },
                    maxLines = 2,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = HawkEyeCyan,
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedContainerColor = PitchSurface,
                        unfocusedContainerColor = PitchSurface,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Quick Google Maps Search Toggle Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { showGroundSearchSection = !showGroundSearchSection },
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(
                            imageVector = if (showGroundSearchSection) Icons.Default.KeyboardArrowUp else Icons.Default.Map,
                            contentDescription = null,
                            tint = HawkEyeCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (showGroundSearchSection) "Hide Maps Search" else "Google Maps Grounding 🗺️",
                            color = HawkEyeCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Open in Google Maps navigation button
                    if (venueName.isNotBlank()) {
                        TextButton(
                            onClick = {
                                GoogleMapsGroundingService.openGoogleMapsForVenue(
                                    context = context,
                                    venue = venueName,
                                    address = venueAddress,
                                    coordinates = venueCoordinates
                                )
                            },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(Icons.Default.Navigation, contentDescription = null, tint = CricketGreen, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Open Map 📍", color = CricketGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Google Maps Grounding Search Section (powered by Gemini 3.5 Flash with googleMaps tool)
                AnimatedVisibility(visible = showGroundSearchSection) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF0F172A), RoundedCornerShape(12.dp))
                            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(12.dp))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "MAPS GROUNDING (Gemini 3.5 Flash)",
                                color = StadiumGold,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            if (isSearching) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    color = StadiumGold,
                                    strokeWidth = 2.dp
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("Search city, ground, turf...", fontSize = 11.sp, color = TextMuted) },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CricketGreen,
                                    unfocusedBorderColor = Color(0xFF334155),
                                    focusedContainerColor = PitchSurface,
                                    unfocusedContainerColor = PitchSurface,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                )
                            )

                            Button(
                                onClick = { onSearchGrounds(searchQuery.ifBlank { venueName }) },
                                colors = ButtonDefaults.buttonColors(containerColor = CricketGreen),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(42.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp)
                            ) {
                                Text("Find 🔎", color = PitchDark, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }

                        // Ground results list
                        if (searchResults.isNotEmpty()) {
                            Text(
                                text = "Select to Auto-Fill & Rename:",
                                color = TextMuted,
                                fontSize = 10.sp
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                searchResults.take(3).forEach { ground ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFF1E293B))
                                            .clickable {
                                                venueName = ground.name
                                                venueAddress = ground.cleanAddress
                                                if (ground.latitude != null && ground.longitude != null) {
                                                    venueCoordinates = "${ground.latitude},${ground.longitude}"
                                                }
                                                showGroundSearchSection = false
                                            }
                                            .padding(horizontal = 10.dp, vertical = 7.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.SportsCricket,
                                            contentDescription = null,
                                            tint = CricketGreen,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = ground.name,
                                                color = TextPrimary,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = ground.cleanAddress,
                                                color = TextSecondary,
                                                fontSize = 10.sp,
                                                maxLines = 1
                                            )
                                        }
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Pick",
                                            tint = StadiumGold,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            val cleanFinalVenue = venueName.trim().ifBlank { "Local Ground" }
                            onSaveVenue(cleanFinalVenue, matchDate, matchTime, venueAddress, venueCoordinates)
                        },
                        modifier = Modifier
                            .weight(1.5f)
                            .testTag("save_venue_schedule_btn"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = StadiumGold)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, tint = PitchDark, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save & Update 📍", color = PitchDark, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}
