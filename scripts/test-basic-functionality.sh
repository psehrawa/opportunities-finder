#!/bin/bash

# TechOpportunity Intelligence Platform - Basic Functionality Test Script

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
BASE_URL="http://localhost:8090"
TEST_RESULTS_FILE="test-results-$(date +%Y%m%d-%H%M%S).json"

echo -e "${BLUE}🧪 TechOpportunity Intelligence Platform - Basic Functionality Tests${NC}"
echo "=================================================================="

# Test results tracking
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0
TEST_RESULTS=()

# Function to run a test
run_test() {
    local test_name="$1"
    local test_command="$2"
    local expected_status="$3"
    local description="$4"
    
    TOTAL_TESTS=$((TOTAL_TESTS + 1))
    
    echo -e "\n${BLUE}Test $TOTAL_TESTS: $test_name${NC}"
    echo "Description: $description"
    echo "Command: $test_command"
    
    # Execute the test
    if eval "$test_command" >/dev/null 2>&1; then
        local actual_status=$?
        if [ "$actual_status" -eq "$expected_status" ]; then
            echo -e "${GREEN}✅ PASSED${NC}"
            PASSED_TESTS=$((PASSED_TESTS + 1))
            TEST_RESULTS+=("{\"test\": \"$test_name\", \"status\": \"PASSED\", \"command\": \"$test_command\"}")
        else
            echo -e "${RED}❌ FAILED - Expected status $expected_status, got $actual_status${NC}"
            FAILED_TESTS=$((FAILED_TESTS + 1))
            TEST_RESULTS+=("{\"test\": \"$test_name\", \"status\": \"FAILED\", \"reason\": \"Status code mismatch\", \"command\": \"$test_command\"}")
        fi
    else
        echo -e "${RED}❌ FAILED - Command execution error${NC}"
        FAILED_TESTS=$((FAILED_TESTS + 1))
        TEST_RESULTS+=("{\"test\": \"$test_name\", \"status\": \"FAILED\", \"reason\": \"Command execution error\", \"command\": \"$test_command\"}")
    fi
}

# Function to run HTTP test with response validation
run_http_test() {
    local test_name="$1"
    local method="$2"
    local endpoint="$3"
    local expected_status="$4"
    local description="$5"
    local data="$6"
    
    TOTAL_TESTS=$((TOTAL_TESTS + 1))
    
    echo -e "\n${BLUE}Test $TOTAL_TESTS: $test_name${NC}"
    echo "Description: $description"
    echo "Endpoint: $method $endpoint"
    
    # Build curl command
    local curl_cmd="curl -s -w '%{http_code}' -o /dev/null"
    
    if [ "$method" = "POST" ] && [ -n "$data" ]; then
        curl_cmd="$curl_cmd -X POST -H 'Content-Type: application/json' -d '$data'"
    elif [ "$method" = "PUT" ]; then
        curl_cmd="$curl_cmd -X PUT"
    fi
    
    curl_cmd="$curl_cmd $BASE_URL$endpoint"
    
    # Execute the test
    local actual_status
    actual_status=$(eval "$curl_cmd" 2>/dev/null || echo "000")
    
    if [ "$actual_status" = "$expected_status" ]; then
        echo -e "${GREEN}✅ PASSED - HTTP $actual_status${NC}"
        PASSED_TESTS=$((PASSED_TESTS + 1))
        TEST_RESULTS+=("{\"test\": \"$test_name\", \"status\": \"PASSED\", \"http_status\": \"$actual_status\"}")
    else
        echo -e "${RED}❌ FAILED - Expected HTTP $expected_status, got $actual_status${NC}"
        FAILED_TESTS=$((FAILED_TESTS + 1))
        TEST_RESULTS+=("{\"test\": \"$test_name\", \"status\": \"FAILED\", \"expected\": \"$expected_status\", \"actual\": \"$actual_status\"}")
    fi
}

# Wait for application to be ready
echo -e "${YELLOW}🔍 Checking if application is running...${NC}"
for i in {1..30}; do
    if curl -s "$BASE_URL/actuator/health" >/dev/null 2>&1; then
        echo -e "${GREEN}✅ Application is ready${NC}"
        break
    fi
    if [ $i -eq 30 ]; then
        echo -e "${RED}❌ Application is not running. Please start it first:${NC}"
        echo "   docker-compose up -d"
        echo "   ./gradlew :discovery-service:bootRun"
        exit 1
    fi
    echo "Waiting for application... ($i/30)"
    sleep 2
done

echo -e "\n${BLUE}🚀 Starting Basic Functionality Tests${NC}"

# TEST SUITE 1: Health and Actuator Endpoints
echo -e "\n${YELLOW}📊 Test Suite 1: Health and Actuator Endpoints${NC}"

run_http_test \
    "Application Health Check" \
    "GET" \
    "/actuator/health" \
    "200" \
    "Verify application health endpoint responds"

run_http_test \
    "Discovery Health Check" \
    "GET" \
    "/api/v1/discovery/health" \
    "200" \
    "Verify discovery service health endpoint"

run_http_test \
    "Data Sources Status" \
    "GET" \
    "/api/v1/discovery/sources" \
    "200" \
    "Verify data sources information endpoint"

# TEST SUITE 2: Opportunity Management
echo -e "\n${YELLOW}📋 Test Suite 2: Opportunity Management${NC}"

run_http_test \
    "Get All Opportunities" \
    "GET" \
    "/api/v1/opportunities" \
    "200" \
    "Retrieve all opportunities with default pagination"

run_http_test \
    "Search Opportunities" \
    "POST" \
    "/api/v1/opportunities/search" \
    "200" \
    "Search opportunities with criteria" \
    '{"page": 0, "size": 10, "sortBy": "discoveredAt", "sortDirection": "DESC"}'

run_http_test \
    "Get Non-existent Opportunity" \
    "GET" \
    "/api/v1/opportunities/999999" \
    "404" \
    "Verify proper handling of non-existent opportunity"

run_http_test \
    "Get Trending Opportunities" \
    "GET" \
    "/api/v1/opportunities/trending?minScore=70&hoursBack=24" \
    "200" \
    "Retrieve trending opportunities"

run_http_test \
    "Get Unscored Opportunities" \
    "GET" \
    "/api/v1/opportunities/unscored?hoursBack=24" \
    "200" \
    "Retrieve opportunities needing scoring"

# TEST SUITE 3: Discovery Operations
echo -e "\n${YELLOW}🔍 Test Suite 3: Discovery Operations${NC}"

run_http_test \
    "Trigger Manual Discovery" \
    "POST" \
    "/api/v1/discovery/trigger?limitPerSource=5" \
    "200" \
    "Trigger manual discovery from all sources"

run_http_test \
    "Trigger GitHub Discovery" \
    "POST" \
    "/api/v1/discovery/trigger/github?limit=3" \
    "200" \
    "Trigger discovery from GitHub specifically"

run_http_test \
    "Trigger Invalid Source Discovery" \
    "POST" \
    "/api/v1/discovery/trigger/invalid-source" \
    "200" \
    "Verify handling of invalid data source"

run_http_test \
    "Trigger Scoring" \
    "POST" \
    "/api/v1/discovery/scoring/trigger" \
    "200" \
    "Trigger manual scoring of opportunities"

# TEST SUITE 4: API Validation
echo -e "\n${YELLOW}🛡️ Test Suite 4: API Validation${NC}"

run_http_test \
    "Invalid Search Criteria" \
    "POST" \
    "/api/v1/opportunities/search" \
    "200" \
    "Search with empty criteria should still work" \
    '{}'

run_http_test \
    "Invalid Score Update" \
    "PUT" \
    "/api/v1/opportunities/1/score?score=150" \
    "400" \
    "Verify score validation (>100 should fail)"

run_http_test \
    "Invalid Status Update" \
    "PUT" \
    "/api/v1/opportunities/1/status?status=INVALID_STATUS" \
    "400" \
    "Verify status validation with invalid enum"

# TEST SUITE 5: Performance Tests (Basic)
echo -e "\n${YELLOW}⚡ Test Suite 5: Basic Performance Tests${NC}"

# Test response time for search endpoint
echo -e "\n${BLUE}Test: Search Response Time${NC}"
echo "Description: Measure response time for search endpoint"

search_response_time=$(curl -w "@-" -o /dev/null -s "$BASE_URL/api/v1/opportunities/search" \
  -X POST \
  -H "Content-Type: application/json" \
  -d '{"page": 0, "size": 20}' \
  <<< 'time_total')

if (( $(echo "$search_response_time < 2.0" | bc -l) )); then
    echo -e "${GREEN}✅ PASSED - Response time: ${search_response_time}s${NC}"
    PASSED_TESTS=$((PASSED_TESTS + 1))
    TEST_RESULTS+=("{\"test\": \"Search Response Time\", \"status\": \"PASSED\", \"response_time\": \"${search_response_time}s\"}")
else
    echo -e "${RED}❌ FAILED - Response time too slow: ${search_response_time}s${NC}"
    FAILED_TESTS=$((FAILED_TESTS + 1))
    TEST_RESULTS+=("{\"test\": \"Search Response Time\", \"status\": \"FAILED\", \"response_time\": \"${search_response_time}s\"}")
fi
TOTAL_TESTS=$((TOTAL_TESTS + 1))

# TEST SUITE 6: Data Validation
echo -e "\n${YELLOW}📊 Test Suite 6: Data Validation${NC}"

# Check if any opportunities were discovered
echo -e "\n${BLUE}Test: Opportunity Discovery Validation${NC}"
echo "Description: Check if opportunities were discovered and have valid data"

opportunities_response=$(curl -s "$BASE_URL/api/v1/opportunities?size=5")
opportunities_count=$(echo "$opportunities_response" | jq -r '.totalElements // 0' 2>/dev/null || echo "0")

if [ "$opportunities_count" -gt 0 ]; then
    echo -e "${GREEN}✅ PASSED - Found $opportunities_count opportunities${NC}"
    PASSED_TESTS=$((PASSED_TESTS + 1))
    TEST_RESULTS+=("{\"test\": \"Opportunity Discovery\", \"status\": \"PASSED\", \"count\": $opportunities_count}")
    
    # Validate opportunity data structure
    echo -e "\n${BLUE}Test: Opportunity Data Structure${NC}"
    first_opportunity=$(echo "$opportunities_response" | jq -r '.content[0] // {}' 2>/dev/null)
    
    if echo "$first_opportunity" | jq -e '.id and .title and .source and .discoveredAt' >/dev/null 2>&1; then
        echo -e "${GREEN}✅ PASSED - Opportunity data structure is valid${NC}"
        PASSED_TESTS=$((PASSED_TESTS + 1))
        TEST_RESULTS+=("{\"test\": \"Opportunity Data Structure\", \"status\": \"PASSED\"}")
    else
        echo -e "${RED}❌ FAILED - Opportunity data structure is invalid${NC}"
        FAILED_TESTS=$((FAILED_TESTS + 1))
        TEST_RESULTS+=("{\"test\": \"Opportunity Data Structure\", \"status\": \"FAILED\"}")
    fi
    TOTAL_TESTS=$((TOTAL_TESTS + 1))
else
    echo -e "${YELLOW}⚠️ WARNING - No opportunities found (this might be expected for new installation)${NC}"
    TEST_RESULTS+=("{\"test\": \"Opportunity Discovery\", \"status\": \"WARNING\", \"count\": 0}")
fi
TOTAL_TESTS=$((TOTAL_TESTS + 1))

# FINAL RESULTS
echo -e "\n${BLUE}📊 Test Results Summary${NC}"
echo "================================="
echo -e "Total Tests: $TOTAL_TESTS"
echo -e "${GREEN}Passed: $PASSED_TESTS${NC}"
echo -e "${RED}Failed: $FAILED_TESTS${NC}"

if [ $FAILED_TESTS -eq 0 ]; then
    echo -e "\n${GREEN}🎉 All tests passed! The application is working correctly.${NC}"
    exit_code=0
else
    echo -e "\n${RED}❌ Some tests failed. Please check the application configuration.${NC}"
    exit_code=1
fi

# Generate detailed test report
echo -e "\n${BLUE}📄 Generating detailed test report...${NC}"
{
    echo "{"
    echo "  \"summary\": {"
    echo "    \"total\": $TOTAL_TESTS,"
    echo "    \"passed\": $PASSED_TESTS,"
    echo "    \"failed\": $FAILED_TESTS,"
    echo "    \"timestamp\": \"$(date -u +%Y-%m-%dT%H:%M:%SZ)\","
    echo "    \"application_url\": \"$BASE_URL\""
    echo "  },"
    echo "  \"results\": ["
    
    for i in "${!TEST_RESULTS[@]}"; do
        echo "    ${TEST_RESULTS[$i]}"
        if [ $i -lt $((${#TEST_RESULTS[@]} - 1)) ]; then
            echo ","
        fi
    done
    
    echo "  ]"
    echo "}"
} > "$TEST_RESULTS_FILE"

echo -e "Test report saved to: ${BLUE}$TEST_RESULTS_FILE${NC}"

# Performance Summary
echo -e "\n${BLUE}⚡ Performance Summary${NC}"
echo "================================="
echo "Search endpoint response time: ${search_response_time}s"
echo "Target response time: < 2.0s"

# Recommendations
echo -e "\n${BLUE}💡 Recommendations${NC}"
echo "================================="
if [ $FAILED_TESTS -gt 0 ]; then
    echo "• Check application logs for detailed error information"
    echo "• Verify all required services are running (PostgreSQL, Redis, Kafka)"
    echo "• Ensure proper network connectivity to external APIs"
fi

if (( $(echo "$search_response_time > 1.0" | bc -l) )); then
    echo "• Consider optimizing database queries for better performance"
    echo "• Monitor database connection pool settings"
fi

echo -e "\n${BLUE}🔗 Useful URLs${NC}"
echo "================================="
echo "• Application Health: $BASE_URL/actuator/health"
echo "• Discovery Health: $BASE_URL/api/v1/discovery/health"
echo "• All Opportunities: $BASE_URL/api/v1/opportunities"
echo "• Trending Opportunities: $BASE_URL/api/v1/opportunities/trending"

exit $exit_code