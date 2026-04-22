# OpenRecords

A modern FOIA (Freedom of Information Act) request management portal — federal modernization portfolio project.

## Stack

- **Frontend:** Angular 18, TypeScript, SCSS, Angular Material, NgRx
- **Backend:** Java 21, Spring Boot 3.3, Spring Security + JWT, Spring Data JPA, Flyway
- **Database:** PostgreSQL 16
- **Infrastructure:** Docker Compose (dev), AWS-ready (S3/RDS/ECS/CloudFront)

## Repository structure

```
openrecords/
├── openrecords-api/        # Spring Boot backend (Java 21 + Maven)
├── openrecords-web/        # Angular 18 frontend
├── openrecords-infra/      # docker-compose.yml (Postgres + pgAdmin)
└── docs/                   # Architecture notes, API specs
```

## Prerequisites

- Java 21, Maven 3.9+, Node.js 20 LTS, Angular CLI 18, Docker Desktop

## Local development

```bash
# 1. Start infrastructure (Postgres + pgAdmin)
cd openrecords-infra
docker compose up -d

# 2. Run backend (in another terminal)
cd openrecords-api
./mvnw spring-boot:run

# 3. Run frontend (in another terminal)
cd openrecords-web
npm start
```

## Features (planned)

- Citizen portal: submit FOIA requests, track status, download released documents
- Agency console: triage queue, assignment, status workflow, SLA tracking
- Document upload + release pipeline
- Message thread between requester and case officer
- Section 508 accessibility compliance
- Full REST API (OpenAPI spec auto-generated)

## License

MIT