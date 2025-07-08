#!/bin/bash

# Development Stop Script
set -e

echo "🛑 Stopping Development Environment..."

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Stop application
echo -e "${YELLOW}🛑 Stopping application...${NC}"
pkill -f "discovery-service" || true

# Stop Docker services
echo -e "${YELLOW}🐳 Stopping Docker services...${NC}"
docker-compose -f docker-compose.dev.yml down

# Optional: Clean up volumes (uncomment if needed)
# echo -e "${YELLOW}🧹 Cleaning up volumes...${NC}"
# docker-compose -f docker-compose.dev.yml down -v

echo -e "${GREEN}✅ Development environment stopped!${NC}"