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
if (-not $NoHistory) { New-Item -ItemType Directory -Force -Path $historyDir | Out-Null }

$timestamp = Get-Date -Format "yyyy-MM-dd_HH-mm-ss"
$startedAt = Get-Date
$latestLog = Join-Path $diagnosticsDir "latest-build.log"
$latestSummary = Join-Path $diagnosticsDir "latest-summary.txt"
$historyLog = Join-Path $historyDir "$timestamp.log"

function Publish-Diagnostics([string]$status) {
    if ($NoPush) { return }
    if ($null -eq (Get-Command git -ErrorAction SilentlyContinue)) {
        Write-Warning "Git is not available; diagnostics were saved locally but not pushed."
        return
    }

    $branch = (git branch --show-current 2>$null).Trim()
    if ([string]::IsNullOrWhiteSpace($branch)) {
        Write-Warning "Cannot determine the current Git branch; diagnostics were saved locally."
        return
    }

    git add -- build-diagnostics/latest-build.log build-diagnostics/latest-summary.txt
    if (-not $NoHistory -and (Test-Path $historyLog)) {
        git add -- "build-diagnostics/history/$timestamp.log"
    }

    $hasChanges = -not [string]::IsNullOrWhiteSpace((git status --porcelain -- build-diagnostics))
    if (-not $hasChanges) { return }

    git commit -m "SRV-BLD build diagnostics $timestamp [$status]"
    if ($LASTEXITCODE -ne 0) {
        Write-Warning "Could not commit build diagnostics. They remain in build-diagnostics/."
        return
    }

    git push origin $branch
    if ($LASTEXITCODE -ne 0) {
        Write-Warning "Diagnostics were committed locally but push failed. Run: git push origin $branch"
    }
}

$gradle = Join-Path $projectRoot "gradlew.bat"
if (-not (Test-Path $gradle)) {
    $finishedAt = Get-Date
    $message = @"
Lexora Service build diagnostics
Status: NOT_STARTED
ExitCode: 127
Task: $Task
Started: $($startedAt.ToString("s"))
Finished: $($finishedAt.ToString("s"))
Reason: gradlew.bat is missing. Generate/add Gradle Wrapper before using this build entry point.
"@
    $message | Set-Content -Path $latestSummary -Encoding UTF8
    $message | Set-Content -Path $latestLog -Encoding UTF8
    if (-not $NoHistory) { $message | Set-Content -Path $historyLog -Encoding UTF8 }
    Write-Host $message
    Publish-Diagnostics "NOT_STARTED"
    exit 127
}

"" | Set-Content -Path $latestLog -Encoding UTF8
Write-Host "Lexora Service: running '$Task' with diagnostics..."
& $gradle $Task --stacktrace --warning-mode all 2>&1 | Tee-Object -FilePath $latestLog
$exitCode = $LASTEXITCODE
$finishedAt = Get-Date
$duration = New-TimeSpan -Start $startedAt -End $finishedAt
$status = if ($exitCode -eq 0) { "SUCCESS" } else { "FAILED" }

if (-not $NoHistory) { Copy-Item $latestLog $historyLog -Force }

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
Publish-Diagnostics $status
exit $exitCode
