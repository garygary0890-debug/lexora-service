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
$workingTreePatch = Join-Path $diagnosticsDir "working-tree.patch"
$gitStatusFile = Join-Path $diagnosticsDir "git-status.txt"
$historyLog = Join-Path $historyDir "$timestamp.log"

function Capture-WorkingTree {
    if ($null -eq (Get-Command git -ErrorAction SilentlyContinue)) {
        "Git is not available." | Set-Content -Path $gitStatusFile -Encoding UTF8
        "" | Set-Content -Path $workingTreePatch -Encoding UTF8
        return
    }
    git status --short 2>&1 | Set-Content -Path $gitStatusFile -Encoding UTF8
    git diff --binary -- . ':(exclude)build-diagnostics/**' 2>&1 | Set-Content -Path $workingTreePatch -Encoding UTF8
    $untracked = git ls-files --others --exclude-standard
    if ($untracked) {
        Add-Content -Path $workingTreePatch -Value "`n# Untracked files present (content not embedded automatically):"
        $untracked | ForEach-Object { Add-Content -Path $workingTreePatch -Value "# $_" }
    }
}

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

    git add -- build-diagnostics/latest-build.log build-diagnostics/latest-summary.txt build-diagnostics/working-tree.patch build-diagnostics/git-status.txt
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

Capture-WorkingTree

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
WorkingTreePatch: build-diagnostics/working-tree.patch
GitStatus: build-diagnostics/git-status.txt
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
Capture-WorkingTree

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
WorkingTreePatch: build-diagnostics/working-tree.patch
GitStatus: build-diagnostics/git-status.txt
"@
$summary | Set-Content -Path $latestSummary -Encoding UTF8
Write-Host $summary
Publish-Diagnostics $status
exit $exitCode
