# Troubleshooting

## Receiver not visible on iOS or macOS

- Confirm both devices share same WiFi network.
- Avoid guest networks with client isolation.
- Confirm receiver mode is enabled.
- Check Logcat for `NsdManager` registration errors.
- Verify port `9100` is not blocked by another process.

## Print job rejected

- Confirm IPP header has at least 8 bytes.
- Confirm operation is supported by MVP route.
- Confirm payload includes JPEG, PDF, URF, or PWG Raster magic bytes.
- Keep job size under the configured app limit.

## Image not visible

- Confirm file exists in app-owned pictures directory.
- Run unit tests for `PrintJobDecoder`.
- Check repository flow refresh after save.
- Inspect Logcat for storage permission or foreground service errors.

## PDF saved instead of image

- Open Settings.
- Set `Received file format` to `PDF to JPEG`.
- Send the AirPrint job again. Existing PDF files are not converted retroactively.
- The converter renders the first PDF page using Android `PdfRenderer`.

## Edit buttons are disabled

- Edit actions currently support JPEG images only.
- If the received item is PDF, enable `PDF to JPEG` in Settings and send the job again.
- `Save gallery` writes JPEG images into the system Pictures collection.
- Android 8 and 9 devices may require storage permission for gallery export.
