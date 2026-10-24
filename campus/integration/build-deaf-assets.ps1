$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$web = Join-Path $root "signal-web"
$assets = Join-Path $root "app\src\main\assets\deaf"
if (-not (Get-Command npm -ErrorAction SilentlyContinue)) { throw "Node.js/npm not found. Install Node.js 22.18+ or 24 from https://nodejs.org, then reopen PowerShell." }
Push-Location $web
try { npm.cmd ci; if ($LASTEXITCODE -ne 0) { throw "npm ci failed." }; npm.cmd run build; if ($LASTEXITCODE -ne 0) { throw "npm run build failed." } } finally { Pop-Location }
$dist = Join-Path $web "dist"
if (-not (Test-Path (Join-Path $dist "index.html"))) { throw "Build output index.html was not created." }
New-Item -ItemType Directory -Force -Path $assets | Out-Null
Get-ChildItem -Force $assets | Remove-Item -Recurse -Force
Copy-Item (Join-Path $dist "*") $assets -Recurse -Force
Write-Host "Deaf Mode web assets copied into: $assets" -ForegroundColor Green
Write-Host "Now open campus in Android Studio, sync Gradle, and build the APK." -ForegroundColor Cyan
