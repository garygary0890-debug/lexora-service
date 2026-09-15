param(
    [string]$GradleVersion = "9.4.1",
    [switch]$NoPush
)

$ErrorActionPreference = "Stop"
$projectRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
Set-Location $projectRoot

$wrapperBat = Join-Path $projectRoot "gradlew.bat"
$wrapperJar = Join-Path $projectRoot "gradle\wrapper\gradle-wrapper.jar"
$wrapperProps = Join-Path $projectRoot "gradle\wrapper\gradle-wrapper.properties"

if ((Test-Path $wrapperBat) -and (Test-Path $wrapperJar) -and (Test-Path $wrapperProps)) {
    Write-Host "Gradle Wrapper already exists."
    exit 0
}

$cacheRoot = Join-Path $env:USERPROFILE ".lexora\gradle-bootstrap"
$zipPath = Join-Path $cacheRoot "gradle-$GradleVersion-bin.zip"
$extractRoot = Join-Path $cacheRoot "gradle-$GradleVersion"
$distributionUrl = "https://services.gradle.org/distributions/gradle-$GradleVersion-bin.zip"

New-Item -ItemType Directory -Force -Path $cacheRoot | Out-Null

if (-not (Test-Path $zipPath)) {
    Write-Host "Downloading Gradle $GradleVersion from official distribution..."
    Invoke-WebRequest -Uri $distributionUrl -OutFile $zipPath -UseBasicParsing
}

$gradleBat = Join-Path $extractRoot "gradle-$GradleVersion\bin\gradle.bat"
if (-not (Test-Path $gradleBat)) {
    if (Test-Path $extractRoot) { Remove-Item -Recurse -Force $extractRoot }
    New-Item -ItemType Directory -Force -Path $extractRoot | Out-Null
    Expand-Archive -Path $zipPath -DestinationPath $extractRoot -Force
}

if (-not (Test-Path $gradleBat)) {
    throw "Gradle executable was not found after extraction: $gradleBat"
}

Write-Host "Generating Gradle Wrapper $GradleVersion..."
& $gradleBat wrapper --gradle-version $GradleVersion --distribution-type bin
if ($LASTEXITCODE -ne 0) {
    throw "Gradle wrapper generation failed with exit code $LASTEXITCODE"
}

$required = @(
    "gradlew",
    "gradlew.bat",
    "gradle/wrapper/gradle-wrapper.jar",
    "gradle/wrapper/gradle-wrapper.properties"
)
foreach ($path in $required) {
    if (-not (Test-Path (Join-Path $projectRoot $path))) {
        throw "Generated wrapper is incomplete. Missing: $path"
    }
}

Write-Host "Gradle Wrapper $GradleVersion generated successfully."

if (-not $NoPush -and $null -ne (Get-Command git -ErrorAction SilentlyContinue)) {
    $branch = (git branch --show-current 2>$null).Trim()
    git add -- gradlew gradlew.bat gradle/wrapper/gradle-wrapper.jar gradle/wrapper/gradle-wrapper.properties
    $changes = git status --porcelain -- gradlew gradlew.bat gradle/wrapper
    if (-not [string]::IsNullOrWhiteSpace($changes)) {
        git commit -m "SRV-000013 Generate Gradle Wrapper $GradleVersion"
        if ($LASTEXITCODE -eq 0 -and -not [string]::IsNullOrWhiteSpace($branch)) {
            git push origin $branch
            if ($LASTEXITCODE -ne 0) {
                Write-Warning "Gradle Wrapper was committed locally but push failed. Run: git push origin $branch"
            }
        }
    }
}

exit 0
