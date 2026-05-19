# IPP Protocol Notes

## Supported Operations

| Operation | ID | Initial behavior |
| :--- | :--- | :--- |
| `Get-Printer-Attributes` | `0x000B` | Returns minimal success response |
| `Print-Job` | `0x0002` | Extracts document payload and stores supported formats |
| `Create-Job` | `0x0005` | Routed through document handler for MVP compatibility |
| `Send-Document` | `0x0006` | Routed through document handler for MVP compatibility |
| `Get-Job-Attributes` | `0x0009` | Returns bad request until job queue exists |
| `Cancel-Job` | `0x0008` | Returns bad request until job queue exists |

## Document Detection

- JPEG: `FF D8`
- PDF: `%PDF`
- URF: `UNIRAST`
- PWG Raster: `RaS2`

Parser currently keeps the IPP request model small: version, operation ID, request ID, and payload. Full attribute parsing belongs to APR-003.
