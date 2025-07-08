# TechOpportunity Intelligence Platform - Testing Guide

## 🎯 **Quick Start Testing**

This guide provides step-by-step instructions for testing the TechOpportunity Intelligence Platform across different scenarios and environments.

---

## 🚀 **Automated Testing (Recommended)**

### **1. Unit Tests - Core Functionality**

```bash
# Run all unit tests
./gradlew test

# Run specific test class
./gradlew test --tests OpportunityServiceTest

# Run tests with coverage report
./gradlew test jacocoTestReport

# View coverage report
open discovery-service/build/reports/jacoco/test/html/index.html
```

**Expected Results:**
- All unit tests pass (✅)
- Coverage report shows 85%+ coverage
- No compilation errors

### **2. Integration Tests - End-to-End Workflows**

```bash
# Run integration tests (requires Docker)
./gradlew integrationTest

# Run specific integration test
./gradlew test --tests EndToEndIntegrationTest
```

**Expected Results:**
- Full application workflow tested
- Database integration validated
- Event publishing confirmed

---

## 🖥️ **Manual Testing - Basic Use Cases**

### **Prerequisites**
1. **Start Infrastructure:**
   ```bash
   docker-compose up -d postgres redis kafka zookeeper
   ```

2. **Start Application:**
   ```bash
   ./gradlew :discovery-service:bootRun
   ```

3. **Wait for Startup:** Application ready when you see:
   ```
   Started DiscoveryServiceApplication in X.XXX seconds
   ```

### **Test Suite 1: Basic Functionality**

#### **Test 1.1: Health Check**
```bash
curl http://localhost:8090/actuator/health
```
**Expected:** `{"status":"UP"}`

#### **Test 1.2: Discovery Health**
```bash
curl http://localhost:8090/api/v1/discovery/health
```
**Expected:** JSON with health status of data sources

#### **Test 1.3: Get All Opportunities**
```bash
curl http://localhost:8090/api/v1/opportunities
```
**Expected:** Paginated list (may be empty initially)

### **Test Suite 2: Discovery Operations**

#### **Test 2.1: Trigger Discovery**
```bash
curl -X POST "http://localhost:8090/api/v1/discovery/trigger?limitPerSource=5"
```
**Expected:** JSON with `"status": "completed"` and opportunities count

#### **Test 2.2: GitHub-Specific Discovery**
```bash
curl -X POST "http://localhost:8090/api/v1/discovery/trigger/github?limit=3"
```
**Expected:** JSON showing GitHub opportunities discovered

#### **Test 2.3: Trigger Scoring**
```bash
curl -X POST http://localhost:8090/api/v1/discovery/scoring/trigger
```
**Expected:** `{"status": "triggered"}`

### **Test Suite 3: Opportunity Management**

#### **Test 3.1: Search Opportunities**
```bash
curl -X POST http://localhost:8090/api/v1/opportunities/search \
  -H "Content-Type: application/json" \
  -d '{"page": 0, "size": 10, "sortBy": "discoveredAt", "sortDirection": "DESC"}'
```
**Expected:** Paginated search results

#### **Test 3.2: Get Trending Opportunities**
```bash
curl "http://localhost:8090/api/v1/opportunities/trending?minScore=70&hoursBack=24"
```
**Expected:** High-scoring recent opportunities

#### **Test 3.3: Engage with Opportunity (if ID exists)**
```bash
# First get an opportunity ID from the search results, then:
curl -X POST http://localhost:8090/api/v1/opportunities/1/engage
```
**Expected:** Opportunity status updated to "ENGAGED"

---

## 🔥 **Performance Testing**

### **Automated Performance Tests**
```bash
# Run comprehensive performance test suite
./scripts/performance-test.sh
```

**This will test:**
- Response times under load
- Concurrent user handling
- Breaking point analysis
- Memory usage patterns

**Expected Results:**
- Response times < 2 seconds
- Support for 50+ concurrent users
- No memory leaks detected
- Stable performance under load

### **Manual Performance Validation**

#### **Response Time Test**
```bash
# Measure response time
time curl http://localhost:8090/api/v1/opportunities
```
**Target:** < 2 seconds

#### **Simple Load Test (with ab)**
```bash
# Install Apache Bench if available
ab -n 100 -c 10 http://localhost:8090/api/v1/opportunities
```
**Target:** > 20 requests/second

---

## 📊 **Complete Test Execution**

### **Comprehensive Test Script**
```bash
# Run all automated tests in sequence
./scripts/test-basic-functionality.sh
```

**This script tests:**
- ✅ Application startup and health
- ✅ All API endpoints functionality
- ✅ Discovery and scoring operations
- ✅ Data validation and error handling
- ✅ Performance benchmarks
- ✅ Concurrent request handling

**Expected Output:**
```
🎉 All tests passed! The application is working correctly.
Test report saved to: test-results-YYYYMMDD-HHMMSS.json
```

---

## 🎯 **Test Scenarios by Use Case**

### **Use Case 1: New Installation Validation**

**Scenario:** Fresh deployment, no data yet

**Test Sequence:**
1. Health checks pass ✅
2. Empty opportunity list ✅
3. Discovery creates new opportunities ✅
4. Scoring assigns proper scores ✅
5. Search and filtering work ✅

```bash
# Execute this sequence:
curl http://localhost:8090/actuator/health
curl http://localhost:8090/api/v1/opportunities
curl -X POST "http://localhost:8090/api/v1/discovery/trigger?limitPerSource=5"
curl -X POST http://localhost:8090/api/v1/discovery/scoring/trigger
curl http://localhost:8090/api/v1/opportunities
```

### **Use Case 2: Daily Operations Validation**

**Scenario:** System with existing data, regular operations

**Test Sequence:**
1. System health monitoring ✅
2. New discovery run ✅
3. Opportunity engagement ✅
4. Performance validation ✅

```bash
# Daily health check sequence:
./scripts/test-basic-functionality.sh
```

### **Use Case 3: Load Testing for Production**

**Scenario:** Pre-production validation under load

**Test Sequence:**
1. Performance baseline ✅
2. Concurrent user simulation ✅
3. Breaking point analysis ✅
4. Recovery validation ✅

```bash
# Load testing sequence:
./scripts/performance-test.sh
```

---

## 🛠️ **Troubleshooting Test Issues**

### **Common Issues & Solutions**

#### **1. Application Won't Start**
**Symptoms:** Connection errors, health check fails
**Solutions:**
```bash
# Check Docker services
docker-compose ps

# Restart infrastructure
docker-compose down && docker-compose up -d

# Check logs
docker-compose logs postgres redis kafka
```

#### **2. Discovery Returns No Results**
**Symptoms:** Empty discovery results, no opportunities created
**Solutions:**
```bash
# Check data source health
curl http://localhost:8090/api/v1/discovery/health

# Verify GitHub API access (if configured)
curl -H "Authorization: token YOUR_GITHUB_TOKEN" https://api.github.com/rate_limit

# Check application logs for API errors
```

#### **3. Tests Fail Due to Missing Dependencies**
**Symptoms:** Compilation errors, missing test classes
**Solutions:**
```bash
# Clean and rebuild
./gradlew clean build

# Check Java version (requires Java 17+)
java -version

# Update dependencies
./gradlew dependencies --refresh-dependencies
```

#### **4. Performance Tests Show Poor Results**
**Symptoms:** High response times, low throughput
**Solutions:**
```bash
# Check system resources
docker stats

# Monitor JVM memory
jps -l
jstat -gc [PID]

# Optimize database connections
# Review application.yml database pool settings
```

---

## 📈 **Test Results Interpretation**

### **Success Criteria**

| Test Type | Success Criteria | Action if Failed |
|-----------|-----------------|------------------|
| **Unit Tests** | 100% pass, 85%+ coverage | Fix failing tests, increase coverage |
| **Integration Tests** | All workflows complete | Check database/service connectivity |
| **API Tests** | All endpoints respond correctly | Review endpoint implementations |
| **Performance Tests** | <2s response, >20 req/s | Optimize queries, increase resources |
| **Discovery Tests** | Opportunities created successfully | Check external API configuration |

### **Performance Benchmarks**

| Metric | Target | Good | Needs Improvement |
|--------|--------|------|-------------------|
| **Response Time** | <1s | <2s | >2s |
| **Throughput** | >50 req/s | >20 req/s | <20 req/s |
| **Concurrent Users** | >100 | >50 | <50 |
| **Error Rate** | <0.1% | <1% | >1% |
| **Memory Usage** | Stable | Growing slowly | Memory leaks |

---

## 🎪 **Advanced Testing Scenarios**

### **Multi-Environment Testing**

#### **Development Environment**
```bash
# Use H2 database for fast testing
SPRING_PROFILES_ACTIVE=test ./gradlew test
```

#### **Staging Environment**
```bash
# Full infrastructure with external APIs
docker-compose -f docker-compose.yml -f docker-compose.staging.yml up -d
./scripts/test-basic-functionality.sh
```

#### **Production Simulation**
```bash
# Production-like configuration
docker-compose -f docker-compose.yml -f docker-compose.prod.yml up -d
./scripts/performance-test.sh
```

### **Security Testing**

#### **API Security Validation**
```bash
# Test SQL injection protection
curl -X POST http://localhost:8090/api/v1/opportunities/search \
  -H "Content-Type: application/json" \
  -d '{"searchTerm": "test'; DROP TABLE opportunities; --"}'

# Expected: Properly escaped, no SQL injection
```

#### **Rate Limiting Validation**
```bash
# Test rate limiting (make rapid requests)
for i in {1..100}; do
  curl -s http://localhost:8090/api/v1/opportunities > /dev/null &
done
wait

# Expected: Some requests return HTTP 429 (Too Many Requests)
```

---

## 📋 **Test Checklist for Release**

### **Pre-Release Testing Checklist**

- [ ] **Unit Tests**: All tests pass, 85%+ coverage
- [ ] **Integration Tests**: End-to-end workflows validated
- [ ] **API Tests**: All endpoints functional
- [ ] **Performance Tests**: Meets response time targets
- [ ] **Discovery Tests**: All data sources working
- [ ] **Scoring Tests**: Algorithm produces valid scores
- [ ] **Error Handling**: Edge cases handled gracefully
- [ ] **Security Tests**: No vulnerabilities detected
- [ ] **Load Tests**: System stable under expected load
- [ ] **Documentation**: All test cases documented

### **Production Readiness Validation**

- [ ] **Health Monitoring**: Actuator endpoints functional
- [ ] **Database**: Connections stable, queries optimized
- [ ] **External APIs**: Rate limits respected, errors handled
- [ ] **Event Streaming**: Kafka events publishing correctly
- [ ] **Caching**: Redis operational, proper TTL settings
- [ ] **Logging**: Appropriate log levels, no sensitive data
- [ ] **Configuration**: Environment-specific settings applied
- [ ] **Backup/Recovery**: Database backup strategy tested

---

## 🏆 **Conclusion**

The TechOpportunity Intelligence Platform includes a comprehensive testing suite that validates:

- **✅ Functional Correctness**: All features work as designed
- **✅ Performance Standards**: Meets response time and throughput targets
- **✅ Reliability**: Handles errors and edge cases gracefully
- **✅ Scalability**: Supports concurrent users and growth
- **✅ Maintainability**: Clear test documentation and automation

**For ongoing success:**
1. Run tests regularly during development
2. Execute full test suite before releases
3. Monitor performance in production
4. Update tests when adding new features
5. Review and improve test coverage continuously

**The platform is thoroughly tested and ready for production deployment with confidence.**