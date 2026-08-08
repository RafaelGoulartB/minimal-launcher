# Repository Guidelines

## Project Structure & Module Organization

This is a single-module Android launcher written in Kotlin and Jetpack Compose. Application code lives under `app/src/main/java/com/rafael/minimallauncher/`: `ui/` contains screens, the theme, and `LauncherViewModel`; `data/` contains launcher models, repositories, usage stats, and DataStore preferences. Android resources and the manifest are in `app/src/main/res/` and `app/src/main/AndroidManifest.xml`. Local JVM tests mirror production packages in `app/src/test/`; device-based Compose tests belong in `app/src/androidTest/`. Treat generated `build/` directories as disposable and never commit them.

## Build, Test, and Development Commands

Use the Gradle wrapper directly or the equivalent Make targets:

- `make debug` (or `.\gradlew.bat :app:assembleDebug`) builds the debug APK.
- `make run DEVICE=emulator-5554` installs and launches the app on a selected device.
- `make check` runs Android lint and local unit tests.
- `make unit-test` runs JUnit tests with `testDebugUnitTest`.
- `make ui-test` runs Compose instrumentation tests on a connected emulator/device.
- `make release` and `make bundle` create release APK and AAB artifacts.

Run `make doctor` to verify Gradle, the Android SDK, and ADB connectivity. On Windows, the Makefile defaults `JAVA_HOME` to Android Studio's bundled JBR.

## Coding Style & Naming Conventions

Follow the official Kotlin style configured in `gradle.properties`: four-space indentation, trailing commas in multiline calls, and idiomatic immutable data where practical. Use `PascalCase` for classes, composables, and files; `camelCase` for functions and properties; and descriptive callback names such as `onSearchChange`. Keep UI behavior in `ui/`, persistence and platform data access in `data/`, and user-facing text in `res/values/strings.xml`. Run `make lint` before submitting changes.

## Testing Guidelines

JUnit 4 powers local tests; AndroidX Compose test APIs and Espresso power instrumentation tests. Name test classes after the unit under test (`UsageFormatTest`) and test methods as behavior statements (`negativeDurationIsClamped`). Add fast JVM coverage for pure logic and Compose tests for gestures, semantics, navigation, or state rendering. No numeric coverage threshold is configured; cover new behavior and regressions explicitly.

## Commit & Pull Request Guidelines

History follows Conventional Commit-style prefixes, including `feat:`, `style:`, and `docs:`. Use an imperative, focused subject (for example, `feat: add folder rename action`) and keep unrelated changes separate. Pull requests should explain user-visible behavior, list validation commands and device/API tested, link relevant issues, and include screenshots or recordings for Compose UI changes.
