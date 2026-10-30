package com.auratv.launcher

import android.content.Context
import android.net.Uri

enum class ThemeMode(val label: String) {
    SYSTEM("Auto"),
    DARK("Dark"),
    LIGHT("Light");

    fun next(): ThemeMode = entries[(ordinal + 1) % entries.size]
}

/** Small local store. Preferences stay on the device. */
class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("aura_prefs", Context.MODE_PRIVATE)

    var themeMode: ThemeMode
        get() = runCatching { ThemeMode.valueOf(sp.getString("theme", ThemeMode.DARK.name)!!) }
            .getOrDefault(ThemeMode.DARK)
        set(value) { sp.edit().putString("theme", value.name).apply() }

    var pinned: List<String>
        get() = (sp.getString("pinned", "") ?: "").split(",").filter { it.isNotBlank() }
        set(value) { sp.edit().putString("pinned", value.joinToString(",")).apply() }

    var backgroundColor: String
        get() = sp.getString("background_color", "default") ?: "default"
        set(value) { sp.edit().putString("background_color", value).apply() }

    var wallpaperUri: String?
        get() = sp.getString("wallpaper_uri", null)
        set(value) { sp.edit().putString("wallpaper_uri", value).apply() }

    var builtInWallpaperId: String?
        get() = sp.getString("built_in_wallpaper_id", null)
        set(value) { sp.edit().putString("built_in_wallpaper_id", value).apply() }

    fun saveWallpaper(context: Context, uri: Uri) {
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
        wallpaperUri = uri.toString()
    }

    fun clearWallpaper(context: Context) {
        wallpaperUri?.let { saved ->
            runCatching {
                context.contentResolver.releasePersistableUriPermission(
                    Uri.parse(saved), android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
        }
        wallpaperUri = null
    }
}
