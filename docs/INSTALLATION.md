# Aura Launcher installation and update guide

This guide covers installing the official release APK on Android TV / Google TV, including TVs that use a manufacturer or operator interface such as CloudTV.

## 1. Download the official APK

1. Open the [Aura Launcher Releases page](https://github.com/SKABDULAHMED8/AuraTVLauncher/releases).
2. Open the latest published release.
3. Download the APK asset, such as `AuraLauncher-v1.0.0.apk`.
4. Optionally download the matching `.apk.sha256` checksum file.

Use the APK attached to the published release. Do not use a debug APK for a normal production installation.

## 2. Optional: verify the downloaded APK on Windows

Open PowerShell in the folder containing the APK and run:

```powershell
Get-FileHash ".\AuraLauncher-v1.0.0.apk" -Algorithm SHA256
```

Compare the resulting hash with the checksum file from the **same release**. A matching SHA-256 confirms the downloaded file matches the file used to create that checksum; it does not alone prove who signed the APK.

For Android signing verification, install Android SDK Build-Tools and run:

```powershell
& "C:\Android\Sdk\build-tools\36.0.0\apksigner.bat" verify --verbose --print-certs ".\AuraLauncher-v1.0.0.apk"
```

The certificate fingerprint can be compared with the project's known release certificate fingerprint. Use the release's documented fingerprint if one is published.

## 3. Install using a USB drive and file manager

1. Copy the downloaded APK to a USB drive.
2. Connect the USB drive to the TV.
3. Open the TV's file manager (for example, CX File Explorer).
4. Select the APK and choose **Install**.
5. If Android asks for permission, allow that file manager to **Install unknown apps**. Menu names differ by TV and Android version.
6. Wait for installation to finish, then open Aura Launcher from the TV's app list.

If the APK is already on the TV, you can open it directly from the Downloads folder.

## 4. Updating Aura Launcher

For a production update, install the new APK over the existing production installation. Keep the permanent release signing key unchanged between releases.

Android normally refuses an update if the installed APK and the new APK are signed with different certificates. A common example is installing a release-signed APK over a debug-signed test build.

If an update fails:

1. Do not repeatedly retry without reading the message.
2. Check that you downloaded the intended APK and that the checksum matches its release checksum.
3. If the app was installed from a debug build, confirm whether its debug signing key is available before considering removal.
4. Back up any data you need. Uninstalling an app normally deletes its private data and settings.
5. Only uninstall the old app if you accept losing that data or have confirmed it is backed up. Then install the official release APK again.

If a fresh install also fails, record the exact error and check device compatibility and available storage before taking further action.

## 5. Choose Aura Launcher as the Home app

Installing the app and making it the default Home app are separate steps.

- On some devices, pressing **Home** opens a chooser. Select Aura Launcher and choose **Always** if that option is offered.
- Other devices have a **Default apps**, **Home app**, or launcher setting in system Settings.
- Some custom TV firmware (including some CloudTV variants) hides or restricts the default launcher selector and Developer options. Aura Launcher may still open from the app list even when it cannot replace the stock Home screen.

Do not disable the manufacturer's launcher unless you have confirmed that Aura Launcher is installed, works, and can be opened reliably. If the stock launcher is disabled and the replacement cannot start, access to the Home screen may be disrupted.

## 6. Optional ADB installation

ADB is only available if the TV firmware exposes debugging and you authorize the computer.

On the computer, install Android Platform Tools, then use the connection method and port shown by your TV's debugging settings:

```powershell
adb connect TV_IP_ADDRESS:PORT
adb devices
adb install AuraLauncher-v1.0.0.apk
```

Replace `TV_IP_ADDRESS:PORT` with the TV's actual address and port. Do not assume that port 5555 is available. If the TV does not expose debugging, use the file-manager method instead.

## Troubleshooting

### “App not installed” while updating

The app may be signed with a different certificate, the package may be incompatible, or the APK may be damaged. A screenshot of the generic message alone does not identify the exact cause. Check the signature, checksum, Android version and exact error before uninstalling.

### “This version is already installed”

The package manager has found an installed app with the same package and version information. Continuing the installation may still fail if the signing certificates differ. Do not assume the message proves the update is compatible.

### Aura Launcher installs but Home still opens CloudTV

The TV's firmware may not allow third-party launchers to become the default Home app. Check the TV's default-app settings or manufacturer documentation. Do not disable the stock launcher as a first step.

### A shortcut does not open

Some manufacturers remove or customize system settings screens. Use the TV's normal Settings app for that function.

### Report a problem

When opening an issue, include the TV brand/model, Android version if known, installation method, exact error text, and steps to reproduce. Never post signing passwords, private keystores, or other credentials.
