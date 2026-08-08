# Project instructions

## Goal

This repository contains a fast, resource-efficient Android launcher. The interface must retain a black background, readable typography, and a text-first application list.

## Stack and structure

- Kotlin, Jetpack Compose, and Material 3.
- `data/`: installed-app discovery and favorite persistence.
- `ui/`: state, ViewModel, and UI components.
- Do not add libraries, background services, analytics, or permissions unless there is a clear need.

## Implementation rules

- List only activities exposing `ACTION_MAIN` with `CATEGORY_LAUNCHER`.
- Store favorites as `ComponentName.flattenToString()` values in DataStore.
- Run `PackageManager` queries, sorting, and filtering off the main thread.
- Use `LazyColumn` for every app list and stable keys based on the component.
- Do not load icons in bulk; the initial UI is intentionally text-first.
- Clock or battery updates may run only while the Activity is visible. Never create a service solely for this.
- Handle `ActivityNotFoundException` when opening an app that may have just been removed.

## Quality

- Before delivering changes, sync and run `assembleDebug` with JDK 17 and the Android SDK installed.
- Prefer ViewModel/repository tests for behavior and UI tests for primary flows.
- Keep compatibility with Android 8.0 (API 26) and higher unless an explicit decision changes it.
