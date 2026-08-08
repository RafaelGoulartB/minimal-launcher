# Minimal Launcher

A minimal Android launcher inspired by the supplied references: a Home screen with a clock and favorite apps, plus a sideways swipe to the full app list with local search.

## Requirements

- A recent Android Studio release
- JDK 17
- Android SDK API 37

Open this folder in Android Studio, let Gradle sync, then run it on a device or emulator. After installation, select **Minimal Launcher** as the device's Home app in Android settings.

## Current features

- Declares itself as a launcher (`HOME` / `DEFAULT`).
- Lists installed apps that expose a launchable activity.
- Adds and removes favorites, persisted with DataStore.
- Provides two horizontal pages, a clock, and local search.

The project intentionally has no services, polling, or background jobs.
