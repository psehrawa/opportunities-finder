#!/bin/bash

# Auto-startup Setup for Mac mini Server
set -e

echo "🚀 Setting up auto-startup for OpportunityFinder..."

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Create startup script
echo -e "${YELLOW}Creating startup script...${NC}"
cat > ~/oppfinder-server/scripts/start-oppfinder.sh << 'EOF'
#!/bin/bash

# OpportunityFinder Startup Script
LOG_DIR="/Users/$(whoami)/oppfinder-server/logs"
LOG_FILE="$LOG_DIR/startup-$(date +%Y%m%d).log"

# Ensure log directory exists
mkdir -p $LOG_DIR

# Function to log messages
log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1" >> $LOG_FILE
}

log "Starting OpportunityFinder services..."

# Wait for network to be available
while ! ping -c 1 google.com &> /dev/null; do
    log "Waiting for network..."
    sleep 5
done

# Load environment variables
source /Users/$(whoami)/oppfinder-server/.env.production

# Start PostgreSQL if not running
if ! pgrep -x "postgres" > /dev/null; then
    log "Starting PostgreSQL..."
    brew services start postgresql@15
    sleep 5
fi

# Start Redis if not running
if ! pgrep -x "redis-server" > /dev/null; then
    log "Starting Redis..."
    brew services start redis
    sleep 3
fi

# Start Nginx if not running
if ! pgrep -x "nginx" > /dev/null; then
    log "Starting Nginx..."
    brew services start nginx
    sleep 2
fi

# Change to application directory
cd /Users/$(whoami)/Documents/projects/opportunities-finder

# Start the application
log "Starting OpportunityFinder application..."
nohup ./gradlew :discovery-service:bootRun \
    --args="--spring.profiles.active=prod" \
    > $LOG_DIR/oppfinder-app.log 2>&1 &

APP_PID=$!
echo $APP_PID > /Users/$(whoami)/oppfinder-server/oppfinder.pid

log "OpportunityFinder started with PID: $APP_PID"

# Wait and check if application started successfully
sleep 30
if kill -0 $APP_PID 2>/dev/null; then
    log "OpportunityFinder is running successfully"
else
    log "ERROR: OpportunityFinder failed to start"
    exit 1
fi
EOF

chmod +x ~/oppfinder-server/scripts/start-oppfinder.sh

# Create stop script
echo -e "${YELLOW}Creating stop script...${NC}"
cat > ~/oppfinder-server/scripts/stop-oppfinder.sh << 'EOF'
#!/bin/bash

# OpportunityFinder Stop Script
PID_FILE="/Users/$(whoami)/oppfinder-server/oppfinder.pid"

if [ -f $PID_FILE ]; then
    PID=$(cat $PID_FILE)
    if kill -0 $PID 2>/dev/null; then
        echo "Stopping OpportunityFinder (PID: $PID)..."
        kill $PID
        rm $PID_FILE
        echo "OpportunityFinder stopped"
    else
        echo "OpportunityFinder is not running"
        rm $PID_FILE
    fi
else
    echo "PID file not found"
fi
EOF

chmod +x ~/oppfinder-server/scripts/stop-oppfinder.sh

# Create LaunchAgent plist
echo -e "${YELLOW}Creating LaunchAgent configuration...${NC}"
cat > ~/Library/LaunchAgents/com.oppfinder.server.plist << EOF
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>Label</key>
    <string>com.oppfinder.server</string>
    
    <key>ProgramArguments</key>
    <array>
        <string>/Users/$(whoami)/oppfinder-server/scripts/start-oppfinder.sh</string>
    </array>
    
    <key>RunAtLoad</key>
    <true/>
    
    <key>KeepAlive</key>
    <dict>
        <key>SuccessfulExit</key>
        <false/>
        <key>Crashed</key>
        <true/>
    </dict>
    
    <key>StandardOutPath</key>
    <string>/Users/$(whoami)/oppfinder-server/logs/launchd-stdout.log</string>
    
    <key>StandardErrorPath</key>
    <string>/Users/$(whoami)/oppfinder-server/logs/launchd-stderr.log</string>
    
    <key>EnvironmentVariables</key>
    <dict>
        <key>PATH</key>
        <string>/usr/local/bin:/usr/bin:/bin:/usr/sbin:/sbin</string>
    </dict>
    
    <key>StartInterval</key>
    <integer>300</integer>
    
    <key>ThrottleInterval</key>
    <integer>60</integer>
</dict>
</plist>
EOF

# Load the LaunchAgent
echo -e "${YELLOW}Loading LaunchAgent...${NC}"
launchctl load ~/Library/LaunchAgents/com.oppfinder.server.plist

# Create monitoring script
echo -e "${YELLOW}Creating monitoring script...${NC}"
cat > ~/oppfinder-server/scripts/monitor-oppfinder.sh << 'EOF'
#!/bin/bash

# OpportunityFinder Monitoring Script
HEALTH_URL="http://localhost:8090/actuator/health"
ALERT_EMAIL="your-email@example.com"
LOG_FILE="/Users/$(whoami)/oppfinder-server/logs/monitor.log"

# Check health endpoint
HEALTH_STATUS=$(curl -s -o /dev/null -w "%{http_code}" $HEALTH_URL)

if [ "$HEALTH_STATUS" != "200" ]; then
    echo "[$(date)] WARNING: Health check failed with status $HEALTH_STATUS" >> $LOG_FILE
    
    # Send alert (configure mail first)
    # echo "OpportunityFinder health check failed" | mail -s "OpportunityFinder Alert" $ALERT_EMAIL
    
    # Try to restart
    /Users/$(whoami)/oppfinder-server/scripts/stop-oppfinder.sh
    sleep 5
    /Users/$(whoami)/oppfinder-server/scripts/start-oppfinder.sh
fi
EOF

chmod +x ~/oppfinder-server/scripts/monitor-oppfinder.sh

# Add monitoring to crontab
(crontab -l 2>/dev/null; echo "*/5 * * * * /Users/$(whoami)/oppfinder-server/scripts/monitor-oppfinder.sh") | crontab -

echo -e "${GREEN}✅ Auto-startup configuration complete!${NC}"
echo ""
echo -e "${BLUE}📋 Management Commands:${NC}"
echo "Start: launchctl start com.oppfinder.server"
echo "Stop: launchctl stop com.oppfinder.server"
echo "Status: launchctl list | grep oppfinder"
echo "Manual start: ~/oppfinder-server/scripts/start-oppfinder.sh"
echo "Manual stop: ~/oppfinder-server/scripts/stop-oppfinder.sh"
echo ""
echo -e "${YELLOW}The service will now:${NC}"
echo "• Start automatically on boot"
echo "• Restart if it crashes"
echo "• Be monitored every 5 minutes"