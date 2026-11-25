-- HSI Utilities: audit table for the "Pembetulan HIS" fix utilities
-- Target: MySQL 5.7, database his_kkm
--
-- Safe to run more than once (CREATE TABLE IF NOT EXISTS). The backend creates the same table
-- itself on first use; run this script when the app's DB account has no CREATE rights, or to
-- apply the change ahead of deployment.
--
-- Run:  mysql -h <host> -u <user> -p his_kkm < 2026-11-25_his_fix_audit.sql

USE his_kkm;

CREATE TABLE IF NOT EXISTS his_fix_audit (
  id BIGINT NOT NULL AUTO_INCREMENT,
  module VARCHAR(40) NOT NULL,
  ticket_no VARCHAR(50) NOT NULL,
  patient_mrn VARCHAR(50), encounter_id VARCHAR(50),
  target_table VARCHAR(64) NOT NULL, target_key VARCHAR(200) NOT NULL,
  before_json TEXT, after_json TEXT, reason VARCHAR(500),
  edited_by VARCHAR(50) NOT NULL, edited_name VARCHAR(120), edited_role VARCHAR(50),
  edited_at DATETIME NOT NULL,
  PRIMARY KEY (id),
  KEY idx_his_fix_audit_mrn (patient_mrn), KEY idx_his_fix_audit_encounter (encounter_id),
  KEY idx_his_fix_audit_ticket (ticket_no), KEY idx_his_fix_audit_module (module, edited_at),
  KEY idx_his_fix_audit_user (edited_by, edited_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
