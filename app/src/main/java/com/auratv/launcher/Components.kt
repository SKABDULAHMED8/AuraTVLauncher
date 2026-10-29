package com.auratv.launcher

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import kotlin.math.abs
import kotlinx.coroutines.delay

/**
 * Apple TV style focus card: the focused card lifts (scale + shadow + soft ring).
 * Long-press OK/Select (or press Menu) triggers [onLongClick].
 */
@Composable
fun TvCard(
    width: Dp,
    height: Dp,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    content: @Composable BoxScope.(focused: Boolean) -> Unit,
) {
    val p = LocalPalette.current
    var focused by remember { mutableStateOf(false) }
    var longFired by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused) 1.1f else 1f, tween(180), label = "scale")
    val elevation by animateDpAsState(if (focused) 28.dp else 2.dp, tween(180), label = "elevation")
    val shape = RoundedCornerShape(18.dp)

    Box(
        modifier = modifier
            .size(width, height)
            .zIndex(if (focused) 1f else 0f)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(elevation, shape)
            .clip(shape)
            .background(p.card)
            .border(if (focused) 3.dp else 0.dp, if (focused) p.ring else Color.Transparent, shape)
            .onPreviewKeyEvent { ev ->
                val isSelect = ev.key == Key.DirectionCenter || ev.key == Key.Enter || ev.key == Key.NumPadEnter
                when {
                    isSelect && ev.type == KeyEventType.KeyDown && ev.nativeKeyEvent.repeatCount == 0 -> {
                        longFired = false
                        false
                    }
                    onLongClick != null && isSelect && ev.type == KeyEventType.KeyDown &&
                        ev.nativeKeyEvent.repeatCount == 1 -> {
                        longFired = true
                        onLongClick()
                        true
                    }
                    onLongClick != null && ev.key == Key.Menu && ev.type == KeyEventType.KeyUp -> {
                        onLongClick()
                        true
                    }
                    else -> false
                }
            }
            .onFocusChanged { focused = it.isFocused }
            .clickable {
                if (longFired) longFired = false else onClick()
            },
    ) {
        content(focused)
    }
}

@Composable
fun AppTile(
    app: AppItem,
    width: Dp,
    height: Dp,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    TvCard(width, height, modifier, onClick, onLongClick) {
        val banner = app.banner
        if (banner != null) {
            Image(
                bitmap = banner,
                contentDescription = app.label,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            val hue = (abs(app.packageName.hashCode()) % 360).toFloat()
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            listOf(Color.hsv(hue, 0.45f, 0.62f), Color.hsv((hue + 40f) % 360f, 0.55f, 0.36f)),
                        ),
                    ),
            )
            app.icon?.let {
                Image(
                    bitmap = it,
                    contentDescription = null,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(bottom = 14.dp)
                        .size(height * 0.5f),
                )
            }
            Text(
                text = app.label,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
            )
        }
    }
}

/** Plain text tile used for the system shortcuts row. */
@Composable
fun LabelTile(
    label: String,
    width: Dp,
    height: Dp,
    onClick: () -> Unit,
) {
    val p = LocalPalette.current
    TvCard(width, height, onClick = onClick) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(label, color = p.text, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun MenuButton(
    text: String,
    modifier: Modifier = Modifier,
    wide: Boolean = true,
    danger: Boolean = false,
    onClick: () -> Unit,
) {
    val p = LocalPalette.current
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused) 1.05f else 1f, tween(150), label = "btn")
    val accent = if (p.dark) Color.White else Color(0xFF0A84FF)
    val bg = if (focused) accent else p.pill
    val fg = when {
        focused -> if (p.dark) Color.Black else Color.White
        danger -> p.danger
        else -> p.text
    }
    Box(
        modifier = modifier
            .then(if (wide) Modifier.fillMaxWidth() else Modifier)
            .height(52.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(26.dp))
            .background(bg)
            .onFocusChanged { focused = it.isFocused }
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = fg, fontSize = 18.sp, fontWeight = FontWeight.Medium, maxLines = 1)
    }
}

@Composable
fun AppMenuDialog(
    app: AppItem,
    pinned: Boolean,
    onDismiss: () -> Unit,
    onTogglePin: () -> Unit,
    onInfo: () -> Unit,
    onUninstall: () -> Unit,
) {
    val p = LocalPalette.current
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        delay(120)
        runCatching { first.requestFocus() }
    }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            modifier = Modifier
                .width(440.dp)
                .clip(RoundedCornerShape(30.dp))
                .background(p.card)
                .padding(28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = app.label,
                color = p.text,
                fontSize = 26.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            MenuButton(
                text = if (pinned) "Remove from Favorites" else "Add to Favorites",
                modifier = Modifier.focusRequester(first),
                onClick = { onTogglePin(); onDismiss() },
            )
            MenuButton("App info", onClick = { onInfo(); onDismiss() })
            MenuButton("Uninstall", danger = true, onClick = { onUninstall(); onDismiss() })
            MenuButton("Cancel", onClick = onDismiss)
        }
    }
}
