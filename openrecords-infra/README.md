# OpenRecords Infrastructure

Local development infrastructure: PostgreSQL 16 + pgAdmin 4.

## Quick start

```bash
docker compose up -d
```

## Services

| Service | URL | Credentials |
|---------|-----|-------------|
| PostgreSQL | `localhost:5432` | user: `openrecords` / pw: `openrecords_dev_password` / db: `openrecords` |
| pgAdmin | http://localhost:5050 | `admin@example.com` / `admin` |

## Common commands

```bash
# Start all services in background
docker compose up -d

# View logs
docker compose logs -f

# Stop services (keep data)
docker compose down

# Stop services AND wipe data
docker compose down -v

# Restart Postgres only
docker compose restart postgres
```

## Connect pgAdmin to Postgres

First time opening pgAdmin, register the Postgres server:

1. Open http://localhost:5050 → log in with `admin@openrecords.local` / `admin`
2. Right-click "Servers" → "Register" → "Server..."
3. **General tab** → Name: `OpenRecords Local`
4. **Connection tab**:
   - Host: `postgres` (the Docker service name, not `localhost`)
   - Port: `5432`
   - Maintenance database: `openrecords`
   - Username: `openrecords`
   - Password: `openrecords_dev_password`
5. Click Save