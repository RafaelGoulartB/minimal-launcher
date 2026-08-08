# Convenience commands for local Android development.
# Override ADB or DEVICE when needed, e.g.:
#   make run ADB="C:/Users/me/AppData/Local/Android/Sdk/platform-tools/adb.exe"
#   make install DEVICE=emulator-5556

APP_ID := com.rafael.minimallauncher
MODULE := :app
DEVICE ?= emulator-5554

ifeq ($(OS),Windows_NT)
GRADLE ?= .\gradlew.bat
JAVA_HOME ?= C:/Program Files/Android/Android Studio/jbr
ANDROID_HOME ?= $(LOCALAPPDATA)/Android/Sdk
ADB ?= $(ANDROID_HOME)/platform-tools/adb.exe
else
GRADLE ?= ./gradlew
ADB ?= adb
endif

ANDROID_SDK_ROOT ?= $(ANDROID_HOME)
export JAVA_HOME ANDROID_HOME ANDROID_SDK_ROOT
ADB_DEVICE := $(ADB) -s $(DEVICE)

.DEFAULT_GOAL := help
.PHONY: help doctor devices build debug release install run stop uninstall clean check lint unit-test ui-test test bundle apk aab home reset-home logs

help: ## Show all available commands.
	@echo Available commands:
	@echo   doctor         Print Gradle and connected-device information.
	@echo   devices        List Android devices and emulators visible to ADB.
	@echo   build, debug   Build the debug APK.
	@echo   release        Assemble the release APK.
	@echo   install        Install or update the debug APK.
	@echo   run            Install, then open the app on the selected device.
	@echo   stop           Stop the app on the selected device.
	@echo   uninstall      Remove the app from the selected device.
	@echo   clean          Remove Gradle build outputs.
	@echo   check          Run lint and local unit tests.
	@echo   lint           Run Android lint.
	@echo   unit-test      Run local JVM unit tests.
	@echo   ui-test        Run instrumentation tests.
	@echo   test           Run local and instrumentation tests.
	@echo   bundle         Assemble the release Android App Bundle.
	@echo   apk, aab       Build and print the artifact path.
	@echo   home           Set the app as the selected device's Home app.
	@echo   reset-home     Remove the app from the device Home role.
	@echo   logs           Stream Logcat from the selected device.

doctor: ## Print Gradle and connected-device information.
	$(GRADLE) --version
	$(ADB) devices -l

devices: ## List Android devices and emulators visible to ADB.
	$(ADB) devices -l

build: debug ## Build the debug APK.

debug: ## Assemble the debug APK.
	$(GRADLE) $(MODULE):assembleDebug

release: ## Assemble the release APK.
	$(GRADLE) $(MODULE):assembleRelease

install: debug ## Install or update the debug APK on the selected device.
	$(GRADLE) $(MODULE):installDebug

run: install home ## Install, keep Minimal Launcher as Home, then open it on the selected device.
	$(ADB_DEVICE) shell am start -W -n $(APP_ID)/.MainActivity

stop: ## Stop Minimal Launcher on the selected device.
	$(ADB_DEVICE) shell am force-stop $(APP_ID)

uninstall: ## Remove Minimal Launcher from the selected device.
	$(ADB_DEVICE) uninstall $(APP_ID)

clean: ## Remove Gradle build outputs.
	$(GRADLE) clean

check: lint unit-test ## Run static analysis and local unit tests.

lint: ## Run Android lint for the debug variant.
	$(GRADLE) $(MODULE):lintDebug

unit-test: ## Run local JVM unit tests.
	$(GRADLE) $(MODULE):testDebugUnitTest

ui-test: ## Run instrumentation tests, then restore the launcher on the selected device.
	$(GRADLE) $(MODULE):connectedDebugAndroidTest
	$(GRADLE) $(MODULE):installDebug
	$(ADB_DEVICE) shell cmd role add-role-holder --user 0 android.app.role.HOME $(APP_ID)

test: unit-test ui-test ## Run all available tests.

bundle: ## Assemble the release Android App Bundle (.aab).
	$(GRADLE) $(MODULE):bundleRelease

apk: debug ## Print the location of the debug APK.
	@echo app/build/outputs/apk/debug/app-debug.apk

aab: bundle ## Print the location of the release App Bundle.
	@echo app/build/outputs/bundle/release/app-release.aab

home: ## Set Minimal Launcher as the selected emulator's Home app.
	$(ADB_DEVICE) shell cmd role add-role-holder --user 0 android.app.role.HOME $(APP_ID)

reset-home: ## Remove Minimal Launcher from the selected emulator's Home role.
	$(ADB_DEVICE) shell cmd role remove-role-holder --user 0 android.app.role.HOME $(APP_ID)

logs: ## Stream Logcat from the selected device; stop with Ctrl+C.
	$(ADB_DEVICE) logcat -v color
