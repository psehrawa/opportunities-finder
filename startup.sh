#!/bin/bash

# TechOpportunity Intelligence Platform - Complete Startup Script
# This script handles the complete application startup process

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
DOCKER_SERVICES="postgres redis kafka zookeeper"
MANAGEMENT_SERVICES="pgadmin redis-commander kafka-ui"
APPLICATION_PORT=8090
HEALTH_CHECK_TIMEOUT=300  # 5 minutes
BUILD_TIMEOUT=180        # 3 minutes

# Create logs directory
mkdir -p "$LOG_DIR"

# Logging function
log() {
    echo -e "$(date '+%Y-%m-%d %H:%M:%S') - $1" | tee -a "$LOG_DIR/startup.log"
}

# Print header
print_header() {
    echo -e "${BLUE}"
    echo "=================================================================="
    echo "🚀 $APP_NAME"
    echo "    Complete Application Startup"
    echo "=================================================================="
    echo -e "${NC}"
    log "Starting application startup process"
}

# Check prerequisites
check_prerequisites() {
    echo -e "\n${BLUE}📋 Checking Prerequisites...${NC}"
    
    local missing_deps=()
    
    # Check Docker
    if ! command -v docker &> /dev/null; then
        missing_deps+=("Docker")
    fi
    
    # Check Docker Compose
    if ! docker compose version &> /dev/null && ! command -v docker-compose &> /dev/null; then
        missing_deps+=("Docker Compose")
    fi
    
    # Set compose command preference
    if docker compose version &> /dev/null; then
        COMPOSE_CMD="docker compose"
    elif command -v docker-compose &> /dev/null; then
        COMPOSE_CMD="docker-compose"
    else
        missing_deps+=("Docker Compose")
    fi
    
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
    
    local ports=(5432 6379 9092 8090 8080 8081 8082)
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
        echo -e "${GREEN}✅ All required ports are available${NC}"
    fi
    
    log "Port availability check completed"
}

# Clean up previous state
cleanup_previous() {
    echo -e "\n${BLUE}🧹 Cleaning Up Previous State...${NC}"
    
    # Stop and clean Docker containers and volumes to avoid Kafka cluster ID conflicts
    echo -e "${YELLOW}Cleaning Docker containers and volumes...${NC}"
    $COMPOSE_CMD down -v 2>/dev/null || true
    
    # Clean gradle cache if requested
    if [ "$1" = "--clean" ]; then
        echo -e "${YELLOW}Performing clean build...${NC}"
        ./gradlew clean
    fi
    
    echo -e "${GREEN}✅ Cleanup completed${NC}"
    log "Cleanup completed"
}

