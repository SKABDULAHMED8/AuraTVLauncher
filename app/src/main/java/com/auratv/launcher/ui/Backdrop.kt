package com.auratv.launcher.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.auratv.launcher.data.BUILTIN_WALLPAPER_COUNT
import com.auratv.launcher.data.WallpaperChoice

private class Glow(val x: Float, val y: Float, val r: Float, val color: Color)

private class WallSpec(val name: String, val darkBase: Color, val lightBase: Color, val glows: List<Glow>)

/**
 * Original wallpapers, drawn in code from flat colours and soft radial glows.
 * They are static, need no image files, no licences, and almost no memory.
 */
private val builtins: List<WallSpec> = listOf(
    WallSpec("Dusk", Color(0xFF17151C), Color(0xFFEFE9F4), listOf(Glow(0.2f, 0.1f, 0.6f, Color(0x4D6B5B95)), Glow(0.85f, 0.9f, 0.5f, Color(0x2ED97D64)))),
    WallSpec("Clay", Color(0xFF1E1613), Color(0xFFF6E7DF), listOf(Glow(0.85f, 0.1f, 0.6f, Color(0x52D97D64)), Glow(0.1f, 0.95f, 0.5f, Color(0x26E08E79)))),
    WallSpec("Sage", Color(0xFF151A17), Color(0xFFE7EFE9), listOf(Glow(0.1f, 0.2f, 0.6f, Color(0x478FA89B)), Glow(0.9f, 0.85f, 0.55f, Color(0x4D456656)))),
    WallSpec("Sand", Color(0xFF1D1A16), Color(0xFFF3EEE4), listOf(Glow(0.5f, 0.0f, 0.8f, Color(0x1FF5EDE3)), Glow(0.0f, 1.0f, 0.6f, Color(0x29B9AC98)))),
    WallSpec("Ink", Color(0xFF0F0F12), Color(0xFFE9EBF0), listOf(Glow(0.8f, 0.2f, 0.6f, Color(0x4D3A4A6B)), Glow(0.2f, 0.9f, 0.5f, Color(0x662A2540)))),
    WallSpec("Rose", Color(0xFF1D1517), Color(0xFFF5E6E8), listOf(Glow(0.15f, 0.15f, 0.6f, Color(0x42D4879A)), Glow(0.9f, 0.9f, 0.5f, Color(0x388E4533)))),
)

private val defaultSpec = WallSpec(
    "Default", Color(0xFF1A1816), Color(0xFFF5EDE3),
    listOf(Glow(0.12f, 0.08f, 0.55f, Color(0x38D97D64)), Glow(0.92f, 0.95f, 0.5f, Color(0x248FA89B))),
)

fun wallpaperName(choice: WallpaperChoice): String = when (choice) {
    WallpaperChoice.Default -> "Default"
    is WallpaperChoice.Builtin -> builtins[choice.index.coerceIn(0, BUILTIN_WALLPAPER_COUNT - 1)].name
    WallpaperChoice.Custom -> "Your image"
}

/** Draws the wallpaper. A custom image gets a soft scrim so text stays readable. */
@Composable
fun Backdrop(choice: WallpaperChoice, custom: ImageBitmap?, modifier: Modifier = Modifier.fillMaxSize()) {
    val colors = LocalColors.current
    if (choice is WallpaperChoice.Custom && custom != null) {
        Box(modifier) {
            Image(bitmap = custom, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            Box(Modifier.fillMaxSize().background(colors.bg.copy(alpha = if (colors.dark) 0.45f else 0.35f)))
        }
        return
    }
    val spec = when (choice) {
        is WallpaperChoice.Builtin -> builtins[choice.index.coerceIn(0, BUILTIN_WALLPAPER_COUNT - 1)]
        else -> defaultSpec
    }
    Box(
        modifier
            .background(if (colors.dark) spec.darkBase else spec.lightBase)
            .drawBehind {
                for (g in spec.glows) {
                    val center = Offset(size.width * g.x, size.height * g.y)
                    val radius = size.width * g.r
                    drawCircle(
                        brush = Brush.radialGradient(listOf(g.color, Color.Transparent), center = center, radius = radius),
                        radius = radius,
                        center = center,
                    )
                }
            },
    )
}
