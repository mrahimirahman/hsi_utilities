package com.hospital.maintenance.web;

import com.hospital.maintenance.domain.SystemUser;
import com.hospital.maintenance.dto.DashboardResponse;
import com.hospital.maintenance.service.AuthService;
import com.hospital.maintenance.service.DashboardService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    public ResponseEntity<DashboardResponse> getDashboard(HttpSession session) {
        SystemUser user = (SystemUser) session.getAttribute(AuthService.SESSION_USER_KEY);
        if (user == null) {
            return new ResponseEntity<DashboardResponse>(HttpStatus.UNAUTHORIZED);
        }
        return ResponseEntity.ok(dashboardService.getDashboard());
    }
}
