# Bhoomi Project Recovery Report

**Inspection date:** 2026-11-13  
**Workspace:** `C:\Users\moham\Bhoomi`  
**Scope:** Recovery inspection and lightweight validation only. No application source, configuration, migration, or database changes were made.

## Executive summary

Bhoomi is an existing two-part project: a React/TypeScript Vite frontend and a Java 25 / Spring Boot 4.1.2 backend using PostgreSQL 18, PostGIS, and Flyway. Both the frontend production build and the backend test lifecycle completed successfully in this inspection.

The main recovery concern is that **there is no `.git` metadata in the workspace or either application folder**. Consequently, Git status, commit history, staged changes, and attribution of changes cannot be recovered from this copy. File timestamps show most source/configuration files were last updated on November 2, 2026, while `Backend/.env.example` was modified on November 13, 2026 at 23:44 local time; timestamps alone cannot establish authorship or completeness.

The environment example has a malformed final line and contains non-placeholder password values. Treat those values as exposed; do not reuse them. The file should be corrected before it is used or shared.

## Phase 1 follow-up — 2026-11-14

This section records the requested baseline repair and validation. It supersedes earlier statements below where configuration repair or verification had not yet been performed.

### Completed

- Replaced the unsafe values in `Backend/.env.example` with explicit `CHANGE_ME` / `REPLACE` placeholders and removed the stray backtick. The template retains the existing configuration keys.
- Left the private `Backend/.env`, `application.yml`, and Flyway V1/V2 migrations untouched. Static checks confirmed that required private settings are non-empty, the database URL's database and port agree with `DB_NAME` and `DB_PORT`, and the JWT key is Base64 with at least 32 decoded bytes. Values were not printed.
- Confirmed `Backend/.gitignore` contains `.env` and `.env.*` ignore patterns and explicitly exempts `.env.example`. Since there is no Git repository, whether `.env` is tracked cannot be checked.
- Added an isolated local database validation runbook at `Backend/DEPLOYMENT.md` and updated `Backend/README.md` with guarded `.env` creation, build commands, and the runbook link.
- `docker compose config --quiet` passed.
- `.\mvnw.cmd clean verify` passed under Java 25: **14 tests, 0 failures, 0 errors, 0 skipped**. Maven printed HTTP 401 metadata warnings for two configured dependency feeds, but they did not prevent the build.
- `npm run build` passed: TypeScript check and Vite production build.

### Blocked database verification

- Docker CLI and Compose are installed, but Docker cannot connect to the Docker Desktop Linux engine. No local PostgreSQL listener or host `psql` / `pg_isready` tool was found, and a TCP connection to the configured database port failed.
- Consequently, no database was started or contacted; PostGIS availability, Flyway application of V1/V2, and Hibernate `ddl-auto: validate` against a live schema remain **unverified**. No SQL or migration was executed, and no existing volume was inspected, stopped, or removed.
- The new runbook uses a separate Compose project and alternate host port, so once Docker is available it can create an isolated volume without touching the existing Compose project's volume.

### Credential follow-up

The private `.env` database passwords match the non-placeholder sample values that were previously present in `.env.example`. Those values must be treated as exposed. They were not reproduced here and `.env` was deliberately not overwritten. After the database service is available, rotate both the application-role and local bootstrap-admin passwords, update only the private `.env`, and validate the new credentials. Do not copy the former template values into another environment.

### Git recovery

No original GitHub repository URL, remote, or trusted backup location was found in the inspected project documentation. Git metadata is still absent, so current tracked-file status and original history cannot be recovered from this workspace. Do not initialize Git in this working copy. Preserve a complete private backup (including `.env`, with restricted access) first; then locate/recover the original repository from a trusted account/backup. If none exists, initialize a new repository only in a separate verified copy, after excluding `.env`, generated files, and any local database artifacts.

## Workspace structure inspected

- `.github/modernize/`: Java-upgrade workflow metadata and hook scripts.
- `.vscode/`: workspace settings and tasks.
- `Backend/`: Maven Wrapper project, Java/Spring source, resources, tests, Docker Compose PostgreSQL/PostGIS setup, API documentation, environment files, and modernization assessment/plan artifacts.
- `Frontend/`: Vite/React/TypeScript project, application source, assets, package manifest/lockfile, and TypeScript/Vite configuration.
- No Android, Gradle, or Capacitor configuration was found.
- Generated/dependency directories (`Backend/target` and `Frontend/node_modules`) were excluded from source inventory. Backend Surefire reports and compiled classes were present under `target`.

### Application source organization

The backend has Spring packages for admin, auth, broker, common, config, document, enquiry, land, listing, and user concerns. It contains six test classes under `Backend/src/test`. The frontend has routes/pages for home, land exploration/details, account, legal notices, and not-found handling, with shared components/layout, demo listing service, and listing types.

## Findings

### 1. Git state and change recovery — **Blocker**

- `git status`, `git rev-parse`, and `git log` fail because no Git repository is present at the workspace root, `Backend`, or `Frontend`; no `.git` directory was found under the workspace.
- There is therefore no trustworthy way to distinguish committed work from interrupted/uncommitted work, inspect diffs, or identify the prior agent's last change. The November 13 timestamp on `Backend/.env.example` is the only notably recent source/configuration timestamp observed.
- Do not initialize a replacement repository over this copy or restore metadata by overwriting files. First preserve a complete copy, then recover the original repository metadata/remotes/history from a known-good clone or backup and compare before reconciling changes.

### 2. Completed and currently working changes

- The frontend TypeScript check and Vite production build succeed (`npm run build`).
- Backend build/test runs under OpenJDK 25.0.5 and Maven Wrapper 3.10.0. `mvnw.cmd -q test` exits successfully; the existing six test classes report **14 tests, 0 failures, 0 errors, 0 skipped**.
- `Backend/pom.xml` and `Backend/README.md` specify Java 25 and Spring Boot 4.1.2, matching the runtime available for this inspection.
- Backend configuration uses Flyway migrations with Hibernate `ddl-auto: validate`; the two existing migrations are `V1__enable_postgis.sql` and `V2__create_marketplace_schema.sql`.
- The existing backend README records the intended foundation and outstanding backend work. A Java 25 runtime-upgrade plan and task metadata also exist under `Backend/.github/modernize/java-25-runtime-upgrade/`. The source/build/docs currently reflect Java 25, but absent Git history and explicit task-result state mean the plan's execution history cannot be proven.

### 3. Incomplete product integration

- Frontend property discovery/details use illustrative in-memory records in `Frontend/src/services/landService.ts`; they are not connected to the backend. The UI explicitly labels this data as demonstration inventory.
- `Frontend/package.json` has no test or lint script. The frontend build is the only frontend validation available from the defined scripts.
- The backend README lists real PostgreSQL/PostGIS migration/schema validation, database-backed integration coverage, initial administrator provisioning, private media/document storage, and frontend API integration as pending.
- Existing backend tests do not establish that Flyway migrations apply cleanly to the configured PostgreSQL/PostGIS image or that Hibernate validation matches a live schema.

### 4. Malformed and sensitive environment template — **High priority**

- At initial inspection, `Backend/.env.example` had a trailing backtick and unsafe sample passwords. The template has now been repaired with unmistakable placeholders and no malformed lines.
- `Backend/.env` remains in place and was not modified. Its database password values match the former sample values; treat them as exposed and rotate them after database access is restored.
- `Backend/.gitignore` has rules for `.env` and `.env.*` and an exception for `.env.example`. Actual Git tracking cannot be verified without Git metadata.
- `application.yml` imports `.env` optionally. Database password and JWT secret have no useful secure default; configure them explicitly for local startup and deployment. Keep all secrets out of reports and source control.

### 5. Duplicate classes and source integrity

- A duplicate-name scan found `CreateRequest` in both `EnquiryDtos.java` and `ListingDtos.java`. These are nested records inside different outer DTO classes, not duplicate top-level declarations; this is valid and does not indicate a class collision.
- No other duplicate top-level Java declarations were detected by the scan.
- Frontend TypeScript compilation and backend Java compilation/tests found no malformed application source or compile failures.

### 6. Dependency and build configuration

- Frontend dependencies are declared in `Frontend/package.json` with a lockfile; the production build passes using the installed dependency tree.
- Backend Maven dependencies resolve sufficiently for the test lifecycle to pass. Maven emitted a warning that metadata retrieval from a configured Maven feed returned HTTP 401; it did not prevent the build/tests from completing. Investigate feed authentication if a clean machine or fresh dependency resolution fails.
- No missing dependency was demonstrated by the available builds. No Android/Capacitor or application runtime container build configuration was present.

### 7. Database, migrations, and data-loss risks

- `application.yml`, `.env.example`, and `docker-compose.yml` use corresponding `DB_URL`, `DB_NAME`, `DB_USER`, and `DB_PASSWORD` settings. Compose additionally uses PostgreSQL bootstrap credentials and `DB_PORT` for the host port. Static checks confirmed the configured URL database and port match `DB_NAME` and `DB_PORT`.
- Flyway is enabled, points at `classpath:db/migration`, and validates migrations. The current schema changes are additive creation of the PostGIS extension and marketplace tables/indexes/constraints; no `DROP`, `TRUNCATE`, or destructive data rewrite was found in the two migrations.
- **No migration was executed and no production or local database was contacted.** Docker Desktop's Linux engine is unavailable and the configured database port does not accept a TCP connection. Live PostGIS, Flyway, and Hibernate validation remain unverified.
- Compose persists PostgreSQL data in the named volume `bhoomi-postgres-data`. Removing that volume (for example, using a volume-deleting Compose command) can permanently remove local database contents. No volume deletion or SQL mutation was performed during recovery.
- Do not edit or replace either existing migration. If future schema correction is needed after a migration has been applied, add a new forward migration and test it against a disposable database first.

### 8. Validation results and limitations

| Check | Result |
|---|---|
| Frontend `npm run build` | Passed: TypeScript check and Vite production bundle |
| Backend `.\mvnw.cmd clean verify` | Passed: 14 tests, no failures/errors/skips |
| Backend runtime | OpenJDK 25.0.5; Maven Wrapper 3.10.0 |
| `docker compose config --quiet` | Passed |
| Environment template validation | Passed: no malformed lines/backtick; password/JWT placeholders present |
| Environment consistency | Passed: required values non-empty; database URL name/port agree; JWT is valid-length Base64 |
| `.env` Git exclusion | Ignore rules present; tracked status unverifiable without Git metadata |
| Git status / recent commits | Unavailable: no repository metadata |
| PostgreSQL/PostGIS migration and Hibernate validation | Blocked: Docker Linux engine unavailable; configured DB port unreachable |
| Frontend automated tests | Not configured in package scripts |
| Android/Capacitor | Not present |

The first Maven invocation also completed successfully but reported the Maven-feed metadata 401 warning noted above. The successful test run relied on dependencies already available to Maven.

## Recommended recovery sequence

1. Preserve this workspace as-is before any restoration or cleanup. Recover the original Git repository/remote/branch metadata from a trusted source, compare files and history, and reconcile rather than overwrite local files.
2. Keep the repaired `Backend/.env.example` placeholders; do not copy them as credentials. Preserve `Backend/.env` privately and rotate its former sample database passwords when the database service is available.
3. Confirm the Java 25 modernization task's completion and the dependent dependency/CVE task from the original project history or task-run artifacts; do not infer completion solely from current files.
4. Start Docker Desktop, follow `Backend/DEPLOYMENT.md` to validate PostGIS, Flyway, and Hibernate against the isolated database project, and preserve the existing named data volume.
5. Add database-backed integration tests and frontend API integration only after the recovered baseline is confirmed. These are roadmap items, not changes made during this recovery.

## Recovery boundary

For the recovery-only inspection, the report was the only source-tree addition. In this Phase 1 follow-up, only `Backend/.env.example`, `Backend/README.md`, this report, and the new `Backend/DEPLOYMENT.md` were edited. No application source, `application.yml`, private `.env`, existing migration, or database was changed. The frontend build emitted its standard generated output under `Frontend/dist`.
