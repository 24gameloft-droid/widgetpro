package com.mydev.widgetpro

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

class NotifService : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.isOngoing) return
        val extras = sbn.notification.extras
        val title = extras.getString("android.title") ?: ""
        val text = extras.getCharSequence("android.text")?.toString() ?: ""
        if (title.isEmpty() && text.isEmpty()) return

        val prefs = getSharedPreferences("wp_notif", MODE_PRIVATE)
        val allowed = prefs.getStringSet("allowed", emptySet()) ?: emptySet()
        if (allowed.isNotEmpty() && !allowed.contains(sbn.packageName)) return

        val arr = JSONArray(prefs.getString("list", "[]"))
        if (arr.length() > 0) {
            val last = arr.getJSONObject(0)
            if (last.optString("t") == title && last.optString("x") == text && last.optString("p") == sbn.packageName) return
        }

        val appName = try { packageManager.getApplicationLabel(packageManager.getApplicationInfo(sbn.packageName, 0)).toString() } catch (e: Exception) { sbn.packageName }
        val item = JSONObject().apply { put("a", appName); put("p", sbn.packageName); put("t", title); put("x", text); put("ts", System.currentTimeMillis()) }
        val newArr = JSONArray()
        newArr.put(item)
        for (i in 0 until minOf(arr.length(), 19)) newArr.put(arr.get(i))
        prefs.edit().putString("list", newArr.toString()).apply()

        val mgr = AppWidgetManager.getInstance(this)
        mgr.getAppWidgetIds(ComponentName(this, NotifWidget::class.java)).forEach { NotifWidget.update(this, mgr, it) }
    }
}
