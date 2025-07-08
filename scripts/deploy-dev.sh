#!/bin/bash

# Development Deployment Script
set -e

echo "🚀 Starting Development Deployment..."

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Check if .env.dev exists
if [ ! -f .env.dev ]; then
    echo -e "${RED}❌ .env.dev file not found!${NC}"
    echo "Please copy .env.dev.example to .env.dev and configure it."
    exit 1
fi

# Load environment variables
export $(cat .env.dev | grep -v '^#' | xargs)

echo -e "${YELLOW}📦 Building application...${NC}"
./gradlew clean build -x test

echo -e "${YELLOW}🐳 Starting infrastructure services...${NC}"
docker-compose -f docker-compose.dev.yml up -d

# Wait for services to be healthy
echo -e "${YELLOW}⏳ Waiting for services to be healthy...${NC}"
sleep 10

# Check PostgreSQL
until docker exec oppfinder-postgres-dev pg_isready -U oppfinder_dev; do
    echo "Waiting for PostgreSQL..."
    sleep 2
done
echo -e "${GREEN}✅ PostgreSQL is ready${NC}"

# Check Redis
until docker exec oppfinder-redis-dev redis-cli ping; do
    echo "Waiting for Redis..."
    sleep 2
done
echo -e "${GREEN}✅ Redis is ready${NC}"

# Run database migrations if needed
echo -e "${YELLOW}🔧 Running database migrations...${NC}"
# Add your migration command here if using Flyway or Liquibase

echo -e "${YELLOW}🚀 Starting application...${NC}"
./gradlew :discovery-service:bootRun --args="--spring.profiles.active=dev" &

echo -e "${GREEN}✅ Development deployment complete!${NC}"
echo ""
echo "📊 Service URLs:"
echo "  - Application: http://localhost:8090"
echo "  - pgAdmin: http://localhost:8081 (dev@oppfinder.com / devadmin123)"
echo "  - Redis Commander: http://localhost:8082"
echo "  - Kafka UI: http://localhost:8080"
echo ""
echo "📝 Logs:"
echo "  - Application: tail -f logs/discovery-dev.log"
echo "  - Docker: docker-compose -f docker-compose.dev.yml logs -f"
echo ""
echo "🛑 To stop: ./scripts/stop-dev.sh"