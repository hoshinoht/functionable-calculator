@echo off

REM SET the app id
SET APPLICATION_ID=edu.singaporetech.inf2007quiz01

SET APP_APK=..\app\build\outputs\apk\debug\app-debug.apk
SET TEST_APK=app-debug-androidTest.apk
SET TEST_RESULTS=test-results.xml
SET SCRIPT_DIR=%~dp0
SET TARGET_DIR=%SCRIPT_DIR%..


echo Building APKs using Gradle from Parent Directory...
cd /d "%TARGET_DIR%"
call .\gradlew clean assembleDebug
cd test

echo "Uninstalling existing app if present..."
adb uninstall "%APPLICATION_ID%" || echo "No existing app to uninstall."

echo "Uninstalling existing test APK if present..."
adb uninstall "%APPLICATION_ID%.test" || echo "No existing test APK to uninstall."

echo Installing App APK...
adb install -r "%APP_APK%"

echo Installing Test APK...
adb install -r "%TEST_APK%"

echo Running Instrumented Tests with Package: %APPLICATION_ID%
adb logcat -c
adb shell am instrument -w -e class %APPLICATION_ID%.GradingInstrumentedTest %APPLICATION_ID%.test/androidx.test.runner.AndroidJUnitRunner
adb logcat -d -v time GradingInstrumentedTest:D *:S

pause