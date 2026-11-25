-- HSI Utilities: audit tables for Update Discharge Time / Update Death Date / PD103 Ticket No
-- Target: MySQL 5.7, database his_kkm
--
-- Safe to run more than once. Works on a fresh database and on one where the app already
-- created the tables (it only adds what is missing). The backend makes the same changes
-- itself on first use; run this script when the app's DB account has no CREATE/ALTER
-- rights, or to apply the change ahead of deployment.
--
-- Run:  mysql -h <host> -u <user> -p his_kkm < 2026-11-25_discharge_death_ticket.sql

USE his_kkm;

-- 1. Audit table for Update Discharge Time / Update Death Date
CREATE TABLE IF NOT EXISTS discharge_date_audit (
    id BIGINT NOT NULL AUTO_INCREMENT,
    module VARCHAR(20),
    ticket_no VARCHAR(50),
    encounter_id VARCHAR(50) NOT NULL,
    admission_id VARCHAR(50),
    patient_mrn VARCHAR(50),
    encounter_discharge_date_before VARCHAR(30),
    encounter_discharge_date_after VARCHAR(30),
    enc_discharge_date_before VARCHAR(30),
    enc_discharge_date_after VARCHAR(30),
    pre_discharge_date_before VARCHAR(30),
    pre_discharge_date_after VARCHAR(30),
    pre_discharge_time_before VARCHAR(20),
    pre_discharge_time_after VARCHAR(20),
    death_date_before VARCHAR(30),
    death_date_after VARCHAR(30),
    reason VARCHAR(500),
    edited_by VARCHAR(50) NOT NULL,
    edited_name VARCHAR(120),
    edited_role VARCHAR(50),
    edited_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    KEY idx_discharge_date_audit_encounter (encounter_id),
    KEY idx_discharge_date_audit_mrn (patient_mrn),
    KEY idx_discharge_date_audit_user (edited_by, edited_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- Columns added after the first release of discharge_date_audit (order matters: module before ticket_no)
SET @sql = (SELECT IF(COUNT(*) = 0,
        'ALTER TABLE discharge_date_audit ADD COLUMN module VARCHAR(20) AFTER id', 'SELECT 1')
    FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'discharge_date_audit' AND column_name = 'module');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(COUNT(*) = 0,
        'ALTER TABLE discharge_date_audit ADD COLUMN ticket_no VARCHAR(50) AFTER module', 'SELECT 1')
    FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'discharge_date_audit' AND column_name = 'ticket_no');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(COUNT(*) = 0,
        'ALTER TABLE discharge_date_audit ADD COLUMN death_date_before VARCHAR(30) AFTER pre_discharge_time_after', 'SELECT 1')
    FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'discharge_date_audit' AND column_name = 'death_date_before');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(COUNT(*) = 0,
        'ALTER TABLE discharge_date_audit ADD COLUMN death_date_after VARCHAR(30) AFTER death_date_before', 'SELECT 1')
    FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'discharge_date_audit' AND column_name = 'death_date_after');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Rows saved before the module column existed all came from Update Discharge Time
UPDATE discharge_date_audit SET module = 'DISCHARGE' WHERE module IS NULL;

-- 2. PD103 report edit audit: Ticket No
CREATE TABLE IF NOT EXISTS report_edit_audit (
    id BIGINT NOT NULL AUTO_INCREMENT,
    report_code VARCHAR(20) NOT NULL,
    ward_code VARCHAR(20) NOT NULL,
    report_date DATE NOT NULL,
    discipline_code VARCHAR(20) NOT NULL,
    metric_index INT NOT NULL,
    previous_male INT NOT NULL,
    previous_female INT NOT NULL,
    new_male INT NOT NULL,
    new_female INT NOT NULL,
    ticket_no VARCHAR(50),
    edited_by VARCHAR(50) NOT NULL,
    edited_name VARCHAR(120),
    edited_role VARCHAR(50),
    edited_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    KEY idx_report_edit_audit_lookup (report_code, ward_code, report_date),
    KEY idx_report_edit_audit_user (edited_by, edited_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

SET @sql = (SELECT IF(COUNT(*) = 0,
        'ALTER TABLE report_edit_audit ADD COLUMN ticket_no VARCHAR(50) AFTER new_female', 'SELECT 1')
    FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'report_edit_audit' AND column_name = 'ticket_no');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 3. Check: expect module, ticket_no, death_date_before, death_date_after, and ticket_no
SELECT table_name, column_name, column_type
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND ((table_name = 'discharge_date_audit' AND column_name IN ('module', 'ticket_no', 'death_date_before', 'death_date_after'))
    OR (table_name = 'report_edit_audit' AND column_name = 'ticket_no'))
ORDER BY table_name, column_name;
