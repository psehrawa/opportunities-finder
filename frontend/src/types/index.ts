// API Response Types
export interface ApiResponse<T> {
  content: T[];
  pageable: {
    pageNumber: number;
    pageSize: number;
    sort: {
      empty: boolean;
      unsorted: boolean;
      sorted: boolean;
    };
    offset: number;
    paged: boolean;
    unpaged: boolean;
  };
  totalElements: number;
  totalPages: number;
  last: boolean;
  size: number;
  number: number;
  numberOfElements: number;
  first: boolean;
  empty: boolean;
}

// Opportunity Types
export type OpportunityType = 
  | 'STARTUP_FUNDING'
  | 'PRODUCT_LAUNCH'
  | 'TECHNOLOGY_TREND'
  | 'MARKET_EXPANSION'
  | 'PARTNERSHIP'
  | 'ACQUISITION_TARGET'
  | 'JOB_POSTING_SIGNAL'
  | 'PATENT_FILING'
  | 'CONFERENCE_ANNOUNCEMENT'
  | 'REGULATORY_CHANGE'
  | 'COMPETITOR_ANALYSIS'
  | 'TECHNOLOGY_ADOPTION';

export type OpportunityStatus = 
  | 'DISCOVERED'
  | 'ANALYZED'
  | 'ENGAGED'
  | 'DISCARDED'
  | 'MONITORING'
  | 'CONVERTED'
  | 'EXPIRED'
  | 'DUPLICATE';

export type DataSource = 
  | 'GITHUB'
  | 'HACKER_NEWS'
  | 'REDDIT'
  | 'PRODUCT_HUNT'
  | 'SEC_EDGAR'
  | 'USPTO_PATENT'
  | 'CRUNCHBASE_BASIC'
  | 'GOOGLE_TRENDS'
  | 'NEWS_API'
  | 'TWITTER_API'
  | 'LINKEDIN_API'
  | 'YOUTUBE_API'
  | 'CRUNCHBASE_PRO'
  | 'PITCHBOOK'
  | 'OWLER'
  | 'ANGELLIST'
  | 'BLIND'
  | 'QUORA';

export type FundingStage = 
  | 'PRE_SEED'
  | 'SEED'
  | 'SERIES_A'
  | 'SERIES_B'
  | 'SERIES_C'
  | 'SERIES_D_PLUS'
  | 'IPO'
  | 'ACQUISITION'
  | 'PRIVATE_EQUITY'
  | 'DEBT_FINANCING'
  | 'GRANT'
  | 'CROWDFUNDING'
  | 'REVENUE_BASED'
  | 'UNKNOWN';

export type CompanySize = 
  | 'STARTUP'
  | 'SMALL'
  | 'MEDIUM'
  | 'LARGE'
  | 'ENTERPRISE'
  | 'UNKNOWN';

export interface Opportunity {
  id: number;
  title: string;
  description?: string;
  url?: string;
  type: OpportunityType;
  status: OpportunityStatus;
  source: DataSource;
  externalId: string;
  score: number;
  confidenceScore?: number;
  engagementPotential?: number;
  companyName?: string;
  companySize?: CompanySize;
  industry?: string;
  location?: string;
  country?: string;
  fundingStage?: FundingStage;
  fundingAmount?: number;
  contactEmail?: string;
  discoveredAt: string;
  lastUpdated: string;
  isActive: boolean;
  version: number;
  metadata?: { [key: string]: string };
  tags?: string[];
}

// Health Check Types
export interface HealthStatus {
  status: 'UP' | 'DOWN' | 'DEGRADED';
  components: {
    [key: string]: {
      status: 'UP' | 'DOWN';
      details?: any;
    };
  };
}

export interface DataSourceHealth {
  isHealthy: boolean;
  status: 'UP' | 'DOWN';
  message: string;
  lastChecked: string;
}

export interface DiscoveryHealth {
  status: 'UP' | 'DOWN' | 'DEGRADED';
  dataSources: {
    [key in DataSource]?: 'HEALTHY' | 'UNHEALTHY' | 'UNKNOWN';
  };
  lastDiscovery?: string;
  totalOpportunities?: number;
}

// Search and Filter Types
export interface OpportunitySearchCriteria {
  types?: OpportunityType[];
  statuses?: OpportunityStatus[];
  sources?: DataSource[];
  countries?: string[];
  industries?: string[];
  fundingStages?: FundingStage[];
  companySizes?: CompanySize[];
  minScore?: number;
  maxScore?: number;
  minFundingAmount?: number;
  maxFundingAmount?: number;
  discoveredAfter?: string;
  discoveredBefore?: string;
  searchTerm?: string;
  tags?: string[];
  isActive?: boolean;
  page?: number;
  size?: number;
  sortBy?: string;
  sortDirection?: 'ASC' | 'DESC';
}