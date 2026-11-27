package com.hospital.maintenance.web.hisfix;

import com.hospital.maintenance.domain.SystemUser;
import com.hospital.maintenance.service.hisfix.HisFixAccessException;
import com.hospital.maintenance.service.hisfix.HisFixSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import javax.servlet.http.HttpSession;
import java.util.Collections;

public abstract class HisFixControllerSupport {

    private static final Logger log = LoggerFactory.getLogger(HisFixControllerSupport.class);

    protected interface HisFixAction {
        Object run(SystemUser user);
    }

    protected ResponseEntity<?> handle(HttpSession session, HisFixAction action) {
        try {
            SystemUser user = HisFixSupport.requireAdmin(session);
            return ResponseEntity.ok(action.run(user));
        } catch (HisFixAccessException ex) {
            return new ResponseEntity<Object>(Collections.singletonMap("message", ex.getMessage()),
                    HttpStatus.valueOf(ex.getStatus()));
        } catch (IllegalArgumentException ex) {
            return new ResponseEntity<Object>(Collections.singletonMap("message", ex.getMessage()), HttpStatus.BAD_REQUEST);
        } catch (DataAccessException ex) {
            log.error("HIS fix database error", ex);
            return new ResponseEntity<Object>(Collections.singletonMap("message",
                    "Ralat pangkalan data, sila hubungi pentadbir."), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
