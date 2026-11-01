package com.auratv.launcher.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.File
import java.io.FileOutputStream

/** Custom wallpaper files. Images are copied into app storage at screen size, so a removed USB
 *  drive or a revoked permission can never break the wallpaper later. */
object WallpaperFiles {
    private const val MAX_W = 1280
    private const val MAX_H = 720

    fun current(context: Context) = File(context.filesDir, "wallpaper.jpg")
    fun pending(context: Context) = File(context.filesDir, "wallpaper_pending.jpg")

    /** Copies and shrinks [uri] into [dest]. Returns false on any failure and leaves [dest] unchanged. */
    fun importImage(context: Context, uri: Uri, dest: File): Boolean {
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
                ?: return false
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return false

            var sample = 1
            while (bounds.outWidth / (sample * 2) >= MAX_W && bounds.outHeight / (sample * 2) >= MAX_H) sample *= 2
            val opts = BitmapFactory.Options().apply {
                inSampleSize = sample
                inPreferredConfig = Bitmap.Config.RGB_565
            }
            val decoded = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
                ?: return false

            val ratio = minOf(MAX_W.toFloat() / decoded.width, MAX_H.toFloat() / decoded.height, 1f)
            val scaled = if (ratio < 1f) {
                Bitmap.createScaledBitmap(decoded, (decoded.width * ratio).toInt().coerceAtLeast(1), (decoded.height * ratio).toInt().coerceAtLeast(1), true)
            } else {
                decoded
            }

            val tmp = File(dest.parentFile, dest.name + ".tmp")
            FileOutputStream(tmp).use { out -> scaled.compress(Bitmap.CompressFormat.JPEG, 88, out) }
            if (scaled !== decoded) decoded.recycle()
            scaled.recycle()

            if (!tmp.renameTo(dest)) {
                tmp.delete()
                return false
            }
            true
        } catch (_: Throwable) {
            false
        }
    }

    /** Moves the pending image into place. Returns false if there is no pending image. */
    fun promotePending(context: Context): Boolean {
        val p = pending(context)
        if (!p.exists()) return false
        val c = current(context)
        return try {
            if (c.exists()) c.delete()
            p.renameTo(c)
        } catch (_: Exception) {
            false
        }
    }

    fun load(file: File): ImageBitmap? {
        if (!file.exists()) return null
        return try {
            val opts = BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.RGB_565 }
            BitmapFactory.decodeFile(file.absolutePath, opts)?.asImageBitmap()
        } catch (_: Throwable) {
            null
        }
    }
}
