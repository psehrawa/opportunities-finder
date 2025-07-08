#!/bin/bash

# TechOpportunity Intelligence Platform - Development Startup Script

set -e

echo "🚀 Starting TechOpportunity Intelligence Platform Development Environment"
echo "=================================================================="

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to check if a command exists
command_exists() {
    command -v "$1" >/dev/null 2>&1
}

# Function to check if a port is available
port_available() {
    ! lsof -Pi :$1 -sTCP:LISTEN -t >/dev/null
}

echo -e "${BLUE}Step 1: Checking prerequisites...${NC}"

# Check Docker
if ! command_exists docker; then
    echo -e "${RED}❌ Docker is not installed. Please install Docker and try again.${NC}"
    exit 1
fi

# Check Docker Compose
if ! command_exists docker-compose && ! docker compose version >/dev/null 2>&1; then
    echo -e "${RED}❌ Docker Compose is not installed. Please install Docker Compose and try again.${NC}"
    exit 1
fi

# Check Java
if ! command_exists java; then
    echo -e "${RED}❌ Java is not installed. Please install Java 17+ and try again.${NC}"
    exit 1
fi

# Check Gradle Wrapper
if [ ! -f "./gradlew" ]; then
    echo -e "${RED}❌ Gradle wrapper not found. Make sure you're in the project root directory.${NC}"
    exit 1
fi

echo -e "${GREEN}✅ All prerequisites met${NC}"

echo -e "${BLUE}Step 2: Checking ports availability...${NC}"

# Check required ports
PORTS=(5432 6379 9092 8090 8080 8081 8082)
for port in "${PORTS[@]}"; do
    if ! port_available $port; then
        echo -e "${YELLOW}⚠️ Port $port is already in use. This might cause conflicts.${NC}"
    fi
done

echo -e "${BLUE}Step 3: Starting infrastructure services...${NC}"

# Start infrastructure services
echo "Starting PostgreSQL, Redis, and Kafka..."
docker-compose up -d postgres redis kafka zookeeper

# Wait for services to be ready
echo "Waiting for services to start..."
sleep 10

# Check if services are running
if docker-compose ps | grep -q "postgres.*Up"; then
    echo -e "${GREEN}✅ PostgreSQL is running${NC}"
else
    echo -e "${RED}❌ PostgreSQL failed to start${NC}"
    exit 1
fi

if docker-compose ps | grep -q "redis.*Up"; then
    echo -e "${GREEN}✅ Redis is running${NC}"
else
    echo -e "${RED}❌ Redis failed to start${NC}"
    exit 1
fi

if docker-compose ps | grep -q "kafka.*Up"; then
    echo -e "${GREEN}✅ Kafka is running${NC}"
else
    echo -e "${RED}❌ Kafka failed to start${NC}"
    exit 1
fi

echo -e "${BLUE}Step 4: Starting management UIs (optional)...${NC}"

# Start management UIs
docker-compose up -d pgadmin redis-commander kafka-ui

echo -e "${BLUE}Step 5: Building application...${NC}"

# Build the application
./gradlew clean build -x test

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✅ Application built successfully${NC}"
else
    echo -e "${RED}❌ Application build failed${NC}"
    exit 1
fi

echo -e "${BLUE}Step 6: Starting Discovery Service...${NC}"

# Start the discovery service
./gradlew :discovery-service:bootRun &
SERVICE_PID=$!

# Wait for service to start
echo "Waiting for Discovery Service to start..."
sleep 15

# Check if service is running
if curl -f http://localhost:8090/actuator/health >/dev/null 2>&1; then
    echo -e "${GREEN}✅ Discovery Service is running${NC}"
else
    echo -e "${YELLOW}⚠️ Discovery Service might still be starting. Check logs if issues persist.${NC}"
fi

echo -e "${GREEN}🎉 TechOpportunity Intelligence Platform is ready!${NC}"
echo "=================================================================="
echo -e "${BLUE}Service URLs:${NC}"
echo "• Discovery Service: http://localhost:8090"
echo "• Health Check: http://localhost:8090/actuator/health"
echo "• Discovery API: http://localhost:8090/api/v1/discovery/health"
echo ""
echo -e "${BLUE}Management UIs:${NC}"
echo "• pgAdmin: http://localhost:8081 (admin@oppfinder.com / admin123)"
echo "• Redis Commander: http://localhost:8082"
echo "• Kafka UI: http://localhost:8080"
echo ""
echo -e "${BLUE}API Examples:${NC}"
echo "• GET http://localhost:8090/api/v1/opportunities"
echo "• POST http://localhost:8090/api/v1/discovery/trigger"
echo "• GET http://localhost:8090/api/v1/discovery/sources"
echo ""
echo -e "${YELLOW}To stop services:${NC}"
echo "• Press Ctrl+C to stop Discovery Service"
echo "• Run: docker-compose down"
echo ""
echo -e "${BLUE}Logs:${NC}"
echo "• Discovery Service logs will appear below"
echo "• Docker logs: docker-compose logs -f [service-name]"
echo ""

# Wait for user interrupt
wait $SERVICE_PID