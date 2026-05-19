# Setup Guide

## Requirements

- Android SDK with API 34.
- JDK 17. Android Studio bundled JBR works.
- Gradle wrapper from this repository.

## Build

```powershell
.\gradlew.bat assembleDebug
```

## Test

```powershell
.\gradlew.bat testDebugUnitTest
```

## Device Check

1. Install debug APK on Android device.
2. Put Android and iOS or macOS device on same WiFi network.
3. Enable receiver mode in app.
4. Optional: open Settings and set `Received file format` to `PDF to JPEG` if AirPrint PDFs should be converted for the gallery.
5. Open iOS or macOS print menu.
6. Select `Archura AirPrint Receiver`.
7. Send JPEG or PDF.
8. Verify received item appears in home grid.

## Image Actions

Tap a received JPEG card to open image actions:

- `Rotate left`
- `Rotate right`
- `Crop square`
- `Save gallery`

PDF files must be converted to JPEG first by setting `Received file format` to `PDF to JPEG` before receiving the AirPrint job.
