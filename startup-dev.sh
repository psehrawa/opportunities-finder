#!/bin/bash

# TechOpportunity Intelligence Platform - Development Startup Script (No Docker)
# This script starts the application with in-memory databases for testing

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Configuration
APP_NAME="TechOpportunity Intelligence Platform"
PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LOG_DIR="$PROJECT_DIR/logs"
APPLICATION_PORT=8090
HEALTH_CHECK_TIMEOUT=180  # 3 minutes
BUILD_TIMEOUT=180        # 3 minutes

# Create logs directory
mkdir -p "$LOG_DIR"

# Logging function
log() {
    echo -e "$(date '+%Y-%m-%d %H:%M:%S') - $1" | tee -a "$LOG_DIR/startup-dev.log"
}

# Print header
print_header() {
    echo -e "${BLUE}"
    echo "=================================================================="
    echo "🚀 $APP_NAME - Development Mode"
    echo "    Using in-memory databases (H2)"
    echo "=================================================================="
    echo -e "${NC}"
    log "Starting development application startup process"
}

# Check prerequisites
check_prerequisites() {
    echo -e "\n${BLUE}📋 Checking Prerequisites...${NC}"
    
    local missing_deps=()
    
    # Check Java
    if ! command -v java &> /dev/null; then
        missing_deps+=("Java 17+")
    else
        java_version=$(java -version 2>&1 | head -1 | cut -d'"' -f2 | cut -d'.' -f1)
        if [ "$java_version" -lt 17 ] 2>/dev/null; then
            missing_deps+=("Java 17+ (current: $java_version)")
        fi
    fi
    
    # Check Gradle wrapper
    if [ ! -f "$PROJECT_DIR/gradlew" ]; then
        missing_deps+=("Gradle wrapper (./gradlew)")
    fi
    
    if [ ${#missing_deps[@]} -ne 0 ]; then
        echo -e "${RED}❌ Missing prerequisites:${NC}"
        for dep in "${missing_deps[@]}"; do
            echo -e "  • $dep"
        done
        echo -e "\n${YELLOW}Please install missing dependencies and try again.${NC}"
        exit 1
    fi
    
    echo -e "${GREEN}✅ All prerequisites satisfied${NC}"
    log "Prerequisites check passed"
}

# Check port availability
check_ports() {
    echo -e "\n${BLUE}🔍 Checking Port Availability...${NC}"
    
    local ports=(8090)
    local occupied_ports=()
    
    for port in "${ports[@]}"; do
        if lsof -Pi :$port -sTCP:LISTEN -t >/dev/null 2>&1; then
            occupied_ports+=($port)
        fi
    done
    
    if [ ${#occupied_ports[@]} -ne 0 ]; then
        echo -e "${YELLOW}⚠️ The following ports are already in use:${NC}"
        for port in "${occupied_ports[@]}"; do
            echo -e "  • Port $port"
        done
        echo -e "\n${YELLOW}This may cause conflicts. Continue anyway? (y/N)${NC}"
        read -r response
        if [[ ! "$response" =~ ^[Yy]$ ]]; then
            echo -e "${RED}Startup cancelled by user${NC}"
            exit 1
        fi
    else
        echo -e "${GREEN}✅ Port $APPLICATION_PORT is available${NC}"
    fi
    
    log "Port availability check completed"
}

# Setup test profile
setup_test_profile() {
    echo -e "\n${BLUE}⚙️ Setting up test profile...${NC}"
    
    # Check if test configuration exists
    local test_config="$PROJECT_DIR/discovery-service/src/test/resources/application-test.yml"
    if [ -f "$test_config" ]; then
        echo -e "${GREEN}✅ Using existing test configuration: $test_config${NC}"
        
        # Copy test configuration to main resources so it can be used at runtime
        cp "$test_config" "$PROJECT_DIR/discovery-service/src/main/resources/application-test.yml"
        echo -e "${GREEN}✅ Test configuration copied to main resources${NC}"
    else
        echo -e "${YELLOW}⚠️ Test configuration not found, creating basic H2 config...${NC}"
        
        # Create minimal H2 configuration
        cat > "$PROJECT_DIR/discovery-service/src/main/resources/application-test.yml" << 'EOF'
spring:
  autoconfigure:
    exclude:
      - org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration
      - org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration
      - org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration
  datasource:
    url: jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
    driver-class-name: org.h2.Driver
    username: sa
    password: ""
  jpa:
    database-platform: org.hibernate.dialect.H2Dialect
    hibernate:
      ddl-auto: create-drop
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.H2Dialect
  h2:
    console:
      enabled: true
      path: /h2-console
  cache:
    type: simple

oppfinder:
  discovery:
    data-sources:
      github:
        enabled: false
      reddit:
        enabled: false
      hacker-news:
        enabled: false

logging:
  level:
    com.psehrawa.oppfinder: INFO
    org.springframework.web: INFO
    org.hibernate.SQL: INFO
EOF
    fi

    echo -e "${GREEN}✅ Test profile configured${NC}"
    log "Test profile setup completed"
}

# Build application
build_application() {
    echo -e "\n${BLUE}🔨 Building Application...${NC}"
    
    log "Starting application build"
    
    # Build without timeout (command not available)
    echo -e "${CYAN}Running Gradle build...${NC}"
    if ./gradlew build -x test; then
        echo -e "${GREEN}✅ Application built successfully${NC}"
    else
        echo -e "${RED}❌ Application build failed${NC}"
        log "ERROR: Application build failed"
        exit 1
    fi
    
    log "Application build completed successfully"
}

# Start application
start_application() {
    echo -e "\n${BLUE}🚀 Starting Discovery Service (Test Mode)...${NC}"
    
    log "Starting discovery service application in test mode"
    
    # Start application in background with test profile
    echo -e "${CYAN}Launching Spring Boot application with test profile...${NC}"
    SPRING_PROFILES_ACTIVE=test nohup ./gradlew :discovery-service:bootRun > "$LOG_DIR/application.log" 2>&1 &
    local gradle_pid=$!
    
    # Wait for gradle and spring boot to start, then find the actual java process
    echo -e "${YELLOW}Waiting for Spring Boot application to start...${NC}"
    sleep 15
    local app_pid=$(pgrep -f "DiscoveryServiceApplication")
    if [ -z "$app_pid" ]; then
        # Try alternative patterns
        app_pid=$(pgrep -f "spring.profiles.active=test")
        if [ -z "$app_pid" ]; then
            echo -e "${YELLOW}Using gradle PID as fallback: $gradle_pid${NC}"
            app_pid=$gradle_pid
        else
            echo -e "${GREEN}Found Spring Boot process: $app_pid${NC}"
        fi
    else
        echo -e "${GREEN}Found DiscoveryServiceApplication process: $app_pid${NC}"
    fi
    
    echo -e "${YELLOW}Application PID: $app_pid${NC}"
    echo -e "${YELLOW}Logs: tail -f $LOG_DIR/application.log${NC}"
    echo -e "${PURPLE}H2 Console: http://localhost:8090/h2-console${NC}"
    
    # Save PID for later use
    echo $app_pid > "$LOG_DIR/app.pid"
    
    log "Discovery service startup initiated (PID: $app_pid)"
    
    # Give the application some time to start up before health checking
    echo -e "${YELLOW}Waiting for application to initialize...${NC}"
    sleep 5
    
    return $app_pid
}

# Wait for application health
wait_for_health() {
    local app_pid=$1
    echo -e "\n${BLUE}⏳ Waiting for Application Health...${NC}"
    
    log "Waiting for application health check"
    
    local elapsed=0
    local health_url="http://localhost:$APPLICATION_PORT/actuator/health"
    
    echo -e "${CYAN}Health check URL: $health_url${NC}"
    
    while [ $elapsed -lt $HEALTH_CHECK_TIMEOUT ]; do
        # Check health endpoint (don't check process status, as gradle complicates PID tracking)
        if curl -s "$health_url" >/dev/null 2>&1; then
            local health_response=$(curl -s "$health_url")
            # In test mode, we accept UP or DOWN status as long as the endpoint responds
            # and database is healthy (Redis is expected to be down in test mode)
            if echo "$health_response" | grep -q '"db":{"status":"UP"'; then
                echo -e "${GREEN}✅ Application is healthy! (Database UP)${NC}"
                log "Application health check passed"
                return 0
            fi
            # If we get a response but DB is not UP, show the status
            if echo "$health_response" | grep -q '"status":'; then
                local overall_status=$(echo "$health_response" | grep -o '"status":"[^"]*"' | head -1 | cut -d'"' -f4)
                echo -e "${YELLOW}⚠️ Health endpoint responding but overall status: $overall_status${NC}"
            fi
        fi
        
        # Progress indicator
        if [ $((elapsed % 10)) -eq 0 ]; then
            echo -e "${YELLOW}  Still waiting... (${elapsed}s/${HEALTH_CHECK_TIMEOUT}s)${NC}"
        fi
        
        sleep 2
        elapsed=$((elapsed + 2))
    done
    
    echo -e "${RED}❌ Application health check timed out${NC}"
    log "ERROR: Application health check timed out"
    return 1
}

# Validate application
validate_application() {
    echo -e "\n${BLUE}✅ Validating Application...${NC}"
    
    log "Starting application validation"
    
    local base_url="http://localhost:$APPLICATION_PORT"
    local tests_passed=0
    local tests_total=0
    
    # Test 1: Health check
    echo -e "${CYAN}Testing health endpoint...${NC}"
    tests_total=$((tests_total + 1))
    if curl -s "$base_url/actuator/health" | grep -q '"status":"UP"'; then
        echo -e "${GREEN}  ✅ Health check passed${NC}"
        tests_passed=$((tests_passed + 1))
    else
        echo -e "${RED}  ❌ Health check failed${NC}"
    fi
    
    # Test 2: Discovery health
    echo -e "${CYAN}Testing discovery health...${NC}"
    tests_total=$((tests_total + 1))
    if curl -s "$base_url/api/v1/discovery/health" >/dev/null 2>&1; then
        echo -e "${GREEN}  ✅ Discovery health passed${NC}"
        tests_passed=$((tests_passed + 1))
    else
        echo -e "${RED}  ❌ Discovery health failed${NC}"
    fi
    
    # Test 3: Opportunities endpoint
    echo -e "${CYAN}Testing opportunities endpoint...${NC}"
    tests_total=$((tests_total + 1))
    if curl -s "$base_url/api/v1/opportunities" >/dev/null 2>&1; then
        echo -e "${GREEN}  ✅ Opportunities endpoint passed${NC}"
        tests_passed=$((tests_passed + 1))
    else
        echo -e "${RED}  ❌ Opportunities endpoint failed${NC}"
    fi
    
    # Test 4: H2 Console
    echo -e "${CYAN}Testing H2 console...${NC}"
    tests_total=$((tests_total + 1))
    if curl -s "$base_url/h2-console" >/dev/null 2>&1; then
        echo -e "${GREEN}  ✅ H2 console accessible${NC}"
        tests_passed=$((tests_passed + 1))
    else
        echo -e "${RED}  ❌ H2 console failed${NC}"
    fi
    
    echo -e "\n${BLUE}Validation Results: $tests_passed/$tests_total tests passed${NC}"
    
    if [ $tests_passed -eq $tests_total ]; then
        echo -e "${GREEN}🎉 All validation tests passed!${NC}"
        log "Application validation completed successfully ($tests_passed/$tests_total)"
        return 0
    else
        echo -e "${YELLOW}⚠️ Some validation tests failed ($tests_passed/$tests_total)${NC}"
        log "WARNING: Some validation tests failed ($tests_passed/$tests_total)"
        return 1
    fi
}

# Show application info
show_application_info() {
    echo -e "\n${GREEN}🎉 Application Started Successfully!${NC}"
    echo -e "${BLUE}=================================================================="
    echo "                  DEVELOPMENT MODE - APPLICATION INFO"
    echo "=================================================================="
    echo -e "${NC}"
    
    echo -e "\n${PURPLE}🌐 Service URLs:${NC}"
    echo -e "  • Discovery Service:  http://localhost:8090"
    echo -e "  • Health Check:       http://localhost:8090/actuator/health"
    echo -e "  • H2 Database Console: http://localhost:8090/h2-console"
    echo -e "  • Discovery Health:   http://localhost:8090/api/v1/discovery/health"
    
    echo -e "\n${PURPLE}📊 API Endpoints:${NC}"
    echo -e "  • GET  /api/v1/opportunities"
    echo -e "  • POST /api/v1/opportunities/search"
    echo -e "  • GET  /api/v1/discovery/health"
    echo -e "  • GET  /api/v1/discovery/sources"
    
    echo -e "\n${PURPLE}🛠️ Development Commands:${NC}"
    echo -e "  • View logs:          tail -f $LOG_DIR/application.log"
    echo -e "  • Stop application:   ./stop-dev.sh"
    echo -e "  • Run tests:          ./gradlew test"
    
    echo -e "\n${PURPLE}📁 Important Files:${NC}"
    echo -e "  • Application logs:   $LOG_DIR/"
    echo -e "  • Test configuration: discovery-service/src/main/resources/application-test.yml"
    
    if [ -f "$LOG_DIR/app.pid" ]; then
        local app_pid=$(cat "$LOG_DIR/app.pid")
        echo -e "\n${PURPLE}⚙️ Process Information:${NC}"
        echo -e "  • Application PID:    $app_pid"
        echo -e "  • Stop command:       kill $app_pid"
    fi
    
    echo -e "\n${YELLOW}⚠️ Development Mode Notes:${NC}"
    echo -e "  • Using H2 in-memory database (data will be lost on restart)"
    echo -e "  • External APIs disabled (GitHub, Reddit, etc.)"
    echo -e "  • Kafka and Redis disabled"
    echo -e "  • Perfect for testing and development"
    
    echo -e "\n${BLUE}=================================================================="
    echo "Ready for development and testing!"
    echo -e "=================================================================="
    echo -e "${NC}"
    
    log "Development application startup completed successfully"
}

# Create stop script
create_stop_script() {
    cat > "$PROJECT_DIR/stop-dev.sh" << 'EOF'
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
EOF

    chmod +x "$PROJECT_DIR/stop-dev.sh"
}

# Cleanup on script exit
cleanup_on_exit() {
    if [ $? -ne 0 ]; then
        echo -e "\n${RED}❌ Startup failed. Cleaning up...${NC}"
        log "ERROR: Development startup process failed"
        
        # Kill application if started
        if [ -f "$LOG_DIR/app.pid" ]; then
            local app_pid=$(cat "$LOG_DIR/app.pid")
            kill $app_pid 2>/dev/null || true
            rm -f "$LOG_DIR/app.pid"
        fi
        
        echo -e "${YELLOW}Check logs at: $LOG_DIR/startup-dev.log${NC}"
    fi
}

# Main execution
main() {
    # Set up cleanup
    trap cleanup_on_exit EXIT
    
    # Change to project directory
    cd "$PROJECT_DIR"
    
    # Print header
    print_header
    
    # Execute startup sequence
    check_prerequisites
    check_ports
    setup_test_profile
    build_application
    local app_pid=$(start_application)
    
    if wait_for_health $app_pid; then
        validate_application
        create_stop_script
        show_application_info
        
        # Disable cleanup on successful exit
        trap - EXIT
        
        echo -e "\n${GREEN}Press Ctrl+C to stop the application${NC}"
        
        # Keep script running and show log tail
        echo -e "\n${BLUE}📋 Application Logs (live):${NC}"
        tail -f "$LOG_DIR/application.log"
    else
        echo -e "${RED}❌ Application startup failed${NC}"
        exit 1
    fi
}

# Execute main function with all arguments
main "$@"