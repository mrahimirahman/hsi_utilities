# POST-DEPLOY Test Checklist

Tarikh kemas kini: 06/04/2026

Dokumen ini digunakan selepas deploy staging selesai untuk semakan fungsi asas melalui browser dan semakan endpoint minimum.

## A. Semakan Service Asas

1. Backend service hidup.

```bash
sudo systemctl status hsi-utilities-backend --no-pager
```

2. Nginx hidup dan config valid.

```bash
sudo systemctl status nginx --no-pager
sudo nginx -t
```

3. Endpoint local server boleh dicapai.

```bash
curl http://127.0.0.1:8080/api/auth/session
curl http://127.0.0.1:8081
```

## B. Semakan Login

1. Buka URL staging.

- `http://10.29.59.55:8081/`

2. Pastikan login page keluar tanpa broken image atau layout rosak.

3. Test login dengan akaun sah.

Perlu semak:
- boleh login
- session diwujudkan
- selepas refresh page, session masih kekal jika backend hidup
- logout berfungsi

## C. Semakan Menu

Selepas login, semak menu sidebar berikut dipaparkan dengan betul:

- `Dashboard`
- `HSI Reports`
- `Audit Log` untuk `ADMIN`

Perlu semak:
- tiada menu lama yang sepatutnya disorok muncul semula
- navigation ke setiap menu berjaya

## D. Semakan HSI Reports

1. Buka `HSI Reports`.

2. Pastikan senarai report dipaparkan.

3. Semak status item:

- `Update PD103 Bancian Harian Wad` = `Ready`
- `Data Diagnosis DAMA Bulanan` = `Ready`
- item lain boleh kekal `Pending`

## E. Semakan DAMA Report

1. Buka `Data Diagnosis DAMA Bulanan`.

2. Test tempoh berikut:

- `From Date = 2026-03-01`
- `To Date = 2026-03-31`

3. Klik `Generate`.

Perlu semak:
- jumlah rekod ikut jangkaan semasa
- preview table dipaparkan
- tiada error API

4. Klik `Generate Excel`.

Perlu semak:
- fail berjaya dimuat turun
- nama fail ikut format `data-diagnosis-dama-YYYY-MM-DD-to-YYYY-MM-DD.xls`
- kandungan fail boleh dibuka dalam Excel
- jumlah row dalam Excel ikut hasil manual semasa

## F. Semakan PD103

1. Buka `Update PD103 Bancian Harian Wad`.

2. Pilih ward dan tarikh yang ada data.

3. Klik `Generate`.

Perlu semak:
- report dipaparkan
- layout preview tidak pecah
- print preview masih boleh digunakan

4. Jika login sebagai `ADMIN`, test edit ringkas.

Perlu semak:
- button `Edit` muncul
- save berjaya
- data dikemaskini
- mesej success muncul

## G. Semakan Audit Log

1. Buka `Audit Log` sebagai `ADMIN`.

2. Pastikan senarai audit boleh dipaparkan.

3. Jika ada edit PD103 baru dibuat, semak entry audit wujud.

4. Klik `Open Report` dari audit log.

Perlu semak:
- terus buka modul PD103
- ward dan tarikh report dibawa sekali

## H. Semakan AI Assistant

1. Login sebagai `ADMIN` dan pastikan butang `AI Assistant` muncul selepas login.

2. Klik butang tersebut.

Perlu semak:
- panel chat terbuka
- layout page belakang tidak pecah
- boleh hantar mesej

3. Jika `OPENAI_API_KEY` telah diisi pada backend env, test beberapa soalan contoh:

- `Report DAMA ni untuk apa?`
- `Macam mana nak deploy staging?`
- `PD103 tu report apa?`

Perlu semak:
- AI menjawab
- jawapan masuk dalam panel chat
- tiada error API pada browser console yang obvious

4. Login sebagai user bukan `ADMIN` dan pastikan butang `AI Assistant` tidak dipaparkan.`r`n`r`n5. Jika `OPENAI_API_KEY` belum diisi, pastikan assistant bagi mesej konfigurasi yang jelas dan aplikasi tidak rosak.

## I. Semakan Visual Ringkas

Perlu semak:
- background login guna `BG1.jpg`
- layout responsive masih okay pada browser biasa
- button dan spacing tidak rosak
- tiada text overlap pada page DAMA report
- panel AI tidak menutup elemen penting secara pelik pada desktop atau mobile

## J. Jika Ada Isu

Catat minimum maklumat berikut:

- tarikh dan masa ujian
- URL yang diuji
- user ID yang digunakan
- langkah ringkas untuk reproduce
- screenshot error
- jika melibatkan report, catat `From Date` dan `To Date`
- jika melibatkan AI, catat prompt yang dihantar

## Dokumen Berkaitan

- `C:\trae_ai\hsi_utilities\docs\STAGING_DEPLOY_CHECKLIST_2026-04-06.md`
- `C:\trae_ai\hsi_utilities\release\ubuntu-deploy\README.md`

