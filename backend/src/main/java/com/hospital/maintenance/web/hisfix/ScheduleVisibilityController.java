package com.hospital.maintenance.web.hisfix;

import com.hospital.maintenance.domain.SystemUser;
import com.hospital.maintenance.dto.hisfix.ScheduleVisibilityRequest;
import com.hospital.maintenance.service.hisfix.ScheduleVisibilityService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/his-fix/schedule-visibility")
public class ScheduleVisibilityController extends HisFixControllerSupport {

    private final ScheduleVisibilityService service;

    public ScheduleVisibilityController(ScheduleVisibilityService service) {
        this.service = service;
    }

    @GetMapping("/search")
    public ResponseEntity<?> search(@RequestParam(value = "mrn", required = false) final String mrn,
                                    @RequestParam(value = "name", required = false) final String name,
                                    HttpSession session) {
        return handle(session, new HisFixAction() {
            @Override
            public Object run(SystemUser user) {
                boolean hasMrn = mrn != null && !mrn.trim().isEmpty();
                boolean hasName = name != null && !name.trim().isEmpty();
                if (hasMrn == hasName) {
                    throw new IllegalArgumentException("Sila masukkan MRN atau nama jadual (satu sahaja).");
                }
                return hasMrn ? service.searchByMrn(mrn) : service.searchByName(name);
            }
        });
    }

    @GetMapping("/opened-today")
    public ResponseEntity<?> openedToday(HttpSession session) {
        return handle(session, new HisFixAction() {
            @Override
            public Object run(SystemUser user) {
                return service.openedToday();
            }
        });
    }

    @PostMapping("/apply")
    public ResponseEntity<?> apply(@RequestBody final ScheduleVisibilityRequest request, HttpSession session) {
        return handle(session, new HisFixAction() {
            @Override
            public Object run(SystemUser user) {
                return service.apply(request, user);
            }
        });
    }

    @PostMapping("/rehide-all")
    public ResponseEntity<?> rehideAll(@RequestBody final ScheduleVisibilityRequest request, HttpSession session) {
        return handle(session, new HisFixAction() {
            @Override
            public Object run(SystemUser user) {
                return service.rehideAll(request, user);
            }
        });
    }

    @GetMapping("/audit")
    public ResponseEntity<?> audit(@RequestParam(value = "mrn", required = false) final String mrn,
                                   @RequestParam(value = "scheduleCode", required = false) final String scheduleCode,
                                   HttpSession session) {
        return handle(session, new HisFixAction() {
            @Override
            public Object run(SystemUser user) {
                return service.audit(mrn, scheduleCode);
            }
        });
    }
}
