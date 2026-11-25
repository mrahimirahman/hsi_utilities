package com.hospital.maintenance.repository;

import com.hospital.maintenance.domain.MaintenanceRequest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MaintenanceRequestRepository extends JpaRepository<MaintenanceRequest, Long> {

    long countByStatus(String status);
}
