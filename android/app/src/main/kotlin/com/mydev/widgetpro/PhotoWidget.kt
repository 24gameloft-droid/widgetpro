package com.mydev.widgetpro

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.SystemClock
import android.widget.RemoteViews
import org.json.JSONArray

class PhotoWidget : AppWidgetProvider() {
    companion object {
        const val ACTION = "com.mydev.widgetpro.PHOTO_NEXT"
        const val INTERVAL = 5 * 60 * 1000L

        fun update(ctx: Context, mgr: AppWidgetManager, id: Int) {
            val prefs = ctx.getSharedPreferences("wp_photo", Context.MODE_PRIVATE)
            val photosJson = prefs.getString("photos_$id", "[]") ?: "[]"
            val idx = prefs.getInt("idx_$id", 0)
            val v = RemoteViews(ctx.packageName, R.layout.photo_widget)
            try {
                val photos = JSONArray(photosJson)
                if (photos.length() > 0) {
                    val uri = Uri.parse(photos.getString(idx % photos.length()))
                    val bmp = ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
                    if (bmp != null) v.setImageViewBitmap(R.id.photo_img, bmp)
                }
            } catch (e: Exception) {}
            val ni = Intent(ctx, PhotoWidget::class.java).setAction(ACTION).putExtra("wid", id)
            val pi = PendingIntent.getBroadcast(ctx, id, ni, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            v.setOnClickPendingIntent(R.id.photo_img, pi)
            mgr.updateAppWidget(id, v)
        }

        fun schedule(ctx: Context, id: Int) {
            val i = Intent(ctx, PhotoWidget::class.java).setAction(ACTION).putExtra("wid", id)
            val pi = PendingIntent.getBroadcast(ctx, id + 9000, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            (ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager)
                .setRepeating(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + INTERVAL, INTERVAL, pi)
        }
    }

    override fun onUpdate(ctx: Context, mgr: AppWidgetManager, ids: IntArray) { ids.forEach { update(ctx, mgr, it); schedule(ctx, it) } }

    override fun onReceive(ctx: Context, intent: Intent) {
        super.onReceive(ctx, intent)
        if (intent.action == ACTION) {
            val id = intent.getIntExtra("wid", -1); if (id == -1) return
            val prefs = ctx.getSharedPreferences("wp_photo", Context.MODE_PRIVATE)
            val photos = JSONArray(prefs.getString("photos_$id", "[]"))
            if (photos.length() > 0) prefs.edit().putInt("idx_$id", (prefs.getInt("idx_$id", 0) + 1) % photos.length()).apply()
            val mgr = AppWidgetManager.getInstance(ctx)
            update(ctx, mgr, id)
        }
    }
}
