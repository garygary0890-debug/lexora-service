param(
    [string]$Task = "assembleDebug",
    [switch]$NoPush,
    [switch]$NoHistory
)

$ErrorActionPreference = "Continue"
$projectRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
Set-Location $projectRoot

$diagnosticsDir = Join-Path $projectRoot "build-diagnostics"
$historyDir = Join-Path $diagnosticsDir "history"
New-Item -ItemType Directory -Force -Path $diagnosticsDir | Out-Null
if (-not $NoHistory) {
    New-Item -ItemType Directory -Force -Path $historyDir | Out-Null
}

$timestamp = Get-Date -Format "yyyy-MM-dd_HH-mm-ss"
$startedAt = Get-Date
$latestLog = Join-Path $diagnosticsDir "latest-build.log"
$latestSummary = Join-Path $diagnosticsDir "latest-summary.txt"
$historyLog = Join-Path $historyDir "$timestamp.log"

$gradle = Join-Path $projectRoot "gradlew.bat"
if (-not (Test-Path $gradle)) {
    $message = @"
Lexora Service build diagnostics
Status: NOT_STARTED
Started: $($startedAt.ToString("s"))
Task: $Task
Reason: gradlew.bat is missing. Generate/add Gradle Wrapper before using this build entry point.
"@
    $message | Set-Content -Path $latestSummary -Encoding UTF8
    $message | Set-Content -Path $latestLog -Encoding UTF8
    if (-not $NoHistory) { $message | Set-Content -Path $historyLog -Encoding UTF8 }
    Write-Host $message
    exit 127
}

"" | Set-Content -Path $latestLog -Encoding UTF8

Write-Host "Lexora Service: running '$Task' with diagnostics..."
& $gradle $Task --stacktrace --warning-mode all 2>&1 | Tee-Object -FilePath $latestLog
$exitCode = $LASTEXITCODE
$finishedAt = Get-Date
$duration = New-TimeSpan -Start $startedAt -End $finishedAt
$status = if ($exitCode -eq 0) { "SUCCESS" } else { "FAILED" }

if (-not $NoHistory) {
    Copy-Item $latestLog $historyLog -Force
}

$branch = "unknown"
$head = "unknown"
try { $branch = (git branch --show-current 2>$null).Trim() } catch {}
try { $head = (git rev-parse HEAD 2>$null).Trim() } catch {}

$summary = @"
Lexora Service build diagnostics
Status: $status
ExitCode: $exitCode
Task: $Task
Started: $($startedAt.ToString("s"))
Finished: $($finishedAt.ToString("s"))
DurationSeconds: $([math]::Round($duration.TotalSeconds, 1))
Branch: $branch
HeadBeforeDiagnosticsCommit: $head
LatestLog: build-diagnostics/latest-build.log
HistoryLog: $(if ($NoHistory) { "disabled" } else { "build-diagnostics/history/$timestamp.log" })
"@
$summary | Set-Content -Path $latestSummary -Encoding UTF8
Write-Host $summary

if (-not $NoPush) {
    $gitAvailable = $null -ne (Get-Command git -ErrorAction SilentlyContinue)
    if ($gitAvailable) {
        git add -- build-diagnostics/latest-build.log build-diagnostics/latest-summary.txt
        if (-not $NoHistory) { git add -- "build-diagnostics/history/$timestamp.log" }

        $hasChanges = -not [string]::IsNullOrWhiteSpace((git status --porcelain -- build-diagnostics))
        if ($hasChanges) {
            git commit -m "SRV-BLD build diagnostics $timestamp [$status]"
            if ($LASTEXITCODE -eq 0) {
                git push origin $branch
                if ($LASTEXITCODE -ne 0) {
                    Write-Warning "Build diagnostics were committed locally but push to GitHub failed. Run: git push origin $branch"
                }
            } else {
                Write-Warning "Could not commit build diagnostics. They remain in build-diagnostics/."
            }
        }
    } else {
        Write-Warning "Git is not available; diagnostics were saved locally but not pushed."
    }
}

exit $exitCode
