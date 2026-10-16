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
    & (Join-Path $projectRoot 'tooling\gradle-8.9\bin\gradle.bat') --console=plain --max-workers=4 :app:assembleRelease @args
    if ($LASTEXITCODE -ne 0) { throw "Release build failed (exit $LASTEXITCODE)." }
    Write-Host "Signed APK: $projectRoot\app\build\outputs\apk\release\app-release.apk"
} finally {
    Pop-Location
}
