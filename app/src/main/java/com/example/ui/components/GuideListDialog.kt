package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.*

@Composable
fun GuideListDialog(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.85f)
                .padding(vertical = 12.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = PitchDark),
            border = BorderStroke(
                1.5.dp,
                Brush.linearGradient(listOf(StadiumGold, CricketGreen, HawkEyeCyan))
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
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
                                imageVector = Icons.Default.MenuBook,
                                contentDescription = null,
                                tint = CricketGreen,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "AyuuCric Quick Guide",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Black,
                                color = TextPrimary
                            )
                            Text(
                                text = "फीचर गाइड - कौन सी चीज़ कहाँ है?",
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

                // Scrollable guide items
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    GuideItem(
                        icon = Icons.Default.Casino,
                        iconTint = StadiumGold,
                        title = "1. Fair Coin Flipper (टॉस)",
                        desc = "Top 3-dot (⋮) menu ya Toss button se kholen. 100% cheat-proof crypto random Heads/Tails spin karta hai. Toss jeet kar Bat ya Bowl chun sakte hain."
                    )

                    GuideItem(
                        icon = Icons.Default.Groups,
                        iconTint = CricketGreen,
                        title = "2. Playing Squad & Players (टीमें व खिलाड़ी)",
                        desc = "3-dot (⋮) menu se 'Playing Squad' chune. App me logged-in players ko @username se add karein ya Guest player ka naam likhkar add karein. Sabhi viewers ko screen par naam dikhenge."
                    )

                    GuideItem(
                        icon = Icons.Default.AccountBox,
                        iconTint = HawkEyeCyan,
                        title = "3. Player Profile Identity Card",
                        desc = "Kisi bhi player ke naam ya profile icon par tap karein - unka verified digital card khul jayega jisme Runs, Wickets, Batting style, Bowling style aur DM button milega."
                    )

                    GuideItem(
                        icon = Icons.Default.CloudSync,
                        iconTint = Color(0xFF38BDF8),
                        title = "4. Online First + Offline Fallback",
                        desc = "App poori tarah online cloud se live update hota hai. Agar match ke dauran ground me internet na ho, to automatic Offline Hotspot Mode switch ho jata hai jisse score kabhi ruke na."
                    )

                    GuideItem(
                        icon = Icons.Default.Mic,
                        iconTint = StadiumGold,
                        title = "5. AI Navjot Singh Sidhu Commentary",
                        desc = "Har ball par live Sidhu Paaji ke shayari aur dynamic punchlines aate hain. Audio sunne ke liye top AI button ya 3-dot menu use karein."
                    )

                    GuideItem(
                        icon = Icons.Default.SlowMotionVideo,
                        iconTint = Color(0xFFF43F5E),
                        title = "6. DRS UltraEdge & HawkEye 3D",
                        desc = "Out hone par DRS tab me jakar Snicko audio wave aur ball-tracking dekh sakte hain."
                    )

                    GuideItem(
                        icon = Icons.Default.Shield,
                        iconTint = Color(0xFFA855F7),
                        title = "7. Role Security (Admin, Scorer, Viewer)",
                        desc = "Header me Role pill par tap karein. Scorer PIN se score update kar sakta hai, aur viewers bina kisi rukawat ke match dekh sakte hain."
                    )

                    GuideItem(
                        icon = Icons.Default.Share,
                        iconTint = Color(0xFF22C55E),
                        title = "8. Share APK to Friends (No USB)",
                        desc = "Menu (Switch bar) me 'Share APK' dabakar direct WhatsApp, Bluetooth ya Nearby Share se apne phone ki updated APK doston ko bhej sakte hain."
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CricketGreen)
                ) {
                    Text(
                        text = "Got It! Match Dekhein 👍",
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
private fun GuideItem(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    desc: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B).copy(alpha = 0.8f)),
        border = BorderStroke(1.dp, Color(0xFF334155))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(iconTint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = desc,
                    fontSize = 11.5.sp,
                    color = TextSecondary,
                    lineHeight = 16.sp
                )
            }
        }
    }
}
