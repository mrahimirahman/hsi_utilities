package com.hospital.maintenance.web;

import com.hospital.maintenance.domain.SystemUser;
import com.hospital.maintenance.dto.DischargeDateUpdateRequest;
import com.hospital.maintenance.service.AuthService;
import com.hospital.maintenance.service.DischargeDateService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;
import java.util.Collections;

@RestController
@RequestMapping("/api/discharge-date")
public class DischargeDateController {

    private final DischargeDateService dischargeDateService;

    public DischargeDateController(DischargeDateService dischargeDateService) {
        this.dischargeDateService = dischargeDateService;
    }

    @GetMapping("/encounters")
    public ResponseEntity<?> search(@RequestParam("mrn") String mrn,
                                    @RequestParam(value = "mode", required = false) String mode,
                                    HttpSession session) {
        if (session.getAttribute(AuthService.SESSION_USER_KEY) == null) {
            return new ResponseEntity<Object>(HttpStatus.UNAUTHORIZED);
        }
        try {
            return ResponseEntity.ok(dischargeDateService.search(mrn, mode));
        } catch (IllegalArgumentException ex) {
            return badRequest(ex);
        }
    }

    @PostMapping("/encounters")
    public ResponseEntity<?> update(@RequestBody DischargeDateUpdateRequest request, HttpSession session) {
        SystemUser user = (SystemUser) session.getAttribute(AuthService.SESSION_USER_KEY);
        if (user == null) {
            return new ResponseEntity<Object>(HttpStatus.UNAUTHORIZED);
        }
        try {
            return ResponseEntity.ok(dischargeDateService.update(request, user));
        } catch (IllegalArgumentException ex) {
            return badRequest(ex);
        }
    }

    @GetMapping("/audit")
    public ResponseEntity<?> getAuditLog(@RequestParam(value = "encounterId", required = false) String encounterId,
                                         @RequestParam(value = "mrn", required = false) String mrn,
                                         HttpSession session) {
        if (session.getAttribute(AuthService.SESSION_USER_KEY) == null) {
            return new ResponseEntity<Object>(HttpStatus.UNAUTHORIZED);
        }
        try {
            return ResponseEntity.ok(dischargeDateService.loadAuditLog(encounterId, mrn));
        } catch (IllegalArgumentException ex) {
            return badRequest(ex);
        }
    }

    private ResponseEntity<?> badRequest(IllegalArgumentException ex) {
        return new ResponseEntity<Object>(Collections.singletonMap("message", ex.getMessage()), HttpStatus.BAD_REQUEST);
    }
}
