# Architecture

The launcher uses one activity and a state-driven Jetpack Compose UI. The
implementation is deliberately small: platform access and persistence live in
`data/`, while screen rendering and user actions live in `ui/`.

## State flow

```text
PackageManager / UsageStats
          |             |
          v             v
    repositories --> LauncherViewModel <--> DataStore preferences
                            |
                            v
                    LauncherUiState
                            |
                            v
                  Compose launcher screens
```

`LauncherViewModel` loads a cached app list first, refreshes launchable
activities from `PackageManager`, reconciles removed package references, and
publishes a single `LauncherUiState`. `LauncherUiStateMapper` derives the
visible drawer, Home items, folders, favorites, and search results from the
installed apps plus persisted preferences.

## Persistence

- `DataStoreLauncherPreferencesRepository` stores Home order, custom labels,
  hidden and blocked app IDs, folders, folder membership, and appearance
  settings.
- Home items use typed references (`app:<id>` and `folder:<id>`), so a folder
  and an app cannot collide in the persisted order.
- The package manager repository keeps a small SharedPreferences cache so the
  launcher can render the last known app list while a refresh is in progress.
- Invalid or removed references are ignored or reconciled during refresh.

## Platform boundaries

The manifest declares `PACKAGE_USAGE_STATS` for the optional daily summary and
`REQUEST_DELETE_PACKAGES` for the Android uninstall confirmation flow. The
launcher queries launchable activities through the package manager and opens
Android settings for Usage Access and app details. It does not request
`INTERNET`.

Blocking is local to Minimal Launcher: a blocked app remains installed and is
not launched through this UI. Hidden apps are also still installed and can be
restored from Settings.

## Tests

Pure reducers, codecs, mappers, usage calculations, and ViewModel behavior are
covered by JVM tests. Compose gestures, semantics, package refresh behavior,
and device integrations belong in `androidTest` and run against a connected
emulator or device.
