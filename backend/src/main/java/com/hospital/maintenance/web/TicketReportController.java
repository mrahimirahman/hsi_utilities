package com.hospital.maintenance.web;

import com.hospital.maintenance.domain.SystemUser;
import com.hospital.maintenance.service.AuthService;
import com.hospital.maintenance.service.TicketReportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;
import java.util.Collections;

// Laporan Tiket is ADMIN-only, like the Audit Log: it shows who changed which patient record.
@RestController
@RequestMapping("/api/tickets")
public class TicketReportController {

    private final TicketReportService ticketReportService;

    public TicketReportController(TicketReportService ticketReportService) {
        this.ticketReportService = ticketReportService;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<?> getDashboard(@RequestParam("fromDate") String fromDate,
                                          @RequestParam("toDate") String toDate,
                                          HttpSession session) {
        ResponseEntity<?> denied = checkAdmin(session);
        if (denied != null) {
            return denied;
        }
        try {
            return ResponseEntity.ok(ticketReportService.loadDashboard(fromDate, toDate));
        } catch (IllegalArgumentException ex) {
            return badRequest(ex);
        }
    }

    @GetMapping("/report")
    public ResponseEntity<?> getReport(@RequestParam("fromDate") String fromDate,
                                       @RequestParam("toDate") String toDate,
                                       HttpSession session) {
        ResponseEntity<?> denied = checkAdmin(session);
        if (denied != null) {
            return denied;
        }
        try {
            return ResponseEntity.ok(ticketReportService.loadReport(fromDate, toDate));
        } catch (IllegalArgumentException ex) {
            return badRequest(ex);
        }
    }

    @GetMapping("/report/export")
    public ResponseEntity<?> exportReport(@RequestParam("fromDate") String fromDate,
                                          @RequestParam("toDate") String toDate,
                                          HttpSession session) {
        ResponseEntity<?> denied = checkAdmin(session);
        if (denied != null) {
            return denied;
        }
        byte[] content;
        try {
            content = ticketReportService.exportExcel(fromDate, toDate);
        } catch (IllegalArgumentException ex) {
            return badRequest(ex);
        }
        // dates are validated as yyyy-MM-dd by the service, so they are safe in the header
        String fileName = "laporan-tiket-" + fromDate.trim() + "-to-" + toDate.trim() + ".xls";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.ms-excel;charset=UTF-8"));
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"");
        return new ResponseEntity<byte[]>(content, headers, HttpStatus.OK);
    }

    private ResponseEntity<?> checkAdmin(HttpSession session) {
        SystemUser user = (SystemUser) session.getAttribute(AuthService.SESSION_USER_KEY);
        if (user == null) {
            return new ResponseEntity<Object>(HttpStatus.UNAUTHORIZED);
        }
        if (!"ADMIN".equalsIgnoreCase(user.getRole())) {
            return new ResponseEntity<Object>(HttpStatus.FORBIDDEN);
        }
        return null;
    }

    private ResponseEntity<?> badRequest(IllegalArgumentException ex) {
        return new ResponseEntity<Object>(Collections.singletonMap("message", ex.getMessage()), HttpStatus.BAD_REQUEST);
    }
}
