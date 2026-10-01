package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.local.CricketDatabase
import com.example.data.model.MatchEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class LiveScoreAppWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // Query current live match asynchronously from Room DB
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = CricketDatabase.getInstance(context)
                val matches: List<MatchEntity> = db.cricketDao().getAllMatches().first()
                val liveMatch = matches.find { it.status == "LIVE" }

                for (appWidgetId in appWidgetIds) {
                    updateAppWidget(context, appWidgetManager, appWidgetId, liveMatch)
                }
            } catch (e: Exception) {
                for (appWidgetId in appWidgetIds) {
                    updateAppWidget(context, appWidgetManager, appWidgetId, null)
                }
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_AUTO_UPDATE_WIDGET) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, LiveScoreAppWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            if (appWidgetIds.isNotEmpty()) {
                onUpdate(context, appWidgetManager, appWidgetIds)
            }
        }
    }

    companion object {
        const val ACTION_AUTO_UPDATE_WIDGET = "com.example.ACTION_UPDATE_LIVE_SCORE_WIDGET"

        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            match: MatchEntity?
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_live_score)

            // Tap intent opens MainActivity
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                if (match != null) {
                    putExtra("EXTRA_MATCH_ID", match.id)
                }
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                appWidgetId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

            if (match != null && match.status == "LIVE") {
                // 1. LIVE MATCH ACTIVE: IPL-STYLE FULL SCORECARD
                views.setViewVisibility(R.id.widget_match_content, View.VISIBLE)
                views.setViewVisibility(R.id.widget_profile_content, View.GONE)

                views.setTextViewText(R.id.widget_tournament, match.tournamentName.ifBlank { "Live Match" })
                val team1 = if (match.teamA.length <= 12) match.teamA else match.getEffectiveTeamAShort()
                val team2 = if (match.teamB.length <= 12) match.teamB else match.getEffectiveTeamBShort()
                views.setTextViewText(R.id.widget_teams, "$team1 vs $team2")

                val oversStr = "${match.legalBalls / 6}.${match.legalBalls % 6}"
                views.setTextViewText(R.id.widget_score, "${match.score}/${match.wickets}")
                views.setTextViewText(R.id.widget_overs, "($oversStr ov)")

                // Current Run Rate & Required Run Rate Calculation
                val crr = if (match.legalBalls > 0) {
                    val rate = (match.score.toFloat() / match.legalBalls) * 6
                    String.format("%.2f", rate)
                } else "0.00"

                val runRateText = if (match.target > 0 && match.legalBalls < (match.totalOvers * 6)) {
                    val remainingRuns = (match.target - match.score).coerceAtLeast(0)
                    val remainingBalls = ((match.totalOvers * 6) - match.legalBalls).coerceAtLeast(1)
                    val rrr = String.format("%.2f", (remainingRuns.toFloat() / remainingBalls) * 6)
                    "CRR: $crr • RRR: $rrr"
                } else {
                    "CRR: $crr"
                }
                views.setTextViewText(R.id.widget_run_rates, runRateText)

                // Batsmen Row (IPL Style: Striker* runs(balls) | Non-Striker runs(balls))
                val strikerName = match.strikerName.ifBlank { "Striker" }
                val nonStrikerName = match.nonStrikerName.ifBlank { "Non-Striker" }
                val batsmenText = "🏏 $strikerName* ${match.strikerRuns}(${match.strikerBalls})  |  $nonStrikerName ${match.nonStrikerRuns}(${match.nonStrikerBalls})"
                views.setTextViewText(R.id.widget_batsmen, batsmenText)

                // Bowler Row (IPL Style: Bowler name: O-M-R-W & Economy)
                val bowlerName = match.bowlerName.ifBlank { "Bowler" }
                val bowlerOvers = "${match.bowlerBalls / 6}.${match.bowlerBalls % 6}"
                val bowlerEcon = if (match.bowlerBalls > 0) {
                    String.format("%.1f", (match.bowlerRuns.toFloat() / match.bowlerBalls) * 6)
                } else "0.0"
                val bowlerText = "🎯 $bowlerName: $bowlerOvers-${match.bowlerMaidens}-${match.bowlerRuns}-${match.bowlerWickets} (Econ: $bowlerEcon)"
                views.setTextViewText(R.id.widget_bowler, bowlerText)

            } else {
                // 2. NO LIVE MATCH RUNNING: SHOW USER'S OFFICIAL AYUUCRIC PRO PASS (EXACT SPEC FROM USER SCREENSHOT)
                views.setViewVisibility(R.id.widget_match_content, View.GONE)
                views.setViewVisibility(R.id.widget_profile_content, View.VISIBLE)

                // Retrieve Profile from SharedPreferences (matching CricketViewModel keys)
                val profilePrefs = context.getSharedPreferences("ayuu_cricheroes_profile", Context.MODE_PRIVATE)
                val rawId = profilePrefs.getString("id", "NTOOF2")?.ifBlank { "NTOOF2" } ?: "NTOOF2"
                val username = profilePrefs.getString("username", "ayush_7")?.ifBlank { "ayush_7" } ?: "ayush_7"
                val playerName = profilePrefs.getString("name", "Ayush Kumar")?.ifBlank { "Ayush Kumar" } ?: "Ayush Kumar"
                val jerseyName = profilePrefs.getString("jersey_name", "Ayush")?.ifBlank { "Ayush" } ?: "Ayush"
                val jerseyNumber = profilePrefs.getInt("jersey_num", 7).let { if (it > 0) it else 7 }
                val avatarEmoji = profilePrefs.getString("avatar", "🦁") ?: "🦁"
                val battingStyle = profilePrefs.getString("bat_style", "Right-hand Bat") ?: "Right-hand Bat"
                val bowlingStyle = profilePrefs.getString("bowl_style", "Right-arm Fast") ?: "Right-arm Fast"
                val teamName = profilePrefs.getString("team", "Local XI")?.ifBlank { "Local XI" } ?: "Local XI"
                val city = profilePrefs.getString("city", "India")?.ifBlank { "India" } ?: "India"
                val isVerified = profilePrefs.getBoolean("is_verified", true)

                val digitalId = "ID: " + if (rawId.length >= 6) rawId.takeLast(6).uppercase() else "NTOOF2"
                views.setTextViewText(R.id.widget_player_digital_id, digitalId)
                views.setTextViewText(R.id.widget_player_avatar, avatarEmoji)
                views.setTextViewText(R.id.widget_full_name, playerName)
                views.setTextViewText(R.id.widget_jersey_num, "#$jerseyNumber")
                views.setTextViewText(R.id.widget_username_sub, "@${username.removePrefix("@")} • $jerseyName")
                views.setTextViewText(R.id.widget_team_city, "$teamName • $city")
                views.setTextViewText(R.id.widget_tag_bat, battingStyle)
                views.setTextViewText(R.id.widget_tag_bowl, bowlingStyle)
                views.setTextViewText(R.id.widget_tag_bowl_verified, if (isVerified) "100% Verified" else "Pro Player")
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        fun updateAllWidgets(context: Context, match: MatchEntity?) {
            try {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val componentName = ComponentName(context, LiveScoreAppWidgetProvider::class.java)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
                for (id in appWidgetIds) {
                    updateAppWidget(context, appWidgetManager, id, match)
                }
            } catch (e: Throwable) {
                // Widget manager safely caught
            }
        }
    }
}
