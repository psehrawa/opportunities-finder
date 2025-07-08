# TechOpportunity Intelligence Platform

A comprehensive multi-tenant SaaS platform for detecting and managing technology business opportunities globally.

## 🎯 Project Overview

**Vision:** Multi-tenant SaaS platform for detecting and managing technology business opportunities globally  
**Target Users:** VCs, consultants, business development teams, entrepreneurs  
**Core Value:** ROI-focused opportunity discovery with engagement tracking

### Architecture
- **Microservices:** Discovery, Intelligence, User, Notification, Analytics, API Gateway
- **Tech Stack:** Spring Boot (Java 17), PostgreSQL, Redis, Kafka, React/TypeScript
- **Deployment:** Docker + Kubernetes ready
- **Scope:** Global operation with country parameterization

## 🚀 Quick Start

### Prerequisites
- Java 17+
- Docker & Docker Compose
- Gradle 7+
- Git

### 1. Clone and Setup
```bash
git clone <repository-url>
cd opportunities-finder
```

### 2. Start Infrastructure
```bash
# Start PostgreSQL, Redis, Kafka
docker-compose up -d postgres redis kafka zookeeper

# Optional: Start management UIs
docker-compose up -d pgadmin redis-commander kafka-ui
```

### 3. Build and Run Discovery Service
```bash
# Build all modules
./gradlew build

# Run discovery service
./gradlew :discovery-service:bootRun
```

### 4. Verify Setup
```bash
# Check service health
curl http://localhost:8090/actuator/health

# Check discovery health
curl http://localhost:8090/api/v1/discovery/health

# Get available data sources
curl http://localhost:8090/api/v1/discovery/sources
```

## 📋 Current Implementation Status

### ✅ Completed
- [x] Multi-module Spring Boot project structure
- [x] Core entity models (OpportunityEntity, enums, DTOs)
- [x] Docker Compose environment (PostgreSQL, Redis, Kafka)
- [x] Discovery service with REST API
- [x] Data source service interfaces and configuration
- [x] GitHub API integration (first data source)
- [x] Opportunity scoring system with ML-ready architecture
- [x] Database repositories and services
- [x] Basic testing infrastructure
- [x] Kafka event streaming
- [x] Rate limiting and health checks
- [x] Scheduled discovery and scoring

### 🚧 In Progress / Next Steps
- [ ] Additional data sources (Hacker News, Reddit, Product Hunt)
- [ ] Intelligence service for advanced analytics
- [ ] User service with authentication
- [ ] React dashboard
- [ ] Multi-tenancy implementation
- [ ] Production deployment setup

## 🔧 Development

### Running Tests
```bash
# Run all tests
./gradlew test

# Run specific service tests
./gradlew :discovery-service:test

# Run with coverage
./gradlew :discovery-service:jacocoTestReport
```

### Database Management
- **pgAdmin:** http://localhost:8081 (admin@oppfinder.com / admin123)
- **Redis Commander:** http://localhost:8082
- **Kafka UI:** http://localhost:8080

### API Documentation
- **Discovery Service:** http://localhost:8090/actuator (health, metrics)
- **OpenAPI/Swagger:** (TODO - add springdoc-openapi)

## 📊 API Endpoints

### Opportunity Management
```bash
# Search opportunities
POST /api/v1/opportunities/search
Content-Type: application/json
{
  "page": 0,
  "size": 20,
  "types": ["STARTUP_FUNDING"],
  "countries": ["US"],
  "minScore": 70.0
}

# Get opportunity by ID
GET /api/v1/opportunities/{id}

# Engage with opportunity
POST /api/v1/opportunities/{id}/engage

# Discard opportunity
POST /api/v1/opportunities/{id}/discard

# Get trending opportunities
GET /api/v1/opportunities/trending?minScore=80&hoursBack=24
```

### Discovery Management
```bash
# Trigger manual discovery
POST /api/v1/discovery/trigger?limitPerSource=20

# Get discovery health
GET /api/v1/discovery/health

# Get data sources status
GET /api/v1/discovery/sources

# Trigger scoring
POST /api/v1/discovery/scoring/trigger
```

## 🎯 Data Sources

### Priority 1 - Implemented
- ✅ **GitHub API** - Repository trends, technology adoption

### Priority 1 - Free/Public APIs (Next)
- 🔲 **Hacker News API** - Community-validated trends
- 🔲 **Reddit API** - Startup and technology subreddits  
- 🔲 **Product Hunt API** - New product launches
- 🔲 **SEC EDGAR API** - Company filings and funding
- 🔲 **USPTO Patent API** - Technology innovation indicators

### Priority 2 - Freemium/Low Cost
- 🔲 **Google Trends API** - Market interest and search volume
- 🔲 **News APIs** - Real-time announcements
- 🔲 **Twitter/X API** - Social signals
- 🔲 **LinkedIn API** - Job postings and company growth
- 🔲 **YouTube API** - Technology content and conference talks

## 🔍 Opportunity Scoring

The platform uses a sophisticated scoring algorithm (0-100) based on:

