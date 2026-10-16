# Shutter Buddy – gewähltes Logo

Der Nutzer hat Variante 2 aus der zweiten Runde gewählt: Blenden-Uhr mit Fenix-inspiriertem Gehäuse, vier Schrauben, drei linken und zwei rechten Tasten sowie mintfarbenem Akzent an der oberen rechten Taste.

Master-Datei: [02-fenix-outdoor.png](logo-proposals/round-2/02-fenix-outdoor.png). Mit dem eingebauten Imagegen-Tool erstellt und vom Nutzer freigegeben. Die ursprüngliche Datei bleibt unverändert; Prompts und gezielte Korrekturen stehen im selben Ordner.

## Verwendung ab 0.8.0

- Android: adaptives Launcher-Symbol mit Sicherheitsabstand für Masken; 512-Pixel-Fassung im Kopfbereich. 192-Pixel-Fassung als Legacy-Fallback.
- Android-Benachrichtigung: eigenes weißes Vektorsymbol mit Uhren- und Blendensilhouette für die Systemleiste.
- Fenix 8 AMOLED 47 mm: 65 × 65 Pixel für die App-Liste gemäß Gerätedefinition, 48 × 48 Pixel im Kopfbereich der App.

Ab Garmin 0.9.0 ersetzt eine 256 × 256 Pixel große Fassung den früheren Kamera-Button in der Mitte. Das kleine Kopflogo entfällt. Während der bestätigten Auslösung wird nur der innere Blendenbereich geometrisch überzeichnet; das gewählte Gehäuse und Armband bleiben unverändert. Keine GPU-Transformationen oder zusätzlichen Bildpuffer.

Die Größenexporte werden ohne gestalterische Änderung aus dem Master erzeugt:

```powershell
./camera-remote/tools/Export-BrandAssets.ps1
```

Androids adaptives Symbol hält das Motiv innerhalb der zentralen Sicherheitszone. Die dunkle Kachel bleibt auch im hellen Design bestehen. Das Uhrenlogo wird einmal geladen und mit dem einfachen `drawBitmap` dargestellt.
