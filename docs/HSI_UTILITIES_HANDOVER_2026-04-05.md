# HSI Utilities System Handover

Tarikh kemas kini: 07/04/2026

## 1. Ringkasan Projek

Sistem ini ialah web application untuk kegunaan HSI/hospital dengan fokus kepada:

- login dan session berasaskan browser
- frontend menggunakan Angular
- backend menggunakan Spring Boot
- sambungan ke MySQL 5.7
- semakan dan kemas kini report melalui browser
- audit trail untuk perubahan report
- pakej deploy ke Ubuntu server
- bantuan `AI Assistant` untuk soalan read-only dan tindakan ringkas tertentu

Ringkasan teknologi:

- Frontend: Angular standalone components
- Backend: Spring Boot
- Database: MySQL `his_kkm`
- Frontend access URL staging: `http://10.29.59.55:8081/`
- Backend local URL staging: `http://127.0.0.1:8080`

Nota database semasa:

- konfigurasi release semasa menunjuk ke host `10.29.217.205`
- fail local `application.properties` masih mengandungi konfigurasi development yang berbeza
- rujukan deploy sebenar hendaklah ikut fail release env, bukan semata-mata `application.properties`

## 2. Modul Sistem Semasa

### 2.1 Login dan Session

Fungsi yang telah siap:

- login melalui `/api/auth/login`
- semakan session semasa melalui `/api/auth/session`
- logout melalui `/api/auth/logout`
- route Angular dilindungi menggunakan auth guard

Ringkasan fail:

- [AuthController.java](C:/trae_ai/hsi_utilities/backend/src/main/java/com/hospital/maintenance/web/AuthController.java)
- [auth.service.ts](C:/trae_ai/hsi_utilities/frontend/src/app/core/auth.service.ts)
- [auth.guard.ts](C:/trae_ai/hsi_utilities/frontend/src/app/core/auth.guard.ts)
- [login.component.ts](C:/trae_ai/hsi_utilities/frontend/src/app/features/auth/login.component.ts)

### 2.2 Layout Aplikasi

Susun atur semasa:

- default route selepas login ialah `HSI Reports`
- menu sidebar utama yang dipaparkan sekarang ialah `HSI Reports`
- menu `Audit Log` hanya dipaparkan kepada `ADMIN`
- komponen `AI Assistant` dipasang dalam dashboard layout dan hanya boleh digunakan oleh `ADMIN`

Ringkasan fail:

- [app.routes.ts](C:/trae_ai/hsi_utilities/frontend/src/app/app.routes.ts)
- [dashboard-layout.component.ts](C:/trae_ai/hsi_utilities/frontend/src/app/layout/dashboard-layout.component.ts)

### 2.3 HSI Reports Menu

Menu reports semasa telah disediakan untuk 9 item permintaan HSI:

- `Update PD103 Bancian Harian Wad` = `Ready`
- `Data Diagnosis DAMA Bulanan` = `Ready`
- item lain telah ada menu/route placeholder tetapi logik report belum dibina

Item placeholder yang sudah mempunyai route:

- `Data Lean`
- `Data Admission (ETD) Bulanan`
- `Referral Jabatan Kecemasan ke Psikiatri`
- `Referral Jabatan Lain ke Psikiatri`
- `Data Semua UserID Aktif`
- `Pesakit Tanpa Discharge Summary Baharu`
- `Kes Rujukan ke Psikiatri Mengikut Jenis Kes`

Ringkasan fail:

- [reports-menu.component.ts](C:/trae_ai/hsi_utilities/frontend/src/app/features/reports/reports-menu.component.ts)
- [report-placeholder.component.ts](C:/trae_ai/hsi_utilities/frontend/src/app/features/reports/report-placeholder.component.ts)

### 2.4 Modul PD103

Fungsi yang telah siap:

- pilih `Ward`
- pilih tarikh report
- jana report `(PD103) Bancian Harian Wad: Pin. 2/2009`
- paparan report diselaraskan hampir sama dengan format asal
- cetakan/print preview disediakan
- query data report menggunakan table `pd104`

Logik penting:

- susunan row dan disiplin dipetakan secara khusus, bukan generic report builder
- formula row tertentu dikawal supaya tidak boleh diedit sesuka hati
- row `DAMA` juga wujud dalam PD103 sebagai sebahagian daripada report harian

Ringkasan fail:

- [InpatientReportService.java](C:/trae_ai/hsi_utilities/backend/src/main/java/com/hospital/maintenance/service/InpatientReportService.java)
- [ReportController.java](C:/trae_ai/hsi_utilities/backend/src/main/java/com/hospital/maintenance/web/ReportController.java)
- [inpatient-report.component.ts](C:/trae_ai/hsi_utilities/frontend/src/app/features/reports/inpatient-report.component.ts)

### 2.5 Edit / Update PD103

Fungsi yang telah siap:

- `Edit`
- ubah nilai cell yang dibenarkan
- `Save`
- `Cancel`
- row formula tidak boleh diedit
- simpan perubahan terus ke table `pd104`

Sekatan akses:

- hanya `ADMIN` boleh edit dan save

Endpoint berkaitan:

- `GET /api/reports/inpatient`
- `POST /api/reports/inpatient`

### 2.6 Audit Log

Fungsi yang telah siap:

- setiap perubahan PD103 direkodkan ke table `report_edit_audit`
- screen audit boleh ditapis mengikut `wardCode` dan `reportDate`
- hanya `ADMIN` boleh akses audit log
- dari audit log, user boleh buka semula screen PD103 untuk ward/tarikh berkaitan

Maklumat audit yang disimpan:

- `report_code`
- `ward_code`
- `report_date`
- `discipline_code`
- `metric_index`
- nilai lama lelaki/perempuan
- nilai baru lelaki/perempuan
- `edited_by`
- `edited_name`
- `edited_role`
- `edited_at`

Ringkasan fail:

- [ReportAuditLogEntry.java](C:/trae_ai/hsi_utilities/backend/src/main/java/com/hospital/maintenance/dto/ReportAuditLogEntry.java)
- [report-audit-log.component.ts](C:/trae_ai/hsi_utilities/frontend/src/app/features/reports/report-audit-log.component.ts)

### 2.7 Modul Data Diagnosis DAMA Bulanan

Fungsi yang telah siap:

- pilih `fromDate`
- pilih `toDate`
- jana report bulanan diagnosis DAMA
- paparkan jumlah rekod dan senarai hasil di screen
- export terus ke fail Excel `.xls`

Endpoint berkaitan:

- `GET /api/reports/dama-diagnosis`
- `GET /api/reports/dama-diagnosis/export`

Format nama fail export:

- `data-diagnosis-dama-YYYY-MM-DD-to-YYYY-MM-DD.xls`

Ringkasan fail:

- [DamaDiagnosisReportService.java](C:/trae_ai/hsi_utilities/backend/src/main/java/com/hospital/maintenance/service/DamaDiagnosisReportService.java)
- [dama-diagnosis-report.component.ts](C:/trae_ai/hsi_utilities/frontend/src/app/features/reports/dama-diagnosis-report.component.ts)

### 2.8 AI Assistant

Fungsi semasa:

- hanya boleh digunakan oleh `ADMIN`
- endpoint chat ialah `POST /api/assistant/chat`
- jika `OPENAI_API_KEY` diisi, assistant akan guna OpenAI Responses API
- jika OpenAI belum dikonfigurasi atau gagal dicapai, sistem akan jawab menggunakan fallback knowledge tempatan
- assistant juga boleh kenal pasti arahan tertentu seperti tindakan download report DAMA bagi bulan atau julat tarikh tertentu

Had semasa:

- kegunaan adalah read-only / assistive
- bukan untuk terus mengubah data database
- pengetahuan assistant tertumpu pada modul semasa seperti `PD103`, `DAMA report`, `Audit Log`, dan deploy staging

Ringkasan fail:

- [AssistantController.java](C:/trae_ai/hsi_utilities/backend/src/main/java/com/hospital/maintenance/web/AssistantController.java)
- [OpenAiAssistantService.java](C:/trae_ai/hsi_utilities/backend/src/main/java/com/hospital/maintenance/service/OpenAiAssistantService.java)
- [LocalAssistantFallbackService.java](C:/trae_ai/hsi_utilities/backend/src/main/java/com/hospital/maintenance/service/LocalAssistantFallbackService.java)
- [ai-assistant.service.ts](C:/trae_ai/hsi_utilities/frontend/src/app/core/ai-assistant.service.ts)

### 2.9 Endpoint Tambahan Yang Wujud

Selain modul utama di atas, kod semasa juga mengandungi:

- endpoint dashboard `GET /api/dashboard`
- endpoint maintenance requests `GET /api/requests`

Nota:

- route Angular untuk `dashboard` dan `requests` masih wujud
- tetapi navigation utama semasa lebih memfokuskan `HSI Reports` dan `Audit Log`

## 3. Flow Operasi Disasarkan

Flow semasa untuk pembetulan report harian:

1. User hantar permintaan semakan atau pembetulan data.
2. Support/Admin login ke sistem.
3. Buka `HSI Reports`.
4. Pilih modul `Update PD103 Bancian Harian Wad`.
5. Pilih wad dan tarikh.
6. Jana report.
7. Jika perlu, `ADMIN` klik `Edit`.
8. Ubah nilai cell yang dibenarkan.
9. Klik `Save`.
10. Sistem kemas kini `pd104`.
11. Sistem simpan audit trail ke `report_edit_audit`.
12. Jika perlu semakan semula, buka `Audit Log`.

Flow semasa untuk report bulanan DAMA:

1. Login ke sistem.
2. Buka `HSI Reports`.
3. Pilih `Data Diagnosis DAMA Bulanan`.
4. Pilih `fromDate` dan `toDate`.
5. Klik `Generate`.
6. Semak jumlah rekod dan senarai hasil.
7. Klik `Generate Excel` jika perlu export.

## 4. Maklumat Deploy Staging

Server sasaran:

- Ubuntu server `10.29.59.55`

Susun atur staging semasa:

- frontend root: `/var/www/hsi-utilities`
- backend home: `/opt/hsi-utilities`
- backend env file: `/etc/hsi-utilities/hsi-utilities-backend.env`
- backend service: `hsi-utilities-backend`
- frontend access URL: `http://10.29.59.55:8081/`
- backend local URL: `http://127.0.0.1:8080`

Sebab guna port `8081`:

- staging HIS sedia ada sudah menggunakan Nginx pada port `80`
- HSI Utilities dipisahkan untuk elakkan ganggu setup `/hiskkm_stg/`

## 5. Artefak Release dan Fail Deploy

Artefak release semasa berada dalam:

- `C:\trae_ai\hsi_utilities\release\`

Artefak utama:

- [maintenance-system-0.0.1-SNAPSHOT.jar](C:/trae_ai/hsi_utilities/release/maintenance-system-0.0.1-SNAPSHOT.jar)
- [hospital-maintenance-frontend.zip](C:/trae_ai/hsi_utilities/release/hospital-maintenance-frontend.zip)
- [hsi-utilities-backend.env](C:/trae_ai/hsi_utilities/release/hsi-utilities-backend.env)

Fail deploy Ubuntu:

- [README.md](C:/trae_ai/hsi_utilities/release/ubuntu-deploy/README.md)
- [hsi-utilities.conf](C:/trae_ai/hsi_utilities/release/ubuntu-deploy/nginx/hsi-utilities.conf)
- [hsi-utilities-backend.service](C:/trae_ai/hsi_utilities/release/ubuntu-deploy/systemd/hsi-utilities-backend.service)
- [hsi-utilities-backend.env](C:/trae_ai/hsi_utilities/release/ubuntu-deploy/env/hsi-utilities-backend.env)
- [install_runtime.sh](C:/trae_ai/hsi_utilities/release/ubuntu-deploy/scripts/install_runtime.sh)
- [deploy_frontend.sh](C:/trae_ai/hsi_utilities/release/ubuntu-deploy/scripts/deploy_frontend.sh)
- [deploy_backend.sh](C:/trae_ai/hsi_utilities/release/ubuntu-deploy/scripts/deploy_backend.sh)
- [restart_services.sh](C:/trae_ai/hsi_utilities/release/ubuntu-deploy/scripts/restart_services.sh)

## 6. Konfigurasi Penting

Frontend API config:

- [api.config.ts](C:/trae_ai/hsi_utilities/frontend/src/app/core/api.config.ts)

Behavior frontend:

- local development guna `http://localhost:8080/api`
- apabila diakses dari host server, frontend akan guna `/api`

Nilai penting release env semasa:

- `SERVER_PORT=8080`
- `SPRING_DATASOURCE_URL=jdbc:mysql://10.29.217.205:3306/his_kkm?...`
- `SPRING_DATASOURCE_USERNAME=utilities_app`
- `OPENAI_MODEL=gpt-4.1-mini`
- `OPENAI_RESPONSES_URL=https://api.openai.com/v1/responses`

Nota operasi:

- jika `OPENAI_API_KEY` kosong, aplikasi masih boleh berjalan
- dalam keadaan itu, `AI Assistant` akan jawab menggunakan fallback tempatan / mesej konfigurasi

## 7. Semakan Operasi Di Server

Command semakan penting:

```bash
sudo systemctl status hsi-utilities-backend --no-pager
sudo systemctl status nginx --no-pager
sudo nginx -t
curl http://127.0.0.1:8080/api/auth/session
curl http://127.0.0.1:8081
```

Jika perlu, semak juga:

- login browser ke `http://10.29.59.55:8081/`
- modul `HSI Reports`
- modul `Data Diagnosis DAMA Bulanan`
- modul `Audit Log` sebagai `ADMIN`

## 8. Isu dan Nota Penting

### 8.1 Dokumen Lama Ada Maklumat Yang Telah Berubah

Beberapa rujukan lama sudah tidak tepat:

- backend staging kini dirujuk pada port `8080`, bukan `18080`
- struktur deploy aktif kini berpandukan folder `release\ubuntu-deploy`
- modul aktif kini bukan lagi PD103 sahaja, tetapi juga `Data Diagnosis DAMA Bulanan` dan `AI Assistant`

### 8.2 Akses Luar Ke Port 8081

Jika browser dari PC luar masih tidak boleh akses:

- kemungkinan besar firewall / network policy belum membuka inbound TCP `8081`
- perlu semakan oleh team infra

### 8.3 Konfigurasi Database

Untuk staging/production:

- elakkan bergantung pada credential development dalam `application.properties`
- jadikan fail env server sebagai sumber kebenaran konfigurasi runtime
- gunakan account DB khusus aplikasi seperti `utilities_app`

### 8.4 Status Menu HSI Lain

Walaupun semua permintaan HSI utama sudah ada menu/route:

- hanya `PD103` dan `Data Diagnosis DAMA Bulanan` yang benar-benar siap digunakan
- menu lain masih placeholder untuk pembangunan seterusnya

## 9. Hasil Utama Setakat 07/04/2026

Hasil kerja utama yang telah siap:

- web system Angular + Spring Boot
- login, logout, dan session check
- route protection untuk pengguna login
- menu `HSI Reports`
- modul `PD103`
- edit dan save PD103 melalui browser
- audit trail perubahan PD103
- modul `Data Diagnosis DAMA Bulanan`
- export Excel untuk report DAMA
- `AI Assistant` dengan fallback apabila OpenAI tidak tersedia
- pakej release dan skrip deploy untuk Ubuntu staging

## 10. Next Step Dicadangkan

Cadangan practical seterusnya:

1. kekalkan dokumen deploy dan handover mengikut struktur `release\ubuntu-deploy`
2. sahkan credential server sebenar dalam `/etc/hsi-utilities/hsi-utilities-backend.env`
3. uji semula browser flow untuk `PD103`, `DAMA`, dan `Audit Log`
4. sahkan sama ada `AI Assistant` hendak diaktifkan penuh dengan `OPENAI_API_KEY`
5. tentukan keutamaan modul placeholder HSI yang hendak dibina seterusnya
