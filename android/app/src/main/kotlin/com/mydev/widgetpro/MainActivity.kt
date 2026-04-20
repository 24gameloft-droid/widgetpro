package com.mydev.widgetpro

import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import android.os.Bundle
import android.content.Intent
import android.provider.Settings
import android.content.ComponentName
import android.appwidget.AppWidgetManager
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject

class MainActivity : FlutterActivity() {
    private val CH = "com.mydev.widgetpro/ch"
    private val PICK = 2001
    private var pickWid = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!isNLEnabled()) startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
    }

    override fun configureFlutterEngine(fe: FlutterEngine) {
        super.configureFlutterEngine(fe)
        val np = getSharedPreferences("wp_notif", MODE_PRIVATE)
        val fp = getSharedPreferences("wp_folder", MODE_PRIVATE)
        val pp = getSharedPreferences("wp_photo", MODE_PRIVATE)

        MethodChannel(fe.dartExecutor.binaryMessenger, CH).setMethodCallHandler { call, result ->
            when (call.method) {
                "load" -> {
                    val pm = packageManager
                    val appList = mutableListOf<Pair<String,String>>()
                    val i = Intent(Intent.ACTION_MAIN, null).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
                    for (ri in pm.queryIntentActivities(i, 0)) {
                        try { val info = ri.activityInfo.applicationInfo; appList.add(pm.getApplicationLabel(info).toString() to info.packageName) } catch (e: Exception) {}
                    }
                    appList.sortBy { it.first }
                    val apps = JSONArray()
                    for ((n, p) in appList) apps.put(JSONObject().apply { put("n", n); put("p", p) })
                    val d = JSONObject()
                    d.put("apps", apps)
                    d.put("notifs", JSONArray(np.getString("list", "[]")))
                    d.put("allowed", JSONArray(np.getStringSet("allowed", emptySet())?.toList() ?: emptyList<String>()))
                    d.put("nr", np.getInt("r", 30)); d.put("ng", np.getInt("g", 30)); d.put("nb", np.getInt("b", 46)); d.put("na", np.getInt("alpha", 200))
                    d.put("folders", JSONArray(fp.getString("folders", "[]")))
                    d.put("fr", fp.getInt("r", 26)); d.put("fg", fp.getInt("g", 26)); d.put("fb", fp.getInt("b", 46)); d.put("fa", fp.getInt("alpha", 200))
                    d.put("firstLaunch", np.getBoolean("first", true))
                    result.success(d.toString())
                }
                "saveNotif" -> {
                    val allowed = call.argument<List<String>>("allowed") ?: emptyList()
                    val a = call.argument<Int>("a") ?: 200
                    val r = call.argument<Int>("r") ?: 30
                    val g = call.argument<Int>("g") ?: 30
                    val b = call.argument<Int>("b") ?: 46
                    np.edit().putStringSet("allowed", allowed.toSet()).putInt("alpha",a).putInt("r",r).putInt("g",g).putInt("b",b).putBoolean("first",false).apply()
                    val mgr = AppWidgetManager.getInstance(this)
                    mgr.getAppWidgetIds(ComponentName(this, NotifWidget::class.java)).forEach { NotifWidget.update(this, mgr, it) }
                    result.success(true)
                }
                "saveFolder" -> {
                    val json = call.argument<String>("folders") ?: "[]"
                    val a = call.argument<Int>("a") ?: 200
                    val r = call.argument<Int>("r") ?: 26
                    val g = call.argument<Int>("g") ?: 26
                    val b = call.argument<Int>("b") ?: 46
                    fp.edit().putString("folders",json).putInt("alpha",a).putInt("r",r).putInt("g",g).putInt("b",b).apply()
                    val mgr = AppWidgetManager.getInstance(this)
                    mgr.getAppWidgetIds(ComponentName(this, FolderWidget::class.java)).forEach { FolderWidget.update(this, mgr, it) }
                    result.success(true)
                }
                "pickPhotos" -> {
                    pickWid = call.argument<Int>("wid") ?: 0
                    val i = Intent(Intent.ACTION_OPEN_DOCUMENT).apply { type = "image/*"; putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true); addCategory(Intent.CATEGORY_OPENABLE) }
                    startActivityForResult(i, PICK)
                    result.success(true)
                }
                "pinNotif" -> { pin(NotifWidget::class.java); result.success(true) }
                "pinFolder" -> { pin(FolderWidget::class.java); result.success(true) }
                "pinPhoto" -> { pin(PhotoWidget::class.java); result.success(true) }
                else -> result.notImplemented()
            }
        }
    }

    private fun <T> pin(cls: Class<T>) {
        val mgr = AppWidgetManager.getInstance(this)
        if (mgr.isRequestPinAppWidgetSupported) mgr.requestPinAppWidget(ComponentName(this, cls), null, null)
    }

    override fun onActivityResult(req: Int, res: Int, data: Intent?) {
        super.onActivityResult(req, res, data)
        if (req == PICK && res == RESULT_OK && data != null) {
            val pp = getSharedPreferences("wp_photo", MODE_PRIVATE)
            val uris = JSONArray()
            val clip = data.clipData
            if (clip != null) for (i in 0 until clip.itemCount) { val u = clip.getItemAt(i).uri; contentResolver.takePersistableUriPermission(u, Intent.FLAG_GRANT_READ_URI_PERMISSION); uris.put(u.toString()) }
            else data.data?.let { u -> contentResolver.takePersistableUriPermission(u, Intent.FLAG_GRANT_READ_URI_PERMISSION); uris.put(u.toString()) }
            pp.edit().putString("photos_$pickWid", uris.toString()).putInt("idx_$pickWid", 0).apply()
            val mgr = AppWidgetManager.getInstance(this)
            PhotoWidget.update(this, mgr, pickWid)
            PhotoWidget.schedule(this, pickWid)
        }
    }

    private fun isNLEnabled() = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")?.contains(packageName) == true
}
