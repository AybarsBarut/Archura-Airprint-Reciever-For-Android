$ErrorActionPreference = "Stop"

$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
$androidStudioJbr = "C:\Program Files\Android\Android Studio\jbr"

if (-not $env:JAVA_HOME -and (Test-Path $androidStudioJbr)) {
    $env:JAVA_HOME = $androidStudioJbr
}

Push-Location $repoRoot
try {
    & ".\gradlew.bat" testDebugUnitTest lint detekt
}
finally {
    Pop-Location
}
