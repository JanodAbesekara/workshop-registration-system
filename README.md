# Workshop Registration Service

Spring Boot 4.1 (Java 17) + MySQL backend, React 19 + Vite + Redux Toolkit frontend.
Built for a community training centre: front desk registers attendees, a programme manager runs the
workshop catalogue, an administrator manages staff accounts. **A workshop can never hold more active
registrations than its capacity**, even when many people register at the same moment.

## Run it locally

Requirements: **Java 17+**, **Maven 3.9+**, **Node 22+**, **MySQL 8+** running on `localhost:3306`.

### 1. Backend (http://localhost:8080)

```bash
cd backend
mvn spring-boot:run
```

Defaults (`src/main/resources/application.properties`) point at
`jdbc:mysql://localhost:3306/AssignmentDB` with user `root`. The database is created automatically
(`createDatabaseIfNotExist=true`) and tables are created/updated by Hibernate (`ddl-auto=update`).
Override anything with environment variables: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `CORS_ORIGINS`.

### 2. Frontend (http://localhost:5173)

```bash
cd frontend
npm install
npm run dev
```

Vite proxies `/api` to `localhost:8080`, so no extra configuration is needed.

### 3. Sign in (dev-only seeded accounts)

| Role | Email | Password |
|---|---|---|
| Admin | `admin@trainingcentre.lk` | `Admin@12345` |
| Manager | `manager@trainingcentre.lk` | `Manager@12345` |
| Staff (front desk) | `staff@trainingcentre.lk` | `Staff@12345` |

The first Admin is seeded; there is no public signup. Demo workshops are seeded on first start
(one almost full, one full, one cancelled, one completed, a few open). Set `SEED_DEMO=false` to seed only the Admin.

### Docker alternative

`docker compose up --build` then open http://localhost:8081 (set `DB_PASSWORD` and `JWT_SECRET` first for anything real).

## Permissions (enforced on the backend, deny-by-default, see `SecurityConfig`)

| Action | Admin | Manager | Staff |
|---|---|---|---|
| Create accounts / set roles / reset passwords / view audit trail | yes | no | no |
| Add & edit workshops | no | yes | no |
| Register & cancel attendees | no | yes | yes |
| View workshops, registrations & history | no | yes | yes |

Unlisted endpoints return 403. The role is read from the database on every request, so disabling an
account or changing a role takes effect immediately, even for tokens already issued.

## API

| Method & path | Who | Notes |
|---|---|---|
| `POST /api/auth/login` | public | returns `{token, expiresAt, user}`; 5 failures lock the email for 15 min |
| `GET /api/auth/me` | any signed-in | |
| `GET/POST /api/users`, `PUT /api/users/{id}`, `POST /api/users/{id}/reset-password` | Admin | cannot change/disable yourself |
| `GET /api/audit?entityType=USER\|WORKSHOP&page=` | Admin | audit trail (bonus) |
| `GET /api/workshops?from&to&status&hasSeats&q&page&size&sort` | Manager, Staff | `from`/`to` ISO instants on start time; `hasSeats=true` = seats still free |
| `GET /api/workshops/{id}` | Manager, Staff | |
| `POST /api/workshops`, `PUT /api/workshops/{id}` | Manager | |
| `GET /api/workshops/{id}/registrations?status=` | Manager, Staff | full history incl. cancelled |
| `POST /api/workshops/{id}/registrations` | Manager, Staff | `409` when full / duplicate / closed |
| `POST /api/workshops/{id}/registrations/{rid}/cancel` | Manager, Staff | frees the seat, keeps the record |

Errors are always `{timestamp, status, error, message, fieldErrors}`.

## Prove the capacity rule

```bash
cd backend && mvn test
```

`CapacityRuleTest` fires 40 simultaneous registrations at a 15-seat workshop and asserts exactly 15
succeed, 25 are rejected, and the stored count is 15. It uses an in-memory H2 database in MySQL mode.

## Layout

```
backend/   Spring Boot (domain, repo, security, service, web, config, exception)
frontend/  React + Vite + Redux Toolkit (store/ slices, pages/, components/)
DESIGN.md  one-page write-up: decisions, trade-offs, assumptions, what was skipped
```
