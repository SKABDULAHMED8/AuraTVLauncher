package com.auratv.launcher.data

import android.content.SharedPreferences

/** Minimal key-value storage, so settings logic can be tested without Android. */
interface KeyValueStore {
    fun getString(key: String): String?
    fun putString(key: String, value: String?)
}

class SharedPrefsStore(private val prefs: SharedPreferences) : KeyValueStore {
    override fun getString(key: String): String? = prefs.getString(key, null)

    override fun putString(key: String, value: String?) {
        val editor = prefs.edit()
        if (value == null) editor.remove(key) else editor.putString(key, value)
        editor.apply()
    }
}

/**
 * Local settings. The file name ("aura_prefs") and the "theme" and "pinned" formats are exactly
 * what v1.0.0 wrote, so an update keeps the user's theme and favorites. New keys are additive.
 */
class SettingsStore(private val kv: KeyValueStore) {

    var theme: ThemeMode
        get() = ThemeMode.fromName(kv.getString("theme"))
        set(value) {
            kv.putString("theme", value.name)
        }

    /** Pinned package names, in dock order. */
    var pinned: List<String>
        get() = decodeList(kv.getString("pinned"))
        set(value) {
            kv.putString("pinned", value.joinToString(","))
        }

    /** Hidden package names. */
    var hidden: Set<String>
        get() = decodeList(kv.getString("hidden")).toSet()
        set(value) {
            kv.putString("hidden", value.sorted().joinToString(","))
        }

    var sort: SortMode
        get() = SortMode.fromId(kv.getString("sort"))
        set(value) {
            kv.putString("sort", value.id)
        }

    var accent: AccentId
        get() = AccentId.fromId(kv.getString("accent"))
        set(value) {
            kv.putString("accent", value.id)
        }

    var wallpaper: WallpaperChoice
        get() = WallpaperChoice.decode(kv.getString("wallpaper"))
        set(value) {
            kv.putString("wallpaper", value.encode())
        }

    /** Restores favorites, hidden apps and sorting. Theme, accent and wallpaper are kept. */
    fun resetLayout() {
        pinned = emptyList()
        hidden = emptySet()
        sort = SortMode.AZ
    }

    private fun decodeList(text: String?): List<String> =
        (text ?: "").split(",").map { it.trim() }.filter { it.isNotEmpty() }.distinct()
}
