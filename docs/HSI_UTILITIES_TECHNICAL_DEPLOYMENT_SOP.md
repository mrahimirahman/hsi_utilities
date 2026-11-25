# HSI Utilities Technical Deployment SOP

Tarikh: 05/04/2026

## 1. Tujuan

Dokumen ini disediakan untuk team infra / support bagi tujuan:

- build release artifact
- deploy frontend dan backend ke Ubuntu server
- semak service dan endpoint
- troubleshoot isu asas selepas deploy

## 2. Maklumat Sistem

Nama sistem:

- HSI Utilities System

Frontend:

- Angular

Backend:

- Spring Boot 1.5.22

Database:

- MySQL 5.7
- Host: `10.29.59.55`
- Database: `his_kkm`

Server deploy:

- Ubuntu server `10.29.59.55`

## 3. Reka Bentuk Deploy

Disebabkan staging HIS sedia ada telah menggunakan Nginx pada:

- `http://10.29.59.55/hiskkm_stg/`

HSI Utilities dipisahkan ke port lain supaya tidak mengganggu config staging:

- Frontend URL: `http://10.29.59.55:8081/`
- Backend internal: `http://127.0.0.1:18080`

Aliran trafik:

1. Browser user buka `http://10.29.59.55:8081/`
2. Nginx serve Angular static files
3. Request `/api/*` diproxy ke Spring Boot di `127.0.0.1:18080`

## 4. Lokasi Fail Penting

Di workstation Windows:

- [build-release.ps1](C:/trae_ai/hsi_utilities/deploy/windows/build-release.ps1)
- [README.md](C:/trae_ai/hsi_utilities/deploy/ubuntu/README.md)
- [hsi-utilities.conf](C:/trae_ai/hsi_utilities/deploy/ubuntu/nginx/hsi-utilities.conf)
- [hsi-utilities-backend.service](C:/trae_ai/hsi_utilities/deploy/ubuntu/systemd/hsi-utilities-backend.service)
- [hsi-utilities-backend.env](C:/trae_ai/hsi_utilities/deploy/ubuntu/env/hsi-utilities-backend.env)
- [install_runtime.sh](C:/trae_ai/hsi_utilities/deploy/ubuntu/scripts/install_runtime.sh)
- [deploy_frontend.sh](C:/trae_ai/hsi_utilities/deploy/ubuntu/scripts/deploy_frontend.sh)
- [deploy_backend.sh](C:/trae_ai/hsi_utilities/deploy/ubuntu/scripts/deploy_backend.sh)
- [restart_services.sh](C:/trae_ai/hsi_utilities/deploy/ubuntu/scripts/restart_services.sh)

Di Ubuntu server:

- frontend root: `/var/www/hsi-utilities`
- backend app: `/opt/hsi-utilities/maintenance-system-0.0.1-SNAPSHOT.jar`
- env file: `/etc/hsi-utilities/hsi-utilities-backend.env`
- systemd service: `/etc/systemd/system/hsi-utilities-backend.service`
- nginx site: `/etc/nginx/sites-available/hsi-utilities.conf`

## 5. Prasyarat

Pastikan perkara berikut tersedia:

- akses Windows ke source code `C:\trae_ai\hsi_utilities`
- akses SSH ke Ubuntu server `10.29.59.55`
- Java runtime tersedia di server
- Nginx tersedia di server
- akses database `his_kkm`
- port `8081` dibuka oleh team infra jika user akan akses dari luar server

## 6. Build Release di Windows

### 6.1 Jalankan build release

```powershell
cd C:\trae_ai\hsi_utilities\deploy\windows
.\build-release.ps1
```

### 6.2 Hasil build

Artifact akan dikeluarkan ke:

```text
C:\trae_ai\hsi_utilities\release
```

Kandungan utama:

- `maintenance-system-0.0.1-SNAPSHOT.jar`
- `hospital-maintenance-frontend.zip`
- `hsi-utilities-backend.env`
- `ubuntu-deploy\`

## 7. Konfigurasi Sebelum Deploy

### 7.1 Semak env file

Buka fail:

- [hsi-utilities-backend.env](C:/trae_ai/hsi_utilities/deploy/ubuntu/env/hsi-utilities-backend.env)

Semak nilai penting:

```env
SERVER_PORT=18080
SPRING_DATASOURCE_URL=jdbc:mysql://10.29.59.55:3306/his_kkm?useSSL=false&allowPublicKeyRetrieval=true
SPRING_DATASOURCE_USERNAME=utilities_app
SPRING_DATASOURCE_PASSWORD=CHANGE_ME
```

Nota:

- untuk peringkat awal, boleh guna credential semasa jika perlu
- untuk operasi sebenar, sangat digalakkan guna DB user khas aplikasi

## 8. Copy Artifact ke Ubuntu

Gunakan `scp`, WinSCP, atau tool setara.

Contoh lokasi sasaran di server:

```text
/tmp/hsi-utilities-release/
```

Item yang perlu dipindahkan:

- `maintenance-system-0.0.1-SNAPSHOT.jar`
- `hospital-maintenance-frontend.zip`
- `hsi-utilities-backend.env`
- folder `ubuntu-deploy`

## 9. Install Runtime di Ubuntu

SSH ke server:

```bash
ssh root@10.29.59.55
```

Pergi ke folder script:

```bash
cd /tmp/hsi-utilities-release/ubuntu-deploy/scripts
```

Jika script hasil copy dari Windows, normalkan line ending:

```bash
sudo sed -i 's/\r$//' /tmp/hsi-utilities-release/ubuntu-deploy/scripts/*.sh
sudo chmod +x /tmp/hsi-utilities-release/ubuntu-deploy/scripts/*.sh
```

Install runtime:

```bash
sudo bash install_runtime.sh
```

## 10. Deploy Frontend

### 10.1 Kaedah disyorkan

Jika `hospital-maintenance-frontend.zip` extract dengan baik di Linux, boleh guna script:

```bash
cd /tmp/hsi-utilities-release/ubuntu-deploy/scripts
sudo bash deploy_frontend.sh /tmp/hsi-utilities-release/hospital-maintenance-frontend.zip
```

### 10.2 Fallback jika zip dari Windows bermasalah

Jika zip keluarkan warning path separator atau frontend tidak masuk ke root yang betul, copy folder dist secara manual dan pastikan file `index.html` berada terus di `/var/www/hsi-utilities`.

Contoh fallback manual:

```bash
sudo mkdir -p /var/www/hsi-utilities
sudo rm -rf /var/www/hsi-utilities/*
sudo cp -R /tmp/hsi-utilities-frontend/hospital-maintenance-frontend/browser/* /var/www/hsi-utilities/
sudo chown -R www-data:www-data /var/www/hsi-utilities
sudo find /var/www/hsi-utilities -type d -exec chmod 755 {} \;
sudo find /var/www/hsi-utilities -type f -exec chmod 644 {} \;
sudo systemctl reload nginx
```

### 10.3 Semakan frontend di server

```bash
ls -la /var/www/hsi-utilities
curl http://127.0.0.1:8081
```

Perlu wujud:

- `index.html`
- `main.js`
- `polyfills.js`
- `styles.css`

## 11. Deploy Backend

Deploy backend:

```bash
cd /tmp/hsi-utilities-release/ubuntu-deploy/scripts
sudo bash deploy_backend.sh /tmp/hsi-utilities-release/maintenance-system-0.0.1-SNAPSHOT.jar /tmp/hsi-utilities-release/hsi-utilities-backend.env
```

Semakan:

```bash
sudo systemctl status hsi-utilities-backend --no-pager
curl http://127.0.0.1:18080/api/auth/session
```

Response API yang normal tanpa login:

```json
{"authenticated":false,"message":"No active session.", ...}
```

## 12. Nginx Konfigurasi Operasi

Fail config:

- `/etc/nginx/sites-available/hsi-utilities.conf`

Konfigurasi operasi semasa:

```nginx
server {
    listen 8081;
    server_name 10.29.59.55;

    root /var/www/hsi-utilities;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }

    location /api/ {
        proxy_pass http://127.0.0.1:18080/api/;
    }
}
```

Semakan:

```bash
sudo nginx -t
sudo systemctl restart nginx
sudo ss -tulpn | grep 8081
curl http://127.0.0.1:8081
```

## 13. URL Operasi

Frontend untuk user:

```text
http://10.29.59.55:8081/
```

Backend internal:

```text
http://127.0.0.1:18080/api/
```

## 14. Command Operasi Harian

Restart backend:

```bash
sudo systemctl restart hsi-utilities-backend
```

Restart nginx:

```bash
sudo systemctl restart nginx
```

Semak backend:

```bash
sudo systemctl status hsi-utilities-backend --no-pager
```

Semak nginx:

```bash
sudo systemctl status nginx --no-pager
```

Semak log backend:

```bash
sudo tail -n 100 /var/log/hsi-utilities/backend.log
sudo tail -n 100 /var/log/hsi-utilities/backend-error.log
```

Semak log systemd:

```bash
sudo journalctl -u hsi-utilities-backend -n 100 --no-pager
```

## 15. Troubleshooting Ringkas

### 15.1 Browser luar tidak boleh buka `10.29.59.55:8081`

Semak di server:

```bash
curl http://127.0.0.1:8081
```

Jika berjaya di server tetapi gagal dari PC user:

- isu besar kemungkinan firewall / network rule
- minta team infra buka inbound TCP `8081`

### 15.2 Backend gagal start

Semak:

```bash
sudo systemctl status hsi-utilities-backend --no-pager
sudo journalctl -u hsi-utilities-backend -n 100 --no-pager
sudo tail -n 100 /var/log/hsi-utilities/backend-error.log
```

Punca yang pernah ditemui:

- port sudah digunakan
- line ending script Windows (`CRLF`)
- config env tidak betul

### 15.3 Frontend masih keluar page `Welcome to nginx!`

Semak:

```bash
ls -la /var/www/hsi-utilities
```

Pastikan `index.html` ada terus pada root `/var/www/hsi-utilities`.

### 15.4 Nginx config conflict

Pada server ini, config staging HIS sedia ada tidak boleh diganggu.

Semak config aktif:

```bash
sudo nginx -T | grep -n "server_name"
```

HSI Utilities mesti kekal pada port `8081`, bukan `80`.

### 15.5 Script `.sh` gagal dengan `pipefail` / `invalid option`

Punca:

- script masih ada line ending Windows

Fix:

```bash
sudo sed -i 's/\r$//' /tmp/hsi-utilities-release/ubuntu-deploy/scripts/*.sh
sudo chmod +x /tmp/hsi-utilities-release/ubuntu-deploy/scripts/*.sh
```

## 16. Status Semasa Deploy

Status pada 05/04/2026:

- frontend berjaya diakses dari server pada `127.0.0.1:8081`
- backend berjaya diakses dari server pada `127.0.0.1:18080`
- `hsi-utilities-backend` service hidup
- nginx hidup
- aplikasi telah siap deploy secara server-side
- akses dari PC luar masih memerlukan pembukaan port `8081` oleh team infra

## 17. Hand-off Operasi Support

Selepas port `8081` dibuka:

1. support team akses `http://10.29.59.55:8081/`
2. login sebagai user yang dibenarkan
3. guna menu `Reports` untuk correction data
4. guna menu `Audit Log` untuk semakan perubahan

