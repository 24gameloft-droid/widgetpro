package com.mydev.widgetpro

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.RemoteViews
import org.json.JSONArray

class FolderWidget : AppWidgetProvider() {
    companion object {
        fun update(ctx: Context, mgr: AppWidgetManager, id: Int) {
            val prefs = ctx.applicationContext.getSharedPreferences("wp_folder", Context.MODE_PRIVATE)
            val alpha = prefs.getInt("alpha", 200)
            val r = prefs.getInt("r", 26)
            val g = prefs.getInt("g", 26)
            val b = prefs.getInt("b", 46)
            val cols = prefs.getInt("cols_$id", 3)
            val fidx = prefs.getInt("wid_$id", 0)
            val fJson = prefs.getString("folders", "[]") ?: "[]"

            val v = RemoteViews(ctx.packageName, R.layout.folder_widget)
            v.setInt(R.id.folder_root, "setBackgroundColor", (alpha shl 24) or (r shl 16) or (g shl 8) or b)
            v.setInt(R.id.folder_grid, "setNumColumns", cols)

            try {
                val folders = JSONArray(fJson)
                if (folders.length() > 0) {
                    val idx = if (fidx < folders.length()) fidx else 0
                    v.setTextViewText(R.id.folder_title, folders.getJSONObject(idx).optString("name", "Folder"))
                }
            } catch (e: Exception) {}

            val svcIntent = Intent(ctx, FolderRemoteService::class.java).putExtra("wid", id)
            v.setRemoteAdapter(R.id.folder_grid, svcIntent)

            val launchIntent = Intent(ctx, FolderWidget::class.java)
                .setAction("com.mydev.widgetpro.FOLDER_LAUNCH")
                .putExtra("wid", id)
            val pi = PendingIntent.getBroadcast(ctx, id, launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)
            v.setPendingIntentTemplate(R.id.folder_grid, pi)

            mgr.updateAppWidget(id, v)
            mgr.notifyAppWidgetViewDataChanged(id, R.id.folder_grid)
        }
    }

    override fun onUpdate(ctx: Context, mgr: AppWidgetManager, ids: IntArray) {
        ids.forEach { update(ctx, mgr, it) }
    }

    override fun onReceive(ctx: Context, intent: Intent) {
        super.onReceive(ctx, intent)
        if (intent.action == "com.mydev.widgetpro.FOLDER_LAUNCH") {
            val pkg = intent.getStringExtra("pkg") ?: return
            val launch = ctx.packageManager.getLaunchIntentForPackage(pkg) ?: return
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            ctx.startActivity(launch)
        }
    }

    override fun onAppWidgetOptionsChanged(ctx: Context, mgr: AppWidgetManager, id: Int, opts: Bundle) {
        val w = opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
        val cols = when { w >= 300 -> 5; w >= 240 -> 4; w >= 180 -> 3; w >= 120 -> 2; else -> 1 }
        ctx.applicationContext.getSharedPreferences("wp_folder", Context.MODE_PRIVATE)
            .edit().putInt("cols_$id", cols).apply()
        update(ctx, mgr, id)
    }
}
