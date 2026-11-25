# HIS@KKM Fix Utilities — Design

Date: 2026-11-25 · Status: approved by user 2026-11-25 (answers to open questions recorded under "Decisions")

## Intent

The HSI IT unit and vendor support (MSR) currently fix recurring HIS@KKM problems by hand-writing
SQL against the live `his_kkm` database (source: `ext_docs/01 Issue Records - System Error as 13102026 (1).pdf`,
`ext_docs/02 Issue Records v2.pdf`, and the ticket log `ext_docs/HISKKM issue 080926 until 201126.xlsx`).
Hand-written SQL is slow, error-prone (the PDFs themselves name non-existent columns) and leaves no audit
trail. The user asked for **one utility screen per issue, each with its own menu item**, built the same way
as the existing Update Discharge Time / Update Death Date tools: search → see current values → Ticket No →
confirm → before/after recorded.

## Goals

- 9 screens, each its own sidebar menu item and route, covering the PDF issues below.
- Every write: Ticket No required (max 50 chars, server-validated), runs in one transaction, re-reads the
  affected rows after the update, rejects "no change", and records before/after in one shared audit table.
- Ticket Report (`TicketReportService`) includes these fixes.

## Non-goals

- PDF2 #8 (deactivate a duplicate newborn registration) — the PDF says only the Central team may do it.
- Staff Management (user discipline/location changes, ~70 tickets) and slot unblocking (~15 tickets) —
  not in the PDFs; possibly in the separate CAS system. Candidate for a later round.
- No generic "run any SQL" screen.

## Anti-goals (would be a failure even if it works)

- A fix that touches more rows than the user saw in the preview.
- A write without an audit row, or an audit row without the write (must be the same transaction).
- Long-running scans on `his_kkm` (e.g. `document_detail` has 3.4M rows, `document_detail_access` 4.4M,
  `app_reservations` 838k): every search must hit an indexed key (MRN → patient → encounter, or a
  primary key / resource+date). Verify with `EXPLAIN` during implementation.

## Constraints

- Backend: Spring Boot 1.5, `NamedParameterJdbcTemplate` raw SQL against `his_kkm`, old-Java style
  (anonymous `RowMapper`s, explicit generic args), session auth (`AuthService.SESSION_USER_KEY`),
  `TransactionTemplate` with DDL kept outside the transaction (see `DischargeDateService`).
- Frontend: Angular 17 standalone components under `src/app/features/<feature>/`, API base from
  `core/api.config.ts`, menu items in `layout/dashboard-layout.component.ts` (`menuItems`, `requiresAdmin`).
- UI text in Bahasa Melayu / English mix, matching existing screens.
- MRN input accepts with or without the `HSI` prefix (reuse `mrnCandidates` logic).
- New DDL must also ship as an idempotent script in `deploy/sql/` (app DB account may lack CREATE/ALTER).
- No test framework is wired; verification is manual against the staging DB, plus `mvn package` and `ng build`.

## Approaches considered

- **A. Shared base, separate screens — chosen by the user.** One audit table, shared ticket validation,
  shared audit-write helper, shared page layout; each fix has its own service, endpoint, component, route
  and menu item.
- B. Copy the Discharge Date tool nine times — ~9× near-identical code and nine audit tables for Ticket
  Report to union. Rejected.
- C. One generic config-driven screen — fixes differ too much (#3 renumbers a list, #8 picks one ward of
  several). Rejected.

## Shared base

**Audit table `his_fix_audit`** (lazy-created like `discharge_date_audit`, plus `deploy/sql/` script):
`id, module VARCHAR(40), ticket_no VARCHAR(50) NOT NULL, patient_mrn VARCHAR(50), encounter_id VARCHAR(50),
target_table VARCHAR(64), target_key VARCHAR(200), before_json TEXT, after_json TEXT, reason VARCHAR(500),
edited_by, edited_name, edited_role, edited_at`; indexes on (patient_mrn), (encounter_id), (ticket_no),
(edited_by, edited_at). One row per fix action; `before_json`/`after_json` hold the changed columns of every
affected row (e.g. `[{"reserve_id":1,"reserve_index":5}]`). MySQL 5.7 has JSON, but TEXT keeps it portable — builder's call.

**Backend shared pieces** (package `service`, names are the builder's call): ticket-no validation,
MRN candidate parsing, `ensureAuditTable`, `insertAudit(module, ticketNo, mrn, encounterId, table, key, before, after, reason, user)`,
and a common controller error shape (`{"message": ...}`, 400 on `IllegalArgumentException`, 401 no session,
403 not allowed). Existing `DischargeDateService` is **not** refactored onto it in this round.

**Frontend shared pieces:** one reusable layout used by all 9 pages — header (title + short Malay
description of the issue and the fix), search bar, result table, "Nilai semasa → Nilai baharu" preview,
Ticket No + optional reason, confirm dialog, success/error banner, and per-record audit history.
Each fix page is its own standalone component and route under `features/his-fixes/`.

**Menu:** a new sidebar group "Pembetulan HIS" below Update Death Date, one item per fix.

**Access:** ADMIN-only for all 9 (user decision): menu items `requiresAdmin: true`, every endpoint
returns 401 without a session and 403 for non-ADMIN, including search and audit-history endpoints.

## The nine fixes

Facts below were confirmed from `information_schema` and read-only sampling on staging (10.29.59.55).

| # | Menu / route | Search by | Shows | Write |
|---|---|---|---|---|
| 1 | Unlock Document `/fix/unlock-document` | MRN → encounters → `document_detail` (note name via `document_template`, date, creator) | `document_detail_access` rows with `access_flag = 1` (who opened it, `datetime_open`) | `UPDATE document_detail_access SET access_flag = 0, datetime_close = NOW()` for the chosen `doc_detail_access_id`(s) only |
| 2 | Complete Transfer (Out Waiting) `/fix/complete-transfer` | MRN → encounter | `enc_transfers` rows with `transfer_confirmed = 0` (ward_source → ward_destination, timestamp), plus the destination ward's `enc_admissions` flags (read-only, for context) | `SET transfer_confirmed = 1` for the chosen `transfer_id` only — one-table write, as both PDFs state |
| 3 | Susun Semula Slot Temujanji `/fix/renumber-slots` | Resource (`app_resources`) + `reserve_date` (+ `schedule_code` when the resource has more than one) | `app_reservations` for that resource+date+schedule, current `reserve_index`, patient, cancelled flag | Renumber `reserve_index` 1..N over **non-cancelled** rows (`reserve_cancelled` 0/NULL) in the shown order (current index, then `reserve_id`); cancelled rows untouched. Group = resource + date + schedule (user decision) |
| 4 | Update Birth Time `/fix/birth-time` | MRN | `persons.person_birthdate`, `person_birthtime` (VARCHAR, stored `HH:mm`) | `SET person_birthtime = :hhmm` (validated `HH:mm`) |
| 5 | Document Status `/fix/document-status` | MRN → encounter → documents | `document_detail.status_code` with `ref_status.status_detail` | Toggle `status_code` between `01` Submitted and `107` Mark As Error; refuse any other current status. `reason` untouched (builder's call) |
| 6 | Encounter Active Flag (“Discharge in ED first”) `/fix/encounter-active` | MRN | That patient's recent encounters (all domains, newest first) with `domain_code`, `encounter_active`, `discharge_date`, ward | User picks the encounter and the new value (0 or 1) explicitly; the tool does not assume a direction (user decision, pending their confirmation of the standard procedure). Only `encounters.encounter_active` is written |
| 7 | Show/Hide Schedule `/fix/schedule-visibility` | MRN (→ `app_reservations` → `schedule_code`) **or** schedule name | `app_schedules` incl. `schedule_is_hidden` | Toggle `schedule_is_hidden`. Panel "Dibuka hari ini": schedules this tool unhid today (from `his_fix_audit`) that are still visible, with a "Sembunyikan semula semua" action (one audit row per schedule). No automatic re-hide job. `app_resources.resource_is_hidden` is never written (user decision) |
| 8 | Fix Patient Shown in Many Wards `/fix/multi-ward` | MRN → encounter | Every `enc_admissions` row (ward, `admission_active`, `admission_confirmed`, arrival/depart) and `enc_transfers` row (source → destination, `transfer_confirmed`) for that encounter | User ticks the specific rows that are wrong and sets the flag values per row; only ticked rows are written. The tool never bulk-clears "all other wards" — earlier real stays and confirmed transfers stay intact (user decision) |
| 9 | Update Bed Class `/fix/bed-class` | MRN → current admission | `enc_admissions.class_code` (seen: `3`, `2`, `1A`) | `SET class_code` from a dropdown of valid codes (source table to confirm: `ref_entitlement_class` or similar) |

PDF errata the builder should not copy: "table `enc_transfers`, column `enc_transfers`" is
`enc_transfers.transfer_confirmed`; "`eperson_birthtime`" is `persons.person_birthtime`; "table `encounter`" is `encounters`.

Observed values: `ref_status` `01` = Submitted, `107` = Mark As Error. `document_detail_access.access_flag`
is 1 for currently-open documents (~1.8k of the latest 20k rows). `enc_transfers.transfer_confirmed = 0` on 65 of ~11.7k rows.

## Decisions (2026-11-25)

User accepted all proposals:

1. #8 — explicit per-row selection, no bulk clear.
2. #6 — user chooses encounter and value; direction to be fixed later once the team confirms its procedure.
3. Access — ADMIN only.
4. #3 — skip cancelled bookings; group by resource + date + schedule.
5. #7 — find from MRN, one-click toggle, "re-hide all opened today" action, no scheduler, never touch resources.

Builder calls made by Claude (user may override): fix 3's date column is `app_reservations.reserve_date`;
fix 2 stays a one-table write. No fix writes `updated_by`/`updated_date` on HIS tables:
`updated_by` is a foreign key to `userid.uid_userid` on every target table (strict SQL mode), so an app
username would fail the update; who-did-it lives in `his_fix_audit` (same as `DischargeDateService`).

**Testing:** staging (10.29.59.55) is assumed to hold real patient data (user did not say otherwise).
On staging run searches/previews only; verify writes inside a transaction that is rolled back, or on a
DB the user confirms is disposable. Never commit a test write to staging.

## Left to the builder

- Class and method names, file split, exact SQL joins (must use indexed paths; check with `EXPLAIN`).
- Whether shared frontend layout is a component, a set of CSS classes, or both.
- Exact Malay wording on screens, following existing tone.
- Ticket Report: add `his_fix_audit` as a third source (module = fix name).
