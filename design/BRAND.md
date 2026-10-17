# Shutter Buddy artwork

The selected logo is variant 2 from round two: a camera aperture inside a Fenix-inspired watch case, with four screws, three left buttons, two right buttons and a mint accent on the upper-right button.

Master: [02-fenix-outdoor.png](logo-proposals/round-2/02-fenix-outdoor.png). Created with the built-in Imagegen tool and approved by the user. The original remains unchanged; generation and correction prompts are preserved alongside it.

## App assets

- Android: adaptive launcher icon with safe margins, a 512-pixel header image and a 192-pixel legacy fallback.
- Android notification: a white watch/aperture vector silhouette.
- Fenix 8 AMOLED 47 mm: 65 × 65 pixels for the launcher, matching the device definition.
- Since Garmin 0.9.0, the watch screen uses a 256 × 256 central logo without a small header logo. The inner aperture is animated geometrically after successful shutter acknowledgement. The case and straps remain unchanged; no GPU transformations or additional bitmap buffers are used.

Regenerate sizes from the master:

```powershell
./tools/Export-BrandAssets.ps1
```

The Android adaptive icon keeps the artwork inside its safe zone. Its dark tile is retained in light mode. The watch loads its logo once and draws it with `drawBitmap`.
