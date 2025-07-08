#!/bin/bash

# Smoke Test Script
set -e

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

BASE_URL=${1:-"http://localhost:8090"}
echo -e "${YELLOW}🔥 Running smoke tests against: ${BASE_URL}${NC}"

# Track test results
TESTS_PASSED=0
TESTS_FAILED=0

# Function to test endpoint
test_endpoint() {
    local endpoint=$1
    local expected_status=$2
    local description=$3
    
    echo -n "Testing ${description}... "
    
    response=$(curl -s -o /dev/null -w "%{http_code}" "${BASE_URL}${endpoint}")
    
    if [ "$response" -eq "$expected_status" ]; then
        echo -e "${GREEN}✅ PASSED${NC} (Status: $response)"
        TESTS_PASSED=$((TESTS_PASSED + 1))
    else
        echo -e "${RED}❌ FAILED${NC} (Expected: $expected_status, Got: $response)"
        TESTS_FAILED=$((TESTS_FAILED + 1))
    fi
}

echo -e "${YELLOW}Running health checks...${NC}"
test_endpoint "/actuator/health" 200 "Health check"
test_endpoint "/actuator/info" 200 "Info endpoint"
test_endpoint "/actuator/metrics" 200 "Metrics endpoint"

echo -e "\n${YELLOW}Testing API endpoints...${NC}"
test_endpoint "/api/v1/opportunities" 200 "List opportunities"
test_endpoint "/api/v1/discovery/health" 200 "Discovery health"
test_endpoint "/api/v1/discovery/sources" 200 "Data sources"

echo -e "\n${YELLOW}Testing error handling...${NC}"
test_endpoint "/api/v1/opportunities/99999" 404 "Non-existent opportunity"
test_endpoint "/api/v1/invalid-endpoint" 404 "Invalid endpoint"

# Test with authentication (if JWT token is available)
if [ ! -z "$TEST_JWT_TOKEN" ]; then
    echo -e "\n${YELLOW}Testing authenticated endpoints...${NC}"
    
    response=$(curl -s -o /dev/null -w "%{http_code}" \
        -H "Authorization: Bearer $TEST_JWT_TOKEN" \
        "${BASE_URL}/api/v1/discovery/trigger")
    
    if [ "$response" -eq "200" ] || [ "$response" -eq "202" ]; then
        echo -e "Discovery trigger... ${GREEN}✅ PASSED${NC} (Status: $response)"
        TESTS_PASSED=$((TESTS_PASSED + 1))
    else
        echo -e "Discovery trigger... ${RED}❌ FAILED${NC} (Got: $response)"
        TESTS_FAILED=$((TESTS_FAILED + 1))
    fi
fi

# Summary
echo -e "\n${YELLOW}📊 Test Summary:${NC}"
echo -e "  Passed: ${GREEN}${TESTS_PASSED}${NC}"
echo -e "  Failed: ${RED}${TESTS_FAILED}${NC}"

if [ $TESTS_FAILED -eq 0 ]; then
    echo -e "\n${GREEN}✅ All smoke tests passed!${NC}"
    exit 0
else
    echo -e "\n${RED}❌ Some tests failed!${NC}"
    exit 1
fi