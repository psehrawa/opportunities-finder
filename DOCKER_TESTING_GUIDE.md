# Docker Application Testing Guide

## Prerequisites

1. **Docker Desktop** must be installed and running
2. **Docker Compose** available (included with Docker Desktop)
3. **Java 17+** for application build
4. **Git** for source code

## Quick Start

```bash
# Start complete application with Docker infrastructure
./startup.sh

# Or with options
./startup.sh --skip-management  # Skip management UIs
./startup.sh --clean           # Clean build first
./startup.sh --skip-tests      # Skip validation tests
```

## Complete Setup Process

### 1. Infrastructure Services
The startup script will launch:
- PostgreSQL database with multiple schemas
- Redis cache server
- Apache Kafka with Zookeeper
- Management UIs (pgAdmin, Redis Commander, Kafka UI)

### 2. Service URLs
After successful startup:

**Application:**
- Discovery Service: http://localhost:8090
- Health Check: http://localhost:8090/actuator/health
- API Documentation: http://localhost:8090/actuator

**Management UIs:**
- pgAdmin: http://localhost:8081
  - Email: admin@oppfinder.com
  - Password: admin123
- Redis Commander: http://localhost:8082
- Kafka UI: http://localhost:8080

**Direct Service Access:**
- PostgreSQL: localhost:5432
- Redis: localhost:6379 (password: redis123)
- Kafka: localhost:9092

### 3. Database Schemas
The PostgreSQL instance creates these databases:
- `oppfinder_discovery` - Discovery service data
- `oppfinder_intelligence` - Intelligence service data
- `oppfinder_user` - User management data
- `oppfinder_notification` - Notification service data
- `oppfinder_analytics` - Analytics service data

## Testing Scenarios

### 1. Infrastructure Health Check
```bash
# Check all services are running
docker ps

# Check service health
docker-compose ps

# View service logs
docker-compose logs postgres
docker-compose logs redis
docker-compose logs kafka
```

### 2. Application Health Check
```bash
# Health endpoint
curl http://localhost:8090/actuator/health

# Discovery health
curl http://localhost:8090/api/v1/discovery/health

# Data sources status
curl http://localhost:8090/api/v1/discovery/sources
```

### 3. API Testing
```bash
# Get opportunities
curl http://localhost:8090/api/v1/opportunities

# Trigger discovery
curl -X POST "http://localhost:8090/api/v1/discovery/trigger?limitPerSource=5"

# Trigger scoring
curl -X POST http://localhost:8090/api/v1/discovery/scoring/trigger

# Search opportunities
curl -X POST http://localhost:8090/api/v1/opportunities/search \
  -H "Content-Type: application/json" \
  -d '{"types":["STARTUP_FUNDING"],"page":0,"size":10}'
```

### 4. Database Testing
Using pgAdmin (http://localhost:8081):
1. Login with admin@oppfinder.com / admin123
2. Add server connection:
   - Host: postgres (or localhost if external)
   - Port: 5432
   - Database: oppfinder_discovery
   - Username: oppfinder
   - Password: oppfinder123
3. Verify tables are created
4. Check data persistence after application restart

### 5. Redis Testing
Using Redis Commander (http://localhost:8082):
1. View cached data
2. Monitor cache hit/miss rates
3. Verify session storage

### 6. Kafka Testing
Using Kafka UI (http://localhost:8080):
1. View topics: opportunity.discovered, opportunity.updated, opportunity.scored
2. Monitor message flow
3. View consumer groups

## Environment Profiles

### Development Profile (dev)
- PostgreSQL database
- Redis caching enabled
- Kafka messaging enabled
- DDL auto: create-drop
- Detailed logging
- All data sources enabled

### Production Profile (prod)
- External database via environment variables
- DDL auto: validate (no schema changes)
- Minimal logging
- Environment-based configuration

## Common Issues & Solutions

### Port Conflicts
If ports are in use:
```bash
# Check what's using ports
lsof -i :5432 :6379 :9092 :8090 :8080 :8081 :8082

# Kill conflicting processes
kill -9 <PID>
```

### Docker Issues
```bash
# Reset Docker state
docker-compose down -v
docker system prune -f

# Rebuild containers
docker-compose build --no-cache
```

### Database Connection Issues
```bash
# Check PostgreSQL logs
docker-compose logs postgres

# Test direct connection
docker exec -it oppfinder-postgres psql -U oppfinder -d oppfinder_discovery
```

### Application Startup Failures
```bash
# Check application logs
tail -f logs/application.log

# Check specific service health
docker-compose ps
docker-compose logs discovery-service
```

## Performance Testing

### Load Testing
```bash
# Install Apache Bench
brew install httpd

# Test endpoints
ab -n 1000 -c 10 http://localhost:8090/api/v1/opportunities
ab -n 100 -c 5 http://localhost:8090/actuator/health
```

### Resource Monitoring
```bash
# Monitor Docker resource usage
docker stats

# Monitor specific containers
docker stats oppfinder-postgres oppfinder-redis oppfinder-kafka
```

## Cleanup

```bash
# Stop everything
./stop.sh

# Remove all data (caution: deletes all data)
docker-compose down -v

# Clean up Docker system
docker system prune -f
```

## Security Considerations

**Default Credentials (Change for Production):**
- PostgreSQL: oppfinder/oppfinder123
- Redis: redis123
- pgAdmin: admin@oppfinder.com/admin123

**Network Security:**
- All services communicate via Docker network
- Only necessary ports exposed to host
- Consider using Docker secrets for production

**Data Security:**
- PostgreSQL data persisted in Docker volumes
- Redis data persisted to disk
- Regular backup recommended for production

## Troubleshooting

### Check Service Dependencies
```bash
# Verify all services are healthy
docker-compose ps --services --filter "status=running"

# Check dependency order
docker-compose config --services
```

### Debug Application Issues
```bash
# Application logs
tail -f logs/application.log

# Spring Boot actuator endpoints
curl http://localhost:8090/actuator/env
curl http://localhost:8090/actuator/metrics
```

### Validate Configuration
```bash
# Check environment variables
docker-compose config

# Verify database schema
docker exec oppfinder-postgres psql -U oppfinder -d oppfinder_discovery -c "\dt"
```