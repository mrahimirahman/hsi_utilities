# STAGING Deployment Checklist

Tarikh kemas kini: 06/04/2026

Dokumen ini merangkum langkah deploy staging untuk HSI Utilities berdasarkan artefak release semasa.

## Artefak Release

Lokasi tempatan:

- `C:\trae_ai\hsi_utilities\release\maintenance-system-0.0.1-SNAPSHOT.jar`
- `C:\trae_ai\hsi_utilities\release\hospital-maintenance-frontend.zip`
- `C:\trae_ai\hsi_utilities\release\hsi-utilities-backend.env`
- `C:\trae_ai\hsi_utilities\release\ubuntu-deploy\`

## Susun Atur Server Staging

- Frontend root: `/var/www/hsi-utilities`
- Backend home: `/opt/hsi-utilities`
- Backend env file: `/etc/hsi-utilities/hsi-utilities-backend.env`
- Backend service: `hsi-utilities-backend`
- Frontend access URL: `http://10.29.59.55:8081/`
- Backend local URL: `http://127.0.0.1:8080`

## Nota Konfigurasi Penting

Fail env release semasa telah dikemas kini untuk guna database host berikut:

- `SPRING_DATASOURCE_URL=jdbc:mysql://10.29.217.205:3306/his_kkm?useSSL=false&allowPublicKeyRetrieval=true`

Sebelum deploy, semak juga nilai berikut dalam env:

- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `SERVER_PORT=8080`
- `OPENAI_API_KEY` jika mahu aktifkan `AI Assistant`
- `OPENAI_MODEL=gpt-4.1-mini`
- `OPENAI_RESPONSES_URL=https://api.openai.com/v1/responses`

Jika `OPENAI_API_KEY` dibiarkan kosong, aplikasi tetap boleh berjalan tetapi `AI Assistant` akan memaparkan mesej belum dikonfigurasi.

## Checklist Deploy

1. Build release di Windows jika perlu semula.

```powershell
cd C:\trae_ai\hsi_utilities
```

Artefak release semasa sudah tersedia dalam folder `release`.

2. Copy artefak berikut ke server, contoh ke `/tmp/hsi-utilities-release/`.

- `maintenance-system-0.0.1-SNAPSHOT.jar`
- `hospital-maintenance-frontend.zip`
- `hsi-utilities-backend.env`
- folder `ubuntu-deploy`

3. Di server, jalankan install runtime jika server belum disediakan.

```bash
cd /tmp/hsi-utilities-release/ubuntu-deploy/scripts
sudo bash install_runtime.sh
```

4. Deploy frontend.

```bash
cd /tmp/hsi-utilities-release/ubuntu-deploy/scripts
sudo bash deploy_frontend.sh /tmp/hsi-utilities-release/hospital-maintenance-frontend.zip
```

5. Deploy backend.

```bash
cd /tmp/hsi-utilities-release/ubuntu-deploy/scripts
sudo bash deploy_backend.sh \
  /tmp/hsi-utilities-release/maintenance-system-0.0.1-SNAPSHOT.jar \
  /tmp/hsi-utilities-release/hsi-utilities-backend.env
```

6. Restart service.

```bash
cd /tmp/hsi-utilities-release/ubuntu-deploy/scripts
sudo bash restart_services.sh
```

7. Semak status service dan config.

```bash
sudo systemctl status hsi-utilities-backend --no-pager
sudo systemctl status nginx --no-pager
sudo nginx -t
```

8. Semak endpoint local server.

```bash
curl http://127.0.0.1:8080/api/auth/session
curl http://127.0.0.1:8081
```

9. Ujian browser minimum.

- buka `http://10.29.59.55:8081/`
- login berjaya
- buka `HSI Reports`
- buka `Data Diagnosis DAMA Bulanan`
- test tarikh `2026-03-01` hingga `2026-03-31`
- semak jumlah rekod ikut jangkaan
- test `Generate Excel`
- jika `OPENAI_API_KEY` telah diisi, test `AI Assistant` menggunakan akaun `ADMIN`

## Fail Rujukan

- `C:\trae_ai\hsi_utilities\release\ubuntu-deploy\README.md`
- `C:\trae_ai\hsi_utilities\release\ubuntu-deploy\systemd\hsi-utilities-backend.service`
- `C:\trae_ai\hsi_utilities\release\ubuntu-deploy\nginx\hsi-utilities.conf`
- `C:\trae_ai\hsi_utilities\release\ubuntu-deploy\env\hsi-utilities-backend.env`

## Semakan Selepas Deploy

Jika browser luar tidak boleh akses tetapi `curl http://127.0.0.1:8081` berjaya di server, besar kemungkinan isu adalah pada firewall atau network policy untuk port `8081`.

