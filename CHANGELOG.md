# Changelog

All notable user-facing changes to Aura Launcher are documented here.

This project uses version tags such as `v1.0.0`. Release dates and changes should be confirmed against the published release.

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

## [1.1.0]

- New home screen: larger focus ring, dimmed unfocused cards, edge-to-edge Favorites carousel.
- Date, clock and a Wi-Fi/network status icon (green connected, red not connected) on Home and Settings.
- New Settings screen: theme, accent colour, sort order, wallpaper, Manage apps, reset layout, Android settings.
- Wallpapers: six built-in wallpapers or your own image (copied and shrunk to fit; size guidance shown on screen).
- App menu (hold OK or press Menu) with Open app, Favorites, Hide, App information and Uninstall; the menu no longer triggers the top option when OK is released.
- Manage apps list reaches every app action with plain OK, for remotes that cannot long-press.
- Faster, lighter artwork loading with a memory cap, and a cached app list for an instant first screen.
- Adds the `ACCESS_NETWORK_STATE` permission (reads connection status only). Still no Internet permission, ads or analytics.
- Updates v1.0.0 in place: same package name and signing key; theme and favorites are kept.
- Verified by automated unit tests and lint on GitHub. On-TV behaviour is being confirmed by the owner.

## Unreleased

Add user-visible changes here as they are implemented and tested. Do not list planned features as completed changes.
