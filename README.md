# Shutter Buddy: Garmin 0.9.1 / Android 0.8.1

<img src="app/src/main/res/drawable-nodpi/brand_logo.png" width="160" alt="Shutter Buddy Logo">

Fernauslöser für die Pixel-Standardkamera auf dem Pixel 8 Pro, bedient mit der Garmin Fenix 8 AMOLED 47 mm. Fotos und Kameraeinstellungen bleiben in der Pixel-Kamera. Kommunikation über Garmin Connect.

**[Installationsdateien herunterladen](https://github.com/wischm0b/ShutterBuddy/releases/latest)** · [Selbst bauen](docs/BUILD.md)

## Funktionen

- Foto über START oder den runden Kamera-Button auf der Uhr aufnehmen.
- Vorlauf AUS / 3 / 5 / 10 Sekunden, wählbar mit UP/DOWN oder Tippen auf die Vorlaufanzeige. Auswahl bleibt gespeichert.
- START oder BACK während des Countdowns fordert den Abbruch an. Die Bestätigung auf der Uhr abwarten.
- Dunkles Design als Standard; am Handy optional Hell oder System.
- Bedienungshilfe wahlweise beim Beenden deaktivieren. Zum nächsten Start muss sie in Android wieder manuell eingeschaltet werden.

## Garmin 0.9.1: Tastenpositionen und Statuszeile

- Tastenmarkierungen anhand der Fenix-8-SDK-Geometrie ausgerichtet: UP links auf Mittelhöhe, DOWN links unten etwas höher als zuvor, START rechts oben etwas tiefer. Größere Symbole ohne zusätzliche Richtungsstriche; dieselben Koordinaten gelten für ihre Touch-Flächen.
- Vorlauf bleibt bei 0 bzw. 10 Sekunden stehen. Die vier Werte 0 / 3 / 5 / 10 bleiben erhalten. Plus bei 10 und Minus bei 0 sind abgedunkelt.
- Stoppuhr und Wert werden als Gruppe zentriert, mit 16 Pixel Abstand zwischen Symbolbereich und tatsächlich gemessener Textbreite. Auch die zweistellige 10 überlappt nicht mehr.
- Alle Statussymbole stehen oben. Bei Verbindungssuche rotiert ein Ladekreis mit 10 Bildern/s. Nach erfolgreicher Verbindung kehrt die Oberfläche zum langsamen 3-Sekunden-Intervall zurück; Verbindungsversuche bleiben auf 3 Sekunden Abstand begrenzt. Keine zusätzlichen Timer.
- Nur Uhr-App aktualisieren. Release und acht Garmin-Eingabe-/Grenzwerttests kompilieren. Die Tests sind mangels funktionierendem lokalen Simulator noch nicht als ausgeführt bestätigt. Der Nutzer hat die Oberfläche von 0.9.1 auf seiner Uhr bestätigt.

## Garmin 0.9.0: Oberfläche mit Symbolen

- Großer Logo-Auslöser in der Mitte; Appname, kleines Kopflogo, Statusbalken und zweizeilige Meldungen entfernt.
- UP erhöht den Vorlauf (+ links mittig), DOWN verringert ihn (− links unten). Rechts oben zeigt ein Auslösersymbol auf START. Die markierten Flächen funktionieren auch per Touch.
- Oben: grüne Kette für bestätigte Verbindung, getrennte gelbe Kette beim Verbindungsaufbau. Unten: Timer-Symbol und Vorlaufwert; 0 bedeutet ohne Vorlauf.
- Rückmeldung unten rechts: gelbe Sanduhr bei laufendem Befehl/Countdown, grüner Haken nach bestätigter Bedienaktion, graues Abbruchsymbol bei bestätigtem Abbruch, rotes Warndreieck bei Fehler oder fehlender Rückmeldung. Details stehen weiterhin in der Handy-App.
- Countdown-Ziffern erscheinen im Logo. Die Blende schließt und öffnet sich nach erfolgreicher Ergebnisantwort vom Handy in 13 Schritten zu 50 ms (ca. 0,65 s). Keine Erfolgsanimation nur aufgrund des Sendens. Grafik mit wenigen Polygonen ohne transformierte Bitmap; bestehender Countdown-Timer wird nach dessen Ende wiederverwendet.
- Nur die Uhr-App aktualisieren. Android 0.8.1 bleibt kompatibel. Garmin-Release und sechs Eingabetests kompilieren; Ablauf und Animation müssen auf der Fenix geprüft werden.

## Umbenennung in 0.8.1

Die App heißt auf Handy und Uhr jetzt **Shutter Buddy**. Anzeigename, Oberfläche, Bedienungshilfen-Bezeichnung, Benachrichtigung und Installationsdateien sind umbenannt. Android-Paket-ID, Garmin-App-ID und Signaturen bleiben für ein normales Update erhalten.

Android- und Garmin-Build erfolgreich, 22 Android-Tests bestanden und Lint erfolgreich. Android-Anzeigename im fertigen APK und übereinstimmende Update-Signatur geprüft. Prüfung auf den tatsächlichen Geräten steht noch aus.

## Logo seit 0.8.0

Ausgewähltes Logo: Variante 2 aus der zweiten Runde, Blenden-Uhr mit Fenix-inspiriertem Gehäuse. Als Launcher-Symbol und im Kopfbereich beider Apps eingebaut; Android erhält zusätzlich ein weißes Benachrichtigungssymbol. [Logoquelle und Größenexporte](design/BRAND.md).

0.8.0: Android- und Garmin-Release-Build erfolgreich. 22 Android-Unit-Tests bestanden, Lint erfolgreich. APK-Signatur mit 0.7.0 identisch. Größenexporte visuell geprüft; Darstellung auf den tatsächlichen Geräten noch zu bestätigen.

## Bereinigung in 0.7.0

Vorschau, Screenshot-Fähigkeit, Testfoto, Kamera-Diagnose und Verbindungsprotokoll vollständig aus den Apps entfernt. Auf der Uhr zeigt die frühere Vorschaufläche nur noch den Bereitschaftsstatus. Einrichtung, Fehlermeldungen mit Handlungshinweisen, Timer und der schnelle Auslöseweg bleiben erhalten. Unter Einstellungen lässt sich bei Bedarf eine andere Uhr freigeben.

Die früheren Logoentwürfe bleiben unter [design/logo-proposals](design/logo-proposals/PROMPTS.md) erhalten.

## Installation und Verwendung

1. `ShutterBuddy-0.8.1.apk` aus dem aktuellen GitHub-Release auf dem Pixel als Update installieren; vorhandene App und Einstellungen behalten.
2. Die bisherige PixelRemote-/ShutterBuddy-PRG unter `GARMIN/APPS` auf der Uhr durch `ShutterBuddy-0.9.1-fenix847mm.prg` aus dem Release ersetzen. Keine zweite Kopie der App liegen lassen.
3. Die Uhr muss in Garmin Connect verbunden sein. Keine zusätzliche Bluetooth-Kopplung für Shutter Buddy nötig.
4. In Shutter Buddy die Bedienungshilfe einrichten und Fernbedienung starten. Shutter Buddy auf der Uhr öffnen.
5. Pixel-Kamera im Fotomodus öffnen. Handy entsperrt und Display eingeschaltet lassen. START auf der Uhr drücken.

Einen zusätzlichen Selbstauslöser in der Pixel-Kamera ausschalten, sonst addiert sich dessen Vorlauf. Der Countdown läuft auf dem Pixel; die Sekundenanzeige auf der Uhr ist wegen der Funklaufzeit angenähert. „Auslöser betätigt“ bestätigt die Bedienaktion; das gespeicherte Foto kommt weiterhin aus der Pixel-Kamera.

## Entwicklung

- Android: `./build.ps1 -Target Android` führt Unit-Tests, Release-Build und Lint aus.
- Garmin: `./build.ps1 -Target Garmin` baut für `fenix847mm` mit dem lokal installierten SDK.
- Paket-ID Android: `de.jr.pixelremote`; Garmin-App-ID: `b7e89053786045fc9a3c9d70dfd8a174`.
- Gleiche Android-Signatur für Updates. Der private Garmin-Schlüssel ist lokal und nicht Teil der Auslieferung.
- Die bisherigen Auslöse- und Timer-Versionen und zuletzt 0.6.1 wurden am Gerät vom Nutzer bestätigt. Der aktuelle Stand mit neuen Symbolen benötigt einen kurzen Gerätetest.
- 0.7.0: 22 Android-Unit-Tests bestanden; Android-Release-Build und Lint erfolgreich. APK-Signatur geprüft und mit 0.6.0 identisch. Garmin-Release und vier Eingabetests kompilieren erfolgreich.
- Garmin-Eingabetests sind vorhanden und kompilieren. Der lokale Simulator lieferte in dieser Umgebung bisher keine Testergebnisse.

Historische Versuche und Einschränkungen: [Entwicklungshistorie](docs/DEVELOPMENT_HISTORY.md), [ehemalige Vorschau](docs/PREVIEW.md).
