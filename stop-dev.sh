#!/bin/bash

# TechOpportunity Intelligence Platform - Stop Script (Development)

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LOG_DIR="$PROJECT_DIR/logs"

echo -e "${BLUE}🛑 Stopping TechOpportunity Intelligence Platform (Dev Mode)${NC}"

# Stop application
if [ -f "$LOG_DIR/app.pid" ]; then
    APP_PID=$(cat "$LOG_DIR/app.pid")
    if kill -0 $APP_PID 2>/dev/null; then
        echo -e "${YELLOW}Stopping application (PID: $APP_PID)...${NC}"
        kill $APP_PID
        rm -f "$LOG_DIR/app.pid"
        echo -e "${GREEN}✅ Application stopped${NC}"
    else
        echo -e "${YELLOW}Application not running${NC}"
        rm -f "$LOG_DIR/app.pid"
    fi
else
    echo -e "${YELLOW}No application PID file found${NC}"
fi

echo -e "${GREEN}🎉 Development application stopped${NC}"
