package com.auratv.launcher.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ResolveInfo
import java.io.File

/** Finds launchable apps. Metadata only: artwork is loaded separately, on demand. */
object AppCatalog {

    fun scan(context: Context): List<AppEntry> {
        val pm = context.packageManager
        val own = context.packageName
        val result = LinkedHashMap<String, AppEntry>()
        val tvPackages = HashSet<String>()
        val installTimes = HashMap<String, Long>()

        fun entryOf(info: ResolveInfo, isTv: Boolean): AppEntry? {
            val ai = info.activityInfo ?: return null
            if (ai.packageName == own) return null
            val installTime = installTimes.getOrPut(ai.packageName) {
                try {
                    pm.getPackageInfo(ai.packageName, 0).firstInstallTime
                } catch (_: Exception) {
                    0L
                }
            }
            val label = info.loadLabel(pm).toString().trim().ifEmpty { ai.packageName }
            return AppEntry(
                key = ai.packageName + "/" + ai.name,
                packageName = ai.packageName,
                className = ai.name,
                label = label,
                isTv = isTv,
                installTime = installTime,
            )
        }

        val tvQuery = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
        for (info in pm.queryIntentActivities(tvQuery, 0)) {
            val entry = entryOf(info, true) ?: continue
            tvPackages += entry.packageName
            result.putIfAbsent(entry.key, entry)
        }

        // Phone-style entries are added only for apps that have no TV entry, so nothing appears twice.
        val phoneQuery = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        for (info in pm.queryIntentActivities(phoneQuery, 0)) {
            val entry = entryOf(info, false) ?: continue
            if (entry.packageName in tvPackages) continue
            result.putIfAbsent(entry.key, entry)
        }
        return result.values.toList()
    }

    fun launchIntent(app: AppEntry): Intent =
        Intent(Intent.ACTION_MAIN)
            .addCategory(if (app.isTv) Intent.CATEGORY_LEANBACK_LAUNCHER else Intent.CATEGORY_LAUNCHER)
            .setComponent(ComponentName(app.packageName, app.className))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)

    private fun cacheFile(context: Context) = File(context.filesDir, "app_list_cache.txt")

    fun loadCache(context: Context): List<AppEntry> =
        try {
            val f = cacheFile(context)
            if (f.exists()) AppCacheCodec.decode(f.readText()) else emptyList()
        } catch (_: Exception) {
            emptyList()
        }

    fun saveCache(context: Context, apps: List<AppEntry>) {
        try {
            val f = cacheFile(context)
            val tmp = File(f.parentFile, f.name + ".tmp")
            tmp.writeText(AppCacheCodec.encode(apps))
            if (!tmp.renameTo(f)) tmp.delete()
        } catch (_: Exception) {
            // The cache only speeds up the next start; losing it is harmless.
        }
    }
}
