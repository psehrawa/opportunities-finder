#!/bin/bash

# Docker Configuration Test Script
# Tests Docker Compose files and configuration without requiring full Docker setup

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}🐳 Docker Configuration Test${NC}"
echo "=============================================="

# Test 1: Docker Compose File Validation
echo -e "\n${BLUE}📋 Testing Docker Compose Configuration...${NC}"

if docker compose config >/dev/null 2>&1; then
    echo -e "${GREEN}✅ docker-compose.yml is valid${NC}"
else
    echo -e "${RED}❌ docker-compose.yml validation failed${NC}"
    docker compose config
    exit 1
fi

# Test 2: Show planned services
echo -e "\n${BLUE}🔍 Planned Services:${NC}"
docker compose config --services | while read service; do
    echo -e "  • $service"
done

# Test 3: Show port mappings
echo -e "\n${BLUE}🌐 Port Mappings:${NC}"
docker compose config | grep -A 1 "ports:" | grep -E "- \"[0-9]+:" | sed 's/.*- "/  • /' | sed 's/"//'

# Test 4: Validate environment variables
echo -e "\n${BLUE}⚙️ Environment Variables:${NC}"
echo -e "  • POSTGRES_DB: $(docker compose config | grep 'POSTGRES_DB:' | cut -d':' -f2 | xargs)"
echo -e "  • POSTGRES_USER: $(docker compose config | grep 'POSTGRES_USER:' | cut -d':' -f2 | xargs)"
echo -e "  • KAFKA_BROKER_ID: $(docker compose config | grep 'KAFKA_BROKER_ID:' | cut -d':' -f2 | xargs)"

# Test 5: Check network configuration
echo -e "\n${BLUE}🌍 Network Configuration:${NC}"
docker compose config | grep -A 5 "networks:" | grep -v "^--$"

# Test 6: Validate volume mounts
echo -e "\n${BLUE}💾 Volume Configuration:${NC}"
docker compose config | grep -A 10 "volumes:" | grep -E "^[[:space:]]*[a-z]" | sed 's/^/  • /'

# Test 7: Health check configuration
echo -e "\n${BLUE}🏥 Health Check Configuration:${NC}"
if docker compose config | grep -q "healthcheck:"; then
    echo -e "${GREEN}✅ Health checks configured for services${NC}"
    docker compose config | grep -B 2 -A 3 "healthcheck:" | grep -E "(postgres|redis|kafka):" | sed 's/:$//' | sed 's/^/  • /'
else
    echo -e "${YELLOW}⚠️ No health checks found${NC}"
fi

# Test 8: Test application configuration
echo -e "\n${BLUE}🚀 Application Configuration Test...${NC}"

echo -e "${CYAN}Checking Spring profiles...${NC}"
if grep -q "spring.profiles.active: dev" discovery-service/src/main/resources/application.yml; then
    echo -e "${GREEN}✅ Development profile configured${NC}"
fi

if grep -q "spring.profiles.active: prod" discovery-service/src/main/resources/application.yml; then
    echo -e "${GREEN}✅ Production profile configured${NC}"
fi

echo -e "${CYAN}Checking database configuration...${NC}"
if grep -q "jdbc:postgresql" discovery-service/src/main/resources/application.yml; then
    echo -e "${GREEN}✅ PostgreSQL configuration found${NC}"
fi

echo -e "${CYAN}Checking Redis configuration...${NC}"
if grep -q "redis:" discovery-service/src/main/resources/application.yml; then
    echo -e "${GREEN}✅ Redis configuration found${NC}"
fi

echo -e "${CYAN}Checking Kafka configuration...${NC}"
if grep -q "kafka:" discovery-service/src/main/resources/application.yml; then
    echo -e "${GREEN}✅ Kafka configuration found${NC}"
fi

# Test 9: Verify startup script compatibility
echo -e "\n${BLUE}📜 Startup Script Compatibility...${NC}"

if grep -q "COMPOSE_CMD" startup.sh; then
    echo -e "${GREEN}✅ Startup script supports multiple Docker Compose versions${NC}"
else
    echo -e "${YELLOW}⚠️ Startup script may need Docker Compose compatibility updates${NC}"
fi

if grep -q "docker compose\|docker-compose" startup.sh; then
    echo -e "${GREEN}✅ Docker Compose commands found in startup script${NC}"
else
    echo -e "${RED}❌ No Docker Compose commands found in startup script${NC}"
fi

# Test 10: File structure validation
echo -e "\n${BLUE}📁 File Structure Validation...${NC}"

required_files=(
    "docker-compose.yml"
    "docker-compose.override.yml"  
    "startup.sh"
    "discovery-service/src/main/resources/application.yml"
    "docker/postgres/init/01-create-databases.sql"
)

for file in "${required_files[@]}"; do
    if [ -f "$file" ]; then
        echo -e "${GREEN}✅ $file${NC}"
    else
        echo -e "${RED}❌ $file (missing)${NC}"
    fi
done

echo -e "\n${GREEN}🎉 Docker Configuration Test Complete!${NC}"
echo "=============================================="
echo -e "${BLUE}Ready for Docker testing when daemon is available${NC}"

# Show next steps
echo -e "\n${YELLOW}Next Steps:${NC}"
echo "1. Start Docker Desktop or Docker daemon"
echo "2. Run: ./startup.sh"
echo "3. Test all services and endpoints"
echo "4. Use management UIs for monitoring"