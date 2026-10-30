package com.auratv.launcher

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.Settings
import android.text.format.DateFormat
import android.view.Window
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

enum class SystemAction(
    val label: String,
    val intentAction: String,
    val dataUri: Uri? = null,
) {
    SETTINGS("Settings", Settings.ACTION_SETTINGS),
    WIFI("Wi-Fi", Settings.ACTION_WIFI_SETTINGS),
    BLUETOOTH("Bluetooth", Settings.ACTION_BLUETOOTH_SETTINGS),
    DISPLAY("Display", Settings.ACTION_DISPLAY_SETTINGS),
    BROWSER("Browser", Intent.ACTION_VIEW, Uri.parse("https://example.com")),
    YOUTUBE("YouTube", Intent.ACTION_MAIN);

    fun createIntent(context: Context): Intent = when (this) {
        BROWSER -> Intent(Intent.ACTION_VIEW, dataUri)
        YOUTUBE -> context.packageManager.getLeanbackLaunchIntentForPackage("com.google.android.youtube.tv")
            ?: context.packageManager.getLaunchIntentForPackage("com.google.android.youtube.tv")
            ?: Intent(Intent.ACTION_MAIN)
        else -> Intent(intentAction)
    }

    fun isAvailable(context: Context): Boolean = runCatching {
        createIntent(context).resolveActivity(context.packageManager) != null
    }.getOrDefault(false)
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
    onSelectBackground: (String) -> Unit = {},
    wallpaperUri: String? = null,
    onWallpaperSelected: (Uri) -> Unit = {},
    onClearWallpaper: () -> Unit = {},
) {
    val p = LocalPalette.current
    val context = LocalContext.current
    val view = LocalView.current
    val initialDeviceSnapshot = remember(context) { DeviceSnapshot("Network: Checking", "RAM: Checking", "Storage: Checking", "Input devices: Checking") }
    val deviceSnapshot by produceState(initialDeviceSnapshot) {
        while (true) {
            value = withContext(Dispatchers.IO) { DeviceStatus.read(context) }
            delay(5_000)
        }
    }
    var showSettings by remember { mutableStateOf(false) }
    var searchText by remember { mutableStateOf("") }
    var volumePercent by remember { mutableStateOf(TvControls.volume(context).percent.toFloat()) }
    var brightnessPercent by remember { mutableStateOf(70f) }
    val wallpaperPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) onWallpaperSelected(uri)
    }
    val wallpaperBitmap by produceState<Bitmap?>(null, wallpaperUri) {
        value = if (wallpaperUri.isNullOrBlank()) null else withContext(Dispatchers.IO) {
            runCatching {
                val resolver = context.contentResolver
                val uri = Uri.parse(wallpaperUri)
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
                if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching null
                var sample = 1
                while (bounds.outWidth / sample > 1920 || bounds.outHeight / sample > 1080) sample *= 2
                val options = BitmapFactory.Options().apply {
                    inSampleSize = sample
                    inPreferredConfig = Bitmap.Config.RGB_565
                }
                resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
            }.getOrNull()
        }
    }
    val firstFocus = remember { FocusRequester() }
    val visibleApps = remember(apps, searchText) {
        if (searchText.isBlank()) apps else apps.filter {
            it.label.contains(searchText.trim(), ignoreCase = true) ||
                it.packageName.contains(searchText.trim(), ignoreCase = true)
        }
    }
    val pinnedApps = remember(visibleApps, pinnedPackages) {
        pinnedPackages.mapNotNull { pkg -> visibleApps.firstOrNull { it.packageName == pkg } }
    }

    BackHandler(enabled = showSettings) { showSettings = false }

    LaunchedEffect(showSettings) {
        if (showSettings) {
            while (true) {
                volumePercent = TvControls.volume(context).percent.toFloat()
                delay(700)
            }
        }
    }

    LaunchedEffect(apps.isNotEmpty(), showSettings) {
        if (apps.isNotEmpty() && !showSettings) {
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
                        listOf(p.glowA, Color.Transparent),
                        center = Offset(size.width * 0.15f, size.height * 0.1f),
                        radius = size.width * 0.5f,
                    ),
                    radius = size.width * 0.5f,
                    center = Offset(size.width * 0.15f, size.height * 0.1f),
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(p.glowB, Color.Transparent),
                        center = Offset(size.width * 0.9f, size.height * 0.95f),
                        radius = size.width * 0.45f,
                    ),
                    radius = size.width * 0.45f,
                    center = Offset(size.width * 0.9f, size.height * 0.95f),
                )
            },
    ) {
        if (wallpaperBitmap != null) {
            Image(
                bitmap = wallpaperBitmap!!.asImageBitmap(),
                contentDescription = "Selected wallpaper",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
        if (showSettings) {
            SettingsPage(
                themeMode = themeMode,
                onCycleTheme = onCycleTheme,
                backgroundColor = backgroundColor,
                onCycleBackground = onCycleBackground,
                onSelectBackground = onSelectBackground,
                onChooseWallpaper = { wallpaperPicker.launch(arrayOf("image/*")) },
                onClearWallpaper = onClearWallpaper,
                hasCustomWallpaper = wallpaperUri != null,
                volumePercent = volumePercent,
                onVolumeChange = {
                    volumePercent = it
                    TvControls.setVolume(context, it.toInt())
                },
                brightnessPercent = brightnessPercent,
                onBrightnessChange = {
                    brightnessPercent = it
                    context.findWindow()?.let { window -> TvControls.setWindowBrightness(window, it.toInt()) }
                },
                onBack = { showSettings = false },
                onSystem = onSystem,
            )
        } else {
            Column(Modifier.fillMaxSize()) {
                TopBar(themeMode = themeMode, onCycleTheme = onCycleTheme, onOpenSettings = { showSettings = true })
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 56.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                ) {
                    Text(deviceSnapshot.wifi, color = p.textDim, fontSize = 14.sp)
                    Text(deviceSnapshot.memory, color = p.textDim, fontSize = 14.sp)
                    Text(deviceSnapshot.storage, color = p.textDim, fontSize = 14.sp)
                    Text(deviceSnapshot.inputDevices, color = p.textDim, fontSize = 14.sp)
                }
                OutlinedTextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 56.dp, vertical = 8.dp),
                    singleLine = true,
                    label = { Text("Search apps") },
                    placeholder = { Text("Search installed apps") },
                )
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
                                            onLongClick = { onTogglePin(app.packageName) },
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
                                items(visibleApps.size) { i ->
                                    val app = visibleApps[i]
                                    AppTile(
                                        app = app,
                                        width = 240.dp,
                                        height = 135.dp,
                                        modifier = if (i == 0 && pinnedApps.isEmpty()) Modifier.focusRequester(firstFocus) else Modifier,
                                        onClick = { onLaunch(app) },
                                        onLongClick = { onTogglePin(app.packageName) },
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
                                items(SystemAction.entries.filter { it.isAvailable(context) }) { action ->
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
    }
}

@Composable
private fun SettingsPage(
    themeMode: ThemeMode,
    onCycleTheme: () -> Unit,
    backgroundColor: String,
    onCycleBackground: () -> Unit,
    onSelectBackground: (String) -> Unit,
    onChooseWallpaper: () -> Unit,
    onClearWallpaper: () -> Unit,
    hasCustomWallpaper: Boolean,
    volumePercent: Float,
    onVolumeChange: (Float) -> Unit,
    brightnessPercent: Float,
    onBrightnessChange: (Float) -> Unit,
    onBack: () -> Unit,
    onSystem: (SystemAction) -> Unit,
) {
    val p = LocalPalette.current
    Column(Modifier.fillMaxSize().padding(horizontal = 56.dp, vertical = 28.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Aura Launcher", color = p.text, fontSize = 34.sp, fontWeight = FontWeight.Bold)
                Text("Settings", color = p.textDim, fontSize = 18.sp)
            }
            MenuButton("Done", wide = false, onClick = onBack)
        }
        Spacer(Modifier.height(18.dp))
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                SettingsPanel("Appearance") {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                        Text("Theme: ${themeMode.label}", color = p.text, fontSize = 18.sp)
                        MenuButton("Change theme", wide = false, onClick = onCycleTheme)
                    }
                    Text("Background collection", color = p.textDim, fontSize = 15.sp)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(listOf("default", "ocean", "forest", "sunset")) { preset ->
                            val selected = backgroundColor == preset
                            Box(
                                Modifier.width(150.dp).height(72.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Brush.linearGradient(backgroundBrushColors(p, preset)))
                                    .border(if (selected) 3.dp else 1.dp, if (selected) p.ring else Color.White.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                                    .clickable { onSelectBackground(preset) },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(preset.replaceFirstChar { it.uppercase() }, color = Color.White, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                    Text("Use the background button to cycle through the built-in colour collection.", color = p.textDim, fontSize = 13.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        MenuButton("Next background", wide = false, onClick = onCycleBackground)
                        MenuButton("Choose device / USB wallpaper", wide = false, onClick = onChooseWallpaper)
                        if (hasCustomWallpaper) MenuButton("Remove custom wallpaper", wide = false, danger = true, onClick = onClearWallpaper)
                    }
                }
            }
            item {
                SettingsPanel("Sound") {
                    Text("Media volume: ${volumePercent.toInt()}%", color = p.text, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                    Slider(value = volumePercent, onValueChange = onVolumeChange, valueRange = 0f..100f)
                    Text("The value refreshes while this page is open. Some TV firmware controls volume outside Android's media stream.", color = p.textDim, fontSize = 13.sp)
                }
            }
            item {
                SettingsPanel("Brightness") {
                    Text("App brightness: ${brightnessPercent.toInt()}%", color = p.text, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                    Slider(value = brightnessPercent, onValueChange = onBrightnessChange, valueRange = 1f..100f)
                    Text("This slider changes Aura Launcher only. System-wide TV brightness is controlled by the TV firmware and is not exposed on every device.", color = p.textDim, fontSize = 13.sp)
                }
            }
            item {
                SettingsPanel("TV shortcuts") {
                    Text("Only shortcuts supported by this TV are shown.", color = p.textDim, fontSize = 14.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        SystemAction.entries.filter { it != SystemAction.BROWSER && it != SystemAction.YOUTUBE }.forEach { action ->
                            if (action.isAvailable(LocalContext.current)) {
                                MenuButton(action.label, wide = false, onClick = { onSystem(action) })
                            } else {
                                Text("${action.label}: Not on TV", color = p.danger, fontSize = 14.sp, modifier = Modifier.padding(12.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsPanel(title: String, content: @Composable ColumnScope.() -> Unit) {
    val p = LocalPalette.current
    Column(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(p.card.copy(alpha = 0.78f))
            .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(22.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(title, color = p.text, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
        content()
    }
}

private fun Context.findWindow(): Window? {
    var current: Context = this
    while (current is ContextWrapper) {
        if (current is android.app.Activity) return current.window
        current = current.baseContext
    }
    return null
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
        modifier = Modifier.fillMaxWidth().padding(start = 56.dp, end = 56.dp, top = 36.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Clock()
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            MenuButton(text = "Theme: ${themeMode.label}", wide = false, onClick = onCycleTheme)
            MenuButton(text = "Settings", wide = false, onClick = onOpenSettings)
        }
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

private fun backgroundBrushColors(p: Palette, choice: String): List<Color> = when (choice) {
    "ocean" -> listOf(Color(0xFF061A40), Color(0xFF0353A4))
    "forest" -> listOf(Color(0xFF081C15), Color(0xFF2D6A4F))
    "sunset" -> listOf(Color(0xFF3A0CA3), Color(0xFFF72585))
    else -> listOf(p.bgTop, p.bgBottom)
}
