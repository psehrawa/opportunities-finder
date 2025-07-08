#!/bin/bash

# Mac Mini Production Server Setup
set -e

echo "🖥️ Setting up Mac mini as Production Server..."

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Check if running on macOS
if [[ "$OSTYPE" != "darwin"* ]]; then
    echo -e "${RED}This script is designed for macOS only!${NC}"
    exit 1
fi

# Create necessary directories
echo -e "${YELLOW}Creating server directories...${NC}"
mkdir -p ~/oppfinder-server/{data,logs,backups,ssl,scripts}
mkdir -p ~/Library/LaunchAgents

# Install Homebrew if not installed
if ! command -v brew &> /dev/null; then
    echo -e "${YELLOW}Installing Homebrew...${NC}"
    /bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
fi

# Install required packages
echo -e "${YELLOW}Installing required packages...${NC}"
brew install postgresql@15 redis nginx certbot jq

# Install Docker Desktop for Mac
if ! command -v docker &> /dev/null; then
    echo -e "${YELLOW}Installing Docker Desktop...${NC}"
    brew install --cask docker
    echo -e "${YELLOW}Please start Docker Desktop manually and wait for it to be ready${NC}"
    read -p "Press enter when Docker Desktop is running..."
fi

# Configure PostgreSQL
echo -e "${YELLOW}Configuring PostgreSQL...${NC}"
brew services start postgresql@15

# Wait for PostgreSQL to start
sleep 5

# Create database and user
psql postgres << EOF
CREATE USER oppfinder WITH PASSWORD 'oppfinder_prod_2024';
CREATE DATABASE oppfinder_production OWNER oppfinder;
GRANT ALL PRIVILEGES ON DATABASE oppfinder_production TO oppfinder;
EOF

# Configure Redis
echo -e "${YELLOW}Configuring Redis...${NC}"
cat > /usr/local/etc/redis.conf << EOF
bind 127.0.0.1
protected-mode yes
port 6379
requirepass redis_prod_2024
maxmemory 512mb
maxmemory-policy allkeys-lru
save 900 1
save 300 10
save 60 10000
dir /usr/local/var/db/redis/
EOF

brew services start redis

# Create production environment file
echo -e "${YELLOW}Creating production environment configuration...${NC}"
cat > ~/oppfinder-server/.env.production << EOF
# Production Environment Configuration
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
REDIS_SSL=false

# Security
JWT_SECRET=$(openssl rand -base64 32)

# External APIs (configure these with your actual values)
GITHUB_API_TOKEN=
REDDIT_USER_AGENT=OpportunityFinder:v1.0.0 (by /u/your_username)
REDDIT_CLIENT_ID=
REDDIT_CLIENT_SECRET=

# Monitoring
SENTRY_DSN=

# Backup
BACKUP_DIRECTORY=/Users/$(whoami)/oppfinder-server/backups

# Logging
LOG_FILE=/Users/$(whoami)/oppfinder-server/logs/oppfinder.log
EOF

echo -e "${GREEN}✅ Mac mini server setup complete!${NC}"
echo ""
echo -e "${BLUE}📋 Important Information:${NC}"
echo "PostgreSQL Database: oppfinder_production"
echo "PostgreSQL User: oppfinder"
echo "PostgreSQL Password: oppfinder_prod_2024"
echo "Redis Password: redis_prod_2024"
echo ""
echo -e "${YELLOW}⚠️ Security Note:${NC}"
echo "Please change the default passwords in production!"
echo "Edit: ~/oppfinder-server/.env.production"