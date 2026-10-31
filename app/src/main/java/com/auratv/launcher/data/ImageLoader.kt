package com.auratv.launcher.data

import android.app.ActivityManager
import android.content.ComponentName
import android.content.Context
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

class Art(val bitmap: ImageBitmap, val isBanner: Boolean) {
    val bytes: Int get() = bitmap.width * bitmap.height * 4
}

/**
 * Loads app artwork on demand at the size actually shown, into a size-capped cache.
 * The cap scales with the device's per-app memory class (3 to 8 MB), so low-memory TVs stay small.
 */
class ImageLoader(context: Context) {
    private val pm = context.packageManager
    private val gate = Semaphore(2)

    val capBytes: Int
    private val cache: LruCache<String, Art>

    init {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        capBytes = ((am.memoryClass * 1024L * 1024L) / 24L).toInt().coerceIn(3 * 1024 * 1024, 8 * 1024 * 1024)
        cache = object : LruCache<String, Art>(capBytes) {
            override fun sizeOf(key: String, value: Art): Int = value.bytes
        }
    }

    fun usedBytes(): Int = cache.size()

    fun evictAll() {
        cache.evictAll()
    }

    /** [preferBanner] true: use the TV banner when the app has one. Otherwise the icon is used. */
    suspend fun load(app: AppEntry, widthPx: Int, heightPx: Int, preferBanner: Boolean): Art? {
        if (widthPx <= 0 || heightPx <= 0) return null
        val key = "${app.key}|$widthPx|$heightPx|$preferBanner"
        cache.get(key)?.let { return it }
        return withContext(Dispatchers.IO) {
            gate.withPermit {
                cache.get(key) ?: decode(app, widthPx, heightPx, preferBanner)?.also { cache.put(key, it) }
            }
        }
    }

    private fun decode(app: AppEntry, widthPx: Int, heightPx: Int, preferBanner: Boolean): Art? {
        return try {
            val component = ComponentName(app.packageName, app.className)
            if (preferBanner && app.isTv) {
                val banner = runCatching { pm.getActivityBanner(component) }.getOrNull()
                    ?: runCatching { pm.getApplicationBanner(app.packageName) }.getOrNull()
                if (banner != null) {
                    return Art(banner.toBitmap(widthPx, heightPx).asImageBitmap(), true)
                }
            }
            val icon = runCatching { pm.getActivityIcon(component) }.getOrNull() ?: return null
            val size = (minOf(widthPx, heightPx) * 0.62f).toInt().coerceAtLeast(32)
            Art(icon.toBitmap(size, size).asImageBitmap(), false)
        } catch (_: Throwable) {
            null
        }
    }
}
