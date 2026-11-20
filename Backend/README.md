# Bhoomi backend

Bhoomi is a Java 25 / Spring Boot 4.1.2 modular monolith backed by PostgreSQL
18 and PostGIS 3.6. Flyway owns schema changes; Hibernate runs in
`ddl-auto: validate` mode. The service treats listing and parcel details as
broker-submitted information, not government records or proof of legal title.

## Requirements

- JDK 25
- Docker Desktop with the Linux container engine (or PostgreSQL 18 with PostGIS
  3.6 already installed)
- Windows PowerShell, Ubuntu on WSL2, or another POSIX shell

The Maven Wrapper is included. No Maven installation is needed.

## Local database with Docker

From `Backend`, create a private environment file only if one does not already
exist. Replace every `CHANGE_ME` and `REPLACE` placeholder with unique local
credentials and a generated JWT signing key. Do not overwrite or commit an
existing `.env`.

```powershell
if (-not (Test-Path .env)) { Copy-Item .env.example .env } else { throw ".env already exists; preserve it and edit it deliberately." }
notepad .env
```

Generate `JWT_SECRET` as at least 32 cryptographically random bytes encoded in
Base64. For example, in WSL:

```sh
openssl rand -base64 32
```

Start the database. The image provisions PostGIS and creates `bhoomi_db` plus
the least-privilege application role `bhoomi_user`.

```powershell
docker compose up -d --build db
docker compose ps
```

The Compose service uses a persistent named volume. Do not use
`docker compose down --volumes` / `docker compose down -v`; those options delete
database data. For an isolated migration validation that leaves the existing
Compose volume untouched, follow [DEPLOYMENT.md](./DEPLOYMENT.md).

Spring Boot reads the ignored `.env` file when present. Run the backend:

```powershell
.\mvnw.cmd clean verify
.\mvnw.cmd spring-boot:run
```

Flyway applies `V5__add_password_reset_support.sql` after the existing V1–V4
migrations. Check
liveness at `http://localhost:8080/api/health`.
The health endpoint intentionally does not report database details.

## Workspace verification

Run the backend build and tests from `Backend`:

```powershell
.\mvnw.cmd clean verify
```

Run the frontend type-check and production build from `Frontend`:

```powershell
Set-Location ..\Frontend
npm run build
```

For read-only PostGIS checks and applying Flyway/Hibernate validation to a new,
isolated local database while retaining the existing Compose volume, follow
[DEPLOYMENT.md](./DEPLOYMENT.md).

### Ubuntu on WSL2

Run these commands from the backend directory inside WSL. Docker Desktop must
have WSL integration enabled for the selected distro.

```sh
if [ ! -e .env ]; then cp .env.example .env; else printf '%s\n' '.env already exists; preserve it and edit it deliberately.'; exit 1; fi
chmod 600 .env
nano .env
docker compose up -d --build db
./mvnw clean verify
./mvnw spring-boot:run
```

Do not source an untrusted `.env` file: shell sourcing executes its contents.

## Existing PostgreSQL server

Using a database administrator account, create the application role and
database, set the role password interactively, and enable PostGIS:

```sql
CREATE ROLE bhoomi_user LOGIN;
\password bhoomi_user
CREATE DATABASE bhoomi_db OWNER bhoomi_user;
\connect bhoomi_db
CREATE EXTENSION postgis;
```

Set `DB_URL=jdbc:postgresql://127.0.0.1:5432/bhoomi_db`,
`DB_NAME=bhoomi_db`, `DB_USER=bhoomi_user`, and `DB_PASSWORD` in the
application environment. The database role needs schema creation privileges
for Flyway on first startup; the database owner has them by default.

To confirm the server has the requested extensions and versions:

```sql
SELECT version();
SELECT postgis_full_version();
```

## Configuration

See [.env.example](./.env.example). Important values:

| Variable | Purpose |
| --- | --- |
| `DB_URL` | JDBC URL for the application database |
| `DB_NAME`, `DB_USER`, `DB_PASSWORD` | Database name and application credentials |
| `DB_PORT` | Host port exposed by the local Compose service (5432 by default) |
| `POSTGRES_ADMIN_USER`, `POSTGRES_ADMIN_PASSWORD` | Local container bootstrap administrator |
| `SERVER_PORT` | HTTP port (8080 by default) |
| `CORS_ALLOWED_ORIGINS` | Comma-separated, exact browser origins; no wildcard |
| `JWT_SECRET` | Base64-encoded random key; at least 32 decoded bytes |
| `JWT_ISSUER`, `JWT_EXPIRATION` | Token issuer and ISO-8601 lifetime |
| `PASSWORD_RESET_EXPIRATION` | Password reset token lifetime (15 minutes by default) |
| `FRONTEND_BASE_URL` | Frontend origin used in recovery email links |
| `BHOOMI_EMAIL_ENABLED` | Explicit opt-in to SMTP password reset email delivery |
| `SMTP_HOST`, `SMTP_PORT`, `SMTP_USERNAME`, `SMTP_PASSWORD`, `SMTP_FROM` | SMTP host, credentials, and verified sender |
| `SMTP_AUTH`, `SMTP_STARTTLS` | SMTP authentication and STARTTLS settings |
| `BHOOMI_DOCUMENT_STORAGE_PATH` | Private local document directory; keep outside static web roots |

Production deployments must inject their own secrets and exact trusted origins.
Never reuse the local environment or expose the database port publicly.
Registration only supports buyer and broker accounts; there is no public admin
registration. A trusted operator must provision the first admin account with
a BCrypt hash through a controlled database administration process. Account
roles must never be changed based on public request data.

## API

The initial endpoint contracts, request examples, filters, and error response
shape are in [docs/API.md](./docs/API.md). Authentication uses stateless
`Authorization: Bearer <token>` JWTs. Public listing discovery returns only
published listings. Broker document storage keys and private documents are not
exposed by public endpoints.

## Database backup and restore

Use a protected backup directory and keep credentials out of command history.
From WSL, with the Compose database running:

```sh
umask 077
docker compose exec -T db pg_dump -U "$POSTGRES_ADMIN_USER" -Fc "$DB_NAME" \
  > "bhoomi-$(date +%Y%m%d-%H%M%S).dump"
```

Restore into a separately verified target database (the `--clean` options are
destructive to objects in that target):

```sh
docker compose exec -T db pg_restore -U "$POSTGRES_ADMIN_USER" \
  --clean --if-exists --dbname "$DB_NAME" < bhoomi-backup.dump
```

Store backups encrypted with access controls and test restores regularly.

## Development roadmap

**Implemented foundation and workflows**

- Environment-backed PostgreSQL configuration, Flyway schema, PostGIS parcel
  geometry, UUID keys, timestamps, integrity constraints, and indexes.
- Buyer/broker registration and login, BCrypt password hashes, stateless
  signed JWTs, explicit CORS origins, role-protected APIs.
- Password recovery using expiring hashed, single-use tokens, SMTP email
  configuration, password-change JWT invalidation, and per-process throttling.
- Buyer profile, favorites, enquiry history, site-visit scheduling/history,
  public published-only discovery with filters/sort/pagination, and details.
- Broker profile, draft/edit/submit listing flows, enquiries, site visits,
  availability updates, private document upload/download, and listing image
  upload with published-only public image access.
- Admin broker and listing moderation queues and document review/download.
- RFC 9457-style request errors, broker/admin audit events, private document
  storage abstraction, metadata/provenance, and review state.

**Pending**

- Run Flyway and Hibernate schema validation against the actual PostgreSQL 18 /
  PostGIS 3.6 database.
- Configure and verify real SMTP delivery and exercise password recovery
  against a database-backed deployment before claiming end-to-end availability.
- Provision an initial admin using an operator-managed BCrypt credential and
  exercise all APIs with real persisted records.
- Configure production-grade private/object storage, malware scanning,
  retention, backups, monitoring, and rate limiting.
- Complete account administration, complaint handling, duplicate review,
  audit-log UI, map integration, and asking-price history.
- Validate the V4 migration and current Hibernate schema against PostgreSQL 18
  with PostGIS; include latitude/longitude and private media storage in runtime
  tests.
- Add browser-level and database-backed journey tests; no runtime E2E pass is
  claimed until PostgreSQL/PostGIS is available.

**Deferred**

- Government data integrations (only after the required authorization and
  official API contracts are verified).
- Legal title determinations, land-record certification, transaction-price
  assertions, and automated ownership claims. This platform does not provide
  these services.
- Production hosting, secret management, observability/alerting, and backup
  automation.

The frontend uses real listing/auth/favorite/enquiry/site-visit APIs. Marketing
content and the image fallback remain illustrative; no mock records are used
to claim live inventory. No competitor scraping or fabricated land records
are used by the backend.
