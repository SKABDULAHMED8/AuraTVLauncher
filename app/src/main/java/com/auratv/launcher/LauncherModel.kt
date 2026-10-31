package com.auratv.launcher

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import com.auratv.launcher.data.AccentId
import com.auratv.launcher.data.AppCatalog
import com.auratv.launcher.data.AppEntry
import com.auratv.launcher.data.HomeLogic
import com.auratv.launcher.data.ImageLoader
import com.auratv.launcher.data.SettingsStore
import com.auratv.launcher.data.SharedPrefsStore
import com.auratv.launcher.data.SortMode
import com.auratv.launcher.data.ThemeMode
import com.auratv.launcher.data.WallpaperChoice
import com.auratv.launcher.data.WallpaperFiles
import com.auratv.launcher.data.WallpaperRules
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Holds launcher state for the Compose UI. Settings persist through [SettingsStore]. */
class LauncherModel(context: Context) {
    private val app = context.applicationContext
    private val store = SettingsStore(SharedPrefsStore(app.getSharedPreferences("aura_prefs", Context.MODE_PRIVATE)))

    val loader = ImageLoader(app)

    var apps by mutableStateOf(AppCatalog.loadCache(app))
        private set
    var theme by mutableStateOf(store.theme)
        private set
    var accent by mutableStateOf(store.accent)
        private set
    var sort by mutableStateOf(store.sort)
        private set
    var pinned by mutableStateOf(store.pinned)
        private set
    var hidden by mutableStateOf(store.hidden)
        private set
    var wallpaper by mutableStateOf(store.wallpaper)
        private set
    var customWallpaper by mutableStateOf<ImageBitmap?>(null)
        private set

    val effectiveWallpaper: WallpaperChoice
        get() = WallpaperRules.effective(wallpaper, customWallpaper != null)

    /** Apps shown on Home: not hidden, in the chosen order. */
    val visibleApps: List<AppEntry>
        get() = HomeLogic.sort(HomeLogic.visible(apps, hidden), sort)

    val favorites: List<AppEntry>
        get() = HomeLogic.favorites(HomeLogic.sort(HomeLogic.visible(apps, hidden), SortMode.AZ), pinned)

    suspend fun refreshApps() {
        val scanned = withContext(Dispatchers.Default) { AppCatalog.scan(app) }
        if (scanned != apps) {
            apps = scanned
            withContext(Dispatchers.IO) { AppCatalog.saveCache(app, scanned) }
        }
    }

    suspend fun loadCustomWallpaper() {
        if (wallpaper !is WallpaperChoice.Custom) return
        customWallpaper = withContext(Dispatchers.IO) { WallpaperFiles.load(WallpaperFiles.current(app)) }
    }

    fun cycleTheme() { theme = theme.next(); store.theme = theme }
    fun setTheme(mode: ThemeMode) { theme = mode; store.theme = mode }
    fun cycleAccent() { accent = AccentId.entries[(accent.ordinal + 1) % AccentId.entries.size]; store.accent = accent }
    fun cycleSort() { sort = SortMode.entries[(sort.ordinal + 1) % SortMode.entries.size]; store.sort = sort }

    fun togglePin(pkg: String) {
        pinned = HomeLogic.togglePin(pinned, favorites.map { it.packageName }, pkg)
        store.pinned = pinned
    }

    fun movePin(pkg: String, delta: Int) {
        pinned = HomeLogic.movePin(pinned, favorites.map { it.packageName }, pkg, delta)
        store.pinned = pinned
    }

    fun toggleHidden(pkg: String) {
        hidden = HomeLogic.toggleHidden(hidden, pkg)
        store.hidden = hidden
    }

    fun resetLayout() {
        store.resetLayout()
        pinned = store.pinned
        hidden = store.hidden
        sort = store.sort
    }

    fun chooseBuiltinWallpaper(choice: WallpaperChoice) {
        wallpaper = choice
        store.wallpaper = choice
    }

    /** Imports a picked image. The previous wallpaper stays in force on any failure. */
    suspend fun importWallpaper(uri: Uri): Boolean {
        val ok = withContext(Dispatchers.IO) {
            WallpaperFiles.importImage(app, uri, WallpaperFiles.pending(app)) &&
                WallpaperRules.apply(store, WallpaperChoice.Custom) { WallpaperFiles.promotePending(app) }
        }
        if (!ok) return false
        customWallpaper = withContext(Dispatchers.IO) { WallpaperFiles.load(WallpaperFiles.current(app)) }
        wallpaper = WallpaperChoice.Custom
        return true
    }
}
