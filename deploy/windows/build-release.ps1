$ErrorActionPreference = 'Stop'

$root = 'C:\trae_ai\hsi_utilities'
$release = Join-Path $root 'release'
$backend = Join-Path $root 'backend'
$frontend = Join-Path $root 'frontend'
$deployUbuntu = Join-Path $root 'deploy\ubuntu'

if (Test-Path $release) {
    Remove-Item -Recurse -Force $release
}
New-Item -ItemType Directory -Path $release | Out-Null

Push-Location $backend
mvn clean package -DskipTests
Pop-Location

Push-Location $frontend
cmd /c npm install
cmd /c npx ng build
Pop-Location

Copy-Item (Join-Path $backend 'target\maintenance-system-0.0.1-SNAPSHOT.jar') $release
Copy-Item (Join-Path $deployUbuntu 'env\hsi-utilities-backend.env') $release
Copy-Item -Recurse $deployUbuntu (Join-Path $release 'ubuntu-deploy')

$dist = Join-Path $frontend 'dist\hospital-maintenance-frontend'
$zip = Join-Path $release 'hospital-maintenance-frontend.zip'
Compress-Archive -Path (Join-Path $dist '*') -DestinationPath $zip -Force

Write-Host 'Release artifacts prepared in:' $release
