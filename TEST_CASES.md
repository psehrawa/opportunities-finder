# TechOpportunity Intelligence Platform - Comprehensive Test Cases

## 🧪 Testing Overview

This document provides comprehensive test cases for manual and automated testing of the TechOpportunity Intelligence Platform.

---

## 📋 **MANUAL TEST CASES**

### **TEST SUITE 1: Infrastructure & Application Startup**

#### **TC001: Application Startup**
- **Objective:** Verify application starts successfully with all dependencies
- **Prerequisites:** Docker, Java 17+, Gradle
- **Steps:**
  1. Start infrastructure: `docker-compose up -d postgres redis kafka zookeeper`
  2. Wait 30 seconds for services to initialize
  3. Start application: `./gradlew :discovery-service:bootRun`
  4. Wait for "Started DiscoveryServiceApplication" message
- **Expected Results:**
  - All Docker containers running (postgres, redis, kafka, zookeeper)
  - Application starts on port 8090
  - Health check passes: `curl http://localhost:8090/actuator/health`
- **Pass Criteria:** HTTP 200 response with status "UP"

#### **TC002: Database Connectivity**
- **Objective:** Verify database connection and schema creation
- **Steps:**
  1. Connect to pgAdmin: http://localhost:8081
  2. Login: admin@oppfinder.com / admin123
  3. Navigate to oppfinder_discovery database
  4. Check tables: opportunities, opportunity_tags, opportunity_metadata
- **Expected Results:**
  - Tables created with correct schema
  - Indexes present on key columns
- **Pass Criteria:** All required tables and indexes exist

#### **TC003: Cache Connectivity**
- **Objective:** Verify Redis connection and functionality
- **Steps:**
  1. Connect to Redis Commander: http://localhost:8082
  2. Check connection to Redis instance
  3. Verify rate limiting keys appear after API calls
- **Expected Results:**
  - Redis connection established
  - Keys visible in Redis Commander
- **Pass Criteria:** Redis responsive and caching functional

---

### **TEST SUITE 2: Opportunity Management APIs**

#### **TC004: Create Opportunity via Search API**
- **Objective:** Test opportunity creation through search endpoint
- **Method:** POST
- **URL:** `http://localhost:8090/api/v1/opportunities/search`
- **Headers:** `Content-Type: application/json`
- **Body:**
```json
{
  "page": 0,
  "size": 20,
  "sortBy": "discoveredAt",
  "sortDirection": "DESC",
  "isActive": true
}
```
- **Expected Results:**
  - HTTP 200 response
  - Empty content array initially
  - Proper pagination metadata
- **Pass Criteria:** Valid response structure with pagination

#### **TC005: Get All Opportunities**
- **Objective:** Test opportunity listing with default parameters
- **Method:** GET
- **URL:** `http://localhost:8090/api/v1/opportunities`
- **Expected Results:**
  - HTTP 200 response
  - Paginated list of opportunities
  - Default page size 20, sorted by discoveredAt DESC
- **Pass Criteria:** Valid paginated response

#### **TC006: Get Opportunity by ID**
- **Objective:** Test retrieving specific opportunity
- **Method:** GET
- **URL:** `http://localhost:8090/api/v1/opportunities/{id}`
- **Test Cases:**
  - **TC006a:** Valid existing ID → HTTP 200 with opportunity data
  - **TC006b:** Non-existent ID → HTTP 404
  - **TC006c:** Invalid ID format → HTTP 400
- **Pass Criteria:** Correct HTTP status and response format

#### **TC007: Update Opportunity Status**
- **Objective:** Test opportunity status updates
- **Method:** PUT
- **URL:** `http://localhost:8090/api/v1/opportunities/{id}/status?status={status}`
- **Test Cases:**
  - **TC007a:** ENGAGED status → HTTP 200, status updated
  - **TC007b:** DISCARDED status → HTTP 200, status updated
  - **TC007c:** Invalid status → HTTP 400
  - **TC007d:** Non-existent ID → HTTP 404
- **Pass Criteria:** Status correctly updated and event published

#### **TC008: Engage/Discard Shortcuts**
- **Objective:** Test convenience endpoints for engagement
- **Test Cases:**
  - **TC008a:** POST `/api/v1/opportunities/{id}/engage` → Status = ENGAGED
  - **TC008b:** POST `/api/v1/opportunities/{id}/discard` → Status = DISCARDED
- **Expected Results:**
  - HTTP 200 response
  - Opportunity status updated
  - Kafka event published
- **Pass Criteria:** Status change persisted and event sent

#### **TC009: Update Opportunity Score**
- **Objective:** Test score update functionality
- **Method:** PUT
- **URL:** `http://localhost:8090/api/v1/opportunities/{id}/score?score={score}`
- **Test Cases:**
  - **TC009a:** Valid score (0-100) → HTTP 200, score updated
  - **TC009b:** Score > 100 → HTTP 400
  - **TC009c:** Score < 0 → HTTP 400
  - **TC009d:** Non-numeric score → HTTP 400
- **Pass Criteria:** Valid scores accepted, invalid scores rejected

---

### **TEST SUITE 3: Discovery & Data Source Management**

#### **TC010: Manual Discovery Trigger**
- **Objective:** Test manual discovery from all sources
- **Method:** POST
- **URL:** `http://localhost:8090/api/v1/discovery/trigger`
- **Parameters:**
  - `countries`: ["US", "GB"] (optional)
  - `hoursBack`: 24 (optional)
  - `limitPerSource`: 10 (optional)
- **Expected Results:**
  - HTTP 200 response
  - JSON with discovery results
  - Opportunities created in database
- **Pass Criteria:** Discovery executes and creates opportunities

#### **TC011: Source-Specific Discovery**
- **Objective:** Test discovery from specific data source
- **Method:** POST
- **URL:** `http://localhost:8090/api/v1/discovery/trigger/github`
- **Expected Results:**
  - HTTP 200 response
  - GitHub-specific opportunities discovered
  - Proper opportunity metadata
- **Pass Criteria:** GitHub repositories converted to opportunities

#### **TC012: Discovery Health Check**
- **Objective:** Test data source health monitoring
- **Method:** GET
- **URL:** `http://localhost:8090/api/v1/discovery/health`
- **Expected Results:**
  - HTTP 200 response
  - Health status for each data source
  - Overall system status
- **Pass Criteria:** Health information accurate and complete

#### **TC013: Data Sources Status**
- **Objective:** Test data source configuration visibility
- **Method:** GET
- **URL:** `http://localhost:8090/api/v1/discovery/sources`
- **Expected Results:**
  - List of enabled data sources
  - Source health status
  - Rate limit information
- **Pass Criteria:** Accurate source status information

---

### **TEST SUITE 4: Scoring System**

#### **TC014: Automatic Scoring**
- **Objective:** Test opportunity scoring after discovery
- **Prerequisites:** Opportunities exist in database
- **Method:** POST
- **URL:** `http://localhost:8090/api/v1/discovery/scoring/trigger`
- **Expected Results:**
  - HTTP 200 response
  - Opportunity scores updated
  - Scoring events published
- **Pass Criteria:** Opportunities receive valid scores (0-100)

#### **TC015: Trending Opportunities**
- **Objective:** Test trending opportunities endpoint
- **Method:** GET
- **URL:** `http://localhost:8090/api/v1/opportunities/trending?minScore=70&hoursBack=24`
- **Expected Results:**
  - HTTP 200 response
  - High-scoring recent opportunities
  - Sorted by trending algorithm
- **Pass Criteria:** Results properly filtered and sorted

#### **TC016: Unscored Opportunities**
- **Objective:** Test unscored opportunities retrieval
- **Method:** GET
- **URL:** `http://localhost:8090/api/v1/opportunities/unscored?hoursBack=24`
- **Expected Results:**
  - HTTP 200 response
  - List of opportunities with score = 0
- **Pass Criteria:** Only unscored opportunities returned

---

### **TEST SUITE 5: Data Quality & Validation**

#### **TC017: Opportunity Data Validation**
- **Objective:** Test data validation in opportunity creation
- **Test Cases:**
  - **TC017a:** Missing required fields → Validation error
  - **TC017b:** Invalid enum values → Validation error
  - **TC017c:** Score out of range → Validation error
  - **TC017d:** Invalid email format → Validation error
- **Pass Criteria:** Validation errors properly returned

#### **TC018: Duplicate Prevention**
- **Objective:** Test duplicate opportunity prevention
- **Steps:**
  1. Create opportunity with specific externalId and source
  2. Attempt to create same opportunity again
- **Expected Results:**
  - First creation succeeds
  - Second attempt updates existing opportunity
  - No duplicate opportunities in database
- **Pass Criteria:** Duplicates prevented, updates work correctly

#### **TC019: Rate Limiting**
- **Objective:** Test API rate limiting functionality
- **Steps:**
  1. Make rapid successive API calls
  2. Observe rate limiting behavior
  3. Check Redis for rate limit counters
- **Expected Results:**
  - Rate limits enforced
  - HTTP 429 responses when exceeded
  - Rate limit headers in responses
- **Pass Criteria:** Rate limiting functional and configurable

---

### **TEST SUITE 6: Event Streaming & Integration**

#### **TC020: Kafka Event Publishing**
- **Objective:** Test event publishing to Kafka
- **Prerequisites:** Kafka UI available at http://localhost:8080
- **Steps:**
  1. Perform opportunity operations (create, update, score)
  2. Check Kafka topics for events
  3. Verify event payload structure
- **Expected Results:**
  - Events published to correct topics
  - Event payloads contain complete data
  - No message delivery failures
- **Pass Criteria:** All events successfully published

#### **TC021: Real-time Processing**
- **Objective:** Test real-time opportunity processing
- **Steps:**
  1. Trigger discovery
  2. Monitor opportunity creation in real-time
  3. Verify scoring happens automatically
- **Expected Results:**
  - Opportunities appear immediately after discovery
  - Scoring completes within reasonable time
  - Events processed in order
- **Pass Criteria:** Real-time processing functional

---

### **TEST SUITE 7: Performance & Load Testing**

