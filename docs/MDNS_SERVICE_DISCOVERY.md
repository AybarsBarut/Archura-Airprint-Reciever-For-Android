# mDNS Service Discovery

The receiver advertises `_ipp._tcp.` through Android `NsdManager` and runs a small multicast DNS responder for AirPrint-specific records.

## TXT Records

| Key | Value |
| :--- | :--- |
| `txtvers` | `1` |
| `qtotal` | `1` |
| `rp` | `ipp/print` |
| `ty` | Configured printer name |
| `product` | `(Archura AirPrint Receiver)` |
| `pdl` | `image/jpeg,application/pdf,image/urf,image/pwg-raster` |
| `URF` | `CP1,IS1-4-5,MT1-2-3-4-5-6,RS600,V1.4,W8,SRGB24` |
| `Color` | `T` |
| `Duplex` | `F` |
| `Scan` | `F` |
| `Fax` | `F` |
| `Binary` | `T` |
| `Transparent` | `T` |
| `air` | `none` |
| `kind` | `document,envelope,label,postcard` |
| `UUID` | Stable app-generated UUID |
| `adminurl` | Local HTTP URL when local IPv4 address is known |

## AirPrint Subtype

iOS and macOS commonly browse `_universal._sub._ipp._tcp.local.` when looking for AirPrint-compatible targets. Android `NsdManager` does not reliably publish that subtype on the MVP device, so `AirPrintMdnsResponder` answers multicast DNS queries for:

- `_ipp._tcp.local.`
- `_universal._sub._ipp._tcp.local.`
- the service instance SRV and TXT records
- the local IPv4 A record
