$ErrorActionPreference = 'Stop'

$root      = Split-Path -Parent (Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path))
$release   = Join-Path $root 'release'
$backend   = Join-Path $root 'backend'
$frontend  = Join-Path $root 'frontend'
$deployWin = Join-Path $root 'deploy\windows'
$pkgDir    = Join-Path $root 'server-package'

Write-Host ""
Write-Host "===================================" -ForegroundColor Cyan
Write-Host "  HSI Utilities - Prepare Package  " -ForegroundColor Cyan
Write-Host "===================================" -ForegroundColor Cyan
Write-Host ""

# ─── BUILD ────────────────────────────────────────────────────────────────────
Write-Host "[1/4] Building backend..." -ForegroundColor Yellow
Push-Location $backend
mvn clean package -DskipTests
Pop-Location

Write-Host "[2/4] Building frontend..." -ForegroundColor Yellow
Push-Location $frontend
cmd /c npm install
cmd /c npx ng build
Pop-Location

# ─── PREPARE PACKAGE FOLDER ───────────────────────────────────────────────────
Write-Host "[3/4] Preparing server-package..." -ForegroundColor Yellow

if (Test-Path $pkgDir) { Remove-Item -Recurse -Force $pkgDir }
New-Item -ItemType Directory -Path $pkgDir | Out-Null
New-Item -ItemType Directory -Path "$pkgDir\frontend" | Out-Null
New-Item -ItemType Directory -Path "$pkgDir\tools" | Out-Null

# Copy jar
$jar = Join-Path $backend 'target\maintenance-system-0.0.1-SNAPSHOT.jar'
Copy-Item $jar "$pkgDir\app.jar"

# Extract frontend dist into package (Angular 17 outputs to browser\ subfolder)
$dist = Join-Path $frontend 'dist\hospital-maintenance-frontend'
$browserDist = Join-Path $dist 'browser'
if (Test-Path $browserDist) {
    Copy-Item -Recurse "$browserDist\*" "$pkgDir\frontend\"
} else {
    Copy-Item -Recurse "$dist\*" "$pkgDir\frontend\"
}

# Copy env template
$envSrc = Join-Path $root 'deploy\ubuntu\env\hsi-utilities-backend.env'
Copy-Item $envSrc "$pkgDir\hsi-utilities-backend.env"

# Copy install script
Copy-Item (Join-Path $deployWin 'install-server.ps1') "$pkgDir\install-server.ps1"

# ─── SETUP.txt ────────────────────────────────────────────────────────────────
$setupTxt = @"
=======================================
  HSI UTILITIES - WINDOWS SERVER SETUP
=======================================

SEBELUM RUN SCRIPT:
-------------------
1. Download NSSM (https://nssm.cc/download)
   - Extract nssm.exe (64-bit) ke folder ini: server-package\tools\nssm.exe

2. Download nginx for Windows (https://nginx.org/en/download.html)
   - Pilih "Stable version" (.zip)
   - Extract semua content (folder nginx-x.x.x) ke: C:\nginx\
   - Pastikan C:\nginx\nginx.exe wujud

3. Edit fail hsi-utilities-backend.env
   - Buka dengan Notepad
   - Set SPRING_DATASOURCE_PASSWORD (password DB)
   - Set OPENAI_API_KEY (kalau guna AI assistant)
   - Simpan

CARA RUN:
---------
1. Copy folder server-package\ ke Windows Server (C:\server-package\)
2. Buka PowerShell sebagai Administrator
3. Run:
      Set-ExecutionPolicy -Scope CurrentUser RemoteSigned
      cd C:\server-package
      .\install-server.ps1

SELEPAS INSTALL:
----------------
- App URL  : http://<server-ip>:8081
- Logs     : C:\hsi-utilities\logs\
- Services : hsi-backend, hsi-nginx

Check status:
   Get-Service hsi-backend, hsi-nginx

Restart services:
   Restart-Service hsi-backend
   Restart-Service hsi-nginx
"@

Set-Content -Path "$pkgDir\SETUP.txt" -Value $setupTxt -Encoding UTF8

# ─── DONE ─────────────────────────────────────────────────────────────────────
Write-Host "[4/4] Done!" -ForegroundColor Green
Write-Host ""
Write-Host "Package ready:" -ForegroundColor Cyan
Write-Host "  $pkgDir" -ForegroundColor White
Write-Host ""
Write-Host "Isi kandungan:" -ForegroundColor Gray
Get-ChildItem $pkgDir | Format-Table Name, Length -AutoSize
Write-Host ""
Write-Host "Seterusnya: Copy folder 'server-package' ke Windows Server." -ForegroundColor Yellow
Write-Host "Baca SETUP.txt untuk arahan lengkap." -ForegroundColor Yellow
Write-Host ""
