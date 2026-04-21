package com.mydev.widgetpro

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import org.json.JSONArray

class FolderRemoteService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent) =
        Factory(applicationContext, intent.getIntExtra("wid", -1))
}

class Factory(private val ctx: Context, private val wid: Int) : RemoteViewsService.RemoteViewsFactory {
    private val pkgs = mutableListOf<String>()
    private val labels = mutableListOf<String>()

    override fun onCreate() { load() }
    override fun onDataSetChanged() { load() }
    override fun onDestroy() { pkgs.clear(); labels.clear() }

    private fun load() {
        pkgs.clear(); labels.clear()
        try {
            val prefs = ctx.getSharedPreferences("wp_folder", Context.MODE_PRIVATE)
            val fJson = prefs.getString("folders", "[]") ?: "[]"
            val fidx = prefs.getInt("wid_$wid", 0)
            val folders = JSONArray(fJson)
            if (folders.length() > 0) {
                val idx = if (fidx < folders.length()) fidx else 0
                val apps = folders.getJSONObject(idx).optJSONArray("apps") ?: JSONArray()
                for (i in 0 until apps.length()) {
                    val pkg = apps.getString(i)
                    pkgs.add(pkg)
                    labels.add(try {
                        ctx.packageManager.getApplicationLabel(
                            ctx.packageManager.getApplicationInfo(pkg, 0)).toString()
                    } catch (e: Exception) { pkg })
                }
            }
        } catch (e: Exception) {}
    }

    override fun getCount() = pkgs.size
    override fun getLoadingView() = null
    override fun getViewTypeCount() = 1
    override fun getItemId(pos: Int) = pos.toLong()
    override fun hasStableIds() = true

    override fun getViewAt(pos: Int): RemoteViews {
        val rv = RemoteViews(ctx.packageName, R.layout.folder_grid_item)
        if (pos >= pkgs.size) return rv
        val pkg = pkgs[pos]
        try {
            rv.setImageViewBitmap(R.id.grid_icon,
                toBmp(ctx.packageManager.getApplicationIcon(pkg)))
            rv.setTextViewText(R.id.grid_label, labels[pos])
        } catch (e: Exception) {}
        rv.setOnClickFillInIntent(R.id.grid_cell, Intent().putExtra("pkg", pkg))
        return rv
    }

    private fun toBmp(d: Drawable): Bitmap {
        if (d is BitmapDrawable && d.bitmap != null) return d.bitmap
        val w = if (d.intrinsicWidth > 0) d.intrinsicWidth else 96
        val h = if (d.intrinsicHeight > 0) d.intrinsicHeight else 96
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        d.setBounds(0, 0, w, h); d.draw(Canvas(bmp)); return bmp
    }
}
