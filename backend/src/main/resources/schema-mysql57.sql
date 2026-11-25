CREATE DATABASE IF NOT EXISTS hospital_maintenance;
USE hospital_maintenance;

CREATE TABLE IF NOT EXISTS system_users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(120) NOT NULL,
    full_name VARCHAR(120) NOT NULL,
    role VARCHAR(50) NOT NULL,
    department VARCHAR(80),
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS maintenance_requests (
    id BIGINT NOT NULL AUTO_INCREMENT,
    title VARCHAR(150) NOT NULL,
    location VARCHAR(100) NOT NULL,
    status VARCHAR(30) NOT NULL,
    priority VARCHAR(30) NOT NULL,
    description VARCHAR(500) NOT NULL,
    assigned_team VARCHAR(120),
    request_date DATETIME NOT NULL,
    PRIMARY KEY (id)
);

INSERT INTO system_users (username, password, full_name, role, department)
SELECT 'admin', 'admin123', 'Hospital Admin', 'ADMIN', 'Facility Management'
WHERE NOT EXISTS (
    SELECT 1 FROM system_users WHERE username = 'admin'
);

INSERT INTO maintenance_requests (title, location, status, priority, description, assigned_team, request_date)
SELECT 'ICU Monitor Calibration', 'ICU Ward', 'OPEN', 'HIGH', 'Monitor calibration is overdue.', 'Biomedical Team', NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM maintenance_requests WHERE title = 'ICU Monitor Calibration'
);

INSERT INTO maintenance_requests (title, location, status, priority, description, assigned_team, request_date)
SELECT 'Aircond Leak at OT Room 2', 'Operation Theatre', 'IN_PROGRESS', 'CRITICAL', 'Cooling issue affecting room temperature stability.', 'Engineering Team', NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM maintenance_requests WHERE title = 'Aircond Leak at OT Room 2'
);

INSERT INTO maintenance_requests (title, location, status, priority, description, assigned_team, request_date)
SELECT 'Lift Service Inspection', 'Main Building', 'COMPLETED', 'MEDIUM', 'Monthly scheduled lift inspection.', 'Vendor Support', NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM maintenance_requests WHERE title = 'Lift Service Inspection'
);
