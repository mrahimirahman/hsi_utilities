# Backlog Permintaan HSI

Tarikh rujukan: 06/04/2026

Dokumen ini merumuskan senarai menu atau task baharu daripada pihak HSI berdasarkan makluman semasa.

## Status Semasa

- `No. 9 Update PD103 Bancian Harian Wad` telah siap pada 05/04/2026.
- Modul yang benar-benar wujud dalam sistem setakat ini ialah `PD103`, `Audit Log`, `Login`, `Dashboard`, dan `Utilities Requests`.
- Item `No. 1` hingga `No. 8` masih belum dikenal pasti sebagai menu atau report yang telah dibina dalam kod semasa.

## Senarai Permintaan

| No. | Permintaan HSI | Cadangan Jenis Modul | Status |
| --- | --- | --- | --- |
| 1 | Data Lean | Report / listing | Belum buat |
| 2 | Data Admission (ETD) Bulanan | Report bulanan | Belum buat |
| 3 | Data Diagnosis Pesakit Yang Discaj Jenis "Discharge Against Medical Advise" (DAMA) Bulanan | Report bulanan | Belum buat |
| 4 | Data Bilangan Referral Dari Jabatan Kecemasan Ke Jabatan Psikiatri (Tahunan) | Report tahunan | Belum buat |
| 5 | Data Bilangan Referral Dari Jabatan Lain (Medical, Surgical, Ortho, O&G, Paeds) Ke Jabatan Psikiatri (Tahunan) | Report tahunan | Belum buat |
| 6 | Data semua userID yang masih aktif sehingga kini | Listing pengguna aktif | Belum buat |
| 7 | Data Pesakit Yang Dr tidak submit Discharge Summary di dalam sistem selepas Discharge Summary dibatalkan dan menghantar yang baharu | Report / exception listing | Belum buat |
| 8 | Data Kes Rujukan dari Jabatan Medical, Surgical, O&G, Paed dan Ortho ke Jabatan Psikiatri dan pecahan bagi 2 kes (Kes Baharu dan Kes Ulangan) (Tahunan) | Report tahunan | Belum buat |
| 9 | Update PD103 Bancian Harian Wad | Report update / maintenance | Siap 05/04/2026 |

## Nota Pelaksanaan

- Perlu sahkan untuk setiap item sama ada ia akan dibina sebagai `menu berasingan`, `submenu di bawah Reports`, atau `export sahaja`.
- Perlu sahkan jadual database, filter tarikh, dan format output bagi item `1` hingga `8`.
- Item `3` berkait rapat dengan kod data `DAMA`, tetapi modul semasa hanya meliputi row `DAMA` dalam report `PD103`, bukan listing bulanan diagnosis pesakit.
- Item `6` berkemungkinan boleh dibina paling cepat jika data sumber `userID aktif` telah jelas.

## Cadangan Susunan Kerja

1. Item 6 `listing userID aktif`
2. Item 2 `Admission ETD bulanan`
3. Item 3 `diagnosis DAMA bulanan`
4. Item 4 dan 5 `referral tahunan`
5. Item 8 `referral tahunan dengan pecahan kes baharu dan ulangan`
6. Item 1 dan 7 selepas struktur data disahkan
