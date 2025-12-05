#!/bin/bash

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd "$SCRIPT_DIR"

print_message() {
    echo -e "${2}${1}${NC}"
}

check_docker() {
    if ! docker info > /dev/null 2>&1; then
        print_message "Docker is not running. Please start Docker first." "$RED"
        exit 1
    fi
}

check_env() {
    if [ ! -f ".env" ]; then
        if [ -f ".env.example" ]; then
            print_message "No .env file found. Creating from .env.example..." "$YELLOW"
            cp .env.example .env
            print_message ".env file created. Please review and modify if needed." "$GREEN"
        else
            print_message "No .env or .env.example file found." "$RED"
            exit 1
        fi
    fi
}

build() {
    print_message "Building all services..." "$YELLOW"
    docker-compose build --parallel
    print_message "Build complete!" "$GREEN"
}

start() {
    check_docker
    check_env
    print_message "Starting all services..." "$YELLOW"
    docker-compose up -d
    print_message "Services started!" "$GREEN"
    print_message "\nApplication URLs:" "$GREEN"
    print_message "  Frontend: http://localhost" "$GREEN"
    print_message "  Backend API: http://localhost:8090" "$GREEN"
    print_message "  PostgreSQL: localhost:5432" "$GREEN"
    print_message "\nUse 'docker-compose logs -f [service]' to view logs" "$YELLOW"
}

stop() {
    print_message "Stopping all services..." "$YELLOW"
    docker-compose down
    print_message "Services stopped!" "$GREEN"
}

restart() {
    stop
    start
}

logs() {
    if [ -z "$1" ]; then
        docker-compose logs -f
    else
        docker-compose logs -f "$1"
    fi
}

exec_cmd() {
    if [ -z "$1" ] || [ -z "$2" ]; then
        print_message "Usage: $0 exec <service> <command>" "$RED"
        exit 1
    fi
    docker-compose exec "$1" "${@:2}"
}

rebuild() {
    print_message "Rebuilding and restarting all services..." "$YELLOW"
    docker-compose down
    docker-compose build --parallel
    docker-compose up -d
    print_message "Services rebuilt and started!" "$GREEN"
    print_message "\nApplication URLs:" "$GREEN"
    print_message "  Frontend: http://localhost" "$GREEN"
    print_message "  Backend API: http://localhost:8090" "$GREEN"
    print_message "  PostgreSQL: localhost:5432" "$GREEN"
}

clean() {
    print_message "WARNING: This will remove all containers, networks, and volumes!" "$RED"
    read -p "Are you sure? (y/N) " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        print_message "Cleaning up everything..." "$YELLOW"
        docker-compose down -v --remove-orphans
        print_message "Cleanup complete!" "$GREEN"
    else
        print_message "Cleanup cancelled." "$YELLOW"
    fi
}

status() {
    print_message "Service Status:" "$YELLOW"
    docker-compose ps
}

migrate() {
    print_message "Running database migrations..." "$YELLOW"
    docker-compose exec backend ./gradlew flywayMigrate
    print_message "Migrations complete!" "$GREEN"
}

psql() {
    print_message "Connecting to PostgreSQL..." "$YELLOW"
    docker-compose exec postgres psql -U postgres -d booking_system
}

backup() {
    TIMESTAMP=$(date +%Y%m%d_%H%M%S)
    BACKUP_FILE="backup_${TIMESTAMP}.sql"
    print_message "Creating database backup: ${BACKUP_FILE}..." "$YELLOW"
    docker-compose exec -T postgres pg_dump -U postgres booking_system > "${BACKUP_FILE}"
    print_message "Backup created: ${BACKUP_FILE}" "$GREEN"
}

restore() {
    if [ -z "$1" ]; then
        print_message "Usage: $0 restore <backup_file>" "$RED"
        exit 1
    fi
    if [ ! -f "$1" ]; then
        print_message "Backup file not found: $1" "$RED"
        exit 1
    fi
    print_message "WARNING: This will replace the current database!" "$RED"
    read -p "Are you sure? (y/N) " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        print_message "Restoring database from: $1..." "$YELLOW"
        docker-compose exec -T postgres psql -U postgres -d booking_system < "$1"
        print_message "Database restored!" "$GREEN"
    else
        print_message "Restore cancelled." "$YELLOW"
    fi
}

show_help() {
    echo "Docker Compose Management Script for Booking System"
    echo ""
    echo "Usage: $0 <command> [options]"
    echo ""
    echo "Commands:"
    echo "  build          Build all Docker images"
    echo "  start          Start all services"
    echo "  stop           Stop all services"
    echo "  restart        Restart all services"
    echo "  rebuild        Rebuild and restart all services"
    echo "  status         Show service status"
    echo "  logs [service] View logs (all services or specific service)"
    echo "  exec <service> <cmd>  Execute command in a service"
    echo "  clean          Remove all containers, networks, and volumes"
    echo "  migrate        Run database migrations"
    echo "  psql           Access PostgreSQL CLI"
    echo "  backup         Create database backup"
    echo "  restore <file> Restore database from backup"
    echo "  help           Show this help message"
    echo ""
    echo "Services: postgres, backend, frontend"
    echo ""
    echo "Examples:"
    echo "  $0 start              # Start all services"
    echo "  $0 logs backend       # View backend logs"
    echo "  $0 exec backend sh    # Shell into backend container"
    echo "  $0 backup            # Create database backup"
}

case "$1" in
    build)
        build
        ;;
    start)
        start
        ;;
    stop)
        stop
        ;;
    restart)
        restart
        ;;
    rebuild)
        rebuild
        ;;
    status)
        status
        ;;
    logs)
        logs "$2"
        ;;
    exec)
        exec_cmd "${@:2}"
        ;;
    clean)
        clean
        ;;
    migrate)
        migrate
        ;;
    psql)
        psql
        ;;
    backup)
        backup
        ;;
    restore)
        restore "$2"
        ;;
    help|--help|-h)
        show_help
        ;;
    *)
        print_message "Unknown command: $1" "$RED"
        echo ""
        show_help
        exit 1
        ;;
esac
