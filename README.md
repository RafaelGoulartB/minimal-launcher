# Minimal Launcher

Minimal Launcher is a distraction-conscious Android home screen built with Kotlin and Jetpack Compose. It uses a simple text-first interface, keeps favorite apps close, and puts the complete app list one swipe away.

## Features

- Minimal dark home screen with configurable clock, date, battery, and daily usage display
- Searchable app drawer with optional automatic search focus
- Alphabet index for fast navigation through the app drawer
- Favorites and folders that can be reordered on the Home screen
- App renaming, hiding, blocking, uninstall shortcuts, and system app-info access
- Persistent preferences backed by Android DataStore
- Optional usage-stat access for today's screen-time summary
- Automatic app-list refresh after installing or removing an app

## Requirements

- Android Studio with its bundled JDK, or JDK 17
- Android SDK 37
- Android 8.0 (API 26) or newer device/emulator
- ADB for installation and device-based tests

## Build and Run

Clone the repository, then build a debug APK with the Gradle wrapper:

```powershell
.\gradlew.bat :app:assembleDebug
```

On macOS or Linux, use `./gradlew` instead. The APK is generated at `app/build/outputs/apk/debug/app-debug.apk`.

The included Makefile provides shortcuts for common tasks:

```sh
make doctor                         # Check Gradle and connected devices
make debug                          # Build the debug APK
make run DEVICE=emulator-5554       # Install and launch on a device
make home DEVICE=emulator-5554      # Assign Minimal Launcher as Home
```

Run `make help` to see every available command. If multiple Android devices are connected, pass the desired ADB serial through `DEVICE`.

After manual installation, press the device's Home button and select **Minimal Launcher**. You can also change the default Home app in Android system settings.

## Using the Launcher

- Swipe left from Home to open the complete app list.
- Tap an app to launch it; long-press it for management actions.
- Long-press and drag Home items to reorder them.
- Tap a folder to view its apps.
- Use the alphabet rail to jump through the app drawer when search is empty.
- Open Settings from the app list to configure Home details and manage hidden or blocked apps.

Blocking applies only inside Minimal Launcher and does not require an accessibility service. Hidden and blocked apps can be restored from Settings.

## Privacy and Usage Access

Launcher preferences stay on the device. The application declares no internet permission, analytics, or background service.

The daily usage summary is optional. When enabled, grant Android's special Usage Access from the launcher's Settings page; all other launcher features work without it. The access is used to calculate today's foreground-app time.

## Testing

```sh
make check       # Android lint and local JUnit tests
make unit-test   # Local JVM tests only
make ui-test     # Compose tests on a connected device
make test        # Local and device tests
```

See [AGENTS.md](AGENTS.md) for repository layout, coding conventions, and contribution expectations.

## Project Structure

```text
app/src/main/java/com/rafael/minimallauncher/
├── data/       # Installed-app discovery, preferences, and usage stats
└── ui/         # Compose screens, launcher state, and interactions
app/src/main/res/          # Android resources
app/src/test/              # Local JUnit tests
app/src/androidTest/       # Device-based Compose tests
```

## Technology

Kotlin, Jetpack Compose Material 3, AndroidX Lifecycle, DataStore Preferences, JUnit 4, and AndroidX Compose testing.
