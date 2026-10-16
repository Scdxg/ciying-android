$ErrorActionPreference = 'Stop'
$projectRoot = $PSScriptRoot
$env:JAVA_HOME = Join-Path $projectRoot 'tooling\jdk17\jdk-17.0.20.1+1'
$env:GRADLE_USER_HOME = if ($env:CIYING_GRADLE_HOME) { $env:CIYING_GRADLE_HOME } else { Join-Path $env:LOCALAPPDATA 'Ciying\gradle-home' }
$temporaryDirectory = Join-Path $projectRoot 'tooling\tmp'
New-Item -ItemType Directory -Path $temporaryDirectory -Force | Out-Null
$env:JAVA_TOOL_OPTIONS = "-Djdk.net.unixdomain.tmpdir=$($temporaryDirectory.Replace('\', '/'))"
$sdkDirectory = (Join-Path $projectRoot 'android-sdk').Replace('\', '/')
Set-Content -LiteralPath (Join-Path $projectRoot 'local.properties') -Value "sdk.dir=$sdkDirectory" -Encoding ascii
Push-Location $projectRoot
try {
    & (Join-Path $projectRoot 'tooling\gradle-8.9\bin\gradle.bat') --console=plain --max-workers=4 :app:assembleDebug @args
    if ($LASTEXITCODE -ne 0) { throw "Gradle build failed (exit $LASTEXITCODE)." }
    Write-Host "APK: $projectRoot\app\build\outputs\apk\debug\app-debug.apk"
} finally {
    Pop-Location
}
