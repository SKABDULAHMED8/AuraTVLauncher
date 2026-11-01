package com.auratv.launcher.data

import java.util.Locale

/** Pure list logic for the home screen. No Android classes, so it is unit tested on the JVM. */
object HomeLogic {

    private fun lower(a: AppEntry) = a.label.lowercase(Locale.ROOT)

    fun sort(apps: List<AppEntry>, mode: SortMode): List<AppEntry> = when (mode) {
        SortMode.AZ -> apps.sortedWith(compareBy<AppEntry>({ lower(it) }, { it.key }))
        SortMode.ZA -> apps.sortedWith(compareByDescending<AppEntry> { lower(it) }.thenBy { it.key })
        SortMode.NEWEST -> apps.sortedWith(
            compareByDescending<AppEntry> { it.installTime }.thenBy { lower(it) }.thenBy { it.key },
        )
    }

    fun visible(apps: List<AppEntry>, hidden: Set<String>): List<AppEntry> =
        apps.filter { it.packageName !in hidden }

    /**
     * Favorites shown in the dock. Pinned apps keep their saved order. If nothing usable is pinned,
     * the first few TV apps are shown, so the dock is never empty on a fresh install.
     */
    fun favorites(visibleApps: List<AppEntry>, pinned: List<String>, defaultCount: Int = 6): List<AppEntry> {
        val picked = pinned.mapNotNull { pkg ->
            visibleApps.firstOrNull { it.packageName == pkg && it.isTv }
                ?: visibleApps.firstOrNull { it.packageName == pkg }
        }
        if (picked.isNotEmpty()) return picked
        return visibleApps.filter { it.isTv }.ifEmpty { visibleApps }.take(defaultCount)
    }

    /**
     * Toggle a pin. While nothing is saved yet the dock shows the default favorites, so the first
     * change starts from those, not from an empty list.
     */
    fun togglePin(pinned: List<String>, effective: List<String>, pkg: String): List<String> {
        val base = if (pinned.isEmpty()) effective else pinned
        return if (pkg in base) base - pkg else base + pkg
    }

    fun movePin(pinned: List<String>, effective: List<String>, pkg: String, delta: Int): List<String> {
        val base = (if (pinned.isEmpty()) effective else pinned).toMutableList()
        val from = base.indexOf(pkg)
        if (from < 0) return base
        val to = (from + delta).coerceIn(0, base.lastIndex)
        if (to == from) return base
        base.removeAt(from)
        base.add(to, pkg)
        return base
    }

    fun toggleHidden(hidden: Set<String>, pkg: String): Set<String> =
        if (pkg in hidden) hidden - pkg else hidden + pkg
}

/** Rules for applying a wallpaper without ever losing the previous one on failure. */
object WallpaperRules {

    /** A custom image that is missing or unreadable falls back to the default wallpaper. */
    fun effective(choice: WallpaperChoice, customAvailable: Boolean): WallpaperChoice =
        if (choice is WallpaperChoice.Custom && !customAvailable) WallpaperChoice.Default else choice

    /**
     * Applies [requested]. For a custom image the pending file is promoted first; if that fails
     * nothing is written and the previous choice stays in force.
     */
    fun apply(store: SettingsStore, requested: WallpaperChoice, promotePending: () -> Boolean): Boolean {
        if (requested is WallpaperChoice.Custom && !promotePending()) return false
        store.wallpaper = requested
        return true
    }
}

/** Plain-text format for the saved app list, so the home screen can appear instantly at start. */
object AppCacheCodec {
    private const val HEADER = "aura-apps-v1"

    private fun clean(s: String) = s.replace('\t', ' ').replace('\n', ' ').replace('\r', ' ')

    fun encode(apps: List<AppEntry>): String = buildString {
        append(HEADER).append('\n')
        for (a in apps) {
            append(clean(a.key)).append('\t')
            append(clean(a.packageName)).append('\t')
            append(clean(a.className)).append('\t')
            append(clean(a.label)).append('\t')
            append(if (a.isTv) "1" else "0").append('\t')
            append(a.installTime).append('\n')
        }
    }

    fun decode(text: String?): List<AppEntry> {
        if (text.isNullOrEmpty()) return emptyList()
        val lines = text.split('\n')
        if (lines.firstOrNull() != HEADER) return emptyList()
        return lines.drop(1).mapNotNull { line ->
            if (line.isBlank()) return@mapNotNull null
            val f = line.split('\t')
            if (f.size != 6) return@mapNotNull null
            val time = f[5].toLongOrNull() ?: return@mapNotNull null
            if (f[0].isEmpty() || f[1].isEmpty() || f[2].isEmpty()) return@mapNotNull null
            AppEntry(key = f[0], packageName = f[1], className = f[2], label = f[3], isTv = f[4] == "1", installTime = time)
        }
    }
}
