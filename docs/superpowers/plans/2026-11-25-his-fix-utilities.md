# HIS@KKM Fix Utilities Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Nine ADMIN-only screens in HSI Utilities, each with its own menu item, that apply the recurring HIS@KKM back-end fixes with a Ticket No and a before/after audit row.

**Architecture:** A shared backend base (`HisFixSupport`: admin guard, ticket/MRN/time validation, `his_fix_audit` table, transactional write runner with dry-run) and a shared Angular shell component. Each fix is one service + one controller under `/api/his-fix/<slug>` and one standalone page under `features/his-fixes/<slug>/`, its own route and menu item.

**Tech Stack:** Spring Boot 1.5.22, `NamedParameterJdbcTemplate`, Jackson `ObjectMapper`, JUnit 4 + Mockito (`spring-boot-starter-test`, already in pom); Angular 17 standalone components with signals; MySQL 5.7 `his_kkm`.

**Spec:** `docs/superpowers/specs/2026-11-25-his-fix-utilities-design.md`

## Global Constraints

- Not a git repository: there are no commit steps. Finish each task by running its verification commands.
- Old-Java style: anonymous `RowMapper`s, explicit generic type arguments (`new ArrayList<String>()`), constructor injection, no lambdas in backend code (match `DischargeDateService`).
- Packages: `com.hospital.maintenance.service.hisfix`, `...web.hisfix`, `...dto.hisfix`.
- Never write `updated_by` / `updated_date` on HIS tables: `updated_by` is a foreign key to `userid.uid_userid` on every target table and the server runs `STRICT_TRANS_TABLES`, so an app username fails the UPDATE. The audit row records who made the change. `document_detail.status_code` (FK → `ref_status`) and `enc_admissions.class_code` (FK → `ref_entitlement_class`) are FKs too: only write values that exist there.
- Every endpoint: 401 if no `AuthService.SESSION_USER_KEY` in the session, 403 if `!"ADMIN".equals(user.getRole())`, 400 `{"message": "..."}` on `IllegalArgumentException`.
- Ticket No: required, trimmed, max 50 chars. Messages: `"No. Tiket diperlukan."` and `"No. Tiket tidak boleh melebihi 50 aksara."` (same as `DischargeDateService`).
- Every write runs in one transaction: read before, update, read after, reject if unchanged (`"Tiada perubahan."`), insert the `his_fix_audit` row. DDL runs outside the transaction.
- WHERE clauses for UPDATEs always use the primary key(s) of rows that were shown in the preview, never a broader filter.
- Searches use indexed paths only: `patients.patient_mrn` → `encounters.patient_id` → (`document_detail.encounter_id`, `enc_transfers.encounter_id`, `enc_admissions.encounter_id`); `document_detail_access.doc_detail_id`; `app_reservations.person_id` / `resource_code` / `reserve_date`; primary keys elsewhere. No `LIKE '%x%'` on large tables.
- Dry run: property `hisfix.dry-run` (default `false`). When `true`, the write runner calls `status.setRollbackOnly()` after the audit insert, and the response carries `dryRun: true`. Local `application.properties` sets `hisfix.dry-run=true`, because it points at staging, which is assumed to be real data. `deploy/ubuntu/env/hsi-utilities-backend.env` sets `HISFIX_DRY_RUN=false`.
- UI copy: Malay/English mix as in existing screens; each page header has a one-line Malay description of the symptom and the fix.
- Menu items: `requiresAdmin: true`, grouped under a sidebar heading `Pembetulan HIS` placed after `Update Death Date`.

## Review Focus

1. **Stale preview.** Another user changes the row between preview and Apply. Expected: the write compares the client-sent "expected before" values to the fresh read, and rejects with `"Rekod telah berubah, sila cari semula."` Pinned by `HisFixSupportTest.rejectsWhenBeforeValuesDiffer`, and used by every fix.
2. **Double-click / repeated Apply.** Expected: the second call finds no change and returns 400 `"Tiada perubahan."`, with no second audit row. Each fix's apply rejects when the before equals the target.
3. **Non-ADMIN or expired session calling the API directly.** Expected: 401 or 403 even for search and audit endpoints. Pinned by `HisFixSupportTest.requireAdmin*`.
4. **MRN with or without `HSI`, in lowercase, or with spaces.** Expected: all find the same patient. Pinned by `HisFixSupportTest.mrnCandidates*`.
5. **Renumber over a list that contains cancelled bookings or duplicate indexes.** Expected: cancelled rows untouched; active rows get 1..N ordered by (current index, reserve_id). Pinned by `SlotRenumberPlannerTest`.

---

### Task 1: Shared backend base

**Files:**
- Create: `backend/src/main/java/com/hospital/maintenance/service/hisfix/HisFixSupport.java`
- Create: `backend/src/main/java/com/hospital/maintenance/service/hisfix/HisFixAccessException.java`
- Create: `backend/src/main/java/com/hospital/maintenance/dto/hisfix/HisFixAuditRecord.java`, `HisFixAuditEntry.java`, `HisFixResult.java`
- Create: `backend/src/main/java/com/hospital/maintenance/web/hisfix/HisFixControllerSupport.java`
- Create: `deploy/sql/2026-11-25_his_fix_audit.sql`
- Modify: `backend/src/main/resources/application.properties` (add `hisfix.dry-run=true`), `deploy/ubuntu/env/hsi-utilities-backend.env` (add `HISFIX_DRY_RUN=false`)
- Test: `backend/src/test/java/com/hospital/maintenance/service/hisfix/HisFixSupportTest.java`

**Interfaces:**
- Produces:
  - `HisFixSupport` (`@Component`, constructor `(NamedParameterJdbcTemplate, PlatformTransactionManager, ObjectMapper, @Value("${hisfix.dry-run:false}") boolean dryRun)`):
    - `String requireTicketNo(String raw)`: trimmed value, or throws `IllegalArgumentException`.
    - `List<String> mrnCandidates(String raw)`: `["HSI"+digits, digits]`, same rules as `DischargeDateService.mrnCandidates`.
    - `String requireTime(String raw, String label)`: `HH:mm`, regex `^([01]\d|2[0-3]):[0-5]\d$`.
    - `void requireUnchanged(Map<String,Object> expected, Map<String,Object> actual)`: throws `IllegalArgumentException("Rekod telah berubah, sila cari semula.")` when any key differs (values compared via `String.valueOf`).
    - `String toJson(Object value)`
    - `<T> HisFixResult<T> write(TransactionCallback<T> work)`: ensures the audit table first, then runs `work` in a `TransactionTemplate`; rolls back if dry run; returns `HisFixResult{ T data; boolean dryRun; }`.
    - `void insertAudit(HisFixAuditRecord record, SystemUser user)`: must be called inside `work`.
    - `List<HisFixAuditEntry> loadAudit(String module, String patientMrn, String targetKeyPrefix)`: newest first, limit 100. At least one filter is required.
    - `boolean isDryRun()`
    - static `SystemUser requireAdmin(HttpSession session)`: throws `HisFixAccessException(401|403)`.
  - `HisFixAuditRecord`: fields `module, ticketNo, patientMrn, encounterId, targetTable, targetKey, beforeJson, afterJson, reason` (getters/setters).
  - `HisFixAuditEntry`: the same fields plus `id, editedBy, editedName, editedAt`.
  - `HisFixControllerSupport` (abstract base): `protected ResponseEntity<?> handle(HttpSession session, HisFixAction action)`, where `interface HisFixAction { Object run(SystemUser user); }`. It maps `HisFixAccessException` to its status, `IllegalArgumentException` to 400 `{"message"}`, and the return value to 200.

- [ ] **Step 1: Write failing tests** in `HisFixSupportTest`. Build `HisFixSupport` with `null` JDBC and a real `ObjectMapper`, so only the pure methods are tested:
  - `requireTicketNoTrims`: `"  T-1 "` → `"T-1"`
  - `requireTicketNoRejectsBlank`: `"  "` → IAE with message `"No. Tiket diperlukan."`
  - `requireTicketNoRejects51Chars`: IAE with message `"No. Tiket tidak boleh melebihi 50 aksara."`; 50 chars passes
  - `mrnCandidatesAcceptsPrefixCaseAndSpaces`: `" hsi 8590753 "` → `["HSI8590753","8590753"]`; `"8590753"` → same list
  - `mrnCandidatesRejectsBlankAndBarePrefix`: `""` and `"HSI"` → IAE `"Sila masukkan MRN pesakit."`
  - `requireTimeAcceptsAndRejects`: `"10:51"` ok; `"24:00"`, `"9:5"` and `"10.51"` → IAE
  - `rejectsWhenBeforeValuesDiffer`: expected `{access_flag:1}`, actual `{access_flag:0}` → IAE `"Rekod telah berubah, sila cari semula."`; equal maps pass
  - `requireAdminNoSession401`, `requireAdminNonAdmin403`, `requireAdminAdminOk`: use `MockHttpSession` with `SystemUser` roles `null`, `"USER"` and `"ADMIN"`

- [ ] **Step 2: Run and see them fail.** `cd backend; mvn -q test -Dtest=HisFixSupportTest` → compilation failure (classes missing).

- [ ] **Step 3: Implement the classes above.** The audit DDL (also put this, in idempotent form, in `deploy/sql/2026-11-25_his_fix_audit.sql`, with the same header style as `2026-11-25_discharge_death_ticket.sql`):

```sql
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
```

- [ ] **Step 4: Run the tests.** `mvn -q test -Dtest=HisFixSupportTest` → all pass. Then `mvn -q clean package -DskipTests` → BUILD SUCCESS.

---

### Task 2: Frontend shell + Fix 2 "Complete Transfer (Out Waiting)", end to end

This is the first consumer of the shared base on both sides. Later fixes copy its shape.

**Files:**
- Create backend: `service/hisfix/CompleteTransferService.java`, `web/hisfix/CompleteTransferController.java`, `dto/hisfix/TransferRow.java`, `dto/hisfix/CompleteTransferRequest.java`
- Create frontend: `frontend/src/app/features/his-fixes/his-fix-api.service.ts`, `frontend/src/app/features/his-fixes/shared/his-fix-shell.component.{ts,html,css}`, `frontend/src/app/features/his-fixes/complete-transfer/complete-transfer.component.{ts,html}`
- Modify: `frontend/src/app/app.routes.ts`, `frontend/src/app/layout/dashboard-layout.component.{ts,html}`

**Interfaces:**
- Consumes: Task 1 `HisFixSupport`, `HisFixControllerSupport`, `HisFixResult`.
- Produces (backend pattern every fix follows):
  - `GET /api/his-fix/<slug>/search?...` returns rows; `POST /api/his-fix/<slug>/apply` takes a request with `ticketNo`, `reason` and the expected before values, and returns `HisFixResult<row>`; `GET /api/his-fix/<slug>/audit?mrn=` returns `List<HisFixAuditEntry>` (module filter = the fix's module constant).
  - Module constant per fix, stored in `his_fix_audit.module`: `COMPLETE_TRANSFER`, `UNLOCK_DOCUMENT`, `RENUMBER_SLOTS`, `BIRTH_TIME`, `DOCUMENT_STATUS`, `ENCOUNTER_ACTIVE`, `SCHEDULE_VISIBILITY`, `MULTI_WARD`, `BED_CLASS`.
- Produces (frontend):
  - `HisFixApiService`: `search<T>(slug: string, params: Record<string,string>): Observable<T>`, `apply<T>(slug: string, body: object): Observable<{data: T; dryRun: boolean}>`, `audit(slug: string, mrn: string): Observable<HisFixAuditEntry[]>`. Base `${API_BASE_URL}/his-fix/`.
  - `<app-his-fix-shell>`: inputs `title`, `description`, `busy`, `errorMessage`, `successMessage`, `dryRun`, `auditEntries`, `canApply`; two-way `ticketNo` and `reason`; output `apply`. Content slots: `[shellSearch]`, `[shellResults]`, `[shellForm]`. It renders the Ticket No (maxlength 50) and reason inputs, the confirm step (`window.confirm` listing the change), the banners, a yellow "DRY RUN — tiada perubahan disimpan" badge when `dryRun`, and the audit table (ticket, time, user, before → after).

- [ ] **Step 1: Backend.**
  - `CompleteTransferService.search(String mrn)` → `List<TransferRow>`: one row per `enc_transfers` with `transfer_confirmed = 0` for the patient's encounters. Include `transfer_id`, `encounter_id`, `patient_mrn`, `person_name`, `ward_source`/`ward_destination` with `ref_locations.location_name`, `transfer_timestamp`, and the destination's `enc_admissions` `admission_active`/`admission_confirmed` (via `admission_destination`, read-only).
  - `apply(CompleteTransferRequest{transferId, expectedTransferConfirmed, ticketNo, reason}, SystemUser)`: `UPDATE enc_transfers SET transfer_confirmed = 1 WHERE transfer_id = :id`. Audit `target_table='enc_transfers'`, `target_key='transfer_id=<id>'`.
  - Reject if the transfer is not found, or already 1 (`"Tiada perubahan."`).

- [ ] **Step 2: Frontend shell + page + route `/fix/complete-transfer` + menu.** Add a `group?: string` field to the menu item type, and render a group heading when it changes from the previous item. The menu item is `{ label: 'Complete Transfer (Out Waiting)', shortLabel: 'Transfer', route: '/fix/complete-transfer', icon: 'move_down', requiresAdmin: true, group: 'Pembetulan HIS' }`.

- [ ] **Step 3: Verify the build.** `cd backend; mvn -q clean package -DskipTests` → success. `cd frontend; npm run build` → success with no new warnings.

- [ ] **Step 4: Verify on staging in dry-run mode.**
  1. Start the backend with `hisfix.dry-run=true`.
  2. Log in as ADMIN, open the page, and search an MRN that has a pending transfer. Get one with a read-only `SELECT p.patient_mrn FROM enc_transfers t JOIN encounters e USING(encounter_id) JOIN patients p USING(patient_id) WHERE t.transfer_confirmed = 0 LIMIT 1`.
  3. Apply with ticket `TEST-DRY`. Expected: the response has `dryRun: true`, the page shows the after value `1`, and a fresh read-only SELECT still shows `transfer_confirmed = 0` with no `his_fix_audit` row.
  4. As a non-ADMIN user, the menu item is hidden, and calling `GET /api/his-fix/complete-transfer/search?mrn=x` returns 403.

---

### Task 3: Fix 1 "Unlock Document"

**Files:** `service/hisfix/UnlockDocumentService.java`, `web/hisfix/UnlockDocumentController.java`, `dto/hisfix/DocumentLockRow.java`, `dto/hisfix/UnlockDocumentRequest.java`, `features/his-fixes/unlock-document/unlock-document.component.{ts,html}`, plus the route `/fix/unlock-document` and the menu item (icon `lock_open`).

**Interfaces:** Consumes the Task 1 and Task 2 patterns. Slug `unlock-document`, module `UNLOCK_DOCUMENT`.

- [ ] **Step 1: Backend.**
  - `search(mrn)`: the patient's encounters → `document_detail` (by `encounter_id`) → `document_detail_access` rows with `access_flag = 1`.
  - Row fields: `doc_detail_access_id`, `doc_detail_id`, `document_template.report_name` (note name), `document_detail.created_date`, `uid_userid` (who holds it open), `datetime_open`, `encounter_id`.
  - Limit to encounters from the last 90 days by `encounter_timestamp`, to keep it bounded.
  - `apply({docDetailAccessIds: long[], ticketNo, reason})`:
    - Every id must have been returned by a fresh search for the same MRN and still have `access_flag = 1`, otherwise IAE `"Rekod telah berubah, sila cari semula."`.
    - `UPDATE document_detail_access SET access_flag = 0, datetime_close = COALESCE(datetime_close, NOW()) WHERE doc_detail_access_id IN (:ids)`.
    - Write one audit row per id.

- [ ] **Step 2: Frontend page.** A checkbox per locked row, shown in the shell.

- [ ] **Step 3: Verify.** `mvn -q clean package -DskipTests`, `npm run build` → success. Do a staging dry run, as in Task 2 Step 4, with an MRN that has `access_flag = 1`: the row stays 1 after the dry run.

---

### Task 4: Fix 4 "Update Birth Time"

**Files:** `service/hisfix/BirthTimeService.java`, `web/hisfix/BirthTimeController.java`, `dto/hisfix/BirthTimeRow.java`, `dto/hisfix/BirthTimeRequest.java`, `features/his-fixes/birth-time/birth-time.component.{ts,html}`, route `/fix/birth-time`, menu icon `child_care`.

- [ ] **Step 1: Backend.**
  - `search(mrn)` → the person row: `person_id`, `patient_mrn`, `person_name`, `person_birthdate`, `person_birthtime`.
  - `apply({personId, expectedBirthTime, newBirthTime, ticketNo, reason})`:
    - Validate `newBirthTime` with `requireTime`.
    - `UPDATE persons SET person_birthtime = :t WHERE person_id = :id`.
    - Audit `target_key='person_id=<id>'`.
- [ ] **Step 2: Frontend.** Use `<input type="time">`. Show the current value.
- [ ] **Step 3: Verify** builds, and run a staging dry run.

---

### Task 5: Fix 5 "Document Status (Mark as Error)"

**Files:** `service/hisfix/DocumentStatusService.java`, `service/hisfix/DocumentStatusRule.java`, `web/hisfix/DocumentStatusController.java`, DTOs `DocumentStatusRow`, `DocumentStatusRequest`, page `features/his-fixes/document-status/`, route `/fix/document-status`, menu icon `rule`.
**Test:** `backend/src/test/java/com/hospital/maintenance/service/hisfix/DocumentStatusRuleTest.java`

**Interfaces:** `DocumentStatusRule.SUBMITTED = "01"`, `MARK_AS_ERROR = "107"`; `static String target(String current)` returns the other code, or throws IAE `"Status dokumen ini (<code>) tidak boleh ditukar di sini."` for any other code. Trim the input, because `status_code` is `CHAR(3)` and may be padded (`"01 "`).

- [ ] **Step 1: Failing test** `DocumentStatusRuleTest`: `"01"` → `"107"`; `"107"` → `"01"`; `"01 "` → `"107"`; `"106"`, `null` → IAE.
- [ ] **Step 2: Run** `mvn -q test -Dtest=DocumentStatusRuleTest` → it fails.
- [ ] **Step 3: Implement the rule and the service.**
  - `search(mrn)`: documents for the patient's encounters from the last 90 days, with `status_code` in (`01`, `107`). Show the note name, date, creator and `ref_status.status_detail`.
  - `apply({docDetailId, expectedStatus, ticketNo, reason})`: `UPDATE document_detail SET status_code = :target WHERE doc_detail_id = :id`.
- [ ] **Step 4: Run the test** → it passes. Run the builds, and a staging dry run.

---

### Task 6: Fix 6 "Encounter Active Flag (Discharge in ED first)"

**Files:** `service/hisfix/EncounterActiveService.java`, `web/hisfix/EncounterActiveController.java`, DTOs `EncounterActiveRow`, `EncounterActiveRequest`, page `features/his-fixes/encounter-active/`, route `/fix/encounter-active`, menu icon `toggle_on`.

- [ ] **Step 1: Backend.**
  - `search(mrn)`: the patient's 20 most recent encounters (`ORDER BY encounter_timestamp DESC`), with `encounter_id`, `domain_code`, `encounter_timestamp`, `encounter_active`, `discharge_date`, and `location_code` with its name.
  - `apply({encounterId, expectedActive, newActive (0|1), ticketNo, reason})`: reject `newActive` values other than 0 or 1. `UPDATE encounters SET encounter_active = :v WHERE encounter_id = :id`.
- [ ] **Step 2: Frontend.** A radio button per encounter, and an explicit 0/1 choice with no default. The header explains the PDF2 #1 symptom.
- [ ] **Step 3: Verify** builds, and run a staging dry run.

---

### Task 7: Fix 9 "Update Bed Class"

**Files:** `service/hisfix/BedClassService.java`, `web/hisfix/BedClassController.java`, DTOs `BedClassRow`, `BedClassRequest`, `ClassOption`, page `features/his-fixes/bed-class/`, route `/fix/bed-class`, menu icon `bed`.

- [ ] **Step 1: Backend.**
  - `GET /classes` → `SELECT entitlement_class, entitlement_class_desc FROM ref_entitlement_class WHERE active_status = 1 ORDER BY class_sequence`. On staging: `'1'` = Kelas VIP/ ROYAL, `'3'` = Kelas 3.
  - `search(mrn)` → the `enc_admissions` rows of the patient's latest inpatient encounter: `admission_id`, `ward_code` with its name, `bed_code`, `class_code` with its description, `admission_active`, `admission_confirmed`.
  - `apply({admissionId, expectedClassCode, newClassCode, ticketNo, reason})`: `newClassCode` must exist in the active class list. `UPDATE enc_admissions SET class_code = :c WHERE admission_id = :id`.
- [ ] **Step 2: Frontend.** A class dropdown.
- [ ] **Step 3: Verify** builds, and run a staging dry run.

---

### Task 8: Fix 3 "Susun Semula Slot Temujanji"

**Files:** `service/hisfix/RenumberSlotsService.java`, `service/hisfix/SlotRenumberPlanner.java`, `web/hisfix/RenumberSlotsController.java`, DTOs `SlotRow`, `SlotChange`, `RenumberSlotsRequest`, `ResourceOption`, page `features/his-fixes/renumber-slots/`, route `/fix/renumber-slots`, menu icon `format_list_numbered`.
**Test:** `backend/src/test/java/com/hospital/maintenance/service/hisfix/SlotRenumberPlannerTest.java`

**Interfaces:**
- `SlotRow{long reserveId; Integer reserveIndex; boolean cancelled; String patientMrn; String personName; String scheduleCode}`
- `SlotChange{long reserveId; Integer fromIndex; int toIndex}`
- `static List<SlotChange> SlotRenumberPlanner.plan(List<SlotRow> rows)`: takes the non-cancelled rows, sorts them by (`reserveIndex` with nulls last, `reserveId`), and assigns 1..N. It returns only the rows whose index changes. Cancelled rows never appear.

- [ ] **Step 1: Failing tests** in `SlotRenumberPlannerTest`:
  - `gapsClosed`: active indexes [1, 3, 7] → changes `3→2`, `7→3`
  - `duplicatesResolvedByReserveId`: ids 10 and 11 both at index 2, plus id 12 at 1 → id 12 stays 1, id 10 becomes 2, id 11 becomes 3
  - `cancelledUntouched`: a cancelled row at index 1 and an active row at index 5 → only `5→1`
  - `nullIndexLast`
  - `alreadySequentialReturnsEmpty`
- [ ] **Step 2: Run** `mvn -q test -Dtest=SlotRenumberPlannerTest` → it fails.
- [ ] **Step 3: Implement the planner and the service.**
  - `GET /resources?scheduleCode=` → `app_resources` (code, name, `schedule_code`) for a schedule picker. Schedules come from `app_schedules` (`schedule_code`, `schedule_name`).
  - `search(resourceCode, date, scheduleCode)` → rows `WHERE resource_code = :r AND reserve_date = :d AND schedule_code = :s` (indexed by `resource_code` and `reserve_date`). `cancelled` is `reserve_cancelled = 1`. Also return the planned changes.
  - `apply({resourceCode, date, scheduleCode, expectedChanges, ticketNo, reason})`:
    - Recompute the plan on fresh rows. If it differs from `expectedChanges`, reject with `"Rekod telah berubah, sila cari semula."`.
    - `UPDATE app_reservations SET reserve_index = :to WHERE reserve_id = :id` for each change.
    - Write one audit row: `target_key='resource=<r>;date=<d>;schedule=<s>'`, with before/after JSON arrays of `{reserve_id, reserve_index}`.
- [ ] **Step 4: Run the tests** → they pass. Run the builds. Do a staging dry run on a resource and date that has a gap.

---

### Task 9: Fix 7 "Show/Hide Schedule"

**Files:** `service/hisfix/ScheduleVisibilityService.java`, `web/hisfix/ScheduleVisibilityController.java`, DTOs `ScheduleRow`, `ScheduleVisibilityRequest`, page `features/his-fixes/schedule-visibility/`, route `/fix/schedule-visibility`, menu icon `visibility`.

- [ ] **Step 1: Backend.**
  - `searchByMrn(mrn)`: `persons.person_id` → `app_reservations` with `person_id = :pid` and `reserve_date >= CURDATE() - INTERVAL 30 DAY`, grouped by `schedule_code` → `app_schedules` with `schedule_name` and `schedule_is_hidden`, plus the patient's reservation dates for that schedule.
  - `searchByName(text)`: `app_schedules` (137 rows; a `LIKE` is fine here).
  - `openedToday()`: schedules for which `his_fix_audit` has module `SCHEDULE_VISIBILITY` with `after_json` hidden = 0 and `edited_at >= CURDATE()`, and which are still `schedule_is_hidden = 0` now.
  - `apply({scheduleCode, expectedHidden, newHidden, ticketNo, reason})`: `UPDATE app_schedules SET schedule_is_hidden = :h WHERE schedule_code = :c`.
  - `rehideAll({ticketNo, reason})`: hides every schedule from `openedToday()` in one transaction, writing one audit row each.
  - `app_resources` is never written.
- [ ] **Step 2: Frontend.** Two search modes (MRN or schedule name), a toggle per row, and an "Dibuka hari ini" panel with a "Sembunyikan semula semua" button.
- [ ] **Step 3: Verify** builds, and run a staging dry run. Note that `openedToday` will be empty after a dry run, because the audit row is rolled back. That's expected; check its query once with a read-only SELECT.

---

### Task 10: Fix 8 "Fix Patient Shown in Many Wards"

**Files:** `service/hisfix/MultiWardService.java`, `web/hisfix/MultiWardController.java`, DTOs `WardAdmissionRow`, `WardTransferRow`, `MultiWardRequest`, `MultiWardRowChange`, page `features/his-fixes/multi-ward/`, route `/fix/multi-ward`, menu icon `call_split`.

**Interfaces:** `MultiWardRowChange{String table ("enc_admissions"|"enc_transfers"); long id; Map<String,Integer> expected; Map<String,Integer> values}`. The allowed keys are `admission_active` and `admission_confirmed` for `enc_admissions`, and `transfer_confirmed` for `enc_transfers`. Values must be 0 or 1.

- [ ] **Step 1: Backend.**
  - `search(mrn)` → for the patient's latest inpatient encounter (or a chosen `encounterId`):
    - all `enc_admissions` rows, ordered by `admission_id`, with `ward_code` and its name, `bed_code`, `admission_active`, `admission_confirmed`, `admission_arrival`, `admission_depart`
    - all `enc_transfers` rows, with ward source → destination, `transfer_confirmed` and `transfer_timestamp`
  - `apply({encounterId, changes: MultiWardRowChange[], ticketNo, reason})`:
    - Reject an empty `changes` list, unknown keys, values other than 0/1, and ids that don't belong to `encounterId`.
    - Check `expected` against a fresh read.
    - Update only the listed columns of the listed ids.
    - Write one audit row per changed row.
- [ ] **Step 2: Frontend.** Two tables. Each row has a "Betulkan" checkbox; ticking it shows editable 0/1 dropdowns for that row's flags. Only ticked rows are sent. There is no "clear all other wards" button.
- [ ] **Step 3: Verify** builds, and run a staging dry run on an encounter with more than one admission row.

---

### Task 11: Ticket Report integration, docs, final verification

**Files:**
- Modify: `backend/src/main/java/com/hospital/maintenance/service/TicketReportService.java` (add a third source)
- Modify: `CLAUDE.md` (Routing and Audit sections)
- Modify: `frontend/src/app/features/tickets/ticket-report.component.*` only if module labels are mapped there.

- [ ] **Step 1: Add the `his_fix_audit` source.** In `loadRows`, guarded by `hasTicketColumn("his_fix_audit")`:
  - one `TicketReportRow` per audit row
  - `module` = the module constant
  - `patientMrn`, `encounterId`, `editedAt`, `editedBy`, `editedName`, `reason` copied from the audit row
  - `changeCount = 1`
  - `changeSummary = target_table + " " + target_key`

  Then check that the dashboard's per-module counts and the Excel export don't assume only DISCHARGE, DEATH and PD103. Map the new modules to readable labels.
- [ ] **Step 2: Update CLAUDE.md.** Add one paragraph: the 9 `/fix/*` ADMIN-only routes, the `his_fix_audit` table, the `hisfix.dry-run` property and why local is `true`, and the `deploy/sql/2026-11-25_his_fix_audit.sql` script.
- [ ] **Step 3: Final verification.**
  - `cd backend; mvn -q clean test` → `HisFixSupportTest`, `DocumentStatusRuleTest` and `SlotRenumberPlannerTest` pass.
  - `mvn -q package -DskipTests` → success.
  - `cd frontend; npm run build` → success.
  - Restart the HSI backend in dry-run mode, then:
    - as ADMIN, open all 9 menu items and run a search on each
    - as a non-ADMIN user, the "Pembetulan HIS" group is hidden
    - the Ticket Report still loads
