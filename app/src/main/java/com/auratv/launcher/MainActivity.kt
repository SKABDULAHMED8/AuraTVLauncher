package com.auratv.launcher

import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.ComponentCallbacks2
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.auratv.launcher.data.AppCatalog
import com.auratv.launcher.ui.AppActions
import com.auratv.launcher.ui.AuraApp
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var model: LauncherModel
    private var wallpaperMessage by mutableStateOf<String?>(null)

    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            refresh()
        }
    }

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@registerForActivityResult
        lifecycleScope.launch {
            wallpaperMessage = if (model.importWallpaper(uri)) "Wallpaper applied." else "That image could not be used. Your previous wallpaper is unchanged."
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        model = LauncherModel(this)

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }
        ContextCompat.registerReceiver(this, packageReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)

        val actions = AppActions(
            launch = { app -> safeStart(AppCatalog.launchIntent(app)) },
            info = { app -> safeStart(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${app.packageName}"))) },
            uninstall = { app -> safeStart(Intent(Intent.ACTION_DELETE, Uri.parse("package:${app.packageName}"))) },
            systemSettings = { safeStart(Intent(Settings.ACTION_SETTINGS)) },
            pickImage = {
                try {
                    pickImage.launch("image/*")
                } catch (_: ActivityNotFoundException) {
                    wallpaperMessage = "This TV has no file picker for images. Choose a built-in wallpaper instead."
                }
            },
        )

        setContent { AuraApp(model, actions, wallpaperMessage) }
        refresh()
    }

    private fun refresh() {
        lifecycleScope.launch { model.refreshApps() }
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN) model.loader.evictAll()
    }

    override fun onDestroy() {
        runCatching { unregisterReceiver(packageReceiver) }
        super.onDestroy()
    }

    private fun safeStart(intent: Intent) {
        try {
            startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: ActivityNotFoundException) {
            // This TV build does not ship that screen.
        } catch (_: SecurityException) {
        }
    }
}
