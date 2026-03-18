#!/bin/bash

# Set the app ID
APPLICATION_ID="edu.singaporetech.inf2007quiz01"

APP_APK="../app/build/outputs/apk/debug/app-debug.apk"
TEST_APK="app-debug-androidTest.apk"
TEST_RESULTS="test-results.xml"
SCRIPT_DIR="$(dirname "$(realpath "$0")")"
TARGET_DIR="$SCRIPT_DIR/.."


echo "Building APKs using Gradle from Parent Directory..."
cd "$TARGET_DIR"
./gradlew clean assembleDebug || { echo "Gradle build failed!"; exit 1; }
cd test

echo "Clearing previous installations..."
adb uninstall "$APPLICATION_ID.test" 2>/dev/null || true
adb uninstall "$APPLICATION_ID" 2>/dev/null || true

echo "Installing App APK..."
adb install "$APP_APK" || { echo "Failed to install App APK!"; exit 1; }

echo "Installing Test APK..."
adb install "$TEST_APK" || { echo "Failed to install Test APK!"; exit 1; }

echo "Running Instrumented Tests with Package: $APPLICATION_ID"
adb logcat -c
adb shell am instrument -w -e class "$APPLICATION_ID.GradingInstrumentedTest" \
    "$APPLICATION_ID.test/androidx.test.runner.AndroidJUnitRunner" || { echo "Tests failed!"; exit 1; }
adb logcat -d -v time GradingInstrumentedTest:D *:S

echo "Tests completed successfully!"

