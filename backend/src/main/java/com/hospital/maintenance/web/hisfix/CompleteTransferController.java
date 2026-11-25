package com.hospital.maintenance.web.hisfix;

import com.hospital.maintenance.domain.SystemUser;
import com.hospital.maintenance.dto.hisfix.CompleteTransferRequest;
import com.hospital.maintenance.service.hisfix.CompleteTransferService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/his-fix/complete-transfer")
public class CompleteTransferController extends HisFixControllerSupport {

    private final CompleteTransferService service;

    public CompleteTransferController(CompleteTransferService service) {
        this.service = service;
    }

    @GetMapping("/search")
    public ResponseEntity<?> search(@RequestParam("mrn") final String mrn, HttpSession session) {
        return handle(session, new HisFixAction() {
            @Override
            public Object run(SystemUser user) {
                return service.search(mrn);
            }
        });
    }

    @PostMapping("/apply")
    public ResponseEntity<?> apply(@RequestBody final CompleteTransferRequest request, HttpSession session) {
        return handle(session, new HisFixAction() {
            @Override
            public Object run(SystemUser user) {
                return service.apply(request, user);
            }
        });
    }

    @GetMapping("/audit")
    public ResponseEntity<?> audit(@RequestParam("mrn") final String mrn, HttpSession session) {
        return handle(session, new HisFixAction() {
            @Override
            public Object run(SystemUser user) {
                return service.audit(mrn);
            }
        });
    }
}
