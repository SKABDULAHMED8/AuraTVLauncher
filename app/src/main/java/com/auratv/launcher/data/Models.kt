package com.auratv.launcher.data

/** One launchable entry. Light metadata only: no bitmaps are kept here. */
data class AppEntry(
    /** "package/class": unique per launcher activity, so apps with several entries show all of them. */
    val key: String,
    val packageName: String,
    val className: String,
    val label: String,
    /** True when the entry is a Leanback (TV) launcher activity. */
    val isTv: Boolean,
    val installTime: Long,
)

enum class SortMode(val id: String, val label: String) {
    AZ("az", "A to Z"),
    ZA("za", "Z to A"),
    NEWEST("newest", "Newest first");

    companion object {
        fun fromId(id: String?): SortMode = entries.firstOrNull { it.id == id } ?: AZ
    }
}

/** Names are stored as-is by v1.0.0 under the "theme" key, so they must not change. */
enum class ThemeMode(val label: String) {
    SYSTEM("Auto"),
    DARK("Dark"),
    LIGHT("Light");

    fun next(): ThemeMode = entries[(ordinal + 1) % entries.size]

    companion object {
        fun fromName(name: String?): ThemeMode = entries.firstOrNull { it.name == name } ?: DARK
    }
}

enum class AccentId(val id: String, val label: String) {
    CLAY("clay", "Clay"),
    CORAL("coral", "Coral"),
    SAGE("sage", "Sage"),
    SAND("sand", "Sand");

    companion object {
        fun fromId(id: String?): AccentId = entries.firstOrNull { it.id == id } ?: CLAY
    }
}

const val BUILTIN_WALLPAPER_COUNT = 6

sealed interface WallpaperChoice {
    data object Default : WallpaperChoice
    data class Builtin(val index: Int) : WallpaperChoice
    data object Custom : WallpaperChoice

    fun encode(): String = when (this) {
        Default -> "default"
        is Builtin -> "builtin:$index"
        Custom -> "custom"
    }

    companion object {
        fun decode(text: String?): WallpaperChoice = when {
            text == "custom" -> Custom
            text != null && text.startsWith("builtin:") -> {
                val i = text.removePrefix("builtin:").toIntOrNull()
                if (i != null && i in 0 until BUILTIN_WALLPAPER_COUNT) Builtin(i) else Default
            }
            else -> Default
        }
    }
}
