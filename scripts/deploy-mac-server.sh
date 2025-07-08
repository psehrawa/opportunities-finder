#!/bin/bash

# Complete Mac Mini Server Deployment Script
set -e

echo "🏠 Deploying OpportunityFinder on Mac mini Home Server..."

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Check prerequisites
echo -e "${YELLOW}Checking prerequisites...${NC}"

if ! command -v brew &> /dev/null; then
    echo -e "${RED}Homebrew is required. Please install it first.${NC}"
    exit 1
fi

if ! command -v java &> /dev/null; then
    echo -e "${YELLOW}Installing Java 17...${NC}"
    brew install openjdk@17
    sudo ln -sfn $(brew --prefix)/opt/openjdk@17/libexec/openjdk.jdk /Library/Java/JavaVirtualMachines/openjdk-17.jdk
fi

# Step 1: Set up Mac server
echo -e "\n${BLUE}Step 1: Setting up Mac server...${NC}"
./scripts/setup-mac-server.sh

# Step 2: Set up Dynamic DNS
echo -e "\n${BLUE}Step 2: Setting up Dynamic DNS...${NC}"
./scripts/setup-ddns.sh

# Step 3: Build the application
echo -e "\n${BLUE}Step 3: Building application...${NC}"
cd /Users/$(whoami)/Documents/projects/opportunities-finder
./gradlew clean build

# Step 4: Set up Nginx with SSL
echo -e "\n${BLUE}Step 4: Setting up Nginx with SSL...${NC}"
./scripts/setup-nginx-ssl.sh

# Step 5: Configure auto-startup
echo -e "\n${BLUE}Step 5: Configuring auto-startup...${NC}"
./scripts/setup-autostart.sh

# Step 6: Configure firewall
echo -e "\n${BLUE}Step 6: Configuring macOS firewall...${NC}"
sudo /usr/libexec/ApplicationFirewall/socketfilterfw --add /usr/local/opt/nginx/bin/nginx
sudo /usr/libexec/ApplicationFirewall/socketfilterfw --unblockapp /usr/local/opt/nginx/bin/nginx

# Step 7: Start the application
echo -e "\n${BLUE}Step 7: Starting OpportunityFinder...${NC}"
launchctl start com.oppfinder.server

# Wait for application to start
echo -e "${YELLOW}Waiting for application to start...${NC}"
sleep 30

# Step 8: Verify deployment
echo -e "\n${BLUE}Step 8: Verifying deployment...${NC}"
if curl -s -o /dev/null -w "%{http_code}" http://localhost:8090/actuator/health | grep -q "200"; then
    echo -e "${GREEN}✅ Application is running!${NC}"
else
    echo -e "${RED}❌ Application failed to start. Check logs:${NC}"
    echo "tail -f ~/oppfinder-server/logs/oppfinder-app.log"
    exit 1
fi

# Display summary
echo -e "\n${GREEN}🎉 OpportunityFinder deployed successfully!${NC}"
echo ""
echo -e "${BLUE}📋 Deployment Summary:${NC}"
echo "Local URL: http://localhost:8090"
echo "Public URL: https://$(cat ~/.ddns-domain 2>/dev/null || echo 'your-domain.com')"
echo ""
echo -e "${BLUE}📊 Service Status:${NC}"
brew services list | grep -E "(postgresql|redis|nginx)"
echo ""
echo -e "${BLUE}📁 Important Locations:${NC}"
echo "Application: /Users/$(whoami)/Documents/projects/opportunities-finder"
echo "Server Files: /Users/$(whoami)/oppfinder-server"
echo "Logs: /Users/$(whoami)/oppfinder-server/logs"
echo "Backups: /Users/$(whoami)/oppfinder-server/backups"
echo ""
echo -e "${BLUE}🔧 Management Commands:${NC}"
echo "View logs: tail -f ~/oppfinder-server/logs/oppfinder-app.log"
echo "Stop server: launchctl stop com.oppfinder.server"
echo "Start server: launchctl start com.oppfinder.server"
echo "Check status: launchctl list | grep oppfinder"
echo ""
echo -e "${YELLOW}⚠️ Don't forget to:${NC}"
echo "1. Configure port forwarding on your router (ports 80 and 443)"
echo "2. Update your external API keys in ~/oppfinder-server/.env.production"
echo "3. Test access from outside your network"
echo "4. Set up regular backups"