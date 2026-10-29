package com.auratv.launcher

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap

data class AppItem(
    val label: String,
    val packageName: String,
    val launch: Intent,
    val banner: ImageBitmap?,
    val icon: ImageBitmap?,
)

object AppRepository {

    /**
     * Lists every launchable app. TV apps (leanback) are read first so their
     * 16:9 banners are used; phone-style apps are added afterwards as icon tiles.
     */
    fun load(context: Context): List<AppItem> {
        val pm = context.packageManager
        val seen = HashSet<String>()
        val result = ArrayList<AppItem>()

        val categories = listOf(Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER)
        for (category in categories) {
            val query = Intent(Intent.ACTION_MAIN).addCategory(category)
            for (info in pm.queryIntentActivities(query, 0)) {
                val pkg = info.activityInfo.packageName
                if (pkg == context.packageName) continue
                if (!seen.add(pkg)) continue

                val launch = Intent(Intent.ACTION_MAIN)
                    .addCategory(category)
                    .setComponent(ComponentName(pkg, info.activityInfo.name))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)

                val banner = if (category == Intent.CATEGORY_LEANBACK_LAUNCHER) {
                    runCatching {
                        (info.activityInfo.loadBanner(pm)
                            ?: info.activityInfo.applicationInfo.loadBanner(pm))
                            ?.toBitmap(320, 180)
                            ?.asImageBitmap()
                    }.getOrNull()
                } else null

                val icon = runCatching { info.loadIcon(pm).toBitmap(192, 192).asImageBitmap() }.getOrNull()

                result += AppItem(
                    label = info.loadLabel(pm).toString(),
                    packageName = pkg,
                    launch = launch,
                    banner = banner,
                    icon = icon,
                )
            }
        }
        return result.sortedBy { it.label.lowercase() }
    }
}
