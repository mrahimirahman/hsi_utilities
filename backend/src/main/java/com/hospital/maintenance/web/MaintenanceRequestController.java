package com.hospital.maintenance.web;

import com.hospital.maintenance.domain.MaintenanceRequest;
import com.hospital.maintenance.domain.SystemUser;
import com.hospital.maintenance.repository.MaintenanceRequestRepository;
import com.hospital.maintenance.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;
import java.util.List;

@RestController
@RequestMapping("/api/requests")
public class MaintenanceRequestController {

    private final MaintenanceRequestRepository maintenanceRequestRepository;

    public MaintenanceRequestController(MaintenanceRequestRepository maintenanceRequestRepository) {
        this.maintenanceRequestRepository = maintenanceRequestRepository;
    }

    @GetMapping
    public ResponseEntity<List<MaintenanceRequest>> getAll(HttpSession session) {
        SystemUser user = (SystemUser) session.getAttribute(AuthService.SESSION_USER_KEY);
        if (user == null) {
            return new ResponseEntity<List<MaintenanceRequest>>(HttpStatus.UNAUTHORIZED);
        }
        return ResponseEntity.ok(maintenanceRequestRepository.findAll());
    }
}
