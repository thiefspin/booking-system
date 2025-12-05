# Docker Setup for Booking System

This document provides instructions for running the complete booking system stack using Docker Compose.

## Architecture

The Docker Compose setup includes three services:
- **PostgreSQL Database**: Stores application data
- **Spring Boot Backend**: REST API server (Java 21)
- **Angular Frontend**: Web UI served by Nginx

All services communicate through a Docker bridge network, ensuring isolated and secure communication.

## Prerequisites

- Docker Desktop (or Docker Engine + Docker Compose)
- At least 8GB of free RAM
- Ports 80, 8090, and 5432 available on the host machine

## Quick Start

### 1. Clone the repository (if not already done)
```bash
git clone git@github.com:thiefspin/booking-system.git
cd booking-system
```

### 2. Copy environment variables
```bash
cp .env.example .env
```
Review and modify `.env` if needed (default values should work out of the box).

### 3. Start all services
```bash
./docker-compose.sh start
```

Or using Docker Compose directly:
```bash
docker-compose up -d
```

### 4. Access the application
- **Frontend**: http://localhost
- **Backend API**: http://localhost:8090
- **PostgreSQL**: localhost:5432 (username: postgres, password: secret)

## Management Script

The `docker-compose.sh` script provides convenient commands for managing the stack:

```bash
./docker-compose.sh <command>
```

Available commands:
- `build` - Build all Docker images
- `start` - Start all services
- `stop` - Stop all services
- `restart` - Restart all services
- `rebuild` - Rebuild images and restart services
- `status` - Show service status
- `logs [service]` - View logs (all or specific service)
- `exec <service> <cmd>` - Execute command in a service
- `clean` - Remove containers, networks, and volumes
- `migrate` - Run database migrations manually
- `psql` - Access PostgreSQL CLI
- `backup` - Create database backup
- `restore <file>` - Restore database from backup

### Examples

View backend logs:
```bash
./docker-compose.sh logs backend
```

Access backend shell:
```bash
./docker-compose.sh exec backend sh
```

Create database backup:
```bash
./docker-compose.sh backup
```

## Service Details

### PostgreSQL Database
- **Image**: postgres:16-alpine
- **Port**: 5432
- **Database**: booking_system
- **Credentials**: postgres/secret
- **Data persistence**: Uses named volume `booking-system-postgres-data`

### Spring Boot Backend
- **Build**: Multi-stage Dockerfile with Gradle 8.5 and JDK 21
- **Port**: 8090
- **Features**:
  - Flyway migrations run automatically on startup
  - Health check endpoint at `/actuator/health`
  - Connects to PostgreSQL using internal Docker network

### Angular Frontend
- **Build**: Multi-stage Dockerfile with Node.js 20 and Nginx
- **Port**: 80
- **Features**:
  - Production build with optimizations
  - Nginx reverse proxy for API calls to backend
  - Angular routing support with HTML5 push state

## Development Workflow

### Making changes to the backend
1. Modify your Java code
2. Rebuild and restart:
   ```bash
   ./docker-compose.sh rebuild
   ```

### Making changes to the frontend
1. Modify your Angular code
2. Rebuild and restart:
   ```bash
   ./docker-compose.sh rebuild
   ```

### Database migrations
Flyway migrations are automatically applied when the backend starts. To run them manually:
```bash
./docker-compose.sh migrate
```

## Networking

All services are connected through a custom bridge network `booking-system-network`:
- Services can communicate using container names as hostnames
- Frontend proxies API requests from `/api` to `backend:8090`
- Backend connects to database at `postgres:5432`
- Only exposed ports are accessible from the host

## Data Persistence

- PostgreSQL data is persisted in a named Docker volume
- To backup data: `./docker-compose.sh backup`
- To restore data: `./docker-compose.sh restore <backup_file>`
- To completely reset (WARNING: deletes all data): `./docker-compose.sh clean`

## Troubleshooting

### Services won't start
Check if required ports are available:
```bash
lsof -i :80 -i :8090 -i :5432
```

### Backend can't connect to database
Ensure PostgreSQL is healthy:
```bash
docker-compose ps postgres
docker-compose logs postgres
```

### Frontend can't reach backend
Check if backend is running and healthy:
```bash
docker-compose logs backend
curl http://localhost:8090/actuator/health
```

### View real-time logs
```bash
# All services
docker-compose logs -f

# Specific service
docker-compose logs -f backend
```

### Reset everything
```bash
./docker-compose.sh clean
./docker-compose.sh start
```

## Environment Variables

Key environment variables (defined in `.env`):
- `POSTGRES_URL`: Database jdbc url (default: jdbc:postgresql://postgres:5432/booking_system)
- `POSTGRES_USER`: Database user (default: postgres)
- `POSTGRES_PASSWORD`: Database password (default: secret)
- `BACKEND_PORT`: Backend port (default: 8090)
- `FRONTEND_PORT`: Frontend port (default: 80)

## Support

For issues or questions:
1. Check the logs: `./docker-compose.sh logs`
2. Verify service health: `./docker-compose.sh status`
3. Ensure Docker is running: `docker info`
4. Review this documentation
5. Check the main README.md for application-specific details
