#Requires -RunAsAdministrator
$ErrorActionPreference = 'Stop'

# ─── CONFIG ───────────────────────────────────────────────────────────────────
$PkgDir      = Split-Path -Parent $MyInvocation.MyCommand.Path
$AppDir      = 'C:\hsi-utilities'
$NginxDir    = 'C:\nginx'
$LogDir      = "$AppDir\logs"
$FrontendDir = "$AppDir\frontend"
$JarPath     = "$AppDir\app.jar"
$EnvFile     = "$AppDir\hsi-utilities-backend.env"
$LauncherBat = "$AppDir\start-backend.bat"

# Look for nssm in package tools\ folder first, then fallback to C:\tools\
$NssmExe = "$PkgDir\tools\nssm.exe"
if (-not (Test-Path $NssmExe)) { $NssmExe = 'C:\tools\nssm.exe' }
# ──────────────────────────────────────────────────────────────────────────────

Write-Host ""
Write-Host "===============================" -ForegroundColor Cyan
Write-Host "  HSI Utilities - Server Setup " -ForegroundColor Cyan
Write-Host "===============================" -ForegroundColor Cyan
Write-Host ""

# ─── CHECK REQUIREMENTS ───────────────────────────────────────────────────────
Write-Host "[1/5] Checking requirements..." -ForegroundColor Yellow
$fail = $false

# Try PATH first, then common install locations
$javaExe = $null
try { $javaExe = (Get-Command java -ErrorAction Stop).Source } catch {}
if (-not $javaExe) {
    $candidates = @(
        'C:\Program Files\Java\jdk-17\bin\java.exe',
        'C:\Program Files\Common Files\Oracle\Java\javapath\java.exe',
        'C:\Program Files\Eclipse Adoptium\jdk-17*\bin\java.exe',
        'C:\Program Files\Microsoft\jdk-17*\bin\java.exe'
    ) | ForEach-Object { Resolve-Path $_ -ErrorAction SilentlyContinue } | Select-Object -First 1
    if ($candidates) { $javaExe = $candidates.Path }
}
if ($javaExe) {
    Write-Host "  [OK] Java found: $javaExe" -ForegroundColor Green
} else {
    Write-Host "  [FAIL] Java not found. Download OpenJDK 17 dan add ke PATH." -ForegroundColor Red
    $fail = $true
}

if (Test-Path $NssmExe) { Write-Host "  [OK] nssm.exe found at $NssmExe" -ForegroundColor Green }
else { Write-Host "  [FAIL] nssm.exe not found. Letak nssm.exe dalam folder tools\ " -ForegroundColor Red; $fail = $true }

if (Test-Path "$NginxDir\nginx.exe") { Write-Host "  [OK] nginx found at $NginxDir" -ForegroundColor Green }
else { Write-Host "  [FAIL] nginx.exe not found at $NginxDir. Extract nginx ke C:\nginx\" -ForegroundColor Red; $fail = $true }

if (Test-Path "$PkgDir\app.jar") { Write-Host "  [OK] app.jar found" -ForegroundColor Green }
else { Write-Host "  [FAIL] app.jar not found in package folder." -ForegroundColor Red; $fail = $true }

# Check index.html — support both flat and browser\ subfolder layouts
$frontendIndexFound = (Test-Path "$PkgDir\frontend\index.html") -or (Test-Path "$PkgDir\frontend\browser\index.html")
if ($frontendIndexFound) { Write-Host "  [OK] Frontend files found" -ForegroundColor Green }
else { Write-Host "  [FAIL] frontend\index.html not found in package folder." -ForegroundColor Red; $fail = $true }

if (Test-Path "$PkgDir\hsi-utilities-backend.env") { Write-Host "  [OK] .env file found" -ForegroundColor Green }
else { Write-Host "  [FAIL] hsi-utilities-backend.env not found in package folder." -ForegroundColor Red; $fail = $true }

if ($fail) {
    Write-Host ""
    Write-Host "Ada requirement yang tak dipenuhi. Sila betulkan dulu, then run semula." -ForegroundColor Red
    exit 1
}

# ─── COPY FILES TO APP DIR ────────────────────────────────────────────────────
Write-Host ""
Write-Host "[2/5] Copying files to $AppDir..." -ForegroundColor Yellow

New-Item -ItemType Directory -Force -Path $AppDir      | Out-Null
New-Item -ItemType Directory -Force -Path $LogDir      | Out-Null
New-Item -ItemType Directory -Force -Path $FrontendDir | Out-Null

Copy-Item -Force "$PkgDir\app.jar" $JarPath
Copy-Item -Force "$PkgDir\hsi-utilities-backend.env" $EnvFile

# Copy frontend — handle browser\ subfolder (Angular 17) or flat layout
if (Test-Path "$PkgDir\frontend\browser\index.html") {
    Copy-Item -Recurse -Force "$PkgDir\frontend\browser\*" $FrontendDir
} else {
    Copy-Item -Recurse -Force "$PkgDir\frontend\*" $FrontendDir
}

Write-Host "  [OK] Files copied" -ForegroundColor Green

# ─── CREATE BATCH LAUNCHER ────────────────────────────────────────────────────
# Use a .bat launcher so JVM --add-opens flags are always applied correctly
$batContent = "@echo off`r`n"
$batContent += "`"$javaExe`" ^`r`n"
$batContent += "  --add-opens java.base/java.lang=ALL-UNNAMED ^`r`n"
$batContent += "  --add-opens java.base/java.util=ALL-UNNAMED ^`r`n"
$batContent += "  --add-opens java.base/java.io=ALL-UNNAMED ^`r`n"
$batContent += "  -jar `"$JarPath`"`r`n"
Set-Content -Path $LauncherBat -Value $batContent -Encoding ASCII

Write-Host "  [OK] Batch launcher created: $LauncherBat" -ForegroundColor Green

# ─── BACKEND SERVICE ──────────────────────────────────────────────────────────
Write-Host ""
Write-Host "[3/5] Installing backend service (hsi-backend)..." -ForegroundColor Yellow

$svc = Get-Service -Name 'hsi-backend' -ErrorAction SilentlyContinue
if ($svc) {
    Write-Host "  Service sedia ada. Stop dan remove dulu..." -ForegroundColor Gray
    & $NssmExe stop hsi-backend confirm 2>&1 | Out-Null
    & $NssmExe remove hsi-backend confirm 2>&1 | Out-Null
    Start-Sleep -Seconds 2
}

& $NssmExe install hsi-backend "cmd.exe"
& $NssmExe set hsi-backend AppParameters "/c `"$LauncherBat`""
& $NssmExe set hsi-backend AppDirectory $AppDir
& $NssmExe set hsi-backend AppEnvironmentExtra "SPRING_CONFIG_LOCATION=file:$EnvFile"
& $NssmExe set hsi-backend AppStdout "$LogDir\backend.log"
& $NssmExe set hsi-backend AppStderr "$LogDir\backend-error.log"
& $NssmExe set hsi-backend AppRotateFiles 1
& $NssmExe set hsi-backend AppRotateBytes 10485760
& $NssmExe set hsi-backend Start SERVICE_AUTO_START
& $NssmExe start hsi-backend

Write-Host "  [OK] hsi-backend service installed and started" -ForegroundColor Green

# ─── NGINX CONFIG ─────────────────────────────────────────────────────────────
Write-Host ""
Write-Host "[4/5] Configuring and installing nginx..." -ForegroundColor Yellow

$frontendPath = $FrontendDir -replace '\\', '/'

$nginxConf = @"
worker_processes 1;
events { worker_connections 1024; }
http {
    include       mime.types;
    default_type  application/octet-stream;
    sendfile      on;
    keepalive_timeout 65;

    server {
        listen 8081;

        root $frontendPath;
        index index.html;

        access_log logs/hsi-access.log;
        error_log  logs/hsi-error.log;

        location / {
            try_files `$uri `$uri/ /index.html;
        }

        location /api/ {
            proxy_pass         http://127.0.0.1:8080/api/;
            proxy_http_version 1.1;
            proxy_set_header   Host            `$host;
            proxy_set_header   X-Real-IP       `$remote_addr;
            proxy_set_header   X-Forwarded-For `$proxy_add_x_forwarded_for;
            proxy_connect_timeout 60s;
            proxy_read_timeout    60s;
        }
    }
}
"@