- **Funding Stage** (25%) - Earlier stages score higher
- **Company Size** (20%) - Smaller companies score higher  
- **Industry Trend** (20%) - AI, Fintech, HealthTech score higher
- **Social Signals** (15%) - GitHub stars, engagement metrics
- **Recency** (10%) - Recently discovered opportunities score higher
- **Data Source Reliability** (10%) - Premium sources score higher

### Scoring Configuration
```yaml
oppfinder:
  discovery:
    scoring:
      weights:
        funding-stage: 0.25
        company-size: 0.20
        industry-trend: 0.20
        social-signals: 0.15
        recency: 0.10
        data-source-reliability: 0.10
```

## 🌍 Global Markets Support

Currently configured for:
- 🇺🇸 US (Primary)
- 🇬🇧 GB  
- 🇨🇦 CA
- 🇦🇺 AU
- 🇩🇪 DE
- 🇫🇷 FR
- 🇮🇳 IN
- 🇸🇬 SG

## 🔐 Configuration

### Environment Variables
```bash
# GitHub API (Optional - increases rate limits)
GITHUB_API_TOKEN=your_github_token

# Database (Production)
DATABASE_URL=jdbc:postgresql://localhost:5432/oppfinder_discovery
DATABASE_USERNAME=oppfinder
DATABASE_PASSWORD=oppfinder123

# Redis (Production)
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=redis123

# Kafka (Production)
KAFKA_BOOTSTRAP_SERVERS=localhost:9092
```

### Application Configuration
Key configuration in `discovery-service/src/main/resources/application.yml`:

```yaml
oppfinder:
  discovery:
    data-sources:
      github:
        enabled: true
        rate-limit:
          requests-per-hour: 5000
          burst-capacity: 100
    
    scheduling:
      discovery-interval: "0 0 */6 * * *"  # Every 6 hours
      cleanup-interval: "0 0 2 * * *"      # Daily at 2 AM
```

## 🐳 Docker Deployment

### Development
```bash
# Start infrastructure only
docker-compose up -d

# Start with development service
docker-compose --profile dev up -d
```

### Production  
```bash
# Build production images
docker build -t oppfinder/discovery-service ./discovery-service

# Deploy with production compose
docker-compose -f docker-compose.yml -f docker-compose.prod.yml up -d
```

## 📈 Monitoring & Observability

### Health Checks
- **Application Health:** http://localhost:8090/actuator/health
- **Discovery Health:** http://localhost:8090/api/v1/discovery/health
- **Database Health:** Included in actuator health
- **Kafka Health:** Included in discovery health

### Metrics
- **Prometheus:** http://localhost:8090/actuator/prometheus
- **Application Metrics:** http://localhost:8090/actuator/metrics

### Logging
- **Level:** DEBUG for com.psehrawa.oppfinder
- **Format:** JSON in production, console in development
- **Destinations:** Console, file, ELK stack (production)

## 🧪 Testing Strategy

### Unit Tests
- Service layer with Mockito
- Repository layer with @DataJpaTest  
- Controller layer with @WebMvcTest

### Integration Tests
- Full Spring context with @SpringBootTest
- Test containers for database
- WireMock for external APIs

### End-to-End Tests
- Docker Compose test environment
- Real API integrations (with test accounts)
- Performance and load testing

## 🔮 Roadmap

### Phase 1 (Current) - Core Foundation ✅
- Basic discovery service
- GitHub integration
- Opportunity scoring
- REST API

### Phase 2 - Data Source Expansion
- Multiple data source integrations
- Enhanced scoring algorithms
- Real-time processing improvements
- Duplicate detection and deduplication

### Phase 3 - Intelligence & Analytics
- ML-powered scoring
- Trend analysis and forecasting  
- Market intelligence reports
- Predictive analytics

### Phase 4 - User Experience
- React TypeScript dashboard
- Real-time notifications
- Advanced filtering and search
- Mobile-responsive design

### Phase 5 - Multi-Tenancy & Scale
- User management and authentication
- Tenant isolation and customization
- Billing and usage tracking
- Global deployment and CDN

### Phase 6 - Advanced Features
- AI-powered opportunity matching
- Integration with CRM systems
- Advanced reporting and exports
- Enterprise SSO and compliance

## 🤝 Contributing

### Development Workflow
1. Create feature branch from `main`
2. Implement changes with tests
3. Run full test suite: `./gradlew test`
4. Create pull request with description
5. Code review and merge

### Code Style
- Java: Google Java Style Guide
- Spring Boot best practices
- Comprehensive JavaDoc for public APIs
- Lombok for reducing boilerplate

### Commit Messages
```
feat: add Reddit API data source integration
fix: resolve rate limiting issue in GitHub service  
docs: update API documentation for scoring endpoint
test: add integration tests for opportunity search
```

## 📞 Support

- **Issues:** GitHub Issues
- **Discussions:** GitHub Discussions  
- **Documentation:** This README + JavaDoc
- **Contact:** [Your contact information]

---

**Built with ❤️ by the TechOpportunity Team**