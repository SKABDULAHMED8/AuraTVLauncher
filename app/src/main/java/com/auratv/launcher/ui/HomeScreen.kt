package com.auratv.launcher.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.auratv.launcher.LauncherModel
import com.auratv.launcher.data.AppEntry

/** Lets a child draw past the grid's side padding, so a scrolling row can run edge to edge. */
private fun Modifier.bleed(extra: Dp): Modifier = layout { measurable, constraints ->
    val e = extra.roundToPx()
    val w = constraints.maxWidth + 2 * e
    val placeable = measurable.measure(constraints.copy(minWidth = w, maxWidth = w))
    layout(constraints.maxWidth, placeable.height) { placeable.place(-e, 0) }
}

@Composable
fun HomeScreen(
    model: LauncherModel,
    onLaunch: (AppEntry) -> Unit,
    onLongPress: (AppEntry) -> Unit,
    onSettings: () -> Unit,
) {
    val c = LocalColors.current
    val d = LocalDimens.current
    val margin = d.dp(48f)
    val gap = d.dp(16f)
    val cardW = (d.widthDp.dp - margin * 2 - gap * (d.columns - 1)) / d.columns
    val cardH = cardW * 9f / 16f
    val favs = model.favorites
    val apps = model.visibleApps
    val first = remember { FocusRequester() }
    val firstKey = favs.firstOrNull()?.key ?: apps.firstOrNull()?.key
    LaunchedEffect(firstKey) { if (firstKey != null) first.requestFocusWhenReady() }

    LazyVerticalGrid(
        columns = GridCells.Fixed(d.columns),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = margin, vertical = d.dp(28f)),
        horizontalArrangement = Arrangement.spacedBy(gap),
        verticalArrangement = Arrangement.spacedBy(gap),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                AppTitle()
                Spacer(Modifier.weight(1f))
                StatusCluster()
                Spacer(Modifier.width(d.dp(20f)))
                AuraButton("Settings", onClick = onSettings)
            }
        }
        if (favs.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                SectionTitle("Favorites", Modifier.padding(top = d.dp(10f)))
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                val tile = d.dp(124f)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(gap),
                    contentPadding = PaddingValues(horizontal = margin, vertical = d.dp(14f)),
                    modifier = Modifier.bleed(margin).height(tile + d.dp(28f)),
                ) {
                    items(favs, key = { it.key }) { app ->
                        DockTile(
                            app = app,
                            loader = model.loader,
                            size = tile,
                            focusRequester = if (app.key == firstKey) first else null,
                            onClick = { onLaunch(app) },
                            onLongClick = { onLongPress(app) },
                        )
                    }
                }
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            SectionTitle("All apps", Modifier.padding(top = d.dp(6f)))
        }
        items(apps, key = { it.key }) { app ->
            AppCard(
                app = app,
                loader = model.loader,
                width = cardW,
                height = cardH,
                focusRequester = if (favs.isEmpty() && app.key == firstKey) first else null,
                onClick = { onLaunch(app) },
                onLongClick = { onLongPress(app) },
            )
        }
    }
}
