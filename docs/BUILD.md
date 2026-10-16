# Shutter Buddy selbst bauen

## Voraussetzungen

- JDK 17 oder neuer, etwa die mit Android Studio installierte Java-Laufzeit.
- Android SDK passend zu `compileSdk` in `app/build.gradle`: aktuell API 36, Minor-Version 1. Pfad über `ANDROID_HOME` oder eine lokale, nicht versionierte `local.properties` mit `sdk.dir` angeben.
- Für Windows-Buildskripte PowerShell 7.
- Für die Uhr: Garmin Connect IQ SDK und die Gerätedefinition `fenix847mm`, installierbar über den Garmin SDK Manager. Der aktuelle Stand wurde mit SDK 9.2.0 gebaut.

```powershell
git clone https://github.com/wischm0b/ShutterBuddy.git
cd ShutterBuddy
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
$env:ANDROID_HOME = Join-Path $env:LOCALAPPDATA 'Android\Sdk'
```

## Android

Der Gradle-Wrapper 8.13 ist enthalten. Beim ersten Start lädt er die Gradle-Distribution; der SHA-256-Wert ist festgelegt.

```powershell
./gradlew.bat testDebugUnitTest assembleRelease lintDebug
# Alternativ mit Kopie der fertigen APK nach dist:
./build.ps1 -Target Android
```

Unter Linux/macOS kann der Wrapper mit `./gradlew` verwendet werden. Ausgabe: `app/build/outputs/apk/release/app-release.apk`, beim PowerShell-Build zusätzlich `dist/ShutterBuddy-<Android-Version>.apk`.

Die aktuelle Buildkonfiguration signiert lokale Release-Builds mit dem lokalen Android-Debugschlüssel. Dieser private Schlüssel wird nicht veröffentlicht. Ein selbst gebautes APK hat deshalb üblicherweise eine andere Signatur als das veröffentlichte APK und kann es nicht direkt als Update ersetzen. Für Updates auf dem bestehenden Gerät die APKs aus den GitHub-Releases verwenden.

## Garmin

```powershell
./build.ps1 -Target Garmin -GarminSdk 'C:\Pfad\zum\ConnectIQ-SDK'
```

Alternativ `GARMIN_SDK_HOME` setzen. Auf Windows erkennt das Skript auch das im SDK Manager ausgewählte SDK. Im ursprünglichen lokalen Workspace werden bereits vorhandene SDK-/Gradle-Installationen unter `../.tools` weiterverwendet; nach einem frischen Klonen werden diese Ordner nicht vorausgesetzt.

Der Garmin-Schlüssel wird bei Bedarf lokal unter `garmin/developer_key.der` erzeugt. Er bleibt unversioniert. Ausgabe: `dist/ShutterBuddy-<Garmin-Version>-fenix847mm.prg` und eine Kopie ohne Versionsnummer. Android- und Garmin-Version können unterschiedlich sein; das aktuelle Paar steht im README und im Release.

## Tests und Logo

Die Android-Unit-Tests laufen über Gradle. Garmin-Eingabe- und Grenzwerttests stehen mit `(:test)` im Quellcode; zum Ausführen ist ein funktionierender Connect-IQ-Simulator nötig. In der ursprünglichen Umgebung konnten diese Tests nur kompiliert werden.

Die Größenexporte des gewählten Logos lassen sich unter Windows unverändert neu erzeugen:

```powershell
./tools/Export-BrandAssets.ps1
```

Signierschlüssel, SDKs, lokale Pfade, Compiler-Caches und Build-Ausgaben werden durch `.gitignore` ausgeschlossen. Aktuelle Installationsdateien werden separat als GitHub-Release bereitgestellt.
