$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$backend  = Join-Path $root 'backend'
$frontend = Join-Path $root 'frontend'

Write-Host "Starting HSI Utilities (dev)..." -ForegroundColor Cyan

# Backend - Spring Boot
Start-Process powershell -ArgumentList "-NoExit", "-Command",
    "Set-Location '$backend'; Write-Host 'Backend starting on :8080' -ForegroundColor Green; mvn spring-boot:run"

# Frontend - Angular dev server
Start-Process powershell -ArgumentList "-NoExit", "-Command",
    "Set-Location '$frontend'; Write-Host 'Frontend starting on :4200' -ForegroundColor Green; npm run start"

Write-Host ""
Write-Host "Backend  -> http://localhost:8080" -ForegroundColor Yellow
Write-Host "Frontend -> http://localhost:4200" -ForegroundColor Yellow
Write-Host ""
Write-Host "Tutup kedua-dua terminal untuk stop." -ForegroundColor Gray
