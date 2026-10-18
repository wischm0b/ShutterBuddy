# Development history

This is an archive of the main implementation milestones. See the README and latest release for current setup instructions.

- **0.1.x:** Initial direct Bluetooth connection experiments. Pairing and connection stability issues led to replacing that transport.
- **0.2.x:** Switched to Garmin’s official Connect IQ Android companion SDK and Garmin Connect transport. Added session and sequence validation to avoid replaying shutter commands.
- **0.4.0:** Prefetched single-use shutter tickets reduced round trips. Added phone-controlled self-timers of 0, 3, 5 and 10 seconds. The user confirmed faster triggering and working timers.
- **0.5.x–0.6.x:** Explored a manually requested screenshot preview. Early touch handling accidentally triggered the shutter because `onSelect` consumed events before coordinate handling. Passing selection through resolved that issue. Preview rendering later hit an IQ crash; the supplied log identified `drawBitmap2` with an incompatible paletted bitmap. Native bitmap format and incremental decoding were used in subsequent fixes.
- **0.7.0:** Removed preview, screenshot capability, image transfer and debugging interfaces at the user’s request. Focus returned to shutter and timer operation.
- **0.8.x:** Adopted the Shutter Buddy name and selected Fenix-inspired aperture logo. Simplified the phone UI, retained appearance controls and optional accessibility shutdown.
- **Garmin 0.9.x:** Introduced the central logo, aperture animation, status icons, button markers, bounded delay selection and connection spinner. Android 0.8.1 / Garmin 0.9.1 was the first public GitHub release.
- **0.10.0:** Added single/interval mode switching through a held DOWN button. Interval shooting uses 1–5 second spacing, starts/stops with START, prevents overlapping camera actions and stops when watch heartbeats disappear.
- **0.11.0:** Added phone-managed watch vibration, shutter tone and gentle countdown pulses. English is the default, with a German option shared by both apps. Translated repository documentation and public GitHub descriptions into English.

- **0.11.1:** Moved mode switching to the native MENU action (hold UP). Holding DOWN opens music controls on the user’s Fenix. Removed custom hold timing and updated the phone instructions.

Garmin tests compile in the original environment, but its simulator has not returned executable test results. Hardware behaviour must be checked on the Pixel/Fenix pair.
