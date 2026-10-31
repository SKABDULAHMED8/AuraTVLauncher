package com.auratv.launcher.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.auratv.launcher.LauncherModel
import com.auratv.launcher.data.AppEntry
import com.auratv.launcher.data.ThemeMode

enum class Screen { Home, Settings, Wallpapers, Manage }

class AppActions(
    val launch: (AppEntry) -> Unit,
    val info: (AppEntry) -> Unit,
    val uninstall: (AppEntry) -> Unit,
    val systemSettings: () -> Unit,
    val pickImage: () -> Unit,
)

@Composable
fun AuraApp(model: LauncherModel, actions: AppActions, wallpaperMessage: String?) {
    val dark = when (model.theme) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    var screen by rememberSaveable { mutableStateOf(Screen.Home) }
    var menuApp by remember { mutableStateOf<AppEntry?>(null) }

    LaunchedEffect(Unit) { model.loadCustomWallpaper() }

    // Back never closes the launcher; from a sub-screen it returns one level.
    BackHandler(enabled = true) {
        screen = when (screen) {
            Screen.Wallpapers -> Screen.Settings
            Screen.Manage -> Screen.Settings
            else -> Screen.Home
        }
    }

    AuraTheme(dark, model.accent) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val dimens = remember(maxWidth, maxHeight) { dimensFor(maxWidth.value, maxHeight.value) }
            CompositionLocalProvider(LocalDimens provides dimens) {
                Backdrop(model.effectiveWallpaper, model.customWallpaper)
                when (screen) {
                    Screen.Home -> HomeScreen(
                        model = model,
                        onLaunch = actions.launch,
                        onLongPress = { menuApp = it },
                        onSettings = { screen = Screen.Settings },
                    )
                    Screen.Settings -> SettingsScreen(
                        model = model,
                        onBack = { screen = Screen.Home },
                        onWallpapers = { screen = Screen.Wallpapers },
                        onManageApps = { screen = Screen.Manage },
                        onSystemSettings = actions.systemSettings,
                    )
                    Screen.Wallpapers -> WallpaperScreen(
                        model = model,
                        message = wallpaperMessage,
                        onPickImage = actions.pickImage,
                        onBack = { screen = Screen.Settings },
                    )
                    Screen.Manage -> ManageAppsScreen(
                        model = model,
                        onBack = { screen = Screen.Settings },
                        onOpenMenu = { menuApp = it },
                    )
                }
                val app = menuApp
                if (app != null) {
                    val favPkgs = model.favorites.map { it.packageName }
                    val idx = favPkgs.indexOf(app.packageName)
                    AppMenuDialog(
                        app = app,
                        isFavorite = idx >= 0,
                        favoriteIndex = idx,
                        favoriteCount = favPkgs.size,
                        isHidden = app.packageName in model.hidden,
                        onDismiss = { menuApp = null },
                        onOpen = { actions.launch(app) },
                        onToggleFavorite = { model.togglePin(app.packageName) },
                        onMove = { model.movePin(app.packageName, it) },
                        onToggleHidden = { model.toggleHidden(app.packageName) },
                        onInfo = { actions.info(app) },
                        onUninstall = { actions.uninstall(app) },
                    )
                }
            }
        }
    }
}
