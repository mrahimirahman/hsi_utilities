package com.hospital.maintenance.web.hisfix;

import com.hospital.maintenance.domain.SystemUser;
import com.hospital.maintenance.dto.hisfix.RenumberSlotsRequest;
import com.hospital.maintenance.service.hisfix.RenumberSlotsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/his-fix/renumber-slots")
public class RenumberSlotsController extends HisFixControllerSupport {

    private final RenumberSlotsService service;

    public RenumberSlotsController(RenumberSlotsService service) {
        this.service = service;
    }

    @GetMapping("/schedules")
    public ResponseEntity<?> schedules(HttpSession session) {
        return handle(session, new HisFixAction() {
            @Override
            public Object run(SystemUser user) {
                return service.schedules();
            }
        });
    }

    @GetMapping("/resources")
    public ResponseEntity<?> resources(@RequestParam("scheduleCode") final String scheduleCode, HttpSession session) {
        return handle(session, new HisFixAction() {
            @Override
            public Object run(SystemUser user) {
                return service.resources(scheduleCode);
            }
        });
    }

    @GetMapping("/search")
    public ResponseEntity<?> search(@RequestParam("resourceCode") final String resourceCode,
                                    @RequestParam("date") final String date,
                                    @RequestParam("scheduleCode") final String scheduleCode,
                                    HttpSession session) {
        return handle(session, new HisFixAction() {
            @Override
            public Object run(SystemUser user) {
                return service.search(resourceCode, date, scheduleCode);
            }
        });
    }

    @PostMapping("/apply")
    public ResponseEntity<?> apply(@RequestBody final RenumberSlotsRequest request, HttpSession session) {
        return handle(session, new HisFixAction() {
            @Override
            public Object run(SystemUser user) {
                return service.apply(request, user);
            }
        });
    }

    @GetMapping("/audit")
    public ResponseEntity<?> audit(@RequestParam(value = "resource", required = false) final String resource,
                                   HttpSession session) {
        return handle(session, new HisFixAction() {
            @Override
            public Object run(SystemUser user) {
                return service.audit(resource);
            }
        });
    }
}
