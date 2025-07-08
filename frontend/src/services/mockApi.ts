import { Opportunity, ApiResponse, HealthStatus, DiscoveryHealth } from '../types';

// Mock opportunities data for demo
const mockOpportunities: Opportunity[] = [
  {
    id: 1,
    title: "AI-powered fintech startup raises $50M Series A",
    description: "Revolutionary payment processing platform using machine learning to reduce fraud by 95%. Looking for enterprise partnerships and technical talent.",
    companyName: "PaySecure AI",
    companySize: "STARTUP",
    industry: "FINTECH",
    type: "STARTUP_FUNDING",
    status: "DISCOVERED",
    source: "REDDIT",
    score: 95.5,
    confidenceScore: 88.0,
    engagementPotential: 92.0,
    fundingStage: "SERIES_A",
    fundingAmount: 50000000,
    location: "San Francisco, CA",
    country: "US",
    contactEmail: "partnerships@paysecureai.com",
    url: "https://www.reddit.com/r/startups/post/ai-fintech-funding",
    discoveredAt: "2025-07-08T10:30:00Z",
    lastUpdated: "2025-07-08T10:30:00Z",
    isActive: true,
    externalId: "reddit-ai-fintech-series-a",
    version: 1,
    metadata: {
      "source_type": "social_media",
      "engagement_score": "high",
      "investor_interest": "confirmed",
      "team_size": "45"
    },
    tags: ["AI", "fintech", "Series A", "fraud detection", "enterprise"]
  },
  {
    id: 2,
    title: "Healthtech platform for remote patient monitoring",
    description: "IoT-enabled devices for continuous health monitoring. FDA approved and seeking distribution partners.",
    companyName: "VitalWatch",
    companySize: "SMALL",
    industry: "HEALTHTECH",
    type: "PRODUCT_LAUNCH",
    status: "ANALYZED",
    source: "QUORA",
    score: 87.2,
    confidenceScore: 85.5,
    engagementPotential: 89.0,
    fundingStage: "SEED",
    fundingAmount: 5000000,
    location: "Austin, TX",
    country: "US",
    contactEmail: "hello@vitalwatch.io",
    url: "https://www.quora.com/healthtech-remote-monitoring",
    discoveredAt: "2025-07-08T09:15:00Z",
    lastUpdated: "2025-07-08T09:15:00Z",
    isActive: true,
    externalId: "quora-healthtech-remote-monitoring",
    version: 1,
    metadata: {
      "fda_approved": "true",
      "patent_status": "pending",
      "market_size": "12B",
      "competitors": "low"
    },
    tags: ["healthtech", "IoT", "FDA approved", "remote monitoring", "seed funding"]
  },
  {
    id: 3,
    title: "Blockchain supply chain transparency platform",
    description: "End-to-end supply chain tracking using blockchain technology. Pilot programs with Fortune 500 companies.",
    companyName: "ChainTrace",
    companySize: "STARTUP",
    industry: "BLOCKCHAIN",
    type: "PARTNERSHIP",
    status: "ENGAGED",
    source: "GITHUB",
    score: 82.8,
    confidenceScore: 78.0,
    engagementPotential: 85.0,
    fundingStage: "PRE_SEED",
    fundingAmount: 2000000,
    location: "Seattle, WA",
    country: "US",
    contactEmail: "partnerships@chaintrace.co",
    url: "https://github.com/chaintrace/supply-chain-platform",
    discoveredAt: "2025-07-08T08:45:00Z",
    lastUpdated: "2025-07-08T08:45:00Z",
    isActive: true,
    externalId: "github-chaintrace-supply-chain",
    version: 1,
    metadata: {
      "github_stars": "2847",
      "contributors": "12",
      "last_commit": "2025-07-07",
      "pilot_companies": "3"
    },
    tags: ["blockchain", "supply chain", "Fortune 500", "transparency", "pilot"]
  },
  {
    id: 4,
    title: "EdTech platform disrupting online learning",
    description: "AI-powered personalized learning platform with 500K+ active users. Expanding internationally.",
    companyName: "LearnSmart",
    companySize: "MEDIUM",
    industry: "EDTECH",
    type: "MARKET_EXPANSION",
    status: "MONITORING",
    source: "BLIND",
    score: 79.5,
    confidenceScore: 82.0,
    engagementPotential: 76.0,
    fundingStage: "SERIES_B",
    fundingAmount: 25000000,
    location: "London, UK",
    country: "GB",
    contactEmail: "expansion@learnsmart.edu",
    url: "https://www.teamblind.com/post/edtech-expansion",
    discoveredAt: "2025-07-08T07:20:00Z",
    lastUpdated: "2025-07-08T07:20:00Z",
    isActive: true,
    externalId: "blind-learnsmart-expansion",
    version: 1,
    metadata: {
      "active_users": "500000",
      "markets": "5",
      "growth_rate": "45%",
      "insider_confidence": "high"
    },
    tags: ["edtech", "AI", "international expansion", "Series B", "500K users"]
  },
  {
    id: 5,
    title: "Cybersecurity startup focusing on zero-trust architecture",
    description: "Next-generation security platform for remote workforce. Enterprise customers include major banks.",
    companyName: "ZeroShield",
    companySize: "STARTUP",
    industry: "CYBERSECURITY",
    type: "TECHNOLOGY_TREND",
    status: "DISCOVERED",
    source: "REDDIT",
    score: 91.2,
    confidenceScore: 89.5,
    engagementPotential: 93.0,
    fundingStage: "SERIES_A",
    fundingAmount: 30000000,
    location: "Tel Aviv, Israel",
    country: "IL",
    contactEmail: "enterprise@zeroshield.com",
    url: "https://www.reddit.com/r/cybersecurity/zero-trust",
    discoveredAt: "2025-07-08T06:30:00Z",
    lastUpdated: "2025-07-08T06:30:00Z",
    isActive: true,
    externalId: "reddit-zeroshield-zero-trust",
    version: 1,
    metadata: {
      "enterprise_customers": "15",
      "security_certifications": "SOC2,ISO27001",
      "team_background": "ex-military",
      "market_validation": "strong"
    },
    tags: ["cybersecurity", "zero-trust", "enterprise", "banks", "Series A"]
  }
];

