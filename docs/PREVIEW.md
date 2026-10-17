# Archived preview experiment — removed in 0.7.0

The user chose to focus on shutter and timer controls after trying the still-image preview. Since 0.7.0 the apps contain no screenshot capability, preview requests, image transfer or preview UI. The following notes describe historical versions only.

## Capture and transport

The experiment used Android 14’s `AccessibilityService.takeScreenshotOfWindow` for the visible Pixel Camera window. It checked the active package, window ID and unlocked state before capture, in the callback and before transfer. There was no full-screen fallback, gallery access, file output or upload.

Frames were reduced to RGB332, at most 128 pixels per side and 9,216 pixels total, then Base64-encoded to at most 12,288 characters. Protocol v2 used session-bound `preview`, `previewFrame` and `previewError` messages with a separate request ID. A serializer test checked that the largest frame plus metadata stayed below 16 KiB.

This was a manually requested screenshot, including camera overlays, rather than a direct camera stream or live view. Later versions cropped the camera window geometrically before reduction; this could remove scene edges and did not identify the exact viewfinder.

## Watch rendering and fixes

A preview initially worked once before an IQ crash. Version 0.6.0 reduced rendering load with incremental decoding and a small buffer. The user-supplied crash log from October 15, 2026 identified an exception in `drawBitmap2`. Version 0.6.1 switched from a paletted bitmap to the device’s native format for GPU scaling and caught rendering exceptions. These experiments were subsequently removed.

Early preview taps also triggered the shutter because Garmin’s selection behaviour consumed touch before coordinate handling. Version 0.5.2 allowed events to reach `onTap` and `onKey`; changing touch target sizes alone had not fixed that issue.

## API references

- [Android AccessibilityService](https://developer.android.com/reference/android/accessibilityservice/AccessibilityService)
- [Garmin StringUtil](https://developer.garmin.com/connect-iq/api-docs/Toybox/StringUtil.html)
- [Garmin Graphics](https://developer.garmin.com/connect-iq/api-docs/Toybox/Graphics.html)
