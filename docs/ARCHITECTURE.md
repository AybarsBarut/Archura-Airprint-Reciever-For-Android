# Architecture

## Layers

| Layer | Responsibility |
| :--- | :--- |
| UI | Compose screens, screen state, user actions |
| Domain | Models, repository contracts, use cases |
| Data | Repository implementations and app-owned file storage |
| Infrastructure | AirPrint service lifecycle, mDNS advertisement, IPP handling |

## Receive Flow

```mermaid
flowchart TD
    A["User enables receiver"] --> B["AirPrintReceiverService"]
    B --> C["NsdAirPrintAdvertiser"]
    B --> D["IppPrintServer"]
    E["iOS/macOS Print"] --> D
    D --> F["IppProtocolHandler"]
    F --> G["PrintJobDecoder"]
    G --> H["ReceivedImagesRepository"]
    H --> I["FileStorageManager"]
    H --> J["HomeViewModel"]
    J --> K["HomeScreen grid"]
```

## Initial Constraints

- Local network only.
- Single process Android app.
- One embedded IPP listener.
- App-owned storage, no external database.
- Hilt for dependency injection.
