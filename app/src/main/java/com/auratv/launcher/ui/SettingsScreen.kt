package com.auratv.launcher.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.font.FontWeight
import com.auratv.launcher.BuildConfig
import com.auratv.launcher.LauncherModel
import com.auratv.launcher.data.AppEntry

@Composable
fun SettingsScreen(
    model: LauncherModel,
    onBack: () -> Unit,
    onWallpapers: () -> Unit,
    onManageApps: () -> Unit,
    onSystemSettings: () -> Unit,
) {
    val c = LocalColors.current
    val d = LocalDimens.current
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) { first.requestFocusWhenReady() }
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = d.dp(64f), vertical = d.dp(32f)),
        verticalArrangement = Arrangement.spacedBy(d.dp(12f)),
    ) {
        androidx.compose.foundation.layout.Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            AppTitle()
            androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
            StatusCluster()
        }
        Text("Settings", color = c.text, fontFamily = BrandFont, fontWeight = FontWeight.Medium, fontSize = d.sp(30f))
        AuraButton("Theme: ${model.theme.label}  (press OK to change)", Modifier.fillMaxWidth(), focusRequester = first) { model.cycleTheme() }
        AuraButton("Accent colour: ${model.accent.label}", Modifier.fillMaxWidth()) { model.cycleAccent() }
        AuraButton("Sort apps: ${model.sort.label}", Modifier.fillMaxWidth()) { model.cycleSort() }
        AuraButton("Wallpaper: ${wallpaperName(model.effectiveWallpaper)}", Modifier.fillMaxWidth(), onClick = onWallpapers)
        AuraButton("Manage apps (favorites, hide, info, uninstall)", Modifier.fillMaxWidth(), onClick = onManageApps)
        AuraButton("Reset favorites, hidden apps and sorting", Modifier.fillMaxWidth()) { model.resetLayout() }
        AuraButton("Open Android settings", Modifier.fillMaxWidth(), onClick = onSystemSettings)
        AuraButton("Back", Modifier.fillMaxWidth(), onClick = onBack)
        val label = BuildConfig.BUILD_LABEL
        Text(
            "Aura Launcher ${BuildConfig.VERSION_NAME}" + (if (label.isNotEmpty()) "  ·  $label" else "") +
                "\nNo ads, no analytics, no internet permission." +
                "\nNetwork: ${if (rememberNetworkConnected()) "connected" else "not connected"}" +
                "\nApps: ${model.apps.size}  ·  Artwork cache: ${model.loader.usedBytes() / 1024} KB of ${model.loader.capBytes / 1024} KB",
            color = c.textDim,
            fontSize = d.sp(16f),
            modifier = Modifier.padding(top = d.dp(8f)),
        )
    }
}

/** Plain-OK route to every per-app action, so nothing depends on long-press working on an IR remote. */
@Composable
fun ManageAppsScreen(model: LauncherModel, onBack: () -> Unit, onOpenMenu: (AppEntry) -> Unit) {
    val c = LocalColors.current
    val d = LocalDimens.current
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) { first.requestFocusWhenReady() }
    val sorted = model.apps.sortedBy { it.label.lowercase() }
    val favPkgs = model.favorites.map { it.packageName }.toSet()
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = d.dp(64f)),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = d.dp(32f)),
        verticalArrangement = Arrangement.spacedBy(d.dp(10f)),
    ) {
        item {
            Text("Manage apps", color = c.text, fontFamily = BrandFont, fontWeight = FontWeight.Medium, fontSize = d.sp(38f))
        }
        item { AuraButton("Back", Modifier.fillMaxWidth(), focusRequester = first, onClick = onBack) }
        items(sorted, key = { it.key }) { app ->
            val tags = buildList {
                if (app.packageName in favPkgs) add("favorite")
                if (app.packageName in model.hidden) add("hidden")
            }.joinToString(", ")
            AuraButton(
                text = app.label + if (tags.isEmpty()) "" else "  ($tags)",
                modifier = Modifier.fillMaxWidth(),
            ) { onOpenMenu(app) }
        }
    }
}
