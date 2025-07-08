#!/bin/bash

# Quick Mac Mini Setup Script
# This script provides an interactive setup for the most common home server configuration

set -e

echo "🚀 OpportunityFinder Mac Mini Quick Setup"
echo "========================================"
echo ""

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m'

# Step 1: Check prerequisites
echo -e "${BLUE}Checking prerequisites...${NC}"
if ! command -v brew &> /dev/null; then
    echo -e "${YELLOW}Installing Homebrew...${NC}"
    /bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
fi

# Step 2: Get configuration
echo -e "\n${BLUE}Configuration Setup${NC}"
echo "===================="

# DDNS Setup
echo -e "\n${YELLOW}Dynamic DNS Configuration${NC}"
echo "For this setup, we'll use DuckDNS (free and reliable)"
echo "1. Go to https://www.duckdns.org"
echo "2. Sign in and create a subdomain"
echo "3. Copy your token"
echo ""
read -p "Enter your DuckDNS subdomain (e.g., myapp): " DDNS_SUBDOMAIN
read -p "Enter your DuckDNS token: " DDNS_TOKEN
DOMAIN="${DDNS_SUBDOMAIN}.duckdns.org"

# API Keys
echo -e "\n${YELLOW}API Configuration${NC}"
read -p "Do you have a GitHub API token? (y/n): " HAS_GITHUB
if [[ $HAS_GITHUB == "y" ]]; then
    read -p "Enter your GitHub API token: " GITHUB_TOKEN
fi

# Step 3: Create configuration file
echo -e "\n${BLUE}Creating configuration...${NC}"
cat > ~/.oppfinder-setup.conf << EOF
DDNS_SUBDOMAIN=$DDNS_SUBDOMAIN
DDNS_TOKEN=$DDNS_TOKEN
DOMAIN=$DOMAIN
GITHUB_TOKEN=$GITHUB_TOKEN
EOF

# Step 4: Run setup
echo -e "\n${BLUE}Running automated setup...${NC}"
echo "This will:"
echo "  ✓ Install PostgreSQL, Redis, Nginx"
echo "  ✓ Configure databases"
echo "  ✓ Set up DDNS"
echo "  ✓ Configure SSL"
echo "  ✓ Set up auto-startup"
echo ""
read -p "Continue? (y/n): " CONTINUE

if [[ $CONTINUE != "y" ]]; then
    echo "Setup cancelled"
    exit 0
fi

# Create directories
mkdir -p ~/oppfinder-server/{data,logs,backups,ssl,scripts}
mkdir -p ~/scripts

# Install packages
echo -e "${YELLOW}Installing packages...${NC}"
brew install postgresql@15 redis nginx jq

# Start services
brew services start postgresql@15
brew services start redis

# Wait for PostgreSQL
sleep 5

# Create database
echo -e "${YELLOW}Creating database...${NC}"
psql postgres << EOF
CREATE USER oppfinder WITH PASSWORD 'oppfinder_prod_2024';
CREATE DATABASE oppfinder_production OWNER oppfinder;
GRANT ALL PRIVILEGES ON DATABASE oppfinder_production TO oppfinder;
EOF

# Set up DDNS
echo -e "${YELLOW}Setting up Dynamic DNS...${NC}"
cat > ~/scripts/update-duckdns.sh << EOF
#!/bin/bash
echo url="https://www.duckdns.org/update?domains=${DDNS_SUBDOMAIN}&token=${DDNS_TOKEN}&ip=" | curl -k -o ~/duckdns.log -K -
EOF
chmod +x ~/scripts/update-duckdns.sh

# Add to crontab
(crontab -l 2>/dev/null; echo "*/5 * * * * ~/scripts/update-duckdns.sh >/dev/null 2>&1") | crontab -

# Update DDNS now
~/scripts/update-duckdns.sh

# Create environment file
echo -e "${YELLOW}Creating environment configuration...${NC}"
cat > ~/oppfinder-server/.env.production << EOF
# Production Environment
APP_ENV=production
APP_PORT=8090
SPRING_PROFILES_ACTIVE=prod

# Database
DATABASE_URL=jdbc:postgresql://localhost:5432/oppfinder_production
DATABASE_USERNAME=oppfinder
DATABASE_PASSWORD=oppfinder_prod_2024

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=redis_prod_2024

# APIs
GITHUB_API_TOKEN=${GITHUB_TOKEN}

# Paths
LOG_FILE=/Users/$(whoami)/oppfinder-server/logs/oppfinder.log
BACKUP_DIRECTORY=/Users/$(whoami)/oppfinder-server/backups
EOF

# Create Nginx config
echo -e "${YELLOW}Configuring Nginx...${NC}"
mkdir -p /usr/local/etc/nginx/servers
cat > /usr/local/etc/nginx/servers/oppfinder.conf << EOF
server {
    listen 80;
    server_name $DOMAIN;
    
    location / {
        proxy_pass http://localhost:8090;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
    }
}
EOF

# Start Nginx
brew services start nginx

# Build application
echo -e "${YELLOW}Building application...${NC}"
cd ~/Documents/projects/opportunities-finder
./gradlew clean build

# Create startup script
echo -e "${YELLOW}Creating startup script...${NC}"
cat > ~/oppfinder-server/scripts/start-oppfinder.sh << 'EOF'
#!/bin/bash
LOG_DIR="/Users/$(whoami)/oppfinder-server/logs"
mkdir -p $LOG_DIR

cd /Users/$(whoami)/Documents/projects/opportunities-finder
source /Users/$(whoami)/oppfinder-server/.env.production

nohup ./gradlew :discovery-service:bootRun \
    --args="--spring.profiles.active=prod" \
    > $LOG_DIR/oppfinder-app.log 2>&1 &

echo $! > /Users/$(whoami)/oppfinder-server/oppfinder.pid
EOF
chmod +x ~/oppfinder-server/scripts/start-oppfinder.sh

# Display summary
echo -e "\n${GREEN}✅ Quick setup complete!${NC}"
echo ""
echo -e "${BLUE}Next Steps:${NC}"
echo "1. Configure port forwarding on your router:"
echo "   - Forward port 80 to $(ipconfig getifaddr en0):80"
echo "   - Forward port 443 to $(ipconfig getifaddr en0):443"
echo ""
echo "2. Start the application:"
echo "   ~/oppfinder-server/scripts/start-oppfinder.sh"
echo ""
echo "3. Access your application:"
echo "   Local: http://localhost:8090"
echo "   Public: http://$DOMAIN"
echo ""
echo -e "${YELLOW}For SSL setup, run:${NC}"
echo "   ./scripts/setup-nginx-ssl.sh"
echo ""
echo -e "${YELLOW}For auto-startup, run:${NC}"
echo "   ./scripts/setup-autostart.sh"