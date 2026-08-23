<#
.SYNOPSIS
    Runs an SDK-team-provided OpenRewrite migration recipe against a consumer
    Android app WITHOUT ever touching that app's own build.gradle.kts.

.DESCRIPTION
    App teams never need to know OpenRewrite exists. This script:
      1. Looks up the requested target version in manifest.json (maintained
         by the SDK team - one entry per breaking-change release).
      2. Renders a temporary Gradle init script with the matching recipe
         artifact/version baked in.
      3. Runs `gradlew rewriteDryRun --init-script <temp file>` so you can
         review the exact diff before anything is written to disk.
      4. Only if -Apply is passed (and after an explicit Y/N confirmation,
         unless -Yes is also passed), runs `gradlew rewriteRun` with the
         same init script to actually apply the changes.
      5. Deletes the temporary init script when done - nothing persists in
         the app's repo, nothing needs to be "cleaned up" by the app team.

.PARAMETER To
    The BoM/migration target version to migrate to, e.g. "5.0.0". Must exist
    as a key in manifest.json.

.PARAMETER ProjectDir
    Path to the consumer app's project root (the directory containing
    gradlew / gradlew.bat). Defaults to the current directory, so running
    this from an Android Studio terminal already opened in the app's root
    "just works" without passing this at all.

.PARAMETER Apply
    If set, applies the changes (rewriteRun) after you confirm the dry-run
    diff looks correct. If omitted, only a dry run is performed.

.PARAMETER Yes
    Skips the interactive Y/N confirmation before applying (use together
    with -Apply for non-interactive / CI usage).

.EXAMPLE
    # Preview only - nothing is written. Run from the app's project root.
    .\migrate.ps1 -To 5.0.0

.EXAMPLE
    # Preview, then apply after confirmation
    .\migrate.ps1 -To 5.0.0 -Apply

.EXAMPLE
    # Non-interactive apply (CI)
    .\migrate.ps1 -To 5.0.0 -Apply -Yes
#>

param(
    [Parameter(Mandatory = $true)]
    [string]$To,

    [Parameter(Mandatory = $false)]
    [string]$ProjectDir = (Get-Location).Path,

    [Parameter(Mandatory = $false)]
    [switch]$Apply,

    [Parameter(Mandatory = $false)]
    [switch]$Yes
)

$ErrorActionPreference = "Stop"

$scriptDir = $PSScriptRoot
$manifestPath = Join-Path $scriptDir "manifest.json"
$templatePath = Join-Path $scriptDir "init.gradle.kts.template"

# --- Pinned tool versions (bump here, not per-migration) ---
$openRewritePluginVersion = "7.38.0"
$rewriteRecipeBomVersion = "3.36.0"

# --- 1. Resolve target from manifest ---
if (-not (Test-Path $manifestPath)) {
    Write-Error "manifest.json not found at $manifestPath"
    exit 1
}

$manifest = Get-Content $manifestPath -Raw | ConvertFrom-Json
$target = $manifest.targets.$To

if (-not $target) {
    Write-Host "Unknown migration target '$To'. Available targets:" -ForegroundColor Yellow
    $manifest.targets.PSObject.Properties | ForEach-Object {
        Write-Host ("  {0,-10} {1}" -f $_.Name, $_.Value.description)
    }
    exit 1
}

Write-Host "Migration target: $To" -ForegroundColor Cyan
Write-Host "  Recipe:      $($target.recipeArtifact):$($target.recipeVersion)"
Write-Host "  activeRecipe: $($target.activeRecipe)"
Write-Host "  Description: $($target.description)"
Write-Host ""

# --- 2. Resolve gradlew in the target project ---
# Note: package registry credentials (GitHub Packages / Artifactory) are NOT
# this script's concern. The consumer app's own settings.gradle.kts already
# declares that repository and resolves credentials from gradle.properties /
# env vars exactly as it does for every other SDK dependency - the init
# script deliberately reuses that configuration instead of adding its own.
$gradlewBat = Join-Path $ProjectDir "gradlew.bat"
$gradlewSh = Join-Path $ProjectDir "gradlew"
if (Test-Path $gradlewBat) {
    $gradlew = $gradlewBat
} elseif (Test-Path $gradlewSh) {
    $gradlew = $gradlewSh
} else {
    Write-Error "No gradlew/gradlew.bat found in $ProjectDir. Pass -ProjectDir pointing at the app's project root."
    exit 1
}

