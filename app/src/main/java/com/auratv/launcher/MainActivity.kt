package com.auratv.launcher

import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    private val reloadTick = mutableIntStateOf(0)

    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            reloadTick.intValue++
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = Prefs(this)

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }
        ContextCompat.registerReceiver(this, packageReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)

        setContent {
            var themeMode by remember { mutableStateOf(prefs.themeMode) }
            var pinned by remember { mutableStateOf(prefs.pinned) }
            var backgroundColor by remember { mutableStateOf(prefs.backgroundColor) }
            var wallpaperUri by remember { mutableStateOf(prefs.wallpaperUri) }

            val apps by produceState(emptyList<AppItem>(), reloadTick.intValue) {
                value = withContext(Dispatchers.Default) { AppRepository.load(applicationContext) }
            }

            val dark = when (themeMode) {
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            // A launcher should never close when Back is pressed.
            BackHandler(enabled = true) { }

            CompositionLocalProvider(LocalPalette provides if (dark) DarkPalette else LightPalette) {
                HomeScreen(
                    apps = apps,
                    pinnedPackages = pinned,
                    themeMode = themeMode,
                    onCycleTheme = {
                        themeMode = themeMode.next()
                        prefs.themeMode = themeMode
                    },
                    onTogglePin = { pkg ->
                        pinned = if (pkg in pinned) pinned - pkg else pinned + pkg
                        prefs.pinned = pinned
                    },
                    onLaunch = { app -> safeStart(app.launch) },
                    onAppInfo = { app ->
                        safeStart(
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${app.packageName}"))
                        )
                    },
                    onUninstall = { app ->
                        safeStart(Intent(Intent.ACTION_DELETE, Uri.parse("package:${app.packageName}")))
                    },
                    onSystem = { action -> safeStart(action.createIntent(this@MainActivity)) },
                    backgroundColor = backgroundColor,
                    wallpaperUri = wallpaperUri,
                    onWallpaperSelected = { uri -> prefs.saveWallpaper(this@MainActivity, uri); wallpaperUri = uri.toString() },
                    onClearWallpaper = { prefs.clearWallpaper(this@MainActivity); wallpaperUri = null },
                    onCycleBackground = {
                        backgroundColor = when (backgroundColor) {
                            "default" -> "ocean"
                            "ocean" -> "forest"
                            "forest" -> "sunset"
                            else -> "default"
                        }
                        prefs.backgroundColor = backgroundColor
                    },
                    onSelectBackground = { preset ->
                        backgroundColor = preset
                        prefs.backgroundColor = preset
                    },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        reloadTick.intValue++
    }

    override fun onDestroy() {
        runCatching { unregisterReceiver(packageReceiver) }
        super.onDestroy()
    }

    private fun safeStart(intent: Intent) {
        try {
            startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: ActivityNotFoundException) {
            // This TV build does not ship that screen; ignore.
        } catch (_: SecurityException) {
        }
    }
}
