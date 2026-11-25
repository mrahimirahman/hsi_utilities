package com.hospital.maintenance.service;

import com.hospital.maintenance.domain.SystemUser;
import org.springframework.stereotype.Service;

@Service
public class LocalAssistantFallbackService {

    public String reply(String message, SystemUser user, String reason) {
        String lower = message == null ? "" : message.toLowerCase();
        StringBuilder reply = new StringBuilder();

        if (reason != null && !reason.trim().isEmpty()) {
            reply.append(reason).append("\n\n");
        }

        if (containsAny(lower, "dama", "diagnosis dama")) {
            reply.append("Report `Data Diagnosis DAMA Bulanan` digunakan untuk semakan kes pesakit yang discaj dengan jenis `Discharge Against Medical Advice (DAMA)` mengikut tempoh tarikh yang dipilih. ");
            reply.append("Dalam sistem sekarang, user boleh pilih `From Date` dan `To Date`, klik `Generate` untuk preview ringkas, dan klik `Generate Excel` untuk muat turun fail `.xls`. ");
            reply.append("Data source semasa sistem adalah database `his_kkm` pada host yang dikonfigurasi dalam backend env atau `application.properties`.");
            return reply.toString();
        }

        if (containsAny(lower, "pd103", "bancian harian wad")) {
            reply.append("`PD103` ialah modul `Update PD103 Bancian Harian Wad`. ");
            reply.append("Ia menyokong generate report ikut ward dan tarikh, print preview, dan edit data tertentu oleh pengguna role `ADMIN`. ");
            reply.append("Setiap perubahan juga direkodkan ke `Audit Log`.");
            return reply.toString();
        }

        if (containsAny(lower, "deploy", "staging", "server", "nginx", "systemd")) {
            reply.append("Untuk staging, artefak utama yang perlu dibawa ialah backend jar, frontend zip, dan env backend. ");
            reply.append("Path server yang digunakan dalam dokumen semasa ialah `/opt/hsi-utilities` untuk backend, `/var/www/hsi-utilities` untuk frontend, dan `/etc/hsi-utilities/hsi-utilities-backend.env` untuk env. ");
            reply.append("Rujuk checklist deploy staging dan post-deploy test dalam folder `docs` untuk langkah penuh.");
            return reply.toString();
        }

        if (containsAny(lower, "audit", "log")) {
            reply.append("`Audit Log` digunakan oleh `ADMIN` untuk semak perubahan pada report PD103. ");
            reply.append("Ia menyimpan siapa yang edit, masa edit, field yang berubah, dan boleh terus buka report berkaitan dari log tersebut.");
            return reply.toString();
        }

        if (containsAny(lower, "report", "menu", "hsi reports", "laporan", "modul")) {
            reply.append("Dalam `HSI Reports`, modul yang sudah aktif setakat ini ialah `Update PD103 Bancian Harian Wad` dan `Data Diagnosis DAMA Bulanan`. ");
            reply.append("Item lain masih placeholder atau belum dibina sepenuhnya.");
            return reply.toString();
        }

        reply.append("Saya masih boleh bantu secara fallback untuk topik asas sistem ini seperti `PD103`, `DAMA report`, `Audit Log`, dan `deploy staging`. ");
        reply.append("Cuba tanya dengan lebih spesifik, contohnya `Report DAMA ni untuk apa?` atau `Macam mana nak deploy staging?`.");
        if (user != null && user.getRole() != null) {
            reply.append(" Role anda sekarang: ").append(user.getRole()).append(".");
        }
        return reply.toString();
    }

    private boolean containsAny(String value, String... patterns) {
        if (value == null || patterns == null) {
            return false;
        }
        for (int i = 0; i < patterns.length; i++) {
            if (patterns[i] != null && value.contains(patterns[i])) {
                return true;
            }
        }
        return false;
    }
}