# --- 3. Auto-detect the app's AGP (Android Gradle Plugin) version ---
# OpenRewrite's Android-aware source parsing (AndroidProjectParser) needs
# com.android.build.gradle.TestedExtension to be resolvable from the SAME
# isolated classloader the init script's plugin was loaded from. That class
# only exists if AGP itself is also declared on the initscript's own
# `classpath`. It doesn't need to be byte-for-byte identical to the app's
# real AGP version, but staying close avoids surprises, so we read it
# straight from the app's own version catalog.
$agpVersion = $null
$versionCatalogPath = Join-Path $ProjectDir "gradle\libs.versions.toml"
if (Test-Path $versionCatalogPath) {
    $catalogContent = Get-Content $versionCatalogPath -Raw
    if ($catalogContent -match '(?m)^\s*agp\s*=\s*"([^"]+)"') {
        $agpVersion = $Matches[1]
    }
}
if (-not $agpVersion) {
    Write-Error "Could not auto-detect the AGP version from $versionCatalogPath (expected an 'agp = ""x.y.z""' entry). Pass it explicitly or add it to the version catalog."
    exit 1
}
Write-Host "Detected AGP version: $agpVersion" -ForegroundColor Cyan

# --- 4. Render the init script from the template ---
if (-not (Test-Path $templatePath)) {
    Write-Error "Template not found at $templatePath"
    exit 1
}

$initScriptContent = Get-Content $templatePath -Raw
$initScriptContent = $initScriptContent.Replace("__OPENREWRITE_PLUGIN_VERSION__", $openRewritePluginVersion)
$initScriptContent = $initScriptContent.Replace("__REWRITE_RECIPE_BOM_VERSION__", $rewriteRecipeBomVersion)
$initScriptContent = $initScriptContent.Replace("__RECIPE_ARTIFACT__", $target.recipeArtifact)
$initScriptContent = $initScriptContent.Replace("__RECIPE_VERSION__", $target.recipeVersion)
$initScriptContent = $initScriptContent.Replace("__AGP_VERSION__", $agpVersion)

$tempInitScript = Join-Path ([System.IO.Path]::GetTempPath()) "sdk-migrate-$([System.Guid]::NewGuid().ToString('N')).init.gradle.kts"
Set-Content -Path $tempInitScript -Value $initScriptContent -Encoding UTF8

try {
    # --- 5. Dry run ---
    Write-Host "Running dry run against $ProjectDir ..." -ForegroundColor Cyan
    Push-Location $ProjectDir
    try {
        $previousEap = $ErrorActionPreference
        $ErrorActionPreference = "Continue"
        & $gradlew rewriteDryRun "--init-script" $tempInitScript "-Drewrite.activeRecipe=$($target.activeRecipe)" 2>&1 | ForEach-Object { "$_" }
        $dryRunExitCode = $LASTEXITCODE
        $ErrorActionPreference = $previousEap
    } finally {
        Pop-Location
    }

    if ($dryRunExitCode -ne 0) {
        Write-Error "rewriteDryRun failed (exit code $dryRunExitCode). See Gradle output above."
        exit $dryRunExitCode
    }

    if (-not $Apply) {
        Write-Host ""
        Write-Host "Dry run complete. Review the report above (or under build/rewrite)." -ForegroundColor Green
        Write-Host "Re-run with -Apply to actually write these changes." -ForegroundColor Green
        exit 0
    }

    # --- 6. Confirm before applying (skip with -Yes, e.g. for CI) ---
    if (-not $Yes) {
        Write-Host ""
        $confirmation = Read-Host "Apply these changes to $ProjectDir now? (y/N)"
        if ($confirmation -notmatch '^[Yy]') {
            Write-Host "Aborted. No changes were made." -ForegroundColor Yellow
            exit 0
        }
    }

    Write-Host "Applying changes ..." -ForegroundColor Cyan
    Push-Location $ProjectDir
    try {
        $previousEap = $ErrorActionPreference
        $ErrorActionPreference = "Continue"
        & $gradlew rewriteRun "--init-script" $tempInitScript "-Drewrite.activeRecipe=$($target.activeRecipe)" 2>&1 | ForEach-Object { "$_" }
        $runExitCode = $LASTEXITCODE
        $ErrorActionPreference = $previousEap
    } finally {
        Pop-Location
    }

    if ($runExitCode -ne 0) {
        Write-Error "rewriteRun failed (exit code $runExitCode). See Gradle output above."
        exit $runExitCode
    }

    Write-Host ""
    Write-Host "Migration to $To applied successfully. Review the git diff, build, test, and commit." -ForegroundColor Green
}
finally {
    # --- 7. Cleanup - nothing persists, nothing for the app team to remove ---
    Remove-Item -Path $tempInitScript -ErrorAction SilentlyContinue
}
