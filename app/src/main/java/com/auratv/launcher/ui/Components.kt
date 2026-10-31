package com.auratv.launcher.ui

import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import com.auratv.launcher.data.AppEntry
import com.auratv.launcher.data.Art
import com.auratv.launcher.data.ImageLoader

/**
 * Asks for focus, retrying frame by frame until the target has been laid out.
 * Replaces the fixed delays v1.0.0 used. Returns true once focus was requested.
 */
suspend fun FocusRequester.requestFocusWhenReady(maxFrames: Int = 20): Boolean {
    repeat(maxFrames) {
        if (runCatching { requestFocus() }.isSuccess) return true
        withFrameNanos { }
    }
    return false
}

/**
 * A focusable card. The focused card grows slightly, gets a thick bright ring, and every other
 * card is dimmed, so the focus is easy to see from a sofa on a budget panel.
 * OK opens it. Long-press OK (or Menu) triggers [onLongClick]; every long-press action also has
 * a plain-OK route, because IR remotes may not repeat keys.
 */
@Composable
fun FocusCard(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    focusRequester: FocusRequester? = null,
    dimWhenUnfocused: Boolean = true,
    content: @Composable BoxScope.(focused: Boolean) -> Unit,
) {
    val c = LocalColors.current
    val d = LocalDimens.current
    val shape: Shape = RoundedCornerShape(d.dp(12f))
    var focused by remember { mutableStateOf(false) }
    var longFired by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused) 1.08f else 1f, tween(220, easing = FastOutSlowInEasing), label = "scale")
    val dim by animateFloatAsState(if (focused || !dimWhenUnfocused) 0f else 0.38f, tween(220), label = "dim")

    Box(
        modifier = modifier
            .zIndex(if (focused) 1f else 0f)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .background(c.card)
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
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .onFocusChanged { focused = it.isFocused }
            .clickable {
                if (longFired) longFired = false else onClick()
            }
            .then(if (focused) Modifier.border(d.dp(4f), c.ring, shape) else Modifier),
    ) {
        content(focused)
        if (dim > 0f) {
            Box(Modifier.matchParentSize().background(Color.Black.copy(alpha = dim)))
        }
    }
}

private val ceramic = listOf(
    Color(0xFFC9705A) to Color(0xFF8E4533),
    Color(0xFFD98A75) to Color(0xFF9C5444),
    Color(0xFFB9AC98) to Color(0xFF7D705F),
    Color(0xFF8FA89B) to Color(0xFF58705F),
)

/** Draws an app's artwork: its TV banner, or its icon on a soft ceramic-toned tile. */
@Composable
fun AppArt(app: AppEntry, loader: ImageLoader, widthPx: Int, heightPx: Int, preferBanner: Boolean) {
    val d = LocalDimens.current
    val cacheKey = "${app.key}|$widthPx|$heightPx|$preferBanner"
    var art by remember(cacheKey) { mutableStateOf<Art?>(null) }
    LaunchedEffect(cacheKey) { art = loader.load(app, widthPx, heightPx, preferBanner) }
    val a = art
    if (a != null && a.isBanner) {
        Image(
            bitmap = a.bitmap,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
    } else {
        val pair = remember(app.packageName) { ceramic[(app.packageName.hashCode() and 0x7fffffff) % ceramic.size] }
        Box(
            Modifier.fillMaxSize().background(Brush.linearGradient(listOf(pair.first, pair.second))),
            contentAlignment = Alignment.Center,
        ) {
            if (a != null) {
                val sizeDp = with(LocalDensity.current) { a.bitmap.height.toDp() }
                Image(
                    bitmap = a.bitmap,
                    contentDescription = null,
                    modifier = Modifier.size(sizeDp).padding(bottom = d.dp(10f)),
                )
            }
        }
    }
}

@Composable
fun BoxScope.CardLabel(text: String) {
    val d = LocalDimens.current
    Box(
        Modifier
            .align(Alignment.BottomStart)
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))))
            .padding(horizontal = d.dp(10f), vertical = d.dp(8f)),
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = d.sp(17f),
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** A wide banner card for the All apps grid. */
@Composable
fun AppCard(
    app: AppEntry,
    loader: ImageLoader,
    width: Dp,
    height: Dp,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val density = LocalDensity.current
    val wPx = with(density) { width.roundToPx() }
    val hPx = with(density) { height.roundToPx() }
    FocusCard(
        modifier = modifier.size(width, height),
        onClick = onClick,
        onLongClick = onLongClick,
        focusRequester = focusRequester,
    ) {
        AppArt(app, loader, wPx, hPx, preferBanner = true)
        CardLabel(app.label)
    }
}

/** A square icon tile for the Favorites dock. */
@Composable
fun DockTile(
    app: AppEntry,
    loader: ImageLoader,
    size: Dp,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val px = with(LocalDensity.current) { size.roundToPx() }
    FocusCard(
        modifier = modifier.size(size),
        onClick = onClick,
        onLongClick = onLongClick,
        focusRequester = focusRequester,
    ) {
        AppArt(app, loader, px, px, preferBanner = false)
        CardLabel(app.label)
    }
}

@Composable
fun AuraButton(
    text: String,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    focusRequester: FocusRequester? = null,
    onClick: () -> Unit,
) {
    val c = LocalColors.current
    val d = LocalDimens.current
    val shape = RoundedCornerShape(d.dp(24f))
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused) 1.05f else 1f, tween(200, easing = FastOutSlowInEasing), label = "btn")
    val bg = if (selected) c.accent else c.panel
    val fg = if (selected) c.onAccent else c.text
    Box(
        modifier = modifier
            .height(d.dp(46f))
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .background(bg)
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .onFocusChanged { focused = it.isFocused }
            .clickable(onClick = onClick)
            .then(if (focused) Modifier.border(d.dp(3f), c.ring, shape) else Modifier)
            .padding(horizontal = d.dp(22f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = fg, fontSize = d.sp(19f), fontWeight = FontWeight.Medium, maxLines = 1)
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    val c = LocalColors.current
    val d = LocalDimens.current
    Text(
        text = text,
        color = c.text,
        fontFamily = BrandFont,
        fontWeight = FontWeight.Medium,
        fontSize = d.sp(24f),
        modifier = modifier,
    )
}

/** Actions for one app, reachable with plain OK from Settings as well as with long-press from Home. */
@Composable
fun AppMenuDialog(
    app: AppEntry,
    isFavorite: Boolean,
    favoriteIndex: Int,
    favoriteCount: Int,
    isHidden: Boolean,
    onDismiss: () -> Unit,
    onToggleFavorite: () -> Unit,
    onMove: (Int) -> Unit,
    onToggleHidden: () -> Unit,
    onInfo: () -> Unit,
    onUninstall: () -> Unit,
) {
    val c = LocalColors.current
    val d = LocalDimens.current
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) { first.requestFocusWhenReady() }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            modifier = Modifier
                .width(d.dp(440f))
                .clip(RoundedCornerShape(d.dp(16f)))
                .background(c.card)
                .padding(d.dp(26f)),
            verticalArrangement = Arrangement.spacedBy(d.dp(10f)),
        ) {
            Text(
                text = app.label,
                color = c.text,
                fontFamily = BrandFont,
                fontWeight = FontWeight.Medium,
                fontSize = d.sp(26f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(bottom = d.dp(6f)),
            )
            AuraButton(
                text = if (isFavorite) "Remove from Favorites" else "Add to Favorites",
                modifier = Modifier.fillMaxWidth(),
                focusRequester = first,
            ) { onToggleFavorite(); onDismiss() }
            if (isFavorite && favoriteIndex > 0) {
                AuraButton("Move earlier", Modifier.fillMaxWidth()) { onMove(-1); onDismiss() }
            }
            if (isFavorite && favoriteIndex in 0 until favoriteCount - 1) {
                AuraButton("Move later", Modifier.fillMaxWidth()) { onMove(1); onDismiss() }
            }
            AuraButton(if (isHidden) "Show on Home" else "Hide from Home", Modifier.fillMaxWidth()) {
                onToggleHidden(); onDismiss()
            }
            AuraButton("App information", Modifier.fillMaxWidth()) { onInfo(); onDismiss() }
            AuraButton("Uninstall", Modifier.fillMaxWidth()) { onUninstall(); onDismiss() }
            AuraButton("Close", Modifier.fillMaxWidth()) { onDismiss() }
        }
    }
}