# Write without BOM — nginx does not support UTF-8 BOM
[System.IO.File]::WriteAllText("$NginxDir\conf\nginx.conf", $nginxConf, [System.Text.UTF8Encoding]::new($false))
Write-Host "  [OK] nginx.conf written" -ForegroundColor Green

$svc = Get-Service -Name 'hsi-nginx' -ErrorAction SilentlyContinue
if ($svc) {
    Write-Host "  Service sedia ada. Removing..." -ForegroundColor Gray
    & $NssmExe stop hsi-nginx confirm 2>&1 | Out-Null
    & $NssmExe remove hsi-nginx confirm 2>&1 | Out-Null
    Start-Sleep -Seconds 2
}

& $NssmExe install hsi-nginx "$NginxDir\nginx.exe"
& $NssmExe set hsi-nginx AppDirectory $NginxDir
& $NssmExe set hsi-nginx Start SERVICE_AUTO_START
& $NssmExe start hsi-nginx

Write-Host "  [OK] hsi-nginx service installed and started" -ForegroundColor Green

# ─── FIREWALL ─────────────────────────────────────────────────────────────────
Write-Host ""
Write-Host "[5/5] Configuring firewall..." -ForegroundColor Yellow

if (-not (Get-NetFirewallRule -DisplayName 'HSI Frontend' -ErrorAction SilentlyContinue)) {
    New-NetFirewallRule -DisplayName 'HSI Frontend' -Direction Inbound -Protocol TCP -LocalPort 8081 -Action Allow | Out-Null
    Write-Host "  [OK] Port 8081 dibuka" -ForegroundColor Green
} else {
    Write-Host "  [OK] Firewall rule port 8081 dah ada" -ForegroundColor Green
}

if (-not (Get-NetFirewallRule -DisplayName 'HSI Block Backend External' -ErrorAction SilentlyContinue)) {
    New-NetFirewallRule -DisplayName 'HSI Block Backend External' -Direction Inbound -Protocol TCP -LocalPort 8080 -Action Block | Out-Null
    Write-Host "  [OK] Port 8080 diblock dari luar" -ForegroundColor Green
} else {
    Write-Host "  [OK] Firewall block rule port 8080 dah ada" -ForegroundColor Green
}

# ─── DONE ─────────────────────────────────────────────────────────────────────
Write-Host ""
Write-Host "===============================" -ForegroundColor Cyan
Write-Host "  Setup complete!" -ForegroundColor Cyan
Write-Host "===============================" -ForegroundColor Cyan
Write-Host ""
Write-Host "  App URL : http://<server-ip>:8081" -ForegroundColor White
Write-Host "  Logs    : $LogDir" -ForegroundColor White
Write-Host ""
Write-Host "Check status services:" -ForegroundColor Gray
Write-Host "  Get-Service hsi-backend, hsi-nginx" -ForegroundColor Gray
Write-Host ""
