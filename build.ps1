param(
    [ValidateSet('Android', 'Garmin')][string]$Target = 'Android',
    [string]$Device = 'fenix847mm',
    [string]$JavaHome = '',
    [string]$GarminSdk = ''
)
$ErrorActionPreference = 'Stop'
$workspace = Split-Path $PSScriptRoot -Parent
if (-not $JavaHome) { $JavaHome = $env:JAVA_HOME }
if (-not $JavaHome -and (Test-Path -LiteralPath 'C:\Program Files\Android\Android Studio\jbr')) {
    $JavaHome = 'C:\Program Files\Android\Android Studio\jbr'
}
if (-not $JavaHome -or -not (Test-Path -LiteralPath (Join-Path $JavaHome 'bin/java.exe'))) {
    throw 'Specify JDK 17 or later through JAVA_HOME or -JavaHome.'
}
$env:JAVA_HOME = $JavaHome
$env:PATH = (Join-Path $JavaHome 'bin') + ';' + $env:PATH
$releaseFolder = Join-Path $PSScriptRoot 'dist'
New-Item -ItemType Directory -Force $releaseFolder | Out-Null
if ($Target -eq 'Android') {
    $gradle = Join-Path $workspace '.tools/gradle-8.13/bin/gradle.bat'
    if (Test-Path -LiteralPath $gradle) {
        if (-not $env:GRADLE_USER_HOME) { $env:GRADLE_USER_HOME = Join-Path $workspace '.gradle-home' }
    } else { $gradle = Join-Path $PSScriptRoot 'gradlew.bat' }
    & $gradle -p $PSScriptRoot --no-daemon --max-workers=1 testDebugUnitTest assembleRelease lintDebug --console=plain
    if ($LASTEXITCODE -ne 0) { throw 'Android build or validation failed.' }
    $version = [regex]::Match((Get-Content -LiteralPath (Join-Path $PSScriptRoot 'app/build.gradle') -Raw), "versionName '([^']+)'").Groups[1].Value
    if (-not $version) { throw 'App version is missing.' }
    Copy-Item -LiteralPath (Join-Path $PSScriptRoot 'app/build/outputs/apk/release/app-release.apk') -Destination (Join-Path $releaseFolder "ShutterBuddy-$version.apk")
} else {
    if (-not $GarminSdk) { $GarminSdk = $env:GARMIN_SDK_HOME }
    if (-not $GarminSdk -and (Test-Path -LiteralPath (Join-Path $workspace '.tools/garmin-sdk'))) {
        $GarminSdk = Join-Path $workspace '.tools/garmin-sdk'
    }
    if (-not $GarminSdk -and $env:APPDATA) {
        $sdkConfig = Join-Path $env:APPDATA 'Garmin/ConnectIQ/current-sdk.cfg'
        if (Test-Path -LiteralPath $sdkConfig) { $GarminSdk = (Get-Content -LiteralPath $sdkConfig -Raw).Trim() }
    }
    if (-not $GarminSdk -or -not (Test-Path -LiteralPath (Join-Path $GarminSdk 'bin/monkeybrains.jar'))) {
        throw 'Install the Garmin Connect IQ SDK and set -GarminSdk or GARMIN_SDK_HOME.'
    }
    $keyFile = Join-Path $PSScriptRoot 'garmin/developer_key.der'
    if (-not (Test-Path -LiteralPath $keyFile)) {
        $signingKey = [System.Security.Cryptography.RSA]::Create(4096)
        try { [System.IO.File]::WriteAllBytes($keyFile, $signingKey.ExportPkcs8PrivateKey()) } finally { $signingKey.Dispose() }
    }
    & (Join-Path $JavaHome 'bin/java.exe') -Xms256m '-Dfile.encoding=UTF-8' -cp (Join-Path $GarminSdk 'bin/monkeybrains.jar') com.garmin.monkeybrains.Monkeybrains -f (Join-Path $PSScriptRoot 'garmin/monkey.jungle') -d $Device -o (Join-Path $releaseFolder "ShutterBuddy-$Device.prg") -y $keyFile -w
    if ($LASTEXITCODE -ne 0) { throw 'Garmin build failed. Check the compiler output above.' }
    [xml]$watchManifest = Get-Content -LiteralPath (Join-Path $PSScriptRoot 'garmin/manifest.xml') -Raw
    $version = $watchManifest.SelectSingleNode("//*[local-name()='application']").GetAttribute('version')
    Copy-Item -LiteralPath (Join-Path $releaseFolder "ShutterBuddy-$Device.prg") -Destination (Join-Path $releaseFolder "ShutterBuddy-$version-$Device.prg") -Force
}
