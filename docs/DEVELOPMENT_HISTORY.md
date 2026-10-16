# Pixel Remote: Android 0.6.0 / Garmin 0.6.1

Fernbedienung für die **Pixel-Standardkamera** (`com.google.android.GoogleCamera`), getestet mit dem **Pixel 8 Pro**; Uhr-App für **Fenix 8 AMOLED 47 mm** (`fenix847mm`). Darkmode ist Standard, optional Hell oder System.

## Garmin 0.6.1: protokollierten Darstellungsabsturz korrigiert

Das bereitgestellte `CIQ_LOG.BAK` protokolliert am 15.10.2026 um 09:56:16 UTC eine unbehandelte Exception in `drawPreview`, `PixelRemote.mc:74`, beim Aufruf von `drawBitmap2`. Der 8-Bit-Palettenpuffer aus 0.6.0 ist mit GPU-Transformationen nicht kompatibel; dies wird auch von einem [Garmin-Entwickler erläutert](https://forums.garmin.com/developer/connect-iq/f/discussion/336610/sdk-6-2-0/1636273). Der Decoder erzeugt jetzt eine Bitmap in nativer Gerätefarbtiefe ohne Palette. Die kleinen Bildabmessungen, der schrittweise Aufbau und die große Darstellung bleiben erhalten. Die Anzeige fängt Grafik-Exceptions zusätzlich ab, verwirft das Bild und zeigt einen Fehler statt die Auslöser-App zu beenden. Der neue Regressionstest dekodiert und skaliert 16 kleine Bilder nacheinander und kontrolliert das native Farbformat. Android bleibt bei 0.6.0.

## 0.6.0: größerer Bildausschnitt und entlasteter Bildaufbau

- **Android und Uhr aktualisieren.** Der Nutzer konnte mit 0.5.2 einmal ein Bild empfangen; danach zeigte die Uhr IQ! (Absturz). Ohne `GARMIN/APPS/LOGS/CIQ_LOG.YML` ist die konkrete Absturzursache noch nicht belegt.
- Auf der Uhr ersetzt ein nativer kleiner 8-Bit-Palettenpuffer die vorher vergrößerte Bitmap mit Standardfarbtiefe. Der Bildaufbau erfolgt in vier Zeilen je 50-ms-Schritt statt in einem einzigen Nachrichtenereignis. Grafikskalierung erfolgt erst beim Zeichnen; kein großer zweiter Bildpuffer. BACK/START/Beenden brechen laufenden Bildaufbau ab und geben Puffer frei.
- Android schneidet vor dem Verkleinern einen zentralen 3:4-Ausschnitt im Hochformat bzw. 4:3-Ausschnitt im Querformat aus dem Kamera-Fenster aus. Die langen Achsen werden bei 46 % zentriert. Dies ist eine geometrische Annäherung, keine Erkennung der exakten Suchergrenzen: Ränder des Motivs können fehlen, Symbole im Sucher bleiben sichtbar. Kein Zugriff auf den direkten Kamerastream.
- Auf der Uhr wird das Bild unter Wahrung des Seitenverhältnisses auf bis zu 78 % der Displaybreite bzw. 59 % der Höhe skaliert. Der Bildinhalt nutzt dadurch mehr der begrenzten Übertragungsauflösung. Es bleibt ein farbreduziertes Standbild, keine Live-Vorschau.
- Neu am Pixel/Fenix zu prüfen: mehrfach abrufen, mit BACK schließen und erneut öffnen, während des Bildaufbaus START auslösen. Falls IQ! wiederkehrt, das Garmin-Absturzprotokoll zur Bestimmung des genauen Fehlers auslesen.

## Garmin 0.5.2: Auswahl-Ereignis und Touch getrennt

Garmins BehaviorDelegate ruft bei Touch zunächst `onSelect` auf; dessen bisheriges `true` unterdrückte `onTap` vollständig und löste sofort aus. `onSelect` gibt nun `false` zurück. Nur `onKey(KEY_ENTER)` löst über START aus; Touch wird anhand seiner Koordinaten verarbeitet. Die reine Flächenkorrektur aus 0.5.1 behob diese Ursache nicht. Vier Garmin-Regressionstests prüfen die Ereignisweitergabe und die getrennten Aktionen. Release und Testprogramm kompilieren; der lokale Simulator lieferte keine Ergebnisse, deshalb sind die Tests noch nicht als bestanden bestätigt. Android bleibt bei 0.5.0.

## Garmin 0.5.1: größere Touch-Flächen (allein nicht ausreichend)

- Vorschau-Schaltfläche und ihr Touch-Bereich vergrößert, mit Abstand zum Auslöser.
- Nur ein Tipp innerhalb des runden Kamera-Auslösers nimmt ein Foto auf. Freie Flächen und das Vorschaubild lösen nicht mehr aus. START bleibt der Hardware-Auslöser.
- Nur die Uhr-App aktualisieren; Android 0.5.0 bleibt kompatibel. Garmin-Build erfolgreich; Touch-Verhalten muss auf der Uhr geprüft werden.

## Neu in 0.5.0: erste Standbild-Vorschau

- Beide Apps auf **0.5.0** aktualisieren. Für die zusätzliche Screenshot-Fähigkeit die Bedienungshilfe nach dem Update bei Bedarf einmal deaktivieren und neu aktivieren.
- Pixel-Kamera sichtbar und Handy entsperrt lassen. Auf der Uhr oben **VORSCHAU** antippen oder **MENU halten**. Das Kamera-Fenster wird einmal aufgenommen, farbreduziert und als kleines Standbild übertragen. Android 14 oder neuer ist Voraussetzung; ein geschütztes Fenster wird nicht umgangen.
- **NEUES STANDBILD** antippen bzw. **DOWN** drücken, um erneut abzurufen. **BACK** oder **UP** schließt das Bild und gibt den Bildpuffer frei. **START** löst weiterhin aus und kehrt zur Aufnahme-/Timeranzeige zurück. Der gespeicherte Vorlauf bleibt wirksam.
- Die Altersanzeige ist angenähert und konservativ ab dem Vorschauabruf berechnet. Das Bild wird nicht automatisch aktualisiert. Aktuell wird das ganze Kamera-Fenster mit seinen Bedienelementen gezeigt; ein sauberer Sucherausschnitt ist noch nicht implementiert.
- Unter **Einstellungen & Hilfe → Vorschau-Test in 5 Sekunden** lässt sich dieselbe Bildaufnahme ohne Uhr testen. Fünf Sekunden in der Kamera bleiben, dann zurückkehren und das kleine Testbild in den Einstellungen ansehen. Es wird nur im Arbeitsspeicher gehalten und bei vollständigem Beenden verworfen.
- Das Bild wird weder in eine Datei noch in die Fotobibliothek geschrieben. Vor und nach der Aufnahme wird kontrolliert, dass weiterhin das Pixel-Kamera-Fenster aktiv und das Handy entsperrt ist. Es wird ausschließlich dieses Fenster über `takeScreenshotOfWindow` aufgenommen; kein Fallback auf den gesamten Bildschirm.
- Eine noch nicht versendete Vorschau wird bei einem Auslöse-/Timerbefehl verworfen. Während eines wartenden Timers startet keine Vorschau. Ein bereits bei Garmin eingereihter Bildtransfer kann jedoch nicht zurückgerufen werden und vorübergehend Bandbreite beanspruchen. Für den ersten Versuch gibt es daher nur manuellen Abruf.
- Transportformat: RGB332 (256 Farben), längste Seite höchstens 128 Pixel und maximal 9.216 Pixel insgesamt; Seitenverhältnis bleibt erhalten. Base64-Nutzlast höchstens 12.288 Zeichen. Ein Test verwendet den echten SDK-Serializer und bestätigt, dass das größte Bild samt Metadaten unter 16 KiB bleibt. Auf der Uhr wird daraus einmalig eine vergrößerte Bitmap aufgebaut.
- **Noch nicht am echten Pixel/Fenix geprüft.** Die Tests validieren Format, Paketgröße, Nichtaufnahme bei fremdem Vordergrundfenster und Verwerfen bei Aufnahme/Beenden. Ob die Pixel-Kamera ein korrektes Kamerabild liefert und wie schnell es übertragen wird, muss der erste Gerätetest zeigen. Schnellere Auslösung und Timer aus 0.4.0 wurden vom Nutzer bestätigt.

## Neu in 0.4.0

- **Schneller Auslöser:** Einmalfreigaben werden bei Verbindungsprüfung und Ergebnisantwort bereits vorbereitet. Beim Tastendruck reicht mit frischer Freigabe eine einzige Befehlsnachricht an das Pixel statt der bisherigen Vorbereitung plus Befehl. Eine konkrete Latenz wurde noch nicht am Gerät gemessen. Garmin-Transport und Pixel-Kamera bestimmen weiterhin einen Teil der Verzögerung. Fehlt eine gültige Freigabe, wird der bisherige sichere Vorbereitungsweg verwendet.
- **Selbstauslöser auf der Uhr:** unten **VORLAUF AUS / 3 / 5 / 10 s** per UP/DOWN oder Tippen auf die Vorlaufanzeige wählen. Auswahl bleibt gespeichert. START oder Tippen auf den Auslöser startet die Aufnahme. Ein zweites START bzw. BACK fordert während des Countdowns den Abbruch an. Die Bestätigung **Selbstauslöser abgebrochen** abwarten; ohne Rückmeldung kann der Timer noch auslösen.
- Der Timer läuft auf dem Pixel ab Empfang des gültigen Befehls; bei Null ist keine weitere Funknachricht zur Auslösung nötig. Die Sekundenanzeige auf der Uhr ist wegen der Funklaufzeit angenähert. Pixel-Kamera und Display müssen während des Vorlaufs sichtbar/entsperrt bleiben. Einen zusätzlichen Timer in der Pixel-Kamera ausschalten, sonst addiert er sich.
- Dienststopp, bestätigter Abbruch und erkannter Garmin-Verbindungsabbruch entfernen einen noch wartenden Timer. Bei stark verspäteter Android-Ausführung wird die Aufnahme verworfen. Beim Verlassen der Uhr-App ohne bestätigten Abbruch ist die Abbruchnachricht nur ein Versuch; für sicheren Abbruch BACK drücken und auf die Bestätigung warten.
- Das Verbindungsprotokoll misst lokal am Pixel die Zeit vom Befehlsempfang bis zur akzeptierten Bedienaktion, einschließlich gewähltem Vorlauf. Diese Messung enthält nicht die vorherige Funklaufzeit und nicht die eigentliche Fotoverarbeitung.
- **Keine Vorschau enthalten.** Diese bleibt ein separater, noch nicht implementierter Versuch.
- Für alle Funktionen **beide Apps auf 0.4.0 aktualisieren**. Eine neue Uhr mit älterer Android-App sperrt den Timer, statt den Vorlauf still zu ignorieren. Sofortauslösung bleibt über den alten Vorbereitungsweg kompatibel.

## Oberfläche und Bedienungshilfe seit 0.3.0

- Überarbeitete Handy-Oberfläche mit Graphit-/Mint-Farbpalette, klarer Hauptaktion, Statuskarte und aufklappbaren Einstellungen bzw. Diagnose. Hell und System bleiben verfügbar.
- Neue Uhr-Oberfläche mit großem rundem Kameraauslöser, Verbindungsanzeige und passendem Icon. START und Tippen funktionieren wie zuvor. Nachrichtenprotokoll bleibt Version 2; 0.2.0 und 0.3.0 sind transportseitig kompatibel.
- Unter **Einstellungen & Hilfe → Beim Beenden deaktivieren** lässt sich einstellen, dass der Knopf **Fernbedienung beenden** auch die Android-Bedienungshilfe über `disableSelf()` ausschaltet. **Jetzt vollständig deaktivieren** beendet Dienst und Bedienungshilfe sofort. Danach ist erneute Aktivierung durch den Nutzer in den Android-Einstellungen nötig; die App kann diese Freigabe nicht still selbst setzen. Standard bleibt unverändert (Option aus).
- „Beenden“ verwirft auch einen noch ausstehenden lokalen Fünf-Sekunden-Test. Der Verbindungsindikator berücksichtigt das Alter der letzten Uhrnachricht.
- **Noch keine Bildvorschau.** Als nächster technischer Versuch ist ein Standbild der geöffneten Kamera vorgesehen; siehe [docs/PREVIEW.md](docs/PREVIEW.md).
- Der Nutzer hat die Verbindung und das Auslösen mit **0.2.0 bestätigt**. Build und bestehende Tests prüfen den neuen Stand; die neue Darstellung und die Abschaltoption sind noch auf den echten Geräten zu prüfen. Lokal war kein Android-Gerät oder konfigurierter Emulator verfügbar.

## Stand und Wechsel des Verbindungswegs

Das lokale Testfoto über den Bedienungshilfen-Dienst wurde vom Nutzer am 15.10.2026 bestätigt. Die Pixel-Kamera und ihre Bildverarbeitung bleiben erhalten.

Die direkten BLE-Versionen 0.1.x stellten keinen nutzbaren Kameradienst-Kanal her. Das letzte Geräteprotokoll zeigte Android-seitig gekoppelte Verbindungen, aber keinen geschützten Kameradienst-Zugriff; die genaue Ursache ist nicht geklärt. Weitere Änderungen allein an der Bonding-Abfolge wären spekulativ.

**0.2.0 verwendet daher Garmins offiziellen Nachrichtenkanal:** Uhr (`Toybox.Communications`) → Garmin Connect → Android-Begleit-App (`ciq-companion-app-sdk` 2.2.0). Es gibt keinen eigenen BLE-GATT-Server und keine zusätzliche Kopplung mehr. Die bestehende Verbindung der Uhr mit Garmin Connect ist Voraussetzung.

**Android und Uhr ab 0.2.0 verwenden denselben Kanal.** Die 0.1.x-Versionen sind nicht kompatibel. Für den aktuellen Vorschautest Android 0.6.0 und Garmin 0.6.1 verwenden. Der Nutzer hat Verbindung, schnellere Auslösung und Timer bestätigt; die Vorschau funktionierte mit 0.5.2 einmal, danach gab es IQ!-Abstürze. Der neue Stand muss auf den Geräten geprüft werden.

## Installation und erster Verbindungstest

1. `dist/PixelRemote-0.6.0.apk` auf dem Pixel als Update installieren. Vorhandene App behalten; Designauswahl und Bedienungshilfe bleiben erhalten. Falls Screenshot-Zugriff fehlt, die Bedienungshilfe einmal aus- und wieder einschalten.
2. Auf der Uhr die bisherige Datei unter `GARMIN/APPS` durch `dist/PixelRemote-0.6.1-fenix847mm.prg` ersetzen. USB trennen. Die neue Oberfläche hat einen großen mintfarbenen Kameraauslöser. Falls die Uhr eine Kommunikationsberechtigung abfragt, bestätigen.
3. Garmin Connect auf dem Pixel öffnen und prüfen, dass die Fenix dort verbunden ist.
4. In Pixel Remote **Fernbedienung starten**. Falls die Bedienungshilfe aus ist, führt der Hauptknopf zunächst zu deren Einrichtung. Die App registriert sich für Nachrichten der bekannten Garmin-Geräte und protokolliert SDK-/Gerätestatus.
5. Uhr-App öffnen. Sie prüft den Kanal durch eine Nachricht an die Pixel-App und deren Antwort. Erst nach dieser Antwort wird der Auslöser mintfarben und die Schaltfläche **VORSCHAU** erscheint. Es ist kein zusätzlicher Bluetooth-Kopplungsdialog vorgesehen.
6. Pixel-Kamera im **Fotomodus** öffnen und Handy entsperrt sowie Display eingeschaltet lassen. Auf der Uhr **START** drücken oder tippen.

Falls keine Verbindung entsteht: Uhrmeldung und die aufklappbare **Verbindungsdiagnose** in Pixel Remote prüfen. „Garmin hat Nachricht“ bestätigt nur die Übergabe an Garmins Transport; erst der mintfarbene Auslöser bestätigt die Antwort unserer Android-App. Garmin Connect muss weiter laufen dürfen, wenn die Kamera geöffnet wird.

**Uhr-Zuordnung zurücksetzen** entfernt nur die Auswahl innerhalb unserer App und stoppt den Dienst. Die Garmin-Connect-Kopplung bleibt bestehen. Das ist nur beim Wechsel der verwendeten Uhr nötig; alte Bluetooth-MAC-Zuordnungen aus 0.1.x werden nicht mehr verwendet.

## Kamera testen, falls nötig

Die Tests **Testfoto in 5 Sekunden** und **Kamera-Diagnose in 5 Sekunden** unter **Einstellungen & Hilfe** funktionieren weiterhin ohne Uhr. Die Diagnose liest IDs/Beschreibungen der sichtbaren Kamera-Bedienelemente und zeigt sie nur lokal an.

Wenn die Bedienungshilfe nach der Installation gesperrt ist: Android **Einstellungen → Apps → Pixel Remote → ⋮ → Eingeschränkte Einstellungen zulassen**. Danach **Bedienungshilfen → Pixel Remote – Kameraauslöser** aktivieren. Bezeichnungen können je nach Android-Version abweichen.

„Auslöser betätigt“ bestätigt eine akzeptierte Bedienaktion, nicht das Speichern eines Fotos; unsere App erhält keine Speicherbestätigung der fremden Kamera-App. Den Fotomodus vor der Nutzung wählen. Bei generischen Schaltflächenbeschreibungen lässt sich der Videomodus nicht sicher erkennen. Zoom, Kamerawechsel und Sperrbildschirmbetrieb sind noch nicht implementiert.

## Nachrichten und Schutz vor Wiederholung

- App-ID auf beiden Seiten: `b7e89053786045fc9a3c9d70dfd8a174`.
- Protokollversion 2: `hello` mit Startkennung → `ready` mit Sitzungstoken. `ping`/`pong` prüfen etwa alle drei Sekunden die Erreichbarkeit, solange kein Aufnahmebefehl läuft. Der Auslöser wird ausschließlich durch Benutzeraktionen angefordert.
- Schneller Weg: `ready`/`pong`/`result` enthalten eine vorbereitete Einmalfreigabe mit höchstens fünf Sekunden Gültigkeit. Die Uhr zieht konservativ die gesamte gemessene Nachrichtenlaufzeit von der Restgültigkeit ab. Beim Tastendruck folgt direkt `shutter`, dann `result`. Ein bereits versendeter Token bleibt gültig, auch wenn parallel eine neue Freigabe ausgestellt wird.
- Fallback: `prepare` → `armed` mit einem nur drei Sekunden gültigen Einmalticket → `shutter` → `result`. Ein verspätetes `prepare` kann allein kein Foto auslösen. Die Uhr nimmt `armed` nur während des noch laufenden Benutzerbefehls an. `shutter` wird nie automatisch wiederholt.
- `shutter` enthält optional `delay` (0, 3, 5 oder 10). Android bestätigt den wartenden Timer mit `scheduled`; die Uhr zeigt dessen angenäherte Restzeit. `cancel` liefert das tatsächliche Abbruch-/Aufnahmeergebnis. Doppelte Nachrichten während eines Timers bestätigen dessen vorhandenen Zustand, statt einen weiteren Timer zu starten oder irreführend „beschäftigt“ zu melden.
- Doppelte Befehle liefern das vorhandene Ergebnis bzw. „beschäftigt“ und betätigen den Auslöser nicht erneut. Alte Tickets, kleinere Sequenznummern, fremde Tokens und abgelaufene Sitzungen werden abgelehnt. Eine neue Sitzung kann einen alten Befehl nicht übernehmen.
- Die Android-App akzeptiert Nachrichten über den offiziellen Garmin-SDK-Kanal für die konfigurierte App-ID. Die erste gültige Startnachricht ordnet das zugehörige Garmin-Gerät zu. Die Zuordnung bleibt lokal gespeichert.
- Die Android-App besitzt keine Kamera-, Foto-, Mikrofon- oder Standortberechtigung und sendet keine Bilder. Der Bedienungshilfen-Dienst liest die Bedienelemente der geöffneten Pixel-Kamera und betätigt deren Auslöser. Die Bluetooth-Connect-Berechtigung wird für den laufenden Verbindungsdienst verwendet; BLE-Werbung entfällt.
- Diagnose und die letzten 30 Verbindungsereignisse bleiben im Prozessspeicher. Gespeichert werden Designauswahl und Garmin-Gerätezuordnung. Garmin Connect ist eine separate App mit eigenen Daten- und Netzwerkeinstellungen; ein vollständiger Offlinebetrieb des neuen Kanals wurde nicht geprüft.

## Builds und Prüfungen

Java aus Android Studio, Android-SDK über `local.properties`, Gradle im Workspace und Connect-IQ-SDK 9.2.0 mit Gerätedefinition `fenix847mm`.

```powershell
./camera-remote/build.ps1 -Target Android
./camera-remote/build.ps1 -Target Garmin -Device fenix847mm
```

Android-Build umfasst Unit-Tests, APK-Erzeugung und Lint. Tests prüfen die Auslöser-Erkennung sowie Ticketablauf, doppelte Nachrichten, falsche Tokens und alte Sequenzen. Sie simulieren keine tatsächliche Garmin-Connect-Verbindung. APK mit Entwicklungssignatur; Signatur muss zu vorherigen Updates passen. Garmin kompiliert für das konkrete Gerät, der Compiler skaliert das Launcher-Icon.

Die älteren Dateien im Ordner `dist` sind frühere Teststände. Für den aktuellen Test Android **0.6.0** und die Uhr-Datei **0.6.1** verwenden. Android: 30 Unit-Tests bestanden und Lint ohne Fehler; Garmin-Release und Testprogramm kompilieren. Garmin-Tests konnten im lokalen Simulator bisher nicht ausgeführt werden.

Die bestehenden Scale2Garmin-/PedalBridge-Projekte wurden nicht verändert. SDK-Lizenz siehe [THIRD_PARTY.md](THIRD_PARTY.md).
