package com.auratv.launcher.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.auratv.launcher.R
import com.auratv.launcher.data.AccentId

/** EB Garamond (SIL Open Font License) for the brand and section titles. Everything else uses the system font. */
val BrandFont = FontFamily(Font(R.font.ebgaramond_medium, FontWeight.Medium))

@Immutable
class AuraColors(
    val dark: Boolean,
    val bg: Color,
    val card: Color,
    val panel: Color,
    val text: Color,
    val textDim: Color,
    val accent: Color,
    val onAccent: Color,
    /** Focus ring: sand on dark, the deep accent on light. Both pass 3:1 against the background. */
    val ring: Color,
)

private fun contrast(a: Color, b: Color): Float {
    val la = a.luminance()
    val lb = b.luminance()
    val hi = maxOf(la, lb)
    val lo = minOf(la, lb)
    return (hi + 0.05f) / (lo + 0.05f)
}

fun accentColor(id: AccentId, dark: Boolean): Color = when (id) {
    AccentId.CLAY -> if (dark) Color(0xFFD97D64) else Color(0xFFA94A33)
    AccentId.CORAL -> if (dark) Color(0xFFE08E79) else Color(0xFFB2432F)
    AccentId.SAGE -> if (dark) Color(0xFF8FA89B) else Color(0xFF456656)
    AccentId.SAND -> if (dark) Color(0xFFF5EDE3) else Color(0xFF6B5A44)
}

fun auraColors(dark: Boolean, accentId: AccentId): AuraColors {
    val accent = accentColor(accentId, dark)
    val inkDark = Color(0xFF1A1816)
    val onAccent = if (contrast(accent, inkDark) >= contrast(accent, Color.White)) inkDark else Color.White
    return if (dark) {
        AuraColors(
            dark = true,
            bg = Color(0xFF1A1816),
            card = Color(0xFF2A2622),
            panel = Color(0xFF34302B),
            text = Color(0xFFF5EDE3),
            textDim = Color(0xFFA99F94),
            accent = accent,
            onAccent = onAccent,
            ring = Color(0xFFF5EDE3),
        )
    } else {
        AuraColors(
            dark = false,
            bg = Color(0xFFF5EDE3),
            card = Color(0xFFFFFFFF),
            panel = Color(0xFFE9DFD2),
            text = Color(0xFF1E1C1A),
            textDim = Color(0xFF5E564D),
            accent = accent,
            onAccent = onAccent,
            ring = accent,
        )
    }
}

val LocalColors = staticCompositionLocalOf { auraColors(true, AccentId.CLAY) }

/**
 * Layout scale. Everything is sized from the measured screen, not from a fixed resolution:
 * [scale] is 1.0 at a 960 x 540 dp screen and follows the smaller of width and height.
 */
@Immutable
class Dimens(val scale: Float, val columns: Int, val widthDp: Float, val heightDp: Float) {
    fun dp(v: Float): Dp = (v * scale).dp
    fun sp(v: Float): TextUnit = (v * scale).sp
}

val LocalDimens = staticCompositionLocalOf { Dimens(1f, 4, 960f, 540f) }

fun dimensFor(widthDp: Float, heightDp: Float): Dimens {
    val s = minOf(widthDp / 960f, heightDp / 540f).coerceIn(0.7f, 1.6f)
    val columns = if (widthDp / s >= 1200f) 5 else 4
    return Dimens(s, columns, widthDp, heightDp)
}

@Composable
fun AuraTheme(dark: Boolean, accent: AccentId, content: @Composable () -> Unit) {
    val colors = remember(dark, accent) { auraColors(dark, accent) }
    CompositionLocalProvider(LocalColors provides colors) { content() }
}
