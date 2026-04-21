package com.mydev.widgetpro

import android.app.Notification
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import org.json.JSONArray
import org.json.JSONObject

class NotifService : NotificationListenerService() {

    private fun prefs(): SharedPreferences =
        applicationContext.getSharedPreferences("wp_notif", Context.MODE_PRIVATE)

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.isOngoing) return
        val extras = sbn.notification.extras
        val title = extras.getString("android.title") ?: ""
        val text = extras.getCharSequence("android.text")?.toString() ?: ""
        if (title.isEmpty() && text.isEmpty()) return

        val prefs = prefs()
        val allowed = prefs.getStringSet("allowed", emptySet()) ?: emptySet()
        if (allowed.isNotEmpty() && !allowed.contains(sbn.packageName)) return

        val listStr = prefs.getString("list", "[]") ?: "[]"
        val arr = try { JSONArray(listStr) } catch (e: Exception) { JSONArray() }

        // منع التكرار
        if (arr.length() > 0) {
            val last = arr.getJSONObject(0)
            if (last.optString("t") == title &&
                last.optString("x") == text &&
                last.optString("p") == sbn.packageName) return
        }

        val appName = try {
            packageManager.getApplicationLabel(
                packageManager.getApplicationInfo(sbn.packageName, 0)
            ).toString()
        } catch (e: Exception) { sbn.packageName }

        val item = JSONObject().apply {
            put("a", appName)
            put("p", sbn.packageName)
            put("t", title)
            put("x", text)
            put("ts", System.currentTimeMillis())
        }

        val newArr = JSONArray()
        newArr.put(item)
        for (i in 0 until minOf(arr.length(), 19)) newArr.put(arr.get(i))

        prefs.edit().putString("list", newArr.toString()).apply()

        // تحديث الويدجت
        val mgr = AppWidgetManager.getInstance(applicationContext)
        val ids = mgr.getAppWidgetIds(ComponentName(applicationContext, NotifWidget::class.java))
        for (id in ids) NotifWidget.update(applicationContext, mgr, id)
    }
}
