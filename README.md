# AirPrint Receiver for Android

Android app for receiving images and PDFs from Apple devices through the native iOS and macOS print flow, without installing anything on the Apple side.

## Scope

- Android 8.0 and newer.
- mDNS/DNS-SD advertisement for `_ipp._tcp`.
- AirPrint subtype discovery for iOS and macOS.
- Embedded IPP endpoint for printer attributes, print jobs, and document receive flow.
- JPEG and PDF persistence to app-owned storage.
- Optional PDF first-page conversion to JPEG.
- JPEG rotate, crop, and gallery export actions.
- Jetpack Compose UI with Clean Architecture and MVVM.

## Project Layout

| Path | Purpose |
| :--- | :--- |
| `app/src/main/kotlin/com/archura/airprint/ui` | Compose screens, components, navigation, ViewModels |
| `app/src/main/kotlin/com/archura/airprint/domain` | Models, repository interfaces, use cases |
| `app/src/main/kotlin/com/archura/airprint/data` | Local storage and repository implementations |
| `app/src/main/kotlin/com/archura/airprint/infrastructure` | mDNS, IPP server, document decoding, network helpers |
| `docs` | Architecture, protocol, setup, troubleshooting |
| `TASK_PROGRESS.yaml` | Nexus-APCP sprint tracking |

## Development

```powershell
powershell -ExecutionPolicy Bypass -File scripts/checkpoint.ps1
```

Direct Gradle commands:

```powershell
.\gradlew.bat testDebugUnitTest lint detekt
```

## Standards

- Follow Nexus-APCP public/private boundary.
- Keep file names in `snake_case`.
- Keep classes in `PascalCase`.
- Keep functions in `camelCase`.
- Keep constants in `UPPER_SNAKE_CASE`.
- Update `TASK_PROGRESS.yaml` when task status changes.
