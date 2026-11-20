# Local deployment and database validation

This runbook describes a local, disposable PostgreSQL/PostGIS validation
environment. It is not a production deployment recipe. The workspace has no
configured production target. Never use a disposable setup against a production
database.

## Prerequisites

- JDK 25
- Docker Desktop with the Linux container engine running
- Node.js and npm for the frontend build

## Prepare local credentials

Run from `Backend` in PowerShell. The guard deliberately refuses to overwrite
an existing `.env`; keep its current values and edit that file only when
necessary. `.env` is listed in `.gitignore`.

```powershell
Set-Location C:\Users\moham\Bhoomi\Backend
if (-not (Test-Path .env)) { Copy-Item .env.example .env } else { throw ".env already exists; preserve it." }
notepad .env
```

Set unique local database passwords and a JWT secret generated with:

```powershell
openssl rand -base64 32
```

Do not paste credentials into source files, command history, reports, or chat.
Never use `.env.example` placeholder values as credentials.

## Start an isolated validation database

The separate Compose project and alternate host port keep this database
separate from the regular `Backend` Compose project and its named volume. Do
not add `--volumes` / `-v` to a Compose `down` command.

```powershell
$env:DB_PORT = '55432'
$env:DB_URL = 'jdbc:postgresql://127.0.0.1:55432/bhoomi_db'
docker compose -p bhoomi-phase1-validation up -d --build db
docker compose -p bhoomi-phase1-validation ps
```

Confirm PostGIS is installed. This is a read-only query:

```powershell
$lines = Get-Content .env
$adminUser = (($lines | Where-Object { $_ -match '^POSTGRES_ADMIN_USER=' }) -split '=', 2)[1]
$dbName = (($lines | Where-Object { $_ -match '^DB_NAME=' }) -split '=', 2)[1]
docker compose -p bhoomi-phase1-validation exec -T db psql -U $adminUser -d $dbName -c "SELECT extname, extversion FROM pg_extension WHERE extname = 'postgis';"
```

## Apply Flyway and validate Hibernate

In the same PowerShell session with `DB_URL` set as above, build and test:

```powershell
.\mvnw.cmd clean verify
```

Then start the backend:

```powershell
.\mvnw.cmd spring-boot:run
```

Successful application startup is the check that Flyway applied the existing
migrations and Hibernate completed `ddl-auto: validate`. Leave this process
running while checking the schema from a second PowerShell window in `Backend`:

```powershell
$lines = Get-Content .env
$adminUser = (($lines | Where-Object { $_ -match '^POSTGRES_ADMIN_USER=' }) -split '=', 2)[1]
$dbName = (($lines | Where-Object { $_ -match '^DB_NAME=' }) -split '=', 2)[1]
docker compose -p bhoomi-phase1-validation exec -T db psql -U $adminUser -d $dbName -c "SELECT version, success FROM flyway_schema_history ORDER BY installed_rank;"
```

The expected history includes successful entries for V1 through V4. Do not
edit applied migrations. If a genuine correction is needed after application,
add a new forward-only migration and validate it against a fresh disposable
database.

Check backend liveness:

```powershell
Invoke-WebRequest -UseBasicParsing http://localhost:8080/api/health
```

Stop the backend with `Ctrl+C`. Stop (but do not remove) the disposable
database container when finished:

```powershell
docker compose -p bhoomi-phase1-validation stop db
```

The isolated Compose project uses a separate named volume; stopping the
container preserves it. Keep the existing default Compose project and its
volume untouched.

## Verify the frontend

In another PowerShell window:

```powershell
Set-Location C:\Users\moham\Bhoomi\Frontend
npm run build
```

This command runs the strict TypeScript check followed by the Vite production
build. The public listing discovery UI uses published backend listings; a
database-backed browser journey still requires the isolated backend/database
setup above.

Private uploaded documents and listing images use `BHOOMI_DOCUMENT_STORAGE_PATH`. Keep the
directory outside static web roots and restrict filesystem access to the
backend service account. The default local filesystem adapter is not a
production object-storage, malware-scanning, or retention solution.

## Secret and data handling

- Keep `Backend/.env` private and preserve it when preparing local setup.
- The `.env` ignore rule prevents adding it to a future Git working tree, but
  because this workspace currently has no Git metadata, tracked-file status
  cannot be confirmed here.
- Never run `docker compose down --volumes` / `docker compose down -v` on the
  existing project; that deletes its database volume.
- The Flyway/Hibernate procedure above writes only to the isolated validation
  database. Do not point it at production or an existing data volume.