#### **TC022: Basic Load Test**
- **Objective:** Test system under moderate load
- **Tools:** curl, ab, or JMeter
- **Test Cases:**
  - **TC022a:** 100 concurrent search requests
  - **TC022b:** 50 concurrent discovery triggers
  - **TC022c:** 200 concurrent opportunity updates
- **Expected Results:**
  - Response times < 2 seconds
  - No errors under load
  - Database connections stable
- **Pass Criteria:** System remains stable under load

#### **TC023: Memory Usage**
- **Objective:** Test memory consumption patterns
- **Steps:**
  1. Monitor JVM memory usage
  2. Run discovery and scoring operations
  3. Check for memory leaks
- **Expected Results:**
  - Memory usage within reasonable bounds
  - No memory leaks detected
  - Garbage collection functioning
- **Pass Criteria:** Stable memory usage patterns

---

## 🤖 **AUTOMATED TEST CASES**

### **Unit Test Coverage Requirements**

#### **Service Layer Tests (90%+ Coverage)**
- OpportunityService: All CRUD operations
- DataSourceOrchestrator: Discovery coordination
- OpportunityScoringService: Scoring algorithms
- DiscoverySchedulerService: Scheduled operations

#### **Repository Layer Tests (85%+ Coverage)**
- OpportunityRepository: All custom queries
- Pagination and sorting functionality
- Complex search criteria handling

#### **Controller Layer Tests (95%+ Coverage)**
- All REST endpoints
- Request/response validation
- Error handling scenarios
- Security and authorization

#### **Integration Tests (Key Workflows)**
- End-to-end discovery workflow
- Opportunity lifecycle management
- Event publishing and consumption
- Database transaction handling

---

## 📊 **TEST DATA SCENARIOS**

### **Opportunity Test Data Sets**

#### **Dataset 1: High-Quality Opportunities**
```json
{
  "title": "AI-Powered Fintech Startup Series A",
  "type": "STARTUP_FUNDING",
  "industry": "ARTIFICIAL_INTELLIGENCE",
  "fundingStage": "SERIES_A",
  "companySize": "STARTUP",
  "country": "US",
  "expectedScore": "> 80"
}
```

#### **Dataset 2: Low-Quality Opportunities**
```json
{
  "title": "Unknown Conference Announcement",
  "type": "CONFERENCE_ANNOUNCEMENT",
  "industry": "WEB_DEVELOPMENT",
  "fundingStage": "UNKNOWN",
  "companySize": "ENTERPRISE",
  "country": "US",
  "expectedScore": "< 40"
}
```

#### **Dataset 3: Edge Cases**
- Opportunities with null/empty fields
- Invalid enum values
- Extreme scores (0, 100)
- Very old and very recent discoveries
- Different country/currency combinations

---

## 🚀 **AUTOMATED TEST EXECUTION**

### **Test Commands**

```bash
# Unit Tests
./gradlew test

# Integration Tests
./gradlew integrationTest

# Load Tests (with Docker)
./gradlew loadTest

# Security Tests
./gradlew securityTest

# Full Test Suite
./gradlew check
```

### **Continuous Integration Pipeline**

```yaml
# .github/workflows/test.yml
name: Test Suite
on: [push, pull_request]
jobs:
  test:
    runs-on: ubuntu-latest
    services:
      postgres:
        image: postgres:15
        env:
          POSTGRES_PASSWORD: postgres
        options: >-
          --health-cmd pg_isready
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-java@v3
        with:
          java-version: '17'
      - name: Run tests
        run: ./gradlew check
```

---

## 📈 **TEST METRICS & REPORTING**

### **Success Criteria**
- **Unit Test Coverage:** > 90%
- **Integration Test Coverage:** > 80%
- **API Response Time:** < 2 seconds (95th percentile)
- **Error Rate:** < 1%
- **Discovery Success Rate:** > 95%
- **Scoring Accuracy:** Validated through feedback

### **Test Reporting**
- JUnit XML reports
- Coverage reports (JaCoCo)
- Performance metrics (JMeter)
- Security scan results
- API documentation tests

---

## 🔧 **TEST ENVIRONMENT SETUP**

### **Local Development Testing**
```bash
# 1. Start infrastructure
docker-compose up -d

# 2. Run application
./gradlew :discovery-service:bootRun

# 3. Execute test suite
./gradlew test
```

### **Docker-based Testing**
```bash
# Full environment testing
docker-compose -f docker-compose.test.yml up --abort-on-container-exit
```

### **Production-like Testing**
```bash
# Kubernetes testing environment
kubectl apply -f k8s/test/
./scripts/run-integration-tests.sh
```

---

## 🎯 **TEST EXECUTION SCHEDULE**

### **Development Phase**
- **Daily:** Unit tests, basic integration tests
- **Weekly:** Full test suite, performance tests
- **Pre-release:** Security tests, load tests

### **Production Phase**
- **Continuous:** Health checks, monitoring
- **Daily:** Smoke tests, API validation
- **Weekly:** Full regression suite
- **Monthly:** Comprehensive security audit

---

This comprehensive testing strategy ensures the TechOpportunity Intelligence Platform meets all functional and non-functional requirements while maintaining high quality and reliability standards.