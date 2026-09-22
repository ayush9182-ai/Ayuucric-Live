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
                val liveMatch = matches.find { it.status == "LIVE" } ?: matches.firstOrNull()

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
                views.setViewVisibility(R.id.widget_match_content, View.VISIBLE)
                views.setViewVisibility(R.id.widget_empty_content, View.GONE)
                views.setViewVisibility(R.id.widget_badge, View.VISIBLE)

                views.setTextViewText(R.id.widget_badge, "LIVE")
                views.setTextViewText(R.id.widget_tournament, match.tournamentName.ifBlank { "Local Match" })
                views.setTextViewText(R.id.widget_teams, "${match.teamA} vs ${match.teamB}")

                val oversStr = "${match.legalBalls / 6}.${match.legalBalls % 6}"
                views.setTextViewText(R.id.widget_score, "${match.score}/${match.wickets} ($oversStr ov)")

                val statusText = match.statusDetail.ifBlank {
                    if (match.target > 0) "Target: ${match.target}" else "1st Innings in progress"
                }
                views.setTextViewText(R.id.widget_status, statusText)

                val strikerStr = "${match.strikerName}* ${match.strikerRuns}(${match.strikerBalls})"
                val bowlerStr = "${match.bowlerName} ${match.bowlerWickets}/${match.bowlerRuns}"
                views.setTextViewText(R.id.widget_players, "🏏 $strikerStr  •  🎯 $bowlerStr")
            } else if (match != null) {
                // Completed or scheduled match
                views.setViewVisibility(R.id.widget_match_content, View.VISIBLE)
                views.setViewVisibility(R.id.widget_empty_content, View.GONE)
                views.setViewVisibility(R.id.widget_badge, View.VISIBLE)

                views.setTextViewText(R.id.widget_badge, match.status.take(6))
                views.setTextViewText(R.id.widget_tournament, match.tournamentName.ifBlank { "Local Match" })
                views.setTextViewText(R.id.widget_teams, "${match.teamA} vs ${match.teamB}")
                val oversStr = "${match.legalBalls / 6}.${match.legalBalls % 6}"
                views.setTextViewText(R.id.widget_score, "${match.score}/${match.wickets} ($oversStr)")
                views.setTextViewText(R.id.widget_status, match.statusDetail)
                views.setTextViewText(R.id.widget_players, "Match Completed • Tap to see scorecard")
            } else {
                // No match exists currently
                views.setViewVisibility(R.id.widget_match_content, View.GONE)
                views.setViewVisibility(R.id.widget_empty_content, View.VISIBLE)
                views.setViewVisibility(R.id.widget_badge, View.GONE)
                views.setTextViewText(R.id.widget_tournament, "AyuuCric Live Score")
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
