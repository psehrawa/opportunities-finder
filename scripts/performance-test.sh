#!/bin/bash

# TechOpportunity Intelligence Platform - Performance Test Script

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
BASE_URL="http://localhost:8090"
CONCURRENT_USERS=10
TEST_DURATION=60  # seconds
RESULTS_DIR="performance-results-$(date +%Y%m%d-%H%M%S)"

echo -e "${BLUE}⚡ TechOpportunity Intelligence Platform - Performance Tests${NC}"
echo "============================================================="

# Create results directory
mkdir -p "$RESULTS_DIR"

# Function to run load test
run_load_test() {
    local test_name="$1"
    local endpoint="$2"
    local method="$3"
    local data="$4"
    local concurrent="$5"
    local duration="$6"
    
    echo -e "\n${BLUE}🔥 Load Test: $test_name${NC}"
    echo "Endpoint: $method $endpoint"
    echo "Concurrent Users: $concurrent"
    echo "Duration: ${duration}s"
    
    local output_file="$RESULTS_DIR/${test_name// /_}.log"
    
    if [ "$method" = "GET" ]; then
        ab -n 1000 -c "$concurrent" -t "$duration" -g "$output_file.tsv" "$BASE_URL$endpoint" > "$output_file" 2>&1
    elif [ "$method" = "POST" ]; then
        # Create temporary file with POST data
        local post_data_file="$RESULTS_DIR/post_data_${test_name// /_}.json"
        echo "$data" > "$post_data_file"
        
        ab -n 1000 -c "$concurrent" -t "$duration" -p "$post_data_file" -T "application/json" -g "$output_file.tsv" "$BASE_URL$endpoint" > "$output_file" 2>&1
    fi
    
    # Parse results
    if [ -f "$output_file" ]; then
        local requests_per_second=$(grep "Requests per second" "$output_file" | awk '{print $4}' || echo "N/A")
        local mean_time=$(grep "Time per request" "$output_file" | head -1 | awk '{print $4}' || echo "N/A")
        local failed_requests=$(grep "Failed requests" "$output_file" | awk '{print $3}' || echo "N/A")
        
        echo "Results:"
        echo "  • Requests per second: $requests_per_second"
        echo "  • Mean response time: ${mean_time}ms"
        echo "  • Failed requests: $failed_requests"
        
        # Performance criteria
        if (( $(echo "$requests_per_second > 50" | bc -l 2>/dev/null) )); then
            echo -e "  • ${GREEN}✅ Performance: Good (>50 req/s)${NC}"
        elif (( $(echo "$requests_per_second > 20" | bc -l 2>/dev/null) )); then
            echo -e "  • ${YELLOW}⚠️ Performance: Acceptable (>20 req/s)${NC}"
        else
            echo -e "  • ${RED}❌ Performance: Poor (<20 req/s)${NC}"
        fi
    else
        echo -e "${RED}❌ Failed to generate load test results${NC}"
    fi
}

# Function to run stress test
run_stress_test() {
    local endpoint="$1"
    local max_concurrent="$2"
    
    echo -e "\n${BLUE}🎯 Stress Test: Finding Breaking Point${NC}"
    echo "Endpoint: $endpoint"
    echo "Max Concurrent Users: $max_concurrent"
    
    for concurrent in 5 10 20 50 100; do
        if [ $concurrent -gt $max_concurrent ]; then
            break
        fi
        
        echo -e "\n  Testing with $concurrent concurrent users..."
        local output_file="$RESULTS_DIR/stress_test_${concurrent}users.log"
        
        ab -n 100 -c $concurrent "$BASE_URL$endpoint" > "$output_file" 2>&1
        
        if [ -f "$output_file" ]; then
            local rps=$(grep "Requests per second" "$output_file" | awk '{print $4}' || echo "0")
            local failed=$(grep "Failed requests" "$output_file" | awk '{print $3}' || echo "0")
            
            echo "    • RPS: $rps, Failed: $failed"
            
            if [ "$failed" != "0" ]; then
                echo -e "    • ${RED}Breaking point reached at $concurrent users${NC}"
                break
            fi
        fi
    done
}

# Check if Apache Bench is available
if ! command -v ab &> /dev/null; then
    echo -e "${YELLOW}⚠️ Apache Bench (ab) not found. Installing basic alternatives...${NC}"
    
    # Alternative using curl for basic performance testing
    echo -e "${BLUE}📊 Running basic performance tests with curl...${NC}"
    
    # Response time test
    echo -e "\n${BLUE}Response Time Test${NC}"
    for i in {1..10}; do
        time_taken=$(curl -w "@-" -o /dev/null -s "$BASE_URL/api/v1/opportunities" <<< 'time_total')
        echo "Request $i: ${time_taken}s"
        sleep 1
    done
    
    # Simple concurrent test
    echo -e "\n${BLUE}Simple Concurrent Test (5 parallel requests)${NC}"
    for i in {1..5}; do
        curl -s "$BASE_URL/api/v1/opportunities" > /dev/null &
    done
    wait
    echo "All 5 concurrent requests completed"
    
    exit 0
fi

# Check if application is running
echo -e "${YELLOW}🔍 Checking if application is running...${NC}"
if ! curl -s "$BASE_URL/actuator/health" >/dev/null 2>&1; then
    echo -e "${RED}❌ Application is not running. Please start it first:${NC}"
    echo "   docker-compose up -d"
    echo "   ./gradlew :discovery-service:bootRun"
    exit 1
fi

echo -e "${GREEN}✅ Application is running${NC}"

# PERFORMANCE TEST SUITE
echo -e "\n${BLUE}🚀 Starting Performance Tests${NC}"

# Test 1: Opportunity List Endpoint
run_load_test \
    "Opportunity List" \
    "/api/v1/opportunities" \
    "GET" \
    "" \
    $CONCURRENT_USERS \
    $TEST_DURATION

# Test 2: Search Endpoint
run_load_test \
    "Opportunity Search" \
    "/api/v1/opportunities/search" \
    "POST" \
    '{"page": 0, "size": 20, "sortBy": "discoveredAt"}' \
    $CONCURRENT_USERS \
    $TEST_DURATION

# Test 3: Health Check Endpoint
run_load_test \
    "Health Check" \
    "/actuator/health" \
    "GET" \
    "" \
    $((CONCURRENT_USERS * 2)) \
    $((TEST_DURATION / 2))

# Test 4: Discovery Health
run_load_test \
    "Discovery Health" \
    "/api/v1/discovery/health" \
    "GET" \
    "" \
    $CONCURRENT_USERS \
    $((TEST_DURATION / 2))

# Stress Test
run_stress_test "/api/v1/opportunities" 100

# MEMORY AND RESOURCE MONITORING
echo -e "\n${BLUE}💾 Resource Usage Monitoring${NC}"

if command -v ps &> /dev/null; then
    echo "Java Process Memory Usage:"
    ps aux | grep java | grep -v grep | awk '{print "  • PID: " $2 ", Memory: " $4 "%, CPU: " $3 "%"}'
fi

if command -v docker &> /dev/null; then
    echo -e "\nDocker Container Stats:"
    docker stats --no-stream --format "table {{.Container}}\t{{.CPUPerc}}\t{{.MemUsage}}" 2>/dev/null || echo "  • Docker stats not available"
fi

# GENERATE PERFORMANCE REPORT
echo -e "\n${BLUE}📊 Generating Performance Report${NC}"

{
    echo "# TechOpportunity Platform - Performance Test Report"
    echo "Generated: $(date)"
    echo ""
    echo "## Test Configuration"
    echo "- Base URL: $BASE_URL"
    echo "- Concurrent Users: $CONCURRENT_USERS"
    echo "- Test Duration: ${TEST_DURATION}s"
    echo ""
    echo "## Test Results"
    echo ""
    
    for log_file in "$RESULTS_DIR"/*.log; do
        if [ -f "$log_file" ]; then
            test_name=$(basename "$log_file" .log | tr '_' ' ')
            echo "### $test_name"
            
            if grep -q "Requests per second" "$log_file"; then
                rps=$(grep "Requests per second" "$log_file" | awk '{print $4}')
                mean_time=$(grep "Time per request" "$log_file" | head -1 | awk '{print $4}')
                failed=$(grep "Failed requests" "$log_file" | awk '{print $3}')
                
                echo "- Requests per second: $rps"
                echo "- Mean response time: ${mean_time}ms"
                echo "- Failed requests: $failed"
            fi
            echo ""
        fi
    done
    
    echo "## Recommendations"
    echo ""
    echo "### Performance Optimization"
    echo "- Monitor database query performance"
    echo "- Consider implementing response caching"
    echo "- Optimize JVM memory settings for production"
    echo ""
    echo "### Scaling Considerations"
    echo "- Implement horizontal scaling with multiple instances"
    echo "- Use load balancer for traffic distribution"
    echo "- Consider database read replicas for heavy read workloads"
    
} > "$RESULTS_DIR/performance-report.md"

echo -e "Performance report saved to: ${BLUE}$RESULTS_DIR/performance-report.md${NC}"

# RECOMMENDATIONS
echo -e "\n${BLUE}💡 Performance Recommendations${NC}"
echo "================================="
echo "• Review detailed results in: $RESULTS_DIR/"
echo "• Monitor application logs during high load"
echo "• Consider database indexing optimization"
echo "• Implement caching for frequently accessed data"
echo "• Use connection pooling for database connections"
echo "• Consider implementing rate limiting for production"

echo -e "\n${GREEN}🏁 Performance testing completed!${NC}"
echo "Results directory: $RESULTS_DIR"