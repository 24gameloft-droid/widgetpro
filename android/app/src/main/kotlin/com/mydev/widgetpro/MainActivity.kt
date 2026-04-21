package com.mydev.widgetpro

import android.Manifest
import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import org.json.JSONArray
import org.json.JSONObject

class MainActivity : FlutterActivity() {
    private val CH = "com.mydev.widgetpro/ch"
    private var pickWid = 0
    private var flutterResult: MethodChannel.Result? = null
    private val PICK = 3001
    private val PERM = 3002

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!isNLEnabled()) startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        requestMediaPermission()
    }

    private fun requestMediaPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_MEDIA_IMAGES), PERM)
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), PERM)
            }
        }
    }

    override fun configureFlutterEngine(fe: FlutterEngine) {
        super.configureFlutterEngine(fe)
        val np = getSharedPreferences("wp_notif", Context.MODE_PRIVATE)
        val fp = getSharedPreferences("wp_folder", Context.MODE_PRIVATE)
        val pp = getSharedPreferences("wp_photo", Context.MODE_PRIVATE)

        MethodChannel(fe.dartExecutor.binaryMessenger, CH).setMethodCallHandler { call, result ->
            when (call.method) {
                "load" -> {
                    val pm = packageManager
                    val appList = mutableListOf<Pair<String, String>>()
                    val i = Intent(Intent.ACTION_MAIN, null).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
                    for (ri in pm.queryIntentActivities(i, 0)) {
                        try {
                            val info = ri.activityInfo.applicationInfo
                            appList.add(pm.getApplicationLabel(info).toString() to info.packageName)
                        } catch (e: Exception) {}
                    }
                    appList.sortBy { it.first }
                    val apps = JSONArray()
                    for ((n, p) in appList) apps.put(JSONObject().apply { put("n", n); put("p", p) })

                    // جلب الألبومات
                    val albums = JSONArray()
                    try {
                        val projection = arrayOf(
                            MediaStore.Images.Media.BUCKET_ID,
                            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
                            MediaStore.Images.Media._ID
                        )
                        val cursor = contentResolver.query(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            projection, null, null,
                            "${MediaStore.Images.Media.DATE_ADDED} DESC"
                        )
                        val seen = mutableSetOf<String>()
                        cursor?.use {
                            val bucketIdCol = it.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_ID)
                            val bucketNameCol = it.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
                            val idCol = it.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                            while (it.moveToNext()) {
                                val bucketId = it.getString(bucketIdCol) ?: continue
                                if (!seen.contains(bucketId)) {
                                    seen.add(bucketId)
                                    val name = it.getString(bucketNameCol) ?: "Unknown"
                                    val imgId = it.getLong(idCol)
                                    albums.put(JSONObject().apply {
                                        put("id", bucketId)
                                        put("name", name)
                                        put("count", 0)
                                        put("thumb", Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, imgId.toString()).toString())
                                    })
                                }
                            }
                        }
                    } catch (e: Exception) {}

                    val d = JSONObject()
                    d.put("apps", apps)
                    d.put("notifs", JSONArray(np.getString("list", "[]")))
                    d.put("allowed", JSONArray(np.getStringSet("allowed", emptySet())?.toList() ?: emptyList<String>()))
                    d.put("nr", np.getInt("r", 30)); d.put("ng", np.getInt("g", 30))
                    d.put("nb", np.getInt("b", 46)); d.put("na", np.getInt("alpha", 200))
                    d.put("folders", JSONArray(fp.getString("folders", "[]")))
                    d.put("fr", fp.getInt("r", 26)); d.put("fg", fp.getInt("g", 26))
                    d.put("fb", fp.getInt("b", 46)); d.put("fa", fp.getInt("alpha", 200))
                    d.put("firstLaunch", np.getBoolean("first", true))
                    d.put("albums", albums)
                    result.success(d.toString())
                }
                "saveNotif" -> {
                    val allowed = call.argument<List<String>>("allowed") ?: emptyList()
                    val a = call.argument<Int>("a") ?: 200
                    val r = call.argument<Int>("r") ?: 30
                    val g = call.argument<Int>("g") ?: 30
                    val b = call.argument<Int>("b") ?: 46
                    np.edit().putStringSet("allowed", allowed.toSet())
                        .putInt("alpha", a).putInt("r", r).putInt("g", g).putInt("b", b)
                        .putBoolean("first", false).apply()
                    val mgr = AppWidgetManager.getInstance(this)
                    mgr.getAppWidgetIds(ComponentName(this, NotifWidget::class.java))
                        .forEach { NotifWidget.update(this, mgr, it) }
                    result.success(true)
                }
                "saveFolder" -> {
                    val json = call.argument<String>("folders") ?: "[]"
                    val a = call.argument<Int>("a") ?: 200; val r = call.argument<Int>("r") ?: 26
                    val g = call.argument<Int>("g") ?: 26; val b = call.argument<Int>("b") ?: 46
                    fp.edit().putString("folders", json)
                        .putInt("alpha", a).putInt("r", r).putInt("g", g).putInt("b", b).apply()
                    val mgr = AppWidgetManager.getInstance(this)
                    mgr.getAppWidgetIds(ComponentName(this, FolderWidget::class.java))
                        .forEach { FolderWidget.update(this, mgr, it) }
                    result.success(true)
                }
                "selectAlbum" -> {
                    pickWid = call.argument<Int>("wid") ?: 0
                    val bucketId = call.argument<String>("bucketId") ?: ""
                    flutterResult = result
                    try {
                        val uris = JSONArray()
                        val cursor = contentResolver.query(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            arrayOf(MediaStore.Images.Media._ID),
                            "${MediaStore.Images.Media.BUCKET_ID} = ?",
                            arrayOf(bucketId),
                            "${MediaStore.Images.Media.DATE_ADDED} DESC"
                        )
                        cursor?.use {
                            val idCol = it.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                            while (it.moveToNext()) {
                                val imgId = it.getLong(idCol)
                                val uri = Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, imgId.toString())
                                uris.put(uri.toString())
                            }
                        }
                        pp.edit().putString("photos_$pickWid", uris.toString()).putInt("idx_$pickWid", 0).apply()
                        val mgr = AppWidgetManager.getInstance(this)
                        PhotoWidget.update(this, mgr, pickWid)
                        PhotoWidget.schedule(this, pickWid)
                        result.success(uris.length())
                        flutterResult = null
                    } catch (e: Exception) {
                        result.error("ERROR", e.message, null)
                        flutterResult = null
                    }
                }
                "pinNotif" -> { pin(NotifWidget::class.java); result.success(true) }
                "pinFolder" -> { pin(FolderWidget::class.java); result.success(true) }
                "pinPhoto" -> { pin(PhotoWidget::class.java); result.success(true) }
                "refreshPermission" -> { requestMediaPermission(); result.success(true) }
                else -> result.notImplemented()
            }
        }
    }

    private fun <T> pin(cls: Class<T>) {
        val mgr = AppWidgetManager.getInstance(this)
        if (mgr.isRequestPinAppWidgetSupported)
            mgr.requestPinAppWidget(ComponentName(this, cls), null, null)
    }

    private fun isNLEnabled() = Settings.Secure.getString(
        contentResolver, "enabled_notification_listeners")?.contains(packageName) == true
}
