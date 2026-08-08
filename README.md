# Minimal Launcher

A fast, text-first Android launcher with a deliberately quiet interface. It uses a black theme, large readable app names, and no icons in the initial app list.

## Features

- Home screen with a live clock and date
- Swipe left to browse every launchable app
- Search apps by name
- Add and remove Home favorites
- Persistent favorites, stored locally on-device
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
5. Tap an app name to open it; use **Add** or **Remove** to control the apps shown on Home.

To switch back later, select another Home app in Android's default-app settings.

## Architecture

The app keeps a small, focused structure:

- `data/` discovers activities that expose `ACTION_MAIN` and `CATEGORY_LAUNCHER`, then sorts them by label off the main thread.
- `data/FavoritesRepository` persists favorite component names in Jetpack DataStore.
- `ui/LauncherViewModel` combines installed apps, favorites, and the search query into screen state.
- `ui/LauncherScreen` provides the Compose UI: Home, all apps, search, and app launching.

## Technology

- Kotlin
- Jetpack Compose and Material 3
- AndroidX ViewModel
- Jetpack DataStore
