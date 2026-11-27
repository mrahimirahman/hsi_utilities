package com.hospital.maintenance.web.hisfix;

import com.hospital.maintenance.domain.SystemUser;
import com.hospital.maintenance.service.hisfix.HisFixSupport;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;
import java.util.Collections;

/** Tells the HIS fix screens whether writes are rolled back (dry run), so the badge shows from page load. */
@RestController
@RequestMapping("/api/his-fix")
public class HisFixModeController extends HisFixControllerSupport {

    private final HisFixSupport support;

    public HisFixModeController(HisFixSupport support) {
        this.support = support;
    }

    @GetMapping("/mode")
    public ResponseEntity<?> mode(HttpSession session) {
        return handle(session, new HisFixAction() {
            @Override
            public Object run(SystemUser user) {
                return Collections.singletonMap("dryRun", Boolean.valueOf(support.isDryRun()));
            }
        });
    }
}
