# Booking System

A full-stack appointment booking system built with Angular and Spring Boot, designed for managing branch appointments with time slot availability.

## 🏗️ Architecture

- **Frontend**: Angular 17 with TypeScript
- **Backend**: Spring Boot 3.x with Java 21
- **Database**: PostgreSQL 16
- **Containerization**: Docker & Docker Compose
- **Database Migrations**: Flyway
- **Testing**: JUnit, Mockito, Karma, Jasmine

## 📋 Prerequisites

- **Java 21** (for local backend development)
- **Node.js 20+** and npm (for local frontend development)
- **Docker & Docker Compose** (for containerized deployment)
- **PostgreSQL 16** (for local development without Docker)

## 🚀 Getting Started

### Option 1: Docker Compose (Recommended)

The fastest way to get the entire stack running:
For more detailed instructions, refer to the [Docker Guide](DOCKER_README.md).

```bash
# Clone the repository
git clone git@github.com:thiefspin/booking-system.git
cd booking-system

# Copy environment variables
cp .env.example .env

# Start all services
./docker-compose.sh start

# Or using docker-compose directly
docker-compose up -d
```

Access the application:
- **Frontend**: http://localhost
- **Backend API**: http://localhost:8090
- **PostgreSQL**: localhost:5432 (postgres/secret)

### Option 2: Local Development

#### Backend Setup

```bash
cd backend

# Set environment variables (or use defaults)
export POSTGRES_URL=jdbc:postgresql://localhost:5432/booking_system
export POSTGRES_USER=postgres
export POSTGRES_PASSWORD=secret

# Build the project
./gradlew build

# Run the application
./gradlew bootRun
```

The backend will be available at http://localhost:8090

#### Frontend Setup

```bash
cd frontend

# Install dependencies
npm install

# Run development server
npm start
```

The frontend will be available at http://localhost:4200

#### Database Setup

If running PostgreSQL locally:

```sql
CREATE DATABASE booking_system;
```

Flyway will automatically handle schema migrations when the backend starts.

## 🧪 Testing

### Backend Tests

```bash
cd backend

# Run all tests
./gradlew test

# Run with coverage
./gradlew test jacocoTestReport

# Run specific test class
./gradlew test --tests "*AppointmentServiceTest"
```

### Frontend Tests

```bash
cd frontend

# Run tests in watch mode
npm test

# Run tests once with coverage
npm test -- --no-watch --code-coverage --browsers=ChromeHeadless
```

### Integration Tests with Docker

```bash
# Run all tests in containers
docker-compose -f docker-compose.test.yml up --build --abort-on-container-exit
```

## 📚 API Documentation

The backend provides comprehensive API documentation through **Swagger/OpenAPI**.

### Accessing API Documentation

When the backend is running, you can access:

- **Swagger UI**: http://localhost:8090/swagger-ui.html
  - Interactive API documentation
  - Try out endpoints directly from the browser
  - View request/response schemas
  - Test with different parameters

- **OpenAPI Specification**: http://localhost:8090/v3/api-docs
  - JSON format API specification
  - Can be imported into Postman or other API tools
  - Useful for generating client SDKs

### Quick API Testing

The Swagger UI provides a complete interface to:
- Browse all available endpoints
- View detailed parameter descriptions
- See example requests and responses
- Execute API calls with automatic authentication handling
- Download the OpenAPI specification for offline use

### Import to Postman

1. Navigate to http://localhost:8090/v3/api-docs
2. Copy the JSON content
3. In Postman: Import → Raw Text → Paste the JSON
4. A complete collection with all endpoints will be created

## 🛠️ Development

### Project Structure

```
booking-system/
├── backend/               # Spring Boot application
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/      # Java source code
│   │   │   └── resources/ # Configuration files
│   │   └── test/          # Test files
│   └── build.gradle       # Gradle build configuration
├── frontend/              # Angular application
│   ├── src/
│   │   ├── app/          # Angular components and services
│   │   ├── assets/       # Static assets
│   │   └── environments/ # Environment configurations
│   └── package.json      # Node dependencies
├── docker-compose.yml    # Docker orchestration
└── .github/
    └── workflows/       # CI/CD pipelines
```

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `POSTGRES_URL` | Database connection URL | `jdbc:postgresql://localhost:5432/booking_system` |
| `POSTGRES_USER` | Database username | `postgres` |
| `POSTGRES_PASSWORD` | Database password | `secret` |
| `SERVER_PORT` | Backend server port | `8090` |
| `SPRING_PROFILES_ACTIVE` | Active Spring profile | `default` |

## 🔄 CI/CD

GitHub Actions workflow automatically:
- Runs tests for both frontend and backend on PR
- Can deploy to staging/production environments

## 🗺️ Future Roadmap

### Phase 1: Authentication & Authorization
- [ ] **Staff Portal**: Secure login system for staff members
  - Role-based access control (Admin, Manager, Staff)
  - JWT-based authentication
  - Password reset functionality
  - Two-factor authentication support

### Phase 2: Advanced Booking Management
- [ ] **Staff Dashboard**
  - View all appointments across branches
  - Search and filter appointments
  - Manual booking creation for walk-ins
  - Bulk appointment management
  
- [ ] **Customer Management**
  - Customer profiles and history
  - Favorite branches and preferences
  - Appointment reminders via email/SMS

### Phase 3: Business Intelligence
- [ ] **Analytics Dashboard**
  - Booking trends and patterns
  - Branch utilization reports
  - Peak hours analysis
  - Customer demographics
  
- [ ] **Reporting System**
  - Automated daily/weekly/monthly reports
  - Custom report builder
  - Export to PDF/Excel
  - Email report scheduling

### Phase 4: Enhanced Features
- [ ] **Notification System**
  - Real-time notifications for staff
  - SMS integration for appointment reminders
  - Email templates customization
  
- [ ] **Queue Management**
  - Virtual queue for walk-ins
  - Estimated wait times
  - Queue position notifications
  - No-show handling

### Phase 5: Integration & Scaling
- [ ] **Calendar Integration**
  - Google Calendar sync
  - Outlook/Office 365 integration

### Phase 6: Advanced Capabilities
- [ ] **Resource Management**
  - Staff scheduling and availability
  - Service-specific time slots
  - Equipment/room booking
  - Capacity planning
  
- [ ] **AI/ML Features**
  - Intelligent appointment suggestions
  - No-show prediction
  - Optimal scheduling recommendations
  - Chatbot for customer support

### Technical Improvements
- [ ] **Performance Optimization**
  - Redis caching layer
  - Database query optimization
  - CDN integration for static assets
  
- [ ] **Security Enhancements**
  - API rate limiting
  - OWASP compliance audit
  - Penetration testing
  - POPI compliance tools
  
- [ ] **Developer Experience**
  - SDK for third-party integrations
  - Webhook support for events