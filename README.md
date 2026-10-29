<div align="center">

# Aura Launcher

**A clean, Apple TV style home screen for Android TV and Google TV.**
No ads. No banners. No tracking. Dark and Light themes.

![Platform](https://img.shields.io/badge/platform-Android%20TV-3ddc84)
![Min SDK](https://img.shields.io/badge/minSdk-24-blue)
![Kotlin](https://img.shields.io/badge/Kotlin-2.0-7F52FF)
![UI](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4)
![Ads](https://img.shields.io/badge/ads-none-brightgreen)

</div>

---

## Preview

| Dark theme | Light theme |
| :---: | :---: |
| ![Dark theme](docs/preview-dark.svg) | ![Light theme](docs/preview-light.svg) |

> The previews are illustrations of the layout. Your own installed apps appear in place of the sample cards, using their official TV banners.

## Features

| Feature | Details |
| --- | --- |
| **Apple TV look** | Large rounded cards. The focused card lifts with a scale, shadow and ring. Big clock and date at the top. |
| **Ad-free by design** | The app has no `INTERNET` permission, no ad SDKs and no analytics, so it cannot load ads, promoted rows or banners. |
| **Dark, Light and Auto** | The Theme button (top right) cycles Auto, Dark and Light. Your choice is remembered. |
| **Favorites row** | Pin the apps you use most. They appear in a larger row at the top. |
| **All Apps row** | Every installed app. TV apps show their 16:9 banner, other apps show a clean icon tile. |
| **System shortcuts** | One-press access to Settings, Wi-Fi, Bluetooth and Display. |
| **Live app list** | The list updates when you install, update or remove an app. |
| **Remote friendly** | Built for D-pad navigation. Back never closes the launcher. |
| **Works on any TV size** | Sizes are in density-independent units, so 32", 43", 55" and 65" TVs show the same proportions. |
| **Private** | Settings stay on the device in local storage. Nothing is sent anywhere. |

## Remote control guide

| Button | Action |
| --- | --- |
| D-pad | Move focus between cards |
| OK / Select | Open the app |
| Long-press OK, or Menu | Open the app menu: Add or Remove Favorites, App info, Uninstall |
| Up, then OK on **Theme** | Switch between Auto, Dark and Light |
| Back | Stays on the home screen |

## Download and build

### Option 1: build in the cloud with GitHub Actions (no setup)

1. Open the **Actions** tab of this repository.
2. Select **Build APK** and press **Run workflow**.
3. When the run finishes, download **AuraLauncher-apk** from the **Artifacts** section.

To publish a release with the APK attached, push a version tag:

```bash
git tag v1.0.0
git push origin v1.0.0
```

### Option 2: build with Android Studio

1. Install the latest Android Studio.
2. Choose **Open** and select this folder, then wait for the Gradle sync.
3. Go to **Build > Build App Bundle(s) / APK(s) > Build APK(s)**.
4. Find the file at `app/build/outputs/apk/debug/app-debug.apk`.

### Option 3: command line

Requires JDK 17, the Android SDK and Gradle 8.10 or newer.

```bash
gradle assembleDebug      # app/build/outputs/apk/debug/app-debug.apk
gradle assembleRelease    # app/build/outputs/apk/release/app-release.apk
```

> The release build is signed with the debug key so it installs directly. Use your own keystore before publishing to an app store.

## Install on your TV

1. Copy the APK to the TV with a USB drive, or install it over the network:
   ```bash
   adb connect <TV-IP>:5555
   adb install app-release.apk
   ```
2. If you use a file manager on the TV, allow **Install unknown apps** when asked.
3. Open **Aura Launcher** from the app list once to confirm it runs.

## Set Aura Launcher as your Home screen

**Android TV 9 and many TV boxes:** press the **Home** button, choose **Aura Launcher**, then select **Always**.

**Google TV and newer Android TV (no chooser):** enable Developer options and network debugging on the TV, then run from a computer:

```bash
adb connect <TV-IP>:5555
adb shell cmd package set-home-activity com.auratv.launcher/.MainActivity
```

**Still seeing sponsored rows from the stock home screen?** After Aura Launcher is installed, you can turn the stock launcher off. Package names differ by brand, so check yours first.

```bash
adb shell pm disable-user --user 0 com.google.android.tvlauncher
adb shell pm disable-user --user 0 com.google.android.apps.tv.launcherx

# To undo:
adb shell pm enable com.google.android.tvlauncher
adb shell pm enable com.google.android.apps.tv.launcherx
```

Only do this once Aura Launcher is installed, so the TV always has a Home app.

## Project structure

```
AuraTVLauncher/
├── app/src/main/
│   ├── AndroidManifest.xml          # HOME + leanback launcher, no INTERNET permission
│   ├── java/com/auratv/launcher/
│   │   ├── MainActivity.kt          # Entry point, theme and favorites state, package events
│   │   ├── HomeScreen.kt            # Home layout: clock, rows, system shortcuts
│   │   ├── Components.kt            # Focus cards, buttons, long-press menu dialog
│   │   ├── AppRepository.kt         # Reads installed apps and their TV banners
│   │   ├── Palette.kt               # Dark and Light color palettes
│   │   └── Prefs.kt                 # Local settings (theme, favorites)
│   └── res/                         # Icon, TV banner, theme
├── docs/                            # README preview images
└── .github/workflows/build.yml      # Cloud APK build
```

## Tech stack

- Kotlin 2.0 and Jetpack Compose
- Android Gradle Plugin 8.7, Gradle 8.10
- Min SDK 24 (Android 7.0), target SDK 34

## Privacy

Aura Launcher requests no network access. The only extra permission is `REQUEST_DELETE_PACKAGES`, used for the Uninstall action in the app menu. Theme and favorites are stored locally on your device.

## Troubleshooting

| Problem | Fix |
| --- | --- |
| Pressing Home still opens the old launcher | Use the `set-home-activity` command above, or disable the stock launcher. |
| An app is missing | Apps without a launcher entry are not listed. Reopen the launcher to refresh. |
| A system shortcut does nothing | Some TV brands remove that settings screen. Use **Settings** instead. |
| Build fails on first sync | Update Android Studio, then run **File > Sync Project with Gradle Files**. |

## Roadmap

- Custom wallpapers
- Reorder and hide apps
- Per-app row sorting
- Optional weather widget (off by default, would require opt-in network access)

## Contributing

Issues and pull requests are welcome. Please keep the project ad-free and network-free by default.
