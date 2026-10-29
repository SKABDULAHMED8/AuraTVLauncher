# Changelog

All notable user-facing changes to Aura Launcher are documented here.

## [1.1.0] — In development

Implemented in this development branch:
- Increase Android versionCode to 2 and versionName to 1.1.0.
- Add an Aura Launcher Settings entry.
- Add persistent preset background palettes (default, ocean, forest and sunset).
- Require a deliberate 500 ms hold before the app context menu opens.
- Hide Settings, Wi-Fi, Bluetooth or Display shortcuts when the system reports no matching activity.
- Keep the app free of new network permissions and external SDK dependencies.

Still requiring implementation and device validation before a production release:
- Custom wallpaper picker and wallpaper rendering.
- Wi-Fi connection name/status, storage/RAM dashboard, brightness and volume controls.
- Reliable TV input source and connected peripheral detection.
- Browser/private-search and YouTube shortcuts.
- Real-device testing on low-memory (1 GB RAM) Android TV hardware, accessibility/focus testing, release signing verification, and APK size/performance measurements.

These items are not claimed as complete. Do not publish v1.1.0 until the build and device validation pass.

## [1.0.0] — Initial public release

- First public release of Aura Launcher for Android TV and Google TV.
- Apple TV–inspired home screen with large app cards, clock and date.
- Dark, Light and Auto theme options.
- Favorites row and installed-app browsing.
- System shortcuts for Settings, Wi-Fi, Bluetooth and Display where supported by the TV.
- D-pad and remote-focused navigation.
- Local storage for theme and favorites.
- No Internet permission, ad SDKs or analytics SDKs in the app manifest/project.
- Release APK and SHA-256 checksum published on GitHub Releases.
