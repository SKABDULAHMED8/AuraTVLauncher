package com.auratv.launcher

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class Palette(
    val dark: Boolean,
    val bgTop: Color,
    val bgBottom: Color,
    val glowA: Color,
    val glowB: Color,
    val card: Color,
    val text: Color,
    val textDim: Color,
    val ring: Color,
    val pill: Color,
    val danger: Color,
)

val DarkPalette = Palette(
    dark = true,
    bgTop = Color(0xFF07070C),
    bgBottom = Color(0xFF17142A),
    glowA = Color(0x553B2FD1),
    glowB = Color(0x3322D3C5),
    card = Color(0xFF26263A),
    text = Color(0xFFF5F5FA),
    textDim = Color(0xFFA6A6BA),
    ring = Color.White,
    pill = Color(0x33FFFFFF),
    danger = Color(0xFFFF6B6B),
)

val LightPalette = Palette(
    dark = false,
    bgTop = Color(0xFFF6F7FB),
    bgBottom = Color(0xFFDDE4F4),
    glowA = Color(0x553B82F6),
    glowB = Color(0x33F472B6),
    card = Color.White,
    text = Color(0xFF14141C),
    textDim = Color(0xFF626478),
    ring = Color(0xFF0A84FF),
    pill = Color(0x1A000000),
    danger = Color(0xFFD70015),
)

val LocalPalette = staticCompositionLocalOf { DarkPalette }