// Mock API responses
export class MockApiService {
  static async getOpportunities(): Promise<ApiResponse<Opportunity>> {
    // Simulate API delay
    await new Promise(resolve => setTimeout(resolve, 500));
    
    return {
      content: mockOpportunities,
      pageable: {
        pageNumber: 0,
        pageSize: 20,
        sort: {
          empty: true,
          unsorted: true,
          sorted: false
        },
        offset: 0,
        paged: true,
        unpaged: false
      },
      totalElements: mockOpportunities.length,
      totalPages: 1,
      size: 20,
      number: 0,
      first: true,
      last: true,
      numberOfElements: mockOpportunities.length,
      empty: false
    };
  }

  static async getOpportunityById(id: number): Promise<Opportunity> {
    await new Promise(resolve => setTimeout(resolve, 300));
    
    const opportunity = mockOpportunities.find(opp => opp.id === id);
    if (!opportunity) {
      throw new Error(`Opportunity with id ${id} not found`);
    }
    return opportunity;
  }

  static async searchOpportunities(): Promise<ApiResponse<Opportunity>> {
    return this.getOpportunities();
  }

  static async getApplicationHealth(): Promise<HealthStatus> {
    await new Promise(resolve => setTimeout(resolve, 200));
    
    return {
      status: "UP",
      components: {
        db: { status: "UP" },
        redis: { status: "UP" },
        diskSpace: { status: "UP" }
      }
    };
  }

  static async getDiscoveryHealth(): Promise<DiscoveryHealth> {
    await new Promise(resolve => setTimeout(resolve, 200));
    
    return {
      status: "UP",
      dataSources: {
        GITHUB: "HEALTHY",
        REDDIT: "HEALTHY", 
        QUORA: "HEALTHY",
        BLIND: "HEALTHY"
      },
      lastDiscovery: "2025-07-08T10:30:00Z",
      totalOpportunities: mockOpportunities.length
    };
  }

  static async triggerDiscovery(): Promise<void> {
    await new Promise(resolve => setTimeout(resolve, 1000));
    console.log("Mock discovery triggered - found 3 new opportunities!");
  }

  static async triggerScoring(): Promise<void> {
    await new Promise(resolve => setTimeout(resolve, 800));
    console.log("Mock scoring triggered - updated opportunity scores!");
  }

  static async getDataSources(): Promise<any> {
    await new Promise(resolve => setTimeout(resolve, 300));
    
    return {
      enabledSources: ["GITHUB", "REDDIT", "QUORA", "BLIND"],
      sources: {
        GITHUB: { enabled: true, healthy: true, lastCheck: "2025-07-08T10:30:00Z" },
        REDDIT: { enabled: true, healthy: true, lastCheck: "2025-07-08T10:30:00Z" },
        QUORA: { enabled: true, healthy: true, lastCheck: "2025-07-08T10:30:00Z" },
        BLIND: { enabled: true, healthy: true, lastCheck: "2025-07-08T10:30:00Z" }
      }
    };
  }
}