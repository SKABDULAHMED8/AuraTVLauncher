package com.auratv.launcher.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.font.FontWeight
import com.auratv.launcher.LauncherModel
import com.auratv.launcher.data.BUILTIN_WALLPAPER_COUNT
import com.auratv.launcher.data.WallpaperChoice

@Composable
fun WallpaperScreen(model: LauncherModel, message: String?, onPickImage: () -> Unit, onBack: () -> Unit) {
    val c = LocalColors.current
    val d = LocalDimens.current
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) { first.requestFocusWhenReady() }
    val current = model.effectiveWallpaper
    val tileW = d.dp(190f)
    val tileH = d.dp(107f)

    @Composable
    fun Tile(choice: WallpaperChoice, requester: FocusRequester? = null) {
        FocusCard(
            modifier = Modifier.size(tileW, tileH),
            onClick = { model.chooseBuiltinWallpaper(choice) },
            focusRequester = requester,
            dimWhenUnfocused = false,
        ) {
            Backdrop(choice, null, Modifier.fillMaxSize())
            CardLabel(wallpaperName(choice) + if (choice == current) "  ✓" else "")
        }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = d.dp(64f), vertical = d.dp(32f)),
        verticalArrangement = Arrangement.spacedBy(d.dp(16f)),
    ) {
        Text("Wallpaper", color = c.text, fontFamily = BrandFont, fontWeight = FontWeight.Medium, fontSize = d.sp(38f))
        Row(horizontalArrangement = Arrangement.spacedBy(d.dp(16f))) {
            Tile(WallpaperChoice.Default, first)
            for (i in 0 until 3) Tile(WallpaperChoice.Builtin(i))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(d.dp(16f))) {
            for (i in 3 until BUILTIN_WALLPAPER_COUNT) Tile(WallpaperChoice.Builtin(i))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(d.dp(12f))) {
            AuraButton("Choose your own image…", Modifier.width(d.dp(340f)), onClick = onPickImage)
            AuraButton("Back", Modifier.width(d.dp(160f)), onClick = onBack)
        }
        if (message != null) Text(message, color = c.textDim, fontSize = d.sp(18f), modifier = Modifier.fillMaxWidth())
        Text(
            "Your own image is copied and shrunk to fit this TV, so it keeps working if the USB drive is removed.",
            color = c.textDim, fontSize = d.sp(16f),
        )
    }
}
