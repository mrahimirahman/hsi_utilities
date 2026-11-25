# HIS fix utilities — staging dry-run test procedure

Run this before switching `HISFIX_DRY_RUN` to `false` in the release env. Repeat steps 3–6 for each of the 9 `/fix/*` screens.


Staging DB 10.29.59.55 `his_kkm` is treated as REAL patient data. Rules:

1. Build: `cd backend && mvn -q clean package -DskipTests`.
2. Start YOUR OWN backend instance on port 18081 (port 8080 belongs to the user's running app — never stop or touch it), in the background, with dry run forced on the command line:
   `java -jar backend/target/maintenance-system-0.0.1-SNAPSHOT.jar --server.port=18081 --hisfix.dry-run=true  (prefix JVM flag: --add-opens java.base/java.lang=ALL-UNNAMED)`
   Use JDK 17+: `"C:\Program Files\Eclipse Adoptium\jdk-17.0.18.8-hotspot\bin\java.exe"` (plain `java` on PATH is 1.8).
   Wait until the log shows "Started".
3. Log in with curl keeping cookies: `curl -s -c ck.txt -b ck.txt -H "Content-Type: application/json" -d "{\"username\":\"admin\",\"password\":\"admin123\"}" http://localhost:18081/api/auth/login` (check AuthController/LoginRequest for exact field names). Keep cookie files in the scratch dir, not the repo.
4. Find test data with READ-ONLY SQL only: Python `pymysql` (credentials from backend/src/main/resources/application.properties; run with `python -I`), and first execute `SET SESSION TRANSACTION READ ONLY` and `SET SESSION max_execution_time=5000`. SELECT only, always through indexed columns, always `LIMIT`. Never print patient names/IC in your report — MRN and ids only.
5. Call search, then apply ONCE with ticket `TEST-DRY`. The apply response MUST contain `"dryRun":true`. If it ever shows `dryRun:false`, STOP immediately and report BLOCKED.
6. Re-read the row with read-only SQL: value must be unchanged, and `SELECT COUNT(*) FROM his_fix_audit WHERE ticket_no='TEST-DRY'` must be 0.
7. Also check 403 for a non-ADMIN user if one exists (create none); otherwise check 401 without a cookie.
8. Stop YOUR backend process (only the one you started — find it by port 18081) before reporting.

