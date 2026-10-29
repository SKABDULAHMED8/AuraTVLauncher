package com.auratv.launcher

import android.content.Context

enum class ThemeMode(val label: String) {
    SYSTEM("Auto"),
    DARK("Dark"),
    LIGHT("Light");

    fun next(): ThemeMode = entries[(ordinal + 1) % entries.size]
}

/** Tiny local store: theme choice and pinned (favorite) apps. Nothing leaves the device. */
class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("aura_prefs", Context.MODE_PRIVATE)

    var themeMode: ThemeMode
        get() = runCatching { ThemeMode.valueOf(sp.getString("theme", ThemeMode.DARK.name)!!) }
            .getOrDefault(ThemeMode.DARK)
        set(value) {
            sp.edit().putString("theme", value.name).apply()
        }

    var pinned: List<String>
        get() = (sp.getString("pinned", "") ?: "").split(",").filter { it.isNotBlank() }
        set(value) {
            sp.edit().putString("pinned", value.joinToString(",")).apply()
        }
}
