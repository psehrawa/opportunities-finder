#!/bin/bash

# Database Backup Script
set -e

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Load environment variables
if [ -f .env.prod ]; then
    export $(cat .env.prod | grep -v '^#' | xargs)
else
    echo -e "${RED}❌ .env.prod file not found!${NC}"
    exit 1
fi

# Backup directory
BACKUP_DIR="${BACKUP_DIRECTORY:-./backups}"
mkdir -p $BACKUP_DIR

# Generate backup filename
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
BACKUP_FILE="$BACKUP_DIR/oppfinder_backup_${TIMESTAMP}.sql"

echo -e "${YELLOW}💾 Creating database backup...${NC}"

# Parse DATABASE_URL
if [[ $DATABASE_URL =~ postgresql://([^:]+):([^@]+)@([^:]+):([^/]+)/(.+) ]]; then
    DB_USER="${BASH_REMATCH[1]}"
    DB_PASS="${BASH_REMATCH[2]}"
    DB_HOST="${BASH_REMATCH[3]}"
    DB_PORT="${BASH_REMATCH[4]}"
    DB_NAME="${BASH_REMATCH[5]}"
else
    echo -e "${RED}❌ Invalid DATABASE_URL format${NC}"
    exit 1
fi

# Create backup
PGPASSWORD=$DB_PASS pg_dump -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME > $BACKUP_FILE

# Compress backup
gzip $BACKUP_FILE

echo -e "${GREEN}✅ Backup created: ${BACKUP_FILE}.gz${NC}"

# Upload to S3 if configured
if [ ! -z "$BACKUP_S3_BUCKET" ] && [ ! -z "$AWS_ACCESS_KEY_ID" ]; then
    echo -e "${YELLOW}☁️ Uploading to S3...${NC}"
    aws s3 cp ${BACKUP_FILE}.gz s3://$BACKUP_S3_BUCKET/database/
    echo -e "${GREEN}✅ Backup uploaded to S3${NC}"
fi

# Clean up old backups (keep last 30 days)
echo -e "${YELLOW}🧹 Cleaning up old backups...${NC}"
find $BACKUP_DIR -name "oppfinder_backup_*.sql.gz" -mtime +30 -delete

echo -e "${GREEN}✅ Backup process complete!${NC}"