# OpenRecords

> A fullstack portfolio project by **CahRock** — Federal FOIA Request Management Portal demonstrating Angular 18, Spring Boot, JWT authentication, async email, and AWS deployment.

A FOIA (Freedom of Information Act) Request Management Portal demonstrating modern federal-grade full-stack development.

Submitted as a portfolio project for federal full-stack developer roles emphasizing Angular, Java, RESTful microservices, AWS readiness, and Section 508 accessibility.

## What's Built

A working two-sided portal with end-to-end data flow:

**Authentication** — JWT-based authentication with bcrypt password hashing (cost factor 12), email verification flow with one-time tokens, refresh tokens for session continuity, and route guards enforcing role-based access. Server-side authorization scopes prevent horizontal privilege escalation — REQUESTERs can only see and access their own filings.

**Email notifications** — Async email dispatch via Spring Mail with a dedicated thread pool. HTML emails rendered from Thymeleaf templates with shared header/footer fragments. MailHog runs in Docker for development; SMTP host swappable via env vars for production. Currently sends three email types: account verification, welcome, and status change notifications (six workflow transitions).

**Citizen portal** — register an account, verify email, file FOIA requests, track status across an 11-state workflow (DRAFT → SUBMITTED → ACKNOWLEDGED → ASSIGNED → UNDER_REVIEW → ... → CLOSED), and view request details with full audit history.

**Staff console** — role-aware queue with multi-filter triage (status, assignment, search), action panel for assigning requests and transitioning statuses, with live audit trail showing every change.

**Backend API** — RESTful endpoints with RFC 7807 ProblemDetail error responses, JPA Specifications for filtered queries, optimistic locking, audit-trail history, and 20-business-day SLA due-date calculation.

**Database** — Schema managed via Flyway migrations (V1–V6), enforced state-machine via CHECK constraints, partial indexes for staff queue queries.

## Screenshots

### Authentication
![Login](docs/screenshots/login-page.png)
![Registration](docs/screenshots/register-page.png)

### Staff Console
![Request detail with staff actions and audit trail](docs/screenshots/request-detail-staff.png)

### Citizen portal
![My Requests](docs/screenshots/my-requests.png)

### Staff queue with filters
![Staff queue](docs/screenshots/staff-queue.png)

### Detail page with staff actions and audit trail
![Request detail](docs/screenshots/request-detail-staff.png)

## Architecture Highlights

- **JWT authentication with refresh tokens** — access tokens (15 min) for API requests; refresh tokens (30 days) stored in `refresh_tokens` table for revocability. Authorization header injection handled by HTTP interceptor.
- **Bcrypt password hashing** — cost factor 12 (~300ms per hash) defeats brute-force attacks. Generic "invalid credentials" error message prevents account enumeration.
- **Email verification flow** — single-use tokens with 24-hour expiration. Idempotent verification handles duplicate clicks gracefully.
- **Async email pipeline** — `@Async` thread pool isolates SMTP latency from request threads. Email sending failures are logged but never block user actions. Templates use Thymeleaf with shared layout fragments for consistency across all email types.
- **Defense-in-depth authorization** — REQUESTER role scoping is enforced at the service layer, not the controller. Query parameters can't bypass it. Detail endpoints return 404 (not 403) on unauthorized access to avoid leaking the existence of resources.
- **Route guards with redirect preservation** — unauthenticated users requesting `/staff/queue` are redirected to login, then routed back to their original destination after sign-in.
- **State-machine domain model** — `FoiaRequestStatus` with allowed-transition rules enforced in service layer; invalid transitions return HTTP 422 with structured error.
- **Audit trail** — every status change writes a `foia_request_status_history` row in the same transaction as the status update, ensuring consistency.
- **RFC 7807 error responses** — global exception handler returns standardized ProblemDetail JSON for 400/404/422/500, never leaking stack traces.
- **20-business-day SLA calculation** — `BusinessDayCalculator` skips weekends and US federal holidays per 5 U.S.C. § 6103.
- **Filtered queue queries** — JPA Specifications compose dynamic WHERE clauses for staff filtering by status, assignee, due-date proximity, and free-text search.
- **Configuration via environment variables** — no secrets in source; dev defaults make the project clone-and-run.
- **Layered architecture** — Controller → Service (transactional) → Repository → Entity, with mappers separating wire format (DTO) from storage (entity).

## Stack

| Layer | Technologies |
|---|---|
| Frontend | Angular 18, TypeScript, SCSS, Reactive Forms, Signals |
| Backend | Java 21, Spring Boot 3.5, Spring Data JPA, Spring Validation |
| Database | PostgreSQL 16, Flyway, JPA Specifications |
| Infrastructure | Docker Compose, environment-based config |
| Tooling | Maven 3.9, npm, Logback rolling file logs |

## Repository Structure
openrecords/
├── openrecords-api/        # Spring Boot backend (Java 21 + Maven)
├── openrecords-web/        # Angular 18 frontend
├── openrecords-infra/      # docker-compose.yml (Postgres + pgAdmin)
└── docs/                   # Architecture notes

## Prerequisites

Java 21, Maven 3.9+, Node.js 20 LTS, Angular CLI 18, Docker Desktop.

## Local Development

```bash
# 1. Start Postgres + pgAdmin
cd openrecords-infra
cp .env.example .env       # one-time setup
docker compose up -d

# 2. Run backend (separate terminal)
cd openrecords-api
./mvnw spring-boot:run     # http://localhost:8080

# 3. Run frontend (separate terminal)
cd openrecords-web
npm start                  # http://localhost:4200
```

### MailHog (development email)

Outgoing emails are captured by MailHog (dev SMTP server). View them at:
- **MailHog UI:** http://localhost:8025
- **SMTP listener:** localhost:1025

No real emails are sent in development. To swap to a real SMTP provider in production, override `OPENRECORDS_SMTP_HOST`, `OPENRECORDS_SMTP_PORT`, `OPENRECORDS_SMTP_USERNAME`, `OPENRECORDS_SMTP_PASSWORD`, and `OPENRECORDS_SMTP_AUTH` environment variables.

### Demo data

Seeded test users (BCrypt password: `password123`, all pre-verified):

| Role | Email | Use |
|---|---|---|
| REQUESTER | testuser@example.com | Files FOIA requests via citizen portal |
| STAFF | intake.officer@example.com | Triage and acknowledgement |
| STAFF | case.officer@example.com | Records review |

New accounts created via `/register` start unverified. Verification links are logged to the Spring Boot console (Phase 7 will wire real email sending).

The Angular UI currently runs as the test requester (auth/JWT planned for Phase 6).

## Environment Variables

Sensible defaults exist for all values. Override only for production or custom dev setups.

### Backend (Spring Boot)

| Variable | Default | Purpose |
|---|---|---|
| `OPENRECORDS_DB_URL` | `jdbc:postgresql://localhost:5432/openrecords` | JDBC connection |
| `OPENRECORDS_DB_USER` | `openrecords` | DB user |
| `OPENRECORDS_DB_PASSWORD` | `openrecords_dev_password` | DB password |
| `OPENRECORDS_ADMIN_USER` | `admin` | Bootstrap admin |
| `OPENRECORDS_ADMIN_PASSWORD` | `admin` | Bootstrap admin password |

### Infrastructure (Docker Compose)

| Variable | Default | Purpose |
|---|---|---|
| `POSTGRES_DB` | `openrecords` | Database name |
| `POSTGRES_USER` | `openrecords` | Postgres superuser |
| `POSTGRES_PASSWORD` | `openrecords_dev_password` | Postgres password |
| `PGADMIN_DEFAULT_EMAIL` | `admin@example.com` | pgAdmin login |
| `PGADMIN_DEFAULT_PASSWORD` | `admin` | pgAdmin password |

### Production Deployment

Never commit production credentials. Inject via:

- AWS ECS task definitions referencing Secrets Manager
- Kubernetes Secrets
- HashiCorp Vault

Rotate passwords on a regular schedule.

## Roadmap

| Phase | Status | What |
|---|---|---|
| Phase 1: Infrastructure | ✅ Done | Docker Compose, Postgres, pgAdmin |
| Phase 2: Backend scaffold | ✅ Done | Spring Boot 3.5, Flyway, health endpoint |
| Phase 3: Frontend scaffold | ✅ Done | Angular 18 with proxy and health check |
| Phase 4a: FOIA CRUD | ✅ Done | Create + list + retrieve API endpoints |
| Phase 4b: Angular UI | ✅ Done | Form, list, and detail pages |
| Phase 5: Staff console | ✅ Done | State machine, assignment, SLA, queue filtering, audit timeline |
| Phase 6: Authentication | ✅ Done | JWT, bcrypt, registration, email verification, route guards |
| Phase 7: Notifications | ✅ Done | Async email pipeline, Thymeleaf templates, MailHog (dev) |
| Phase 8: Deployment | Planned | AWS ECS + RDS + S3, HttpOnly cookies, silent token refresh |

## Screenshots

### Authentication
![Login page](docs/screenshots/login-page.png)
![Registration form with live password complexity feedback](docs/screenshots/register-page.png)

### Email Notifications
![MailHog inbox showing the three email types](docs/screenshots/mailhog-inbox.png)
![Verification email rendered HTML](docs/screenshots/email-verification.png)
![Status change email with embedded request card](docs/screenshots/email-status-change.png)

### Staff Console
![Request detail with staff actions and audit trail](docs/screenshots/request-detail-staff.png)

## License

MIT — see [LICENSE](LICENSE).