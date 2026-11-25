# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

HSI Utilities — internal web app for a Malaysian hospital (KKM). UI strings are mixed English / Bahasa Melayu; preserve the original language when editing existing text.

## Stack

- **Backend:** Spring Boot 1.5.22 parent, Java 17 target (`backend/pom.xml`). Notable consequence: old-style APIs still in use — `WebMvcConfigurerAdapter`, `javax.servlet.*`, MySQL connector 5.1.49 with driver class `com.mysql.jdbc.Driver`. Don't migrate these unless asked.
- **Frontend:** Angular 17 standalone components, TypeScript 5.4, no test framework wired (`ng test` exists but no specs).
- **Database:** MySQL 5.7 (`his_kkm`). JPA `ddl-auto=update`. Most report code uses `NamedParameterJdbcTemplate` against `his_kkm` tables (`ref_locations`, `ref_generaldisciplines`, `report_edit_audit`, etc.) — only a handful of app-owned tables (`system_users`, `maintenance_requests`) go through JPA.
- **AI assistant:** OpenAI Responses API (`OPENAI_API_KEY`, `OPENAI_MODEL`, default `gpt-4.1-mini`) with `LocalAssistantFallbackService` when key absent or 429.

## Run / build / deploy

Dev (Windows, two PowerShell windows):
```powershell
.\run-dev.ps1               # spawns backend (mvn spring-boot:run, :8080) + frontend (npm start, :4200)
```

Backend alone:
```powershell
cd backend
mvn spring-boot:run         # dev
mvn clean package -DskipTests   # produces target/maintenance-system-0.0.1-SNAPSHOT.jar
mvn test                    # no backend/src/test dir exists yet; create it to add tests
mvn test -Dtest=SomeServiceTest            # single test class
mvn test -Dtest=SomeServiceTest#someMethod # single test method
```

Frontend alone:
```powershell
cd frontend
npm install
npm run start               # ng serve --host 0.0.0.0 --port 4200
npm run build               # outputs dist/hospital-maintenance-frontend
```

Release bundle for Ubuntu staging:
```powershell
cd deploy\windows
.\build-release.ps1         # builds backend jar + frontend dist, zips into ..\..\release\
```

Windows Server alternative (see [deploy/windows/README.md](deploy/windows/README.md), in Malay): `prepare-package.ps1` on the dev laptop builds into `server-package\`, then `install-server.ps1` runs on the server. `release/`, `server-package/` and `server-package.zip` are build outputs — regenerate them, don't hand-edit.

Full deploy procedure (Ubuntu target `10.29.59.55`, nginx + systemd, backend on port `18080` behind nginx on `8081`) lives in [docs/HSI_UTILITIES_TECHNICAL_DEPLOYMENT_SOP.md](docs/HSI_UTILITIES_TECHNICAL_DEPLOYMENT_SOP.md). Release env file: [deploy/ubuntu/env/hsi-utilities-backend.env](deploy/ubuntu/env/hsi-utilities-backend.env) — this is the source of truth for prod config, NOT `application.properties`.

Schema changes ship as dated, idempotent SQL scripts in `deploy/sql/` (e.g. `2026-11-25_discharge_death_ticket.sql`). They mirror what the backend's lazy `ensure...Table()` code does at runtime, for servers where the app's DB account lacks CREATE/ALTER rights. When you change a lazy-created table, update or add a matching script.

## Architecture notes that span files

**Two-database mental model.** The app reads/writes a small set of its own tables (managed by JPA + `DataSeeder`) but also queries the host hospital's existing `his_kkm` schema directly via JDBC. Report services (`InpatientReportService`, `DamaDiagnosisReportService`) hand-build SQL against `his_kkm` reference tables. When adding report logic, expect to write raw SQL against host tables, not entities.

**Auth = HTTP session.** No JWT, no Spring Security. `AuthService` stuffs `SystemUser` into `HttpSession` under key `AUTH_USER`; the frontend's `core/session.interceptor.ts` sets `withCredentials: true` on every request so the session cookie crosses origins in dev. Controllers re-read the session and return 401/403 manually (see `AssistantController` for the canonical role check pattern — `ADMIN`-only features check `user.getRole()` directly). Passwords are stored in plaintext in `system_users`. The seed admin is `admin / admin123` (see `DataSeeder`). Don't introduce password hashing without asking — it would break the seed and existing accounts.

**CORS is hardcoded to localhost:4200** in `WebConfig`. In prod, frontend and backend share an nginx origin, so CORS doesn't fire. If you add a new origin, update `WebConfig.addCorsMappings`.

**API base URL switches by hostname.** `frontend/src/app/core/api.config.ts` uses `http://localhost:8080/api` when the browser host is `localhost`/`127.0.0.1`, otherwise relative `/api` (so nginx proxies it). Don't hardcode absolute URLs in services.

**Routing default.** After login the user lands on `/reports` (HSI Reports menu), NOT `/dashboard`. Many report routes are placeholders (`ReportPlaceholderComponent`) with Malay description text — only `pd103`, `dama-diagnosis`, and `audit` are wired to real logic, plus the top-level `/discharge-date` (Update Discharge Time) and `/death-date` (Update Death Date) tools, which share one component and service (`mode` = DISCHARGE / DEATH), and the ADMIN-only `/ticket-report` (Laporan Tiket: dashboard + ticket list + Excel) that reads `ticket_no` rows from both audit tables via `TicketReportService`. See `frontend/src/app/app.routes.ts`.

**Audit log.** Editable grid reports (PD103 inpatient) write every cell edit into `report_edit_audit`. `InpatientReportService.ensureAuditTable()` lazy-creates the table; report writes always go through the audit path. Only `ADMIN` sees the Audit Log menu. `DischargeDateService` follows the same pattern: it writes directly to `his_kkm` clinical tables (`encounters`, `enc_discharges`, `enc_admissions`) in one `@Transactional` update, and records before/after values in the lazy-created `discharge_date_audit` table. Any logged-in user can edit (a deliberate choice). Every write (PD103 save, discharge/death update) requires a Ticket No (max 50 chars), validated server-side and stored in the `ticket_no` column of both audit tables; the column is added to pre-existing tables on first use.

**Assistant fallback chain.** `OpenAiAssistantService.chat()` → tries `AssistantCommandService.tryHandle()` for built-in slash-style commands first → if no `OPENAI_API_KEY` configured, returns `LocalAssistantFallbackService` reply → on OpenAI 429 / network failure, also falls back. All paths return `success=true`; check `configured` flag to know if OpenAI was actually used.

## Conventions

- Backend package root: `com.hospital.maintenance` with sub-packages `config`, `domain`, `dto`, `repository`, `service`, `web`. Keep new classes under the matching layer.
- Backend uses old-Java idioms (`new HttpEntity<Map<String, Object>>(...)` with explicit type args, anonymous inner classes for RowMappers) — Java 17 features are available but the codebase isn't using them yet. Match surrounding style.
- Frontend prefix `app`, standalone components, no NgModules. Features live under `src/app/features/<feature>/`.
- Stray `.new`, `.txt`, and `.class` files exist in `backend/` and `backend/src/main/java/.../service/` (e.g. `InpatientReportService.new`, `testfrag.txt`, top-level `DamaCountCheck.class`). These are scratch/leftovers — don't edit or compile against them. The same goes for `tmp_test.txt`/`tmp2.txt` at the repo root.
- Non-code reference material at the repo root: `ext_docs/` (HIS KKM issue records, xlsx/pdf), `stitch_hospital_sultan_ismail_utilities_portal/` (UI design mockups per screen), `pd103.pdf` (the PD103 form layout).
- The working directory is not a git repository, so there's no history to consult and no diffs to review.

## Docs worth reading

`docs/` holds the handover (`HSI_UTILITIES_HANDOVER_2026-04-05.md`), the request backlog, and staging / post-deploy checklists. DB setup SQL lives in `notes_05042026.txt` at the repo root. `application.properties` currently points the local backend at `10.29.59.55` (the staging DB, as `root`); the old dev host `10.29.217.205` is commented out. With `ddl-auto=update`, running the backend locally can therefore alter the staging `his_kkm` schema and writes go to staging data — check which URL is active before running write paths. The release env file sets prod/staging config.
