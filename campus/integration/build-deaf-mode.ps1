$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$signal = Join-Path $root 'signal-web'
$assets = Join-Path $root 'app\src\main\assets\deaf'
if (!(Test-Path (Join-Path $signal 'package.json'))) { throw "Signal source folder missing: $signal" }
Push-Location $signal
try {
  npm.cmd ci
  if ($LASTEXITCODE -ne 0) { throw 'npm ci failed' }
  npm.cmd run build
  if ($LASTEXITCODE -ne 0) { throw 'Signal web build failed' }
} finally { Pop-Location }
if (Test-Path $assets) { Remove-Item $assets -Recurse -Force }
New-Item -ItemType Directory -Path $assets -Force | Out-Null
Copy-Item (Join-Path $signal 'dist\*') $assets -Recurse -Force
Write-Host 'Deaf Mode web assets copied into CampusLens. Now open the CampusLens project in Android Studio and build the APK.' -ForegroundColor Green
