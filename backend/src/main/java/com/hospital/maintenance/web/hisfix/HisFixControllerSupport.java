package com.hospital.maintenance.web.hisfix;

import com.hospital.maintenance.domain.SystemUser;
import com.hospital.maintenance.service.hisfix.HisFixAccessException;
import com.hospital.maintenance.service.hisfix.HisFixSupport;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import javax.servlet.http.HttpSession;
import java.util.Collections;

public abstract class HisFixControllerSupport {

    protected interface HisFixAction {
        Object run(SystemUser user);
    }

    protected ResponseEntity<?> handle(HttpSession session, HisFixAction action) {
        try {
            SystemUser user = HisFixSupport.requireAdmin(session);
            return ResponseEntity.ok(action.run(user));
        } catch (HisFixAccessException ex) {
            return new ResponseEntity<Object>(HttpStatus.valueOf(ex.getStatus()));
        } catch (IllegalArgumentException ex) {
            return new ResponseEntity<Object>(Collections.singletonMap("message", ex.getMessage()), HttpStatus.BAD_REQUEST);
        }
    }
}
