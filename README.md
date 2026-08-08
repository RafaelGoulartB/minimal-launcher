# Minimal Launcher

A fast, text-first Android launcher with a deliberately quiet interface. It uses a black theme, large readable app names, and no icons in the initial app list.

## Features

- Home screen with a live clock and date
- Swipe left to browse every launchable app
- Search apps by name
- Reorder Home favorites with long-press and drag
- Contextual app management with rename, hide, block, folders, uninstall, and app info
- Alphabet index for fast navigation through the app drawer
- Configurable clock, date, battery, and daily usage summary
- Persistent favorites and launcher preferences, stored locally on-device
- Automatic refresh when returning to the launcher after installing or removing an app
- No background service, analytics, or extra runtime permissions

## Requirements

- Android 8.0 (API 26) or newer
- JDK 17
- Android SDK with API 37 installed

## Build and install

Open the project in the latest Android Studio, let Gradle sync, then run the `app` configuration on a connected device or emulator.

From a terminal with Gradle available:

```powershell
gradle assembleDebug
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

The debug APK is created at `app/build/outputs/apk/debug/app-debug.apk`.

## Use as your launcher

1. Install and open **Minimal Launcher**.
2. Press the Android Home button or gesture.
3. When Android asks which Home app to use, choose **Minimal Launcher** and optionally select **Always**.
4. On the Home page, swipe left to open the complete app list.
5. Tap an app name to open it. Long-press an app in the complete list to add it to Home or use the other management actions.

## Gestures and management

- **Home:** tap an app to open it. Long-press and drag to reorder it. Long-press without moving to reveal its contextual **Remove** action.
- **All apps:** tap to open; long-press for Home, rename, hide, block, folder, uninstall, and app-info actions.
- **Folders:** create or choose a folder from an app's management panel. Folders can be added to Home and reordered like apps.
- **Search:** type an app or folder name, or use the alphabet rail when search is empty.
- **Settings:** tap the gear beside search to manage hidden/blocked apps, custom names, folders, and Home appearance.

Blocking applies only inside Minimal Launcher and does not require an accessibility service. Hidden and blocked apps can always be restored from Settings.

## Daily usage access

The optional Home footer uses Android's Usage Access special permission to calculate today's foreground-app time. Open **Settings → Usage access → Grant usage access** and enable Minimal Launcher. The launcher does not request a normal runtime permission and does not run a background service.

To switch back later, select another Home app in Android's default-app settings.

## Architecture

The app keeps a small, focused structure:

- `data/` discovers activities that expose `ACTION_MAIN` and `CATEGORY_LAUNCHER`, then sorts them by label off the main thread.
- `data/LauncherPreferencesRepository` persists ordered Home items, app customization, folders, and appearance preferences in Jetpack DataStore.
- `data/UsageStatsRepository` reads the optional same-day usage total when Android Usage Access is granted.
- `ui/LauncherViewModel` combines installed apps, preferences, usage, folders, and search into screen state.
- `ui/` provides the Compose Home, app drawer, management panels, folder content, and Settings screens.

## Technology

- Kotlin
- Jetpack Compose and Material 3
- AndroidX ViewModel
- Jetpack DataStore

## Command shortcuts

After installing `make`, use the root `Makefile` as a convenient interface for Gradle and ADB:

```powershell
make help
make run
make lint
make test
make bundle
```

`make run` defaults to `emulator-5554`. Override it when needed:

```powershell
make run DEVICE=emulator-5556
```
