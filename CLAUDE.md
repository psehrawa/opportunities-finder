# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Essential Commands

### Quick Start
```bash
# Backend: Full application startup (production-like)
./startup.sh

# Backend: Development mode (in-memory H2 database, no external dependencies)
./startup-dev.sh

# Frontend: Start React development server
cd frontend && npm install && npm start

# Stop application
./stop.sh  # or ./stop-dev.sh for dev mode
```

### Build and Test
```bash
# Build all modules
./gradlew build

# Run tests (all services)
./gradlew test

# Run tests for specific service
./gradlew :discovery-service:test

# Run with test coverage
./gradlew :discovery-service:jacocoTestReport

# Clean build
./gradlew clean build
```

### Development Commands
```bash
# Run only discovery service
./gradlew :discovery-service:bootRun

# Start infrastructure services only
docker-compose up -d postgres redis kafka zookeeper

# Check application health
curl http://localhost:8090/actuator/health

# Trigger discovery manually
curl -X POST http://localhost:8090/api/v1/discovery/trigger?limitPerSource=20
```

## Architecture Overview

### Multi-Service Architecture
This is a microservices-based platform with the following services:
- **discovery-service**: Core service for opportunity discovery and scoring (port 8090)
- **intelligence-service**: Advanced analytics and ML (planned)
- **user-service**: User management and authentication (planned)
- **notification-service**: Event-driven notifications (planned)
- **analytics-service**: Usage analytics and reporting (planned)
- **api-gateway**: API Gateway for routing and security (planned)
- **common**: Shared utilities and DTOs

### Technology Stack
- **Backend**: Spring Boot 3.2.0 with Java 17
- **Database**: PostgreSQL with JPA/Hibernate
- **Caching**: Redis for performance
- **Messaging**: Apache Kafka for event streaming
- **Build**: Gradle with multi-module structure
- **Containerization**: Docker Compose for development

### Key Data Flow
1. **Discovery**: Scheduled jobs fetch data from external APIs (GitHub, Reddit, etc.)
2. **Processing**: Opportunities are scored using weighted algorithms
3. **Storage**: Opportunities stored in PostgreSQL with caching in Redis
4. **Events**: Kafka events for opportunity lifecycle changes
5. **API**: REST endpoints for searching and managing opportunities

## Important Configuration

### Database Setup
The application uses PostgreSQL with multiple databases:
- `oppfinder_discovery` - Main discovery service database
- Database connection managed through Spring Boot configuration

### Environment Variables
```bash
# GitHub API (increases rate limits)
GITHUB_API_TOKEN=your_github_token

# Database (if not using Docker)
DATABASE_URL=jdbc:postgresql://localhost:5432/oppfinder_discovery
DATABASE_USERNAME=oppfinder
DATABASE_PASSWORD=oppfinder123

# Redis (if not using Docker)
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=redis123
```

### Key Configuration Files
- `application.yml` - Main Spring Boot configuration
- `application-test.yml` - Test profile (created by startup-dev.sh)
- `docker-compose.yml` - Infrastructure services

## Development Workflow

### Starting Development
1. Use `./startup-dev.sh` for quick development (no Docker dependencies)
2. Use `./startup.sh` for full environment testing
3. Check health at http://localhost:8090/actuator/health

### Testing Strategy
- **Unit Tests**: Service and repository layers with Mockito
- **Integration Tests**: Full Spring context with test containers
- **API Tests**: REST endpoints validation
- **Health Checks**: Actuator endpoints for monitoring

### Opportunity Scoring System
The platform scores opportunities (0-100) based on:
- **Funding Stage** (25%) - Earlier stages score higher
- **Company Size** (20%) - Smaller companies score higher
- **Industry Trend** (20%) - AI, Fintech, HealthTech prioritized
- **Social Signals** (15%) - GitHub stars, engagement metrics
- **Recency** (10%) - Recently discovered opportunities
- **Data Source Reliability** (10%) - Premium sources weighted higher

## Common Development Tasks

### Adding New Data Sources
1. Implement `DataSource` interface in discovery-service
2. Add configuration in `application.yml`
3. Register in `DataSourceRegistry`
4. Add integration tests

### Database Migrations
- JPA/Hibernate handles DDL with `ddl-auto: update`
- Manual migrations should be added to `src/main/resources/db/migration`

### API Development
- Controllers in `com.psehrawa.oppfinder.discovery.controller`
- Follow REST conventions
- Add OpenAPI documentation (planned)
- Include proper error handling

### Event Processing
- Kafka producers in service layer
- Event schemas in common module
- Async processing for non-blocking operations

## Monitoring and Debugging

### Application Logs
```bash
# View live application logs
tail -f logs/application.log

# Docker service logs
docker-compose logs discovery-service
docker-compose logs postgres
```

### Health Monitoring
- Application health: http://localhost:8090/actuator/health
- Discovery health: http://localhost:8090/api/v1/discovery/health
- Metrics: http://localhost:8090/actuator/metrics

### Management UIs
- pgAdmin: http://localhost:8081 (admin@oppfinder.com / admin123)
- Redis Commander: http://localhost:8082
- Kafka UI: http://localhost:8080

## Build System Notes

### Gradle Multi-Module Structure
- Root `build.gradle` defines common dependencies
- Each service has its own `build.gradle`
- Common module shared across services
- All services inherit Spring Boot configuration

### Dependencies
- Spring Boot starters for web, data, actuator
- Spring Cloud for microservices (planned)
- Lombok for reducing boilerplate
- MapStruct for DTO mapping
- TestContainers for integration testing

## Current Implementation Status

### Completed Features
- Discovery service with GitHub API integration
- Opportunity scoring and storage
- REST API for opportunity management
- Docker environment with PostgreSQL, Redis, Kafka
- Scheduled discovery and scoring jobs
- Rate limiting and health checks

### ✅ Recently Completed
- **React TypeScript Frontend** - Basic UI for opportunities management
- Opportunities dashboard with search and filtering
- Health monitoring and system status displays
- API integration with the discovery service

### In Development
- Additional data sources (Reddit, Hacker News, Product Hunt)
- Multi-tenancy support
- Advanced analytics and intelligence service
- User management and authentication
- Frontend authentication integration

The codebase follows Spring Boot best practices with proper separation of concerns, dependency injection, and comprehensive testing strategies.