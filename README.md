<p align="center">
  <img src="docs/images/banner.png" alt="Archura AirPrint Receiver for Android" width="100%" />
</p>

# Archura AirPrint Receiver for Android — Zero-Install Wireless Print and Document Transfer

[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.0-purple.svg?style=flat-square&logo=kotlin)](https://kotlinlang.org/)
[![Android SDK](https://img.shields.io/badge/Android-8.0%2B%20(API%2026%2B)-green.svg?style=flat-square&logo=android)](https://developer.android.com/)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Navy.svg?style=flat-square&logo=jetpackcompose)](https://developer.android.com/jetpack/compose)
[![Clean Architecture](https://img.shields.io/badge/Architecture-Clean%20%2F%20MVVM-blue.svg?style=flat-square)](#architecture-layout)
[![Detekt](https://img.shields.io/badge/Code%20Style-Detekt-yellow.svg?style=flat-square)](https://detekt.dev/)
[![License: MIT](https://img.shields.io/badge/License-MIT-red.svg?style=flat-square)](LICENSE)

An extremely lightweight, secure, and local-first Android application that turns your Android device into a wireless AirPrint and AirScan Receiver. This allows you to transfer photos, documents, and PDFs from any iOS device (iPhone, iPad) or macOS device directly to your Android device via Apple's native print menu or secure scanner applications like OpenAirScan — without installing any companion app on the Apple side.

By leveraging native printing and scanning protocols, this app functions as a zero-configuration, seamless Apple AirDrop alternative for local cross-platform file sharing.

---

## Key Features

- **Zero-Install Apple Side:** No client app, profile, or configuration needed on iOS/macOS. Simply tap "Print" in any Apple app (Photos, Safari, Files, Notes, etc.) and select your Android device.
- **On-Demand Secure Scanning (AirScan/eSCL):** Full integration with iOS scanner applications such as **OpenAirScan**. When a scan is initiated on the iOS side, Android presents an interactive prompt for file selection.
- **On-the-Fly Image to PDF Conversion:** Selected raw images (JPEG, PNG, etc.) are wrapped on-the-fly into a highly compatible, standard single-page PDF document to guarantee seamless, error-free rendering on iOS.
- **Dynamic In-Memory SSL Server:** Built-in dual-port architecture with a plain HTTP server on port 8080 (for IPP) and an HTTPS SSL server on port 8081 (for secure AirScan capabilities and jobs) utilizing self-signed in-memory certs generated on-the-fly.
- **mDNS / DNS-SD Service Advertisement:** Concurrently advertises standard IPP printer services (`_ipp._tcp`) and secure scanner services (`_uscans._tcp`) to ensure immediate zero-configuration network discovery.
- **Interactive Document Operations:** Clean Compose UI to preview received files, perform rotation, crop JPEGs, and export directly to the Android System Gallery via MediaStore.
- **Modern Jetpack Compose UI:** Designed with strict adherence to Clean Architecture and MVVM pattern, featuring a dark mode interface and responsive user elements.

---

## User Interface Preview

<p align="center">
  <img width="1080" height="2400" alt="Screenshot_2026-05-19-14-35-14-866_com archura airprint" src="https://github.com/user-attachments/assets/40ba71ca-924b-477c-870c-46b4c9939526" />
</p>

---

## Protocol Workflow

The application achieves zero-install document transfer by mimicking an enterprise-grade AirPrint printer and an eSCL-compatible network scanner. The network sequence operates as follows:

```mermaid
sequenceDiagram
    autonumber
    participant Apple as iOS / macOS Client (e.g. OpenAirScan)
    participant Android as Archura AirPrint/AirScan Service
    participant Storage as Local Storage & MediaStore

    Note over Android: Starts Background Service & mDNS Advertisements (_ipp and _uscans)
    Apple->>Android: mDNS Discovery Query (_uscans._tcp.local)
    Android-->>Apple: TXT/SRV Record Response (https scheme, secure port 8081)
    Note over Apple: Android device appears in network scanner list
    Apple->>Android: HTTPS GET /eSCL/ScannerCapabilities
    Android-->>Apple: Secure XML Response (A4, Supported resolutions, Document formats)
    Apple->>Android: HTTPS POST /eSCL/ScanJobs
    Note over Android: Triggers on-screen interactive scan dialog in Android UI
    Android-->>Apple: HTTPS Response (201 Created with Job URL)
    loop Document Polling
        Apple->>Android: HTTPS GET /eSCL/ScanJobs/{id}/NextDocument
        alt User is selecting file
            Android-->>Apple: HTTP 503 Service Unavailable (Retries indefinitely)
        else File selected (Image/PDF)
            Note over Android: Automatically packages images to PDF on-the-fly
            Android-->>Apple: HTTP 200 OK (With PDF document payload)
        end
    end
    Apple->>Android: HTTPS GET /eSCL/ScanJobs/{id}/NextDocument (Subsequent call)
    Android-->>Apple: HTTP 404 Not Found (Signal scan completion)
    Note over Apple: Document loaded successfully and ready for save/share
```

---

## Project Layout

The codebase strictly follows the Nexus-APCP public/private boundary layout to ensure optimal modularity and code maintenance:

| Directory/File Path | Purpose |
| :--- | :--- |
| **`app/src/main/kotlin/com/archura/airprint/ui`** | Compose screens, custom components, styling, navigation, and ViewModels. |
| **`app/src/main/kotlin/com/archura/airprint/domain`** | Pure domain objects, entities, repository interfaces, and core business use cases. |
| **`app/src/main/kotlin/com/archura/airprint/data`** | Local data sources, storage services, repository implementations, and entity mappers. |
| **`app/src/main/kotlin/com/archura/airprint/infrastructure`** | Low-level network responders, mDNS advertisements, IPP socket parser, eSCL protocol handler, and format decoders. |
| **`docs/`** | Technical deep dives including architecture, IPP protocol detail, setup, and troubleshooting. |
| **`scripts/`** | Repository maintenance and rapid local environment setup tools. |

---

## Development & Build Setup

### System Requirements
- **JDK 17** or newer
- **Android SDK 26** (Android 8.0 Oreo) or newer
- **Gradle 8.2+** (configured in gradle wrapper)

### Checkpoint Operations
To ensure all public and private API boundaries are perfectly intact during active development, utilize the provided script:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/checkpoint.ps1
```

### Direct Gradle Execution
Run standard unit tests, detekt code style checkers, and linters using:

```powershell
powershell -ExecutionPolicy Bypass -Command ".\gradlew.bat testDebugUnitTest lint detekt"
```

---

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

Developed by **Aybars Barut** and the **Archura** team.
