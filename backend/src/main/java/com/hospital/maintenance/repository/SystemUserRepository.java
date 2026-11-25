package com.hospital.maintenance.repository;

import com.hospital.maintenance.domain.SystemUser;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SystemUserRepository extends JpaRepository<SystemUser, Long> {

    SystemUser findByUsername(String username);
}
