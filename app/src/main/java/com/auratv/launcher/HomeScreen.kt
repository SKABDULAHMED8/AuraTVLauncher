package com.auratv.launcher

import android.content.Intent
import android.provider.Settings
import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay

enum class SystemAction(val label: String, val intentAction: String) {
    SETTINGS("Settings", Settings.ACTION_SETTINGS),
    WIFI("Wi-Fi", Settings.ACTION_WIFI_SETTINGS),
    BLUETOOTH("Bluetooth", Settings.ACTION_BLUETOOTH_SETTINGS),
    DISPLAY("Display", Settings.ACTION_DISPLAY_SETTINGS),
}

@Composable
fun HomeScreen(
    apps: List<AppItem>,
    pinnedPackages: List<String>,
    themeMode: ThemeMode,
    onCycleTheme: () -> Unit,
    onTogglePin: (String) -> Unit,
    onLaunch: (AppItem) -> Unit,
    onAppInfo: (AppItem) -> Unit,
    onUninstall: (AppItem) -> Unit,
    onSystem: (SystemAction) -> Unit,
    backgroundColor: String = "default",
    onCycleBackground: () -> Unit = {},
) {
    val p = LocalPalette.current
    val context = LocalContext.current
    var menuApp by remember { mutableStateOf<AppItem?>(null) }
    var showSettings by remember { mutableStateOf(false) }
    val firstFocus = remember { FocusRequester() }

    val pinnedApps = remember(apps, pinnedPackages) {
        pinnedPackages.mapNotNull { pkg -> apps.firstOrNull { it.packageName == pkg } }
    }
    val pinnedSet = remember(pinnedPackages) { pinnedPackages.toSet() }

    LaunchedEffect(apps.isNotEmpty()) {
        if (apps.isNotEmpty()) {
            delay(200)
            runCatching { firstFocus.requestFocus() }
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(backgroundBrushColors(p, backgroundColor)))
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(p.glowA, androidx.compose.ui.graphics.Color.Transparent),
                        center = Offset(size.width * 0.15f, size.height * 0.1f),
                        radius = size.width * 0.5f,
                    ),
                    radius = size.width * 0.5f,
                    center = Offset(size.width * 0.15f, size.height * 0.1f),
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(p.glowB, androidx.compose.ui.graphics.Color.Transparent),
                        center = Offset(size.width * 0.9f, size.height * 0.95f),
                        radius = size.width * 0.45f,
                    ),
                    radius = size.width * 0.45f,
                    center = Offset(size.width * 0.9f, size.height * 0.95f),
                )
            },
    ) {
        Column(Modifier.fillMaxSize()) {
            TopBar(themeMode = themeMode, onCycleTheme = onCycleTheme, onOpenSettings = { showSettings = true })

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 48.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (pinnedApps.isNotEmpty()) {
                    item {
                        Section("Favorites") {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 56.dp, vertical = 24.dp),
                                horizontalArrangement = Arrangement.spacedBy(28.dp),
                            ) {
                                items(pinnedApps.size) { i ->
                                    val app = pinnedApps[i]
                                    AppTile(
                                        app = app,
                                        width = 300.dp,
                                        height = 169.dp,
                                        modifier = if (i == 0) Modifier.focusRequester(firstFocus) else Modifier,
                                        onClick = { onLaunch(app) },
                                        onLongClick = { menuApp = app },
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Section("All Apps") {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 56.dp, vertical = 24.dp),
                            horizontalArrangement = Arrangement.spacedBy(24.dp),
                        ) {
                            items(apps.size) { i ->
                                val app = apps[i]
                                AppTile(
                                    app = app,
                                    width = 240.dp,
                                    height = 135.dp,
                                    modifier = if (i == 0 && pinnedApps.isEmpty()) {
                                        Modifier.focusRequester(firstFocus)
                                    } else Modifier,
                                    onClick = { onLaunch(app) },
                                    onLongClick = { menuApp = app },
                                )
                            }
                        }
                    }
                }

                item {
                    Section("System") {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 56.dp, vertical = 24.dp),
                            horizontalArrangement = Arrangement.spacedBy(24.dp),
                        ) {
                            items(SystemAction.entries.filter { action ->
                                runCatching { Intent(action.intentAction).resolveActivity(context.packageManager) != null }.getOrDefault(false)
                            }) { action ->
                                LabelTile(
                                    label = action.label,
                                    width = 200.dp,
                                    height = 112.dp,
                                    onClick = { onSystem(action) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSettings) {
        AlertDialog(
            onDismissRequest = { showSettings = false },
            title = { Text("Aura Launcher Settings") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Theme: ${themeMode.label}")
                    TextButton(onClick = onCycleTheme) { Text("Change theme") }
                    Text("Background")
                    TextButton(onClick = onCycleBackground) {
                        Text("Cycle background color ($backgroundColor)")
                    }
                    Text("Wallpaper selection and advanced TV controls are being added in a later step.")
                }
            },
            confirmButton = { TextButton(onClick = { showSettings = false }) { Text("Done") } },
        )
    }

    menuApp?.let { app ->
        AppMenuDialog(
            app = app,
            pinned = app.packageName in pinnedSet,
            onDismiss = { menuApp = null },
            onTogglePin = { onTogglePin(app.packageName) },
            onInfo = { onAppInfo(app) },
            onUninstall = { onUninstall(app) },
        )
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    val p = LocalPalette.current
    Column(Modifier.fillMaxWidth()) {
        Text(
            text = title,
            color = p.textDim,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 56.dp, top = 8.dp),
        )
        content()
    }
}

@Composable
private fun TopBar(themeMode: ThemeMode, onCycleTheme: () -> Unit, onOpenSettings: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 56.dp, end = 56.dp, top = 36.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Clock()
        MenuButton(
            text = "Theme: ${themeMode.label}",
            wide = false,
            onClick = onCycleTheme,
        )
    }
}

@Composable
private fun Clock() {
    val p = LocalPalette.current
    val context = LocalContext.current
    val now by produceState(System.currentTimeMillis()) {
        while (true) {
            value = System.currentTimeMillis()
            delay(5_000)
        }
    }
    val minute = now / 60_000
    val time = remember(minute) { DateFormat.getTimeFormat(context).format(Date(now)) }
    val date = remember(minute) { SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(Date(now)) }
    Column {
        Text(time, color = p.text, fontSize = 52.sp, fontWeight = FontWeight.Light)
        Text(date, color = p.textDim, fontSize = 18.sp)
    }
}


private fun backgroundBrushColors(p: Palette, choice: String): List<androidx.compose.ui.graphics.Color> = when (choice) {
    "ocean" -> listOf(androidx.compose.ui.graphics.Color(0xFF061A40), androidx.compose.ui.graphics.Color(0xFF0353A4))
    "forest" -> listOf(androidx.compose.ui.graphics.Color(0xFF081C15), androidx.compose.ui.graphics.Color(0xFF2D6A4F))
    "sunset" -> listOf(androidx.compose.ui.graphics.Color(0xFF3A0CA3), androidx.compose.ui.graphics.Color(0xFFF72585))
    else -> listOf(p.bgTop, p.bgBottom)
}
