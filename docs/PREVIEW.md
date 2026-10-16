# Historischer Vorschauversuch – ab 0.7.0 entfernt

Der Nutzer hat sich nach dem erfolgreichen Standbildversuch für den Fokus auf Auslöser und Timer entschieden. Screenshot-Code, Screenshot-Fähigkeit, Bildübertragung und Vorschauoberflächen sind mit 0.7.0 entfernt. Die folgenden Abschnitte dokumentieren ausschließlich frühere Versionen.

## Korrektur 0.6.1 nach Absturzprotokoll

`CIQ_LOG.BAK` vom Nutzer: 15.10.2026 09:56:16 UTC, Firmware 23.40, CIQ-API 6.0.3, `drawPreview`, Zeile 74: Exception in `drawBitmap2`. Der native kleine Bildpuffer darf für die GPU-Skalierung keine Palette haben. 0.6.1 lässt `:palette` und `:colorDepth` bei der Bitmap-Erstellung weg und verwendet das native Geräteformat. Die Array-Farbtabelle wird nur zum Dekodieren der RGB332-Pixel verwendet, nicht als Bitmap-Palette. Anzeige-Exceptions werden abgefangen. Ursache dieser Kombination ist durch eine [Erklärung eines Garmin-Entwicklers](https://forums.garmin.com/developer/connect-iq/f/discussion/336610/sdk-6-2-0/1636273) gestützt. Garmin-Regressionstest für wiederholtes Dekodieren und transformiertes Zeichnen ergänzt; auf dem tatsächlichen Gerät noch zu bestätigen.

## Änderungen nach dem ersten Gerätetest

0.5.2 lieferte einmal ein Bild; danach meldete die Uhr IQ! (Absturz). Die genaue Ursache ist ohne Garmin-Log unbekannt. 0.6.0 reduziert die Grafikbelastung: maximal 9.216 Pixel im nativen 8-Bit-Palettenpuffer, schrittweiser Aufbau von vier Zeilen pro 50 ms und Vergrößerung per `drawBitmap2`-Transformation erst beim Anzeigen. Keine vorab vergrößerte Bitmap mit Standardfarbtiefe mehr. Abbruch und Beenden stoppen den Decoder und lösen Referenzen.

Die Android-Aufnahme wird vor der Verkleinerung geometrisch beschnitten: Hochformat maximal 3:4, Querformat maximal 4:3, Mittelpunkt auf der langen Achse bei 46 %. Dieser zentrale Ausschnitt entfernt die großen äußeren Bedienflächen typischer Kameraansichten; exakte Suchergrenzen werden nicht erkannt. Symbole im Sucher bleiben sichtbar, Bildränder können fehlen. Übertragungslimits bleiben unverändert. Beide Apps auf 0.6.0 aktualisieren. Darstellung bis 78 % der Uhrbreite bzw. 59 % der Höhe unter Beibehaltung des Seitenverhältnisses.

Die folgenden Abschnitte beschreiben den ursprünglichen Versuch 0.5.0; Aufnahmebeschränkung und Übertragungsformat gelten weiterhin. Der ehemalige vollständige Screenshot und der vorab vergrößerte Bitmap-Aufbau sind mit 0.6.0 ersetzt.

Garmin 0.5.2 lässt das allgemeine Auswahl-Ereignis zu `onTap` / `onKey` durchfallen. Zuvor fing `onSelect` Touch ab und löste aus, bevor die Koordinatenprüfung erreicht wurde; größere Flächen in 0.5.1 allein halfen daher nicht. Nur der runde Kamera-Button bzw. der physische START-Knopf lösen aus. Android bleibt bei 0.5.0.

Erster implementierter Versuch. Noch kein Nachweis auf dem tatsächlichen Pixel/Fenix-Paar.

## Kameraaufnahme

Android `AccessibilityService.takeScreenshotOfWindow` ab API 34, mit deklarierter Fähigkeit `canTakeScreenshot`. Der Dienst prüft vor der Anfrage, im Screenshot-Callback und vor der Übergabe des kleinen Bildes: aktives Paket `com.google.android.GoogleCamera`, identische Fenster-ID, Handy entsperrt. Es gibt keinen Fallback auf ein anderes Fenster oder den vollständigen Bildschirm. Geschützte Fenster führen regulär zum Fehler.

Die Hardwareaufnahme wird auf einem separaten Worker in ein kleines RGB332-Bild umgewandelt. HardwareBuffer und große Bitmaps werden geschlossen/recycelt. Keine Dateiausgabe, keine Speicherung in der Galerie, kein Upload. Ein lokales Testbild kann auf expliziten Test hin im Activity-Speicher verbleiben und wird beim Beenden der Funktion verworfen.

Der erste Stand zeigt das gesamte Kamera-Fenster; UI-Elemente werden nicht erraten oder abgeschnitten. Die Aufnahme kann durch Änderungen der Pixel-Kamera oder geschützte Surface-Inhalte schwarz/fehlerhaft sein. Dies muss der Gerätetest zeigen.

## Übertragung

Protokollversion 2, bestehendes Sitzungstoken. Anfrage `preview` mit eigenem `pid`, unabhängig von Auslösesequenzen. Erfolgsantwort `previewFrame` mit `pid`, `width`, `height`, `encoding=rgb332-base64`, `pixels`, `ageMs`. Fehlerantwort `previewError` mit lokal protokollierter Beschreibung.

Höchstens 128 Pixel je Seite und 9.216 Pixel insgesamt, Seitenverhältnis erhalten. Ein Byte pro Pixel: Rot in Bits 7–5, Grün in 4–2, Blau in 1–0. Base64 ohne Zeilenumbrüche; maximal 12.288 Zeichen. Der SDK-Serializer wird mit einer maximalen Beispielnachricht getestet (unter 16 KiB einschließlich Metadaten).

Eine neue Anfrage wird während laufender Aufnahme/Timer abgelehnt und auf mindestens zwei Sekunden Abstand begrenzt. Eine noch nicht versendete Vorschau wird bei `prepare`, `shutter` oder `cancel` ungültig. Dienstende, neue Sitzung und Verbindungsabbruch verhindern die spätere Weitergabe. Bereits gesendete Nachrichten lassen sich im Garmin-Transport nicht priorisiert zurückrufen; dessen reale Laufzeit ist zu messen.

## Darstellung auf der Uhr

RGB332 wird über `StringUtil.convertEncodedString` dekodiert, Form und Nutzlastgröße werden geprüft. Einmaliges Zeichnen in eine BufferedBitmap mit Vergrößerung um maximal Faktor 3; danach normale Bitmap-Darstellung. Alter wird konservativ ab Anfragezeit angenähert. Es gibt keine automatische Aufnahme, Wiederholung oder laufende Live-Vorschau.

**VORSCHAU** oder **MENU halten** öffnet den Abruf. **NEUES STANDBILD** / **DOWN** aktualisiert. **BACK** / **UP** schließt und verwirft den Puffer. **START** behält seine Auslösefunktion, verwirft das Bild und kehrt in die Aufnahmeansicht zurück. Antworten auf inzwischen abgebrochene oder durch Auslösen überholte Anfragen werden ignoriert.

## Primärquellen

- [Android AccessibilityService](https://developer.android.com/reference/android/accessibilityservice/AccessibilityService)
- [Garmin StringUtil](https://developer.garmin.com/connect-iq/api-docs/Toybox/StringUtil.html)
- [Garmin Graphics](https://developer.garmin.com/connect-iq/api-docs/Toybox/Graphics.html)
