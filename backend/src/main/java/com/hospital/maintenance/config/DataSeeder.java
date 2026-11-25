package com.hospital.maintenance.config;

import com.hospital.maintenance.domain.MaintenanceRequest;
import com.hospital.maintenance.domain.SystemUser;
import com.hospital.maintenance.repository.MaintenanceRequestRepository;
import com.hospital.maintenance.repository.SystemUserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class DataSeeder implements CommandLineRunner {

    private final SystemUserRepository systemUserRepository;
    private final MaintenanceRequestRepository maintenanceRequestRepository;

    public DataSeeder(SystemUserRepository systemUserRepository,
                      MaintenanceRequestRepository maintenanceRequestRepository) {
        this.systemUserRepository = systemUserRepository;
        this.maintenanceRequestRepository = maintenanceRequestRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        seedAdminUser();
        seedSampleRequests();
    }

    private void seedAdminUser() {
        if (systemUserRepository.findByUsername("admin") == null) {
            SystemUser user = new SystemUser();
            user.setUsername("admin");
            user.setPassword("admin123");
            user.setFullName("Hospital Admin");
            user.setRole("ADMIN");
            user.setDepartment("Facility Management");
            systemUserRepository.save(user);
        }
    }

    private void seedSampleRequests() {
        if (maintenanceRequestRepository.count() > 0) {
            return;
        }

        maintenanceRequestRepository.save(new MaintenanceRequest(
                "ICU Monitor Calibration",
                "ICU Ward",
                "OPEN",
                "HIGH",
                "Monitor calibration is overdue.",
                "Biomedical Team",
                new Date()
        ));

        maintenanceRequestRepository.save(new MaintenanceRequest(
                "Aircond Leak at OT Room 2",
                "Operation Theatre",
                "IN_PROGRESS",
                "CRITICAL",
                "Cooling issue affecting room temperature stability.",
                "Engineering Team",
                new Date()
        ));

        maintenanceRequestRepository.save(new MaintenanceRequest(
                "Lift Service Inspection",
                "Main Building",
                "COMPLETED",
                "MEDIUM",
                "Monthly scheduled lift inspection.",
                "Vendor Support",
                new Date()
        ));
    }
}
