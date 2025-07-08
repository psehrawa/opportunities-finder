#!/bin/bash

# Production Deployment Script
set -e

echo "🚀 Starting Production Deployment..."

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Check if .env.prod exists
if [ ! -f .env.prod ]; then
    echo -e "${RED}❌ .env.prod file not found!${NC}"
    echo "Please copy .env.prod.example to .env.prod and configure it."
    exit 1
fi

# Load environment variables
export $(cat .env.prod | grep -v '^#' | xargs)

# Deployment environment
DEPLOY_ENV=${1:-production}
echo -e "${BLUE}📌 Deploying to: ${DEPLOY_ENV}${NC}"

# Build application
echo -e "${YELLOW}📦 Building application...${NC}"
./gradlew clean build

# Run tests
echo -e "${YELLOW}🧪 Running tests...${NC}"
./gradlew test

# Build Docker image
echo -e "${YELLOW}🐳 Building Docker image...${NC}"
docker build -t oppfinder/discovery-service:latest .
docker tag oppfinder/discovery-service:latest oppfinder/discovery-service:$(git rev-parse --short HEAD)

# Database backup before deployment
echo -e "${YELLOW}💾 Creating database backup...${NC}"
./scripts/backup-database.sh

# Deploy based on platform
case $DEPLOY_ENV in
    "railway")
        echo -e "${YELLOW}🚂 Deploying to Railway...${NC}"
        railway up --service discovery-service
        ;;
    "docker-compose")
        echo -e "${YELLOW}🐳 Deploying with Docker Compose...${NC}"
        docker-compose -f docker-compose.prod.yml down
        docker-compose -f docker-compose.prod.yml up -d
        ;;
    "kubernetes")
        echo -e "${YELLOW}☸️ Deploying to Kubernetes...${NC}"
        kubectl apply -f k8s/
        kubectl rollout status deployment/discovery-service
        ;;
    *)
        echo -e "${RED}❌ Unknown deployment environment: ${DEPLOY_ENV}${NC}"
        exit 1
        ;;
esac

# Wait for health check
echo -e "${YELLOW}⏳ Waiting for application to be healthy...${NC}"
MAX_ATTEMPTS=30
ATTEMPT=0
while [ $ATTEMPT -lt $MAX_ATTEMPTS ]; do
    if curl -f http://localhost:8090/actuator/health > /dev/null 2>&1; then
        echo -e "${GREEN}✅ Application is healthy!${NC}"
        break
    fi
    ATTEMPT=$((ATTEMPT + 1))
    echo "Waiting for application... (${ATTEMPT}/${MAX_ATTEMPTS})"
    sleep 5
done

if [ $ATTEMPT -eq $MAX_ATTEMPTS ]; then
    echo -e "${RED}❌ Application failed to start!${NC}"
    exit 1
fi

# Run smoke tests
echo -e "${YELLOW}🔥 Running smoke tests...${NC}"
./scripts/smoke-test.sh

# Notify deployment success
echo -e "${GREEN}✅ Production deployment complete!${NC}"
echo ""
echo "📊 Deployment Summary:"
echo "  - Environment: ${DEPLOY_ENV}"
echo "  - Version: $(git rev-parse --short HEAD)"
echo "  - Time: $(date)"
echo ""

# Send notification (optional)
if [ ! -z "$SLACK_WEBHOOK_URL" ]; then
    curl -X POST -H 'Content-type: application/json' \
        --data "{\"text\":\"✅ OpportunityFinder deployed to ${DEPLOY_ENV} - Version: $(git rev-parse --short HEAD)\"}" \
        $SLACK_WEBHOOK_URL
fi