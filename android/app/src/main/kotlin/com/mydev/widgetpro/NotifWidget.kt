package com.mydev.widgetpro

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.view.View
import android.widget.RemoteViews
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.*

class NotifWidget : AppWidgetProvider() {
    companion object {
        fun update(ctx: Context, mgr: AppWidgetManager, id: Int) {
            val prefs = ctx.applicationContext.getSharedPreferences("wp_notif", Context.MODE_PRIVATE)
            val alpha = prefs.getInt("alpha", 200)
            val r = prefs.getInt("r", 30)
            val g = prefs.getInt("g", 30)
            val b = prefs.getInt("b", 46)
            val listStr = prefs.getString("list", "[]") ?: "[]"
            val arr = try { JSONArray(listStr) } catch (e: Exception) { JSONArray() }
            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            val pm = ctx.packageManager
            val v = RemoteViews(ctx.packageName, R.layout.notif_widget)

            v.setInt(R.id.notif_root, "setBackgroundColor",
                (alpha shl 24) or (r shl 16) or (g shl 8) or b)
            v.setTextViewText(R.id.notif_title, "Notifications (${arr.length()})")

            val rows = listOf(R.id.row1,R.id.row2,R.id.row3,R.id.row4,R.id.row5)
            val texts = listOf(R.id.text1,R.id.text2,R.id.text3,R.id.text4,R.id.text5)
            val icons = listOf(R.id.icon1,R.id.icon2,R.id.icon3,R.id.icon4,R.id.icon5)
            val divs = listOf(R.id.div1,R.id.div2,R.id.div3,R.id.div4)

            for (i in 0..4) {
                if (i < arr.length()) {
                    val n = arr.getJSONObject(i)
                    val time = sdf.format(Date(n.optLong("ts", 0)))
                    v.setViewVisibility(rows[i], View.VISIBLE)
                    v.setTextViewText(texts[i],
                        "${n.optString("a")}  $time\n${n.optString("t")}\n${n.optString("x")}")
                    try {
                        v.setImageViewBitmap(icons[i], toBmp(pm.getApplicationIcon(n.optString("p"))))
                    } catch (e: Exception) {
                        v.setImageViewResource(icons[i], android.R.drawable.sym_def_app_icon)
                    }
                    if (i < divs.size && i < arr.length()-1)
                        v.setViewVisibility(divs[i], View.VISIBLE)
                } else {
                    v.setViewVisibility(rows[i], View.GONE)
                    if (i < divs.size) v.setViewVisibility(divs[i], View.GONE)
                }
            }
            mgr.updateAppWidget(id, v)
        }

        fun toBmp(d: Drawable): Bitmap {
            if (d is BitmapDrawable && d.bitmap != null) return d.bitmap
            val w = if (d.intrinsicWidth > 0) d.intrinsicWidth else 96
            val h = if (d.intrinsicHeight > 0) d.intrinsicHeight else 96
            val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            d.setBounds(0, 0, w, h); d.draw(Canvas(bmp)); return bmp
        }
    }

    override fun onUpdate(ctx: Context, mgr: AppWidgetManager, ids: IntArray) {
        ids.forEach { update(ctx, mgr, it) }
    }

    override fun onReceive(ctx: Context, intent: Intent) {
        super.onReceive(ctx, intent)
        if (intent.action == "com.mydev.widgetpro.UPDATE_NOTIF") {
            val mgr = AppWidgetManager.getInstance(ctx)
            mgr.getAppWidgetIds(ComponentName(ctx, NotifWidget::class.java))
                .forEach { update(ctx, mgr, it) }
        }
    }
}
