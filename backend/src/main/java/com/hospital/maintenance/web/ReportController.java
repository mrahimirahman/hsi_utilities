package com.hospital.maintenance.web;

import com.hospital.maintenance.domain.SystemUser;
import com.hospital.maintenance.dto.DamaDiagnosisReportResponse;
import com.hospital.maintenance.dto.InpatientReportResponse;
import com.hospital.maintenance.dto.InpatientReportUpdateRequest;
import com.hospital.maintenance.dto.ReportAuditLogEntry;
import com.hospital.maintenance.dto.WardOption;
import com.hospital.maintenance.service.AuthService;
import com.hospital.maintenance.service.DamaDiagnosisReportService;
import com.hospital.maintenance.service.InpatientReportService;
import com.hospital.maintenance.service.LocationLookupService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final LocationLookupService locationLookupService;
    private final InpatientReportService inpatientReportService;
    private final DamaDiagnosisReportService damaDiagnosisReportService;

    public ReportController(LocationLookupService locationLookupService,
                            InpatientReportService inpatientReportService,
                            DamaDiagnosisReportService damaDiagnosisReportService) {
        this.locationLookupService = locationLookupService;
        this.inpatientReportService = inpatientReportService;
        this.damaDiagnosisReportService = damaDiagnosisReportService;
    }

    @GetMapping("/wards")
    public ResponseEntity<List<WardOption>> getWards(HttpSession session) {
        SystemUser user = (SystemUser) session.getAttribute(AuthService.SESSION_USER_KEY);
        if (user == null) {
            return new ResponseEntity<List<WardOption>>(HttpStatus.UNAUTHORIZED);
        }
        return ResponseEntity.ok(locationLookupService.getWardOptions());
    }

    @GetMapping("/audit")
    public ResponseEntity<List<ReportAuditLogEntry>> getReportAuditLog(@RequestParam(value = "wardCode", required = false) String wardCode,
                                                                       @RequestParam(value = "reportDate", required = false) String reportDate,
                                                                       HttpSession session) {
        SystemUser user = (SystemUser) session.getAttribute(AuthService.SESSION_USER_KEY);
        if (user == null) {
            return new ResponseEntity<List<ReportAuditLogEntry>>(HttpStatus.UNAUTHORIZED);
        }
        if (!"ADMIN".equalsIgnoreCase(user.getRole())) {
            return new ResponseEntity<List<ReportAuditLogEntry>>(HttpStatus.FORBIDDEN);
        }
        return ResponseEntity.ok(inpatientReportService.loadAuditLog(wardCode, reportDate));
    }

    @GetMapping("/inpatient")
    public ResponseEntity<InpatientReportResponse> getInpatientReport(@RequestParam("wardCode") String wardCode,
                                                                      @RequestParam("reportDate") String reportDate,
                                                                      HttpSession session) {
        SystemUser user = (SystemUser) session.getAttribute(AuthService.SESSION_USER_KEY);
        if (user == null) {
            return new ResponseEntity<InpatientReportResponse>(HttpStatus.UNAUTHORIZED);
        }
        return ResponseEntity.ok(inpatientReportService.generate(wardCode, reportDate));
    }

    @PostMapping("/inpatient")
    public ResponseEntity<?> saveInpatientReport(@RequestBody InpatientReportUpdateRequest request,
                                                 HttpSession session) {
        SystemUser user = (SystemUser) session.getAttribute(AuthService.SESSION_USER_KEY);
        if (user == null) {
            return new ResponseEntity<Object>(HttpStatus.UNAUTHORIZED);
        }
        if (!"ADMIN".equalsIgnoreCase(user.getRole())) {
            return new ResponseEntity<Object>(HttpStatus.FORBIDDEN);
        }
        try {
            return ResponseEntity.ok(inpatientReportService.save(request, user));
        } catch (IllegalArgumentException ex) {
            return new ResponseEntity<Object>(Collections.singletonMap("message", ex.getMessage()), HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/dama-diagnosis")
    public ResponseEntity<DamaDiagnosisReportResponse> getDamaDiagnosisReport(@RequestParam("fromDate") String fromDate,
                                                                              @RequestParam("toDate") String toDate,
                                                                              HttpSession session) {
        SystemUser user = (SystemUser) session.getAttribute(AuthService.SESSION_USER_KEY);
        if (user == null) {
            return new ResponseEntity<DamaDiagnosisReportResponse>(HttpStatus.UNAUTHORIZED);
        }
        try {
            return ResponseEntity.ok(damaDiagnosisReportService.loadReport(fromDate, toDate));
        } catch (IllegalArgumentException ex) {
            return new ResponseEntity<DamaDiagnosisReportResponse>(HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/dama-diagnosis/export")
    public ResponseEntity<byte[]> exportDamaDiagnosisReport(@RequestParam("fromDate") String fromDate,
                                                            @RequestParam("toDate") String toDate,
                                                            HttpSession session) {
        SystemUser user = (SystemUser) session.getAttribute(AuthService.SESSION_USER_KEY);
        if (user == null) {
            return new ResponseEntity<byte[]>(HttpStatus.UNAUTHORIZED);
        }

        String fileName = "data-diagnosis-dama-" + fromDate + "-to-" + toDate + ".xls";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.ms-excel;charset=UTF-8"));
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"");
        try {
            return new ResponseEntity<byte[]>(damaDiagnosisReportService.exportExcel(fromDate, toDate), headers, HttpStatus.OK);
        } catch (IllegalArgumentException ex) {
            return new ResponseEntity<byte[]>(HttpStatus.BAD_REQUEST);
        }
    }
}
