package com.auratv.launcher.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private class MemStore : KeyValueStore {
    val map = HashMap<String, String>()
    override fun getString(key: String) = map[key]
    override fun putString(key: String, value: String?) {
        if (value == null) map.remove(key) else map[key] = value
    }
}

private fun app(pkg: String, label: String = pkg, tv: Boolean = true, time: Long = 0, cls: String = "Main") =
    AppEntry("$pkg/$cls", pkg, cls, label, tv, time)

class HomeLogicTest {
    @Test fun sortAz_isCaseInsensitive() {
        val r = HomeLogic.sort(listOf(app("b", "banana"), app("a", "Apple"), app("c", "cherry")), SortMode.AZ)
        assertEquals(listOf("a", "b", "c"), r.map { it.packageName })
    }

    @Test fun sortZa_reverses() {
        val r = HomeLogic.sort(listOf(app("a", "Apple"), app("b", "Banana")), SortMode.ZA)
        assertEquals(listOf("b", "a"), r.map { it.packageName })
    }

    @Test fun sortNewest_usesInstallTime() {
        val r = HomeLogic.sort(listOf(app("old", time = 1), app("new", time = 9)), SortMode.NEWEST)
        assertEquals("new", r.first().packageName)
    }

    @Test fun hiddenAppsAreRemoved() {
        val r = HomeLogic.visible(listOf(app("a"), app("b")), setOf("a"))
        assertEquals(listOf("b"), r.map { it.packageName })
    }

    @Test fun favorites_defaultToFirstSixTvApps() {
        val apps = (1..10).map { app("p$it") }
        assertEquals(6, HomeLogic.favorites(apps, emptyList()).size)
    }

    @Test fun favorites_keepPinnedOrderAndSkipMissing() {
        val apps = listOf(app("a"), app("b"), app("c"))
        val r = HomeLogic.favorites(apps, listOf("c", "gone", "a"))
        assertEquals(listOf("c", "a"), r.map { it.packageName })
    }

    @Test fun togglePin_firstChangeStartsFromDefaults() {
        val r = HomeLogic.togglePin(emptyList(), listOf("a", "b"), "a")
        assertEquals(listOf("b"), r)
    }

    @Test fun togglePin_addsWhenAbsent() {
        assertEquals(listOf("a", "z"), HomeLogic.togglePin(listOf("a"), listOf("a"), "z"))
    }

    @Test fun movePin_clampsAtEnds() {
        assertEquals(listOf("a", "b"), HomeLogic.movePin(listOf("a", "b"), emptyList(), "a", -1))
        assertEquals(listOf("b", "a"), HomeLogic.movePin(listOf("a", "b"), emptyList(), "a", 1))
    }

    @Test fun toggleHidden_roundTrips() {
        val once = HomeLogic.toggleHidden(emptySet(), "a")
        assertTrue("a" in once)
        assertFalse("a" in HomeLogic.toggleHidden(once, "a"))
    }
}

class SettingsStoreTest {
    @Test fun readsV1Values() {
        val kv = MemStore().apply { putString("theme", "LIGHT"); putString("pinned", "a.b,c.d") }
        val s = SettingsStore(kv)
        assertEquals(ThemeMode.LIGHT, s.theme)
        assertEquals(listOf("a.b", "c.d"), s.pinned)
    }

    @Test fun unknownValuesFallBackToDefaults() {
        val s = SettingsStore(MemStore().apply { putString("theme", "???"); putString("sort", "x"); putString("wallpaper", "builtin:99") })
        assertEquals(ThemeMode.DARK, s.theme)
        assertEquals(SortMode.AZ, s.sort)
        assertEquals(WallpaperChoice.Default, s.wallpaper)
    }

    @Test fun writesV1CompatibleFormats() {
        val kv = MemStore()
        val s = SettingsStore(kv)
        s.theme = ThemeMode.SYSTEM
        s.pinned = listOf("x", "y")
        assertEquals("SYSTEM", kv.map["theme"])
        assertEquals("x,y", kv.map["pinned"])
    }

    @Test fun resetLayout_keepsThemeAccentWallpaper() {
        val s = SettingsStore(MemStore())
        s.theme = ThemeMode.LIGHT; s.accent = AccentId.SAGE; s.wallpaper = WallpaperChoice.Builtin(2)
        s.pinned = listOf("a"); s.hidden = setOf("b"); s.sort = SortMode.ZA
        s.resetLayout()
        assertTrue(s.pinned.isEmpty()); assertTrue(s.hidden.isEmpty()); assertEquals(SortMode.AZ, s.sort)
        assertEquals(ThemeMode.LIGHT, s.theme); assertEquals(AccentId.SAGE, s.accent)
        assertEquals(WallpaperChoice.Builtin(2), s.wallpaper)
    }

    @Test fun wallpaperEncodeDecodeRoundTrip() {
        for (w in listOf(WallpaperChoice.Default, WallpaperChoice.Custom, WallpaperChoice.Builtin(0), WallpaperChoice.Builtin(5))) {
            assertEquals(w, WallpaperChoice.decode(w.encode()))
        }
    }
}

class WallpaperRulesTest {
    @Test fun missingCustomFallsBackToDefault() {
        assertEquals(WallpaperChoice.Default, WallpaperRules.effective(WallpaperChoice.Custom, false))
        assertEquals(WallpaperChoice.Custom, WallpaperRules.effective(WallpaperChoice.Custom, true))
    }

    @Test fun failedPromotionKeepsPreviousChoice() {
        val s = SettingsStore(MemStore())
        s.wallpaper = WallpaperChoice.Builtin(1)
        assertFalse(WallpaperRules.apply(s, WallpaperChoice.Custom) { false })
        assertEquals(WallpaperChoice.Builtin(1), s.wallpaper)
    }

    @Test fun successfulPromotionApplies() {
        val s = SettingsStore(MemStore())
        assertTrue(WallpaperRules.apply(s, WallpaperChoice.Custom) { true })
        assertEquals(WallpaperChoice.Custom, s.wallpaper)
    }
}

class AppCacheCodecTest {
    @Test fun roundTrip() {
        val apps = listOf(app("a.b", "My App", true, 5), app("c.d", "Other", false, 7, "Alt"))
        assertEquals(apps, AppCacheCodec.decode(AppCacheCodec.encode(apps)))
    }

    @Test fun tabsAndNewlinesInLabelsAreSanitised() {
        val r = AppCacheCodec.decode(AppCacheCodec.encode(listOf(app("a", "x\ty\nz"))))
        assertEquals(1, r.size)
        assertEquals("x y z", r[0].label)
    }

    @Test fun corruptOrWrongHeaderGivesEmpty() {
        assertTrue(AppCacheCodec.decode("garbage").isEmpty())
        assertTrue(AppCacheCodec.decode(null).isEmpty())
        assertTrue(AppCacheCodec.decode("aura-apps-v1\nbad line").isEmpty())
    }
}
