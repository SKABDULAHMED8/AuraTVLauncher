package com.auratv.launcher

import android.content.Context
import android.media.AudioManager
import android.provider.Settings
import android.view.WindowManager

/**
 * Device controls used by Aura Settings.
 *
 * Volume is a media-stream value. Brightness is the current app-window value
 * unless the user grants WRITE_SETTINGS, in which case the system value can be
 * changed. This avoids silently claiming control over a TV display when Android
 * or the TV firmware does not expose it.
 */
object TvControls {
    data class Level(val current: Int, val maximum: Int) {
        val percent: Int get() = if (maximum <= 0) 0 else (current * 100 / maximum).coerceIn(0, 100)
    }

    fun volume(context: Context): Level {
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        return Level(
            audio.getStreamVolume(AudioManager.STREAM_MUSIC),
            audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1),
        )
    }

    fun setVolume(context: Context, percent: Int) {
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        val target = (percent.coerceIn(0, 100) * max / 100f).toInt().coerceIn(0, max)
        audio.setStreamVolume(AudioManager.STREAM_MUSIC, target, 0)
    }

    fun canWriteSystemSettings(context: Context): Boolean =
        Settings.System.canWrite(context)

    fun systemBrightness(context: Context): Int? {
        if (!canWriteSystemSettings(context)) return null
        return runCatching {
            Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS)
                .coerceIn(0, 255) * 100 / 255
        }.getOrNull()
    }

    fun setSystemBrightness(context: Context, percent: Int): Boolean {
        if (!canWriteSystemSettings(context)) return false
        return runCatching {
            Settings.System.putInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS,
                (percent.coerceIn(0, 100) * 255 / 100f).toInt(),
            )
        }.getOrDefault(false)
    }

    fun setWindowBrightness(window: android.view.Window, percent: Int) {
        val params = window.attributes
        params.screenBrightness = (percent.coerceIn(0, 100) / 100f).coerceAtLeast(0.01f)
        window.attributes = params
    }

    /**
     * There is no public Android API for generic HDMI/AV source switching.
     * This only reports whether known, OEM-provided TV input activities exist.
     * It never fabricates a successful switch.
     */
    fun availableInputIntents(context: Context): List<Pair<String, android.content.Intent>> {
        val candidates = listOf(
            "Live TV" to android.content.Intent("android.intent.action.VIEW").setClassName(
                "com.google.android.tv", "com.android.tv.MainActivity"
            ),
            "HDMI 1" to android.content.Intent("android.intent.action.VIEW").setClassName(
                "com.google.android.tv", "com.android.tv.TvActivity"
            ),
            "TV Inputs" to android.content.Intent("android.settings.TV_INPUT_SETTINGS"),
        )
        return candidates.filter { (_, intent) ->
            runCatching { intent.resolveActivity(context.packageManager) != null }.getOrDefault(false)
        }
    }
}
