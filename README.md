# AirPrint Receiver for Android

Android AirPrint receiver application for local-network photo and PDF transfer from iOS and macOS print menus.

## Scope

- Android 8.0 and newer.
- mDNS/DNS-SD advertisement for `_ipp._tcp`.
- Embedded IPP endpoint for `Get-Printer-Attributes`, `Print-Job`, and document receive flow.
- JPEG and PDF persistence to app-owned storage.
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
