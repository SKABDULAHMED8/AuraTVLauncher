# Changelog

All notable user-facing changes to Aura Launcher are documented here.

## [1.1.0] — In development

Implemented in this development branch:
- Increase Android versionCode to 2 and versionName to 1.1.0.
- Add an Aura Launcher Settings entry.
- Add persistent preset background palettes (default, ocean, forest and sunset).
- Bundle six optimised WebP space wallpapers for offline use (M82, Andromeda, Trifid, Orion, Crab and Rosette).
- Add a built-in wallpaper selector with persistent selection and retain the custom document-provider wallpaper picker.
- Decode selected built-in and custom wallpapers off the main thread with sampled RGB_565 bitmaps (maximum decoded bounds 1920×1080).
- Require a deliberate 500 ms hold before the app context menu opens.
- Hide Settings, Wi-Fi, Bluetooth or Display shortcuts when the system reports no matching activity.
- Keep the app free of new network permissions and external SDK dependencies.

Still requiring implementation and device validation before a production release:
- Wi-Fi connection type/status (SSID is not exposed), RAM and internal storage dashboard. In-app media volume controls are implemented. Brightness control remains device-specific and is not implemented.
- Reliable TV input source and connected peripheral detection.
- Browser and YouTube shortcuts when compatible apps are installed. Private browsing mode selection is not implemented because browser-specific APIs vary.
- Real-device testing on low-memory (1 GB RAM) Android TV hardware, accessibility/focus testing, release signing verification, and APK size/performance measurements.

The built-in image files and selector are implemented in this branch. Actual TV display quality, focus navigation, memory use and custom USB-provider behaviour still require device validation. Do not publish v1.1.0 until CI and device validation pass.

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
