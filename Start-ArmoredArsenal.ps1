param(
    [switch]$Online,
    [switch]$SkipNetworkCheck
)

$ErrorActionPreference = "Stop"

Set-Location -LiteralPath $PSScriptRoot

$logDir = Join-Path $PSScriptRoot "logs"
New-Item -ItemType Directory -Path $logDir -Force | Out-Null
$launchLog = Join-Path $logDir "launcher-latest.log"
$script:TranscriptStarted = $false

function Stop-LauncherTranscript {
    if ($script:TranscriptStarted) {
        try {
            Stop-Transcript | Out-Null
        } catch {
        }
    }
}

trap {
    Stop-LauncherTranscript
    throw
}

Start-Transcript -Path $launchLog -Force | Out-Null
$script:TranscriptStarted = $true
Write-Host "Launcher log: $launchLog" -ForegroundColor DarkGray

$javaHome = "C:\Program Files\Java\jdk-25"
$javaExe = Join-Path $javaHome "bin\java.exe"

if (-not (Test-Path -LiteralPath $javaExe)) {
    Write-Host "JDK 25 was not found at: $javaHome" -ForegroundColor Red
    Write-Host "Install JDK 25, then run this script again."
    Stop-LauncherTranscript
    exit 1
}

$env:JAVA_HOME = $javaHome
$env:Path = "$javaHome\bin;$env:Path"
$env:GRADLE_OPTS = "-Dorg.gradle.daemon=false -Dorg.gradle.internal.http.connectionTimeout=120000 -Dorg.gradle.internal.http.socketTimeout=120000"

Write-Host "Using Java:" -ForegroundColor Cyan
& $javaExe -version

if ($Online -and -not $SkipNetworkCheck) {
    Write-Host ""
    Write-Host "Checking NeoForge Maven network access..." -ForegroundColor Cyan
    try {
        $response = Invoke-WebRequest `
            -Uri "https://maven.neoforged.net/releases/net/neoforged/neoform-runtime/2.0.18/neoform-runtime-2.0.18.pom" `
            -UseBasicParsing `
            -TimeoutSec 30
        Write-Host "NeoForge Maven responded with HTTP $($response.StatusCode)." -ForegroundColor Green
    } catch {
        Write-Host "NeoForge Maven is not reachable from this PowerShell session." -ForegroundColor Red
        Write-Host "This is the same problem Gradle hit. Try a different network, disable/enable VPN, or try again later."
        Write-Host "Details: $($_.Exception.Message)"
        Stop-LauncherTranscript
        exit 2
    }
}

Write-Host ""
Write-Host "Stopping any old Gradle daemons..." -ForegroundColor Cyan
$cachedGradle = Join-Path $env:USERPROFILE ".gradle\wrapper\dists\gradle-9.1.0-bin\9agqghryom9wkf8r80qlhnts3\gradle-9.1.0\bin\gradle.bat"
$gradleCommand = if (Test-Path -LiteralPath $cachedGradle) { $cachedGradle } else { Join-Path $PSScriptRoot "gradlew.bat" }

try {
    & $gradleCommand --stop | Out-Host
} catch {
    Write-Host "Gradle daemon stop skipped: $($_.Exception.Message)" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "Starting Armored Arsenal dev client..." -ForegroundColor Cyan
$gradleArgs = @("--no-daemon", "--console=plain")
if (-not $Online) {
    $gradleArgs += "--offline"
    Write-Host "Using offline Gradle mode with cached NeoForge/Minecraft files." -ForegroundColor DarkGray
}
$gradleArgs += @("runClient", "--stacktrace")
& $gradleCommand @gradleArgs
$exitCode = $LASTEXITCODE
Stop-LauncherTranscript
exit $exitCode