# Start Docker infrastructure
start_infrastructure() {
    echo -e "\n${BLUE}🐳 Starting Infrastructure Services...${NC}"
    
    log "Starting Docker infrastructure services"
    
    # Start core services
    echo -e "${CYAN}Starting core services: $DOCKER_SERVICES${NC}"
    if $COMPOSE_CMD up -d $DOCKER_SERVICES; then
        echo -e "${GREEN}✅ Core infrastructure services started${NC}"
    else
        echo -e "${RED}❌ Failed to start core infrastructure services${NC}"
        log "ERROR: Failed to start infrastructure services"
        exit 1
    fi
    
    # Wait for services to be ready
    echo -e "\n${YELLOW}⏳ Waiting for services to initialize...${NC}"
    sleep 30
    
    # Check service health with retries
    local failed_services=()
    
    echo -e "${CYAN}Checking PostgreSQL...${NC}"
    if ! docker exec oppfinder-postgres pg_isready -U oppfinder >/dev/null 2>&1; then
        failed_services+=("PostgreSQL")
    else
        echo -e "${GREEN}  ✅ PostgreSQL is ready${NC}"
    fi
    
    echo -e "${CYAN}Checking Redis...${NC}"
    if ! docker exec oppfinder-redis redis-cli ping >/dev/null 2>&1; then
        failed_services+=("Redis")
    else
        echo -e "${GREEN}  ✅ Redis is ready${NC}"
    fi
    
    echo -e "${CYAN}Checking Kafka...${NC}"
    # Kafka needs more time and retries due to Zookeeper dependency
    kafka_ready=false
    for attempt in {1..6}; do
        if docker exec oppfinder-kafka kafka-broker-api-versions --bootstrap-server localhost:9092 >/dev/null 2>&1; then
            kafka_ready=true
            break
        fi
        if [ $attempt -lt 6 ]; then
            echo -e "${YELLOW}  ⏳ Kafka not ready, waiting... (attempt $attempt/6)${NC}"
            sleep 10
        fi
    done
    
    if [ "$kafka_ready" = true ]; then
        echo -e "${GREEN}  ✅ Kafka is ready${NC}"
    else
        failed_services+=("Kafka")
    fi
    
    if [ ${#failed_services[@]} -ne 0 ]; then
        echo -e "${RED}❌ Some services failed to start properly:${NC}"
        for service in "${failed_services[@]}"; do
            echo -e "  • $service"
        done
        echo -e "\n${YELLOW}Check logs with: docker-compose logs [service-name]${NC}"
        log "ERROR: Some infrastructure services failed health check"
        exit 1
    fi
    
    log "Infrastructure services started successfully"
}

# Start management UIs (optional)
start_management_uis() {
    echo -e "\n${BLUE}🖥️ Starting Management UIs...${NC}"
    
    echo -e "${CYAN}Starting management services: $MANAGEMENT_SERVICES${NC}"
    if $COMPOSE_CMD up -d $MANAGEMENT_SERVICES; then
        echo -e "${GREEN}✅ Management UIs started${NC}"
        echo -e "\n${PURPLE}Management URLs:${NC}"
        echo -e "  • pgAdmin:        http://localhost:8081 (admin@oppfinder.com / admin123)"
        echo -e "  • Redis Commander: http://localhost:8082"
        echo -e "  • Kafka UI:       http://localhost:8080"
    else
        echo -e "${YELLOW}⚠️ Management UIs failed to start (optional)${NC}"
    fi
    
    log "Management UIs startup completed"
}

# Build application
build_application() {
    echo -e "\n${BLUE}🔨 Building Application...${NC}"
    
    log "Starting application build"
    
    # Build with timeout (macOS compatible)
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
    echo -e "\n${BLUE}🚀 Starting Discovery Service...${NC}"
    
    log "Starting discovery service application"
    
    # Start application in background with full environment
    echo -e "${CYAN}Launching Spring Boot application with production environment...${NC}"
    SPRING_PROFILES_ACTIVE=dev ./gradlew :discovery-service:bootRun > "$LOG_DIR/application.log" 2>&1 &
    local app_pid=$!
    
    echo -e "${YELLOW}Application PID: $app_pid${NC}"
    echo -e "${YELLOW}Logs: tail -f $LOG_DIR/application.log${NC}"
    
    # Save PID for later use
    echo $app_pid > "$LOG_DIR/app.pid"
    
    log "Discovery service startup initiated (PID: $app_pid)"
    
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
        # Check if process is still running
        if ! kill -0 $app_pid 2>/dev/null; then
            echo -e "${RED}❌ Application process died${NC}"
            log "ERROR: Application process died during startup"
            return 1
        fi
        
        # Check health endpoint
        if curl -s "$health_url" >/dev/null 2>&1; then
            local health_response=$(curl -s "$health_url")
            local health_status=$(echo "$health_response" | grep -o '"status":"[^"]*"' | head -1 | cut -d'"' -f4)
            if [ "$health_status" = "UP" ]; then
                echo -e "${GREEN}✅ Application is healthy! (All services UP)${NC}"
                log "Application health check passed"
                return 0
            else
                # In production mode, show detailed status for debugging
                echo -e "${YELLOW}⚠️ Health endpoint responding but status: $health_status${NC}"
                # Check if at least database is UP for partial health
                if echo "$health_response" | grep -q '"db":{"status":"UP"'; then
                    echo -e "${YELLOW}  Database is UP, continuing...${NC}"
                fi
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
    
    # Test 4: Data sources status
    echo -e "${CYAN}Testing data sources status...${NC}"
    tests_total=$((tests_total + 1))
    if curl -s "$base_url/api/v1/discovery/sources" >/dev/null 2>&1; then
        echo -e "${GREEN}  ✅ Data sources status passed${NC}"
        tests_passed=$((tests_passed + 1))
    else
        echo -e "${RED}  ❌ Data sources status failed${NC}"
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

# Run basic discovery test
run_discovery_test() {
    echo -e "\n${BLUE}🔍 Running Discovery Test...${NC}"
    
    log "Running basic discovery test"
    
    local base_url="http://localhost:$APPLICATION_PORT"
    
    echo -e "${CYAN}Triggering discovery...${NC}"
    if curl -s -X POST "$base_url/api/v1/discovery/trigger?limitPerSource=3" >/dev/null 2>&1; then
        echo -e "${GREEN}✅ Discovery triggered successfully${NC}"
        
        echo -e "${CYAN}Triggering scoring...${NC}"
        if curl -s -X POST "$base_url/api/v1/discovery/scoring/trigger" >/dev/null 2>&1; then
            echo -e "${GREEN}✅ Scoring triggered successfully${NC}"
        else
            echo -e "${YELLOW}⚠️ Scoring trigger failed${NC}"
        fi
    else
        echo -e "${YELLOW}⚠️ Discovery trigger failed${NC}"
    fi
    
    log "Discovery test completed"
}

# Show application info
show_application_info() {
    echo -e "\n${GREEN}🎉 Application Started Successfully!${NC}"
    echo -e "${BLUE}=================================================================="
    echo "                    APPLICATION INFORMATION"
    echo "==================================================================${NC}"
    
    echo -e "\n${PURPLE}🌐 Service URLs:${NC}"
    echo -e "  • Discovery Service:  http://localhost:8090"
    echo -e "  • Health Check:       http://localhost:8090/actuator/health"
    echo -e "  • API Documentation:  http://localhost:8090/actuator"
    echo -e "  • Discovery Health:   http://localhost:8090/api/v1/discovery/health"
    
    echo -e "\n${PURPLE}🎛️ Management UIs:${NC}"
    echo -e "  • pgAdmin:           http://localhost:8081 (admin@oppfinder.com / admin123)"
    echo -e "  • Redis Commander:   http://localhost:8082"
    echo -e "  • Kafka UI:          http://localhost:8080"
    
    echo -e "\n${PURPLE}📊 API Endpoints:${NC}"
    echo -e "  • GET  /api/v1/opportunities"
    echo -e "  • POST /api/v1/opportunities/search"
    echo -e "  • POST /api/v1/discovery/trigger"
    echo -e "  • GET  /api/v1/discovery/sources"
    
    echo -e "\n${PURPLE}🛠️ Development Commands:${NC}"
    echo -e "  • View logs:          tail -f $LOG_DIR/application.log"
    echo -e "  • Stop application:   ./stop.sh"
    echo -e "  • Run tests:          ./scripts/test-basic-functionality.sh"
    echo -e "  • Performance test:   ./scripts/performance-test.sh"
    
    echo -e "\n${PURPLE}📁 Important Files:${NC}"
    echo -e "  • Application logs:   $LOG_DIR/"
    echo -e "  • Docker logs:        docker-compose logs [service]"
    echo -e "  • Configuration:      discovery-service/src/main/resources/application.yml"
    
    if [ -f "$LOG_DIR/app.pid" ]; then
        local app_pid=$(cat "$LOG_DIR/app.pid")
        echo -e "\n${PURPLE}⚙️ Process Information:${NC}"
        echo -e "  • Application PID:    $app_pid"
        echo -e "  • Stop command:       kill $app_pid"
    fi
    
    echo -e "\n${BLUE}=================================================================="
    echo "Ready for development and testing!"
    echo -e "==================================================================${NC}"
    
    log "Application startup completed successfully"
}

# Create stop script
create_stop_script() {
    cat > "$PROJECT_DIR/stop.sh" << 'EOF'
#!/bin/bash

# TechOpportunity Intelligence Platform - Stop Script

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LOG_DIR="$PROJECT_DIR/logs"

echo -e "${BLUE}🛑 Stopping TechOpportunity Intelligence Platform${NC}"

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

# Stop Docker services
echo -e "${YELLOW}Stopping Docker services...${NC}"
if docker compose version &> /dev/null; then
    docker compose down
elif command -v docker-compose &> /dev/null; then
    docker-compose down
else
    echo -e "${RED}No Docker Compose found${NC}"
fi

echo -e "${GREEN}🎉 All services stopped${NC}"
EOF

    chmod +x "$PROJECT_DIR/stop.sh"
}

# Cleanup on script exit
cleanup_on_exit() {
    if [ $? -ne 0 ]; then
        echo -e "\n${RED}❌ Startup failed. Cleaning up...${NC}"
        log "ERROR: Startup process failed"
        
        # Stop any started services and clean volumes to avoid Kafka cluster ID conflicts
        $COMPOSE_CMD down -v 2>/dev/null || true
        
        # Kill application if started
        if [ -f "$LOG_DIR/app.pid" ]; then
            local app_pid=$(cat "$LOG_DIR/app.pid")
            kill $app_pid 2>/dev/null || true
            rm -f "$LOG_DIR/app.pid"
        fi
        
        echo -e "${YELLOW}Check logs at: $LOG_DIR/startup.log${NC}"
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
    
    # Check command line arguments
    local clean_build=false
    local skip_tests=false
    local skip_management=false
    
    while [[ $# -gt 0 ]]; do
        case $1 in
            --clean)
                clean_build=true
                shift
                ;;
            --skip-tests)
                skip_tests=true
                shift
                ;;
            --skip-management)
                skip_management=true
                shift
                ;;
            --help)
                echo "Usage: $0 [OPTIONS]"
                echo "Options:"
                echo "  --clean            Clean build (gradle clean)"
                echo "  --skip-tests       Skip validation tests"
                echo "  --skip-management  Skip management UI startup"
                echo "  --help             Show this help message"
                exit 0
                ;;
            *)
                echo "Unknown option: $1"
                echo "Use --help for usage information"
                exit 1
                ;;
        esac
    done
    
    # Execute startup sequence
    check_prerequisites
    check_ports
    cleanup_previous $($clean_build && echo "--clean")
    start_infrastructure
    
    if [ "$skip_management" != true ]; then
        start_management_uis
    fi
    
    build_application
    local app_pid=$(start_application)
    
    if wait_for_health $app_pid; then
        if [ "$skip_tests" != true ]; then
            validate_application
            run_discovery_test
        fi
        
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