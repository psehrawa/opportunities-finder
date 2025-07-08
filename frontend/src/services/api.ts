import axios, { AxiosResponse } from 'axios';
import {
  Opportunity,
  OpportunitySearchCriteria,
  ApiResponse,
  HealthStatus,
  DiscoveryHealth
} from '../types';
import { MockApiService } from './mockApi';

const API_BASE_URL = process.env.REACT_APP_API_URL || 'http://localhost:8090';

// Demo mode - use mock data when backend is unavailable
const DEMO_MODE = process.env.REACT_APP_DEMO_MODE === 'true' || 
                  window.location.hostname.includes('github.io');

// Log the API URL in development
if (process.env.NODE_ENV === 'development') {
  console.log('API Base URL:', API_BASE_URL);
  console.log('Demo Mode:', DEMO_MODE);
}

// Create axios instance with base configuration
const api = axios.create({
  baseURL: API_BASE_URL,
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor for logging
api.interceptors.request.use(
  (config) => {
    console.log(`API Request: ${config.method?.toUpperCase()} ${config.url}`);
    return config;
  },
  (error) => {
    console.error('API Request Error:', error);
    return Promise.reject(error);
  }
);

// Response interceptor for error handling
api.interceptors.response.use(
  (response) => {
    return response;
  },
  (error) => {
    console.error('API Response Error:', error.response?.data || error.message);
    
    // If in demo mode and API fails, don't reject - let the service handle fallback
    if (DEMO_MODE && (error.code === 'NETWORK_ERROR' || error.response?.status >= 500)) {
      console.log('🔄 Falling back to demo mode');
    }
    
    return Promise.reject(error);
  }
);

// API Service Class
export class ApiService {
  // Opportunities API
  static async getOpportunities(criteria: OpportunitySearchCriteria = {}): Promise<ApiResponse<Opportunity>> {
    if (DEMO_MODE) {
      console.log('🎭 Using demo data for opportunities');
      return MockApiService.getOpportunities();
    }

    try {
      const params = new URLSearchParams();
      
      // Add search criteria as query parameters
      if (criteria.page !== undefined) params.append('page', criteria.page.toString());
      if (criteria.size !== undefined) params.append('size', criteria.size.toString());
      if (criteria.sortBy) params.append('sortBy', criteria.sortBy);
      if (criteria.sortDirection) params.append('sortDirection', criteria.sortDirection);
      if (criteria.searchTerm) params.append('searchTerm', criteria.searchTerm);
      if (criteria.minScore !== undefined) params.append('minScore', criteria.minScore.toString());
      if (criteria.maxScore !== undefined) params.append('maxScore', criteria.maxScore.toString());
      if (criteria.isActive !== undefined) params.append('isActive', criteria.isActive.toString());
      
      // Add array parameters
      if (criteria.types) criteria.types.forEach(type => params.append('types', type));
      if (criteria.statuses) criteria.statuses.forEach(status => params.append('statuses', status));
      if (criteria.sources) criteria.sources.forEach(source => params.append('sources', source));
      if (criteria.industries) criteria.industries.forEach(industry => params.append('industries', industry));
      if (criteria.fundingStages) criteria.fundingStages.forEach(stage => params.append('fundingStages', stage));

      const response: AxiosResponse<ApiResponse<Opportunity>> = await api.get(
        `/api/v1/opportunities?${params.toString()}`
      );
      return response.data;
    } catch (error) {
      console.log('🔄 API failed, falling back to demo data');
      return MockApiService.getOpportunities();
    }
  }

  static async getOpportunityById(id: number): Promise<Opportunity> {
    if (DEMO_MODE) {
      return MockApiService.getOpportunityById(id);
    }

    try {
      const response: AxiosResponse<Opportunity> = await api.get(`/api/v1/opportunities/${id}`);
      return response.data;
    } catch (error) {
      console.log('🔄 API failed, falling back to demo data for opportunity:', id);
      return MockApiService.getOpportunityById(id);
    }
  }

  static async searchOpportunities(criteria: OpportunitySearchCriteria): Promise<ApiResponse<Opportunity>> {
    if (DEMO_MODE) {
      return MockApiService.searchOpportunities();
    }

    try {
      const response: AxiosResponse<ApiResponse<Opportunity>> = await api.post(
        '/api/v1/opportunities/search',
        criteria
      );
      return response.data;
    } catch (error) {
      console.log('🔄 API failed, falling back to demo data for search');
      return MockApiService.searchOpportunities();
    }
  }

  // Health Check APIs
  static async getApplicationHealth(): Promise<HealthStatus> {
    if (DEMO_MODE) {
      return MockApiService.getApplicationHealth();
    }

    try {
      const response: AxiosResponse<HealthStatus> = await api.get('/actuator/health');
      return response.data;
    } catch (error) {
      console.log('🔄 API failed, falling back to demo health status');
      return MockApiService.getApplicationHealth();
    }
  }

  static async getDiscoveryHealth(): Promise<DiscoveryHealth> {
    if (DEMO_MODE) {
      return MockApiService.getDiscoveryHealth();
    }

    try {
      const response: AxiosResponse<DiscoveryHealth> = await api.get('/api/v1/discovery/health');
      return response.data;
    } catch (error) {
      console.log('🔄 API failed, falling back to demo discovery health');
      return MockApiService.getDiscoveryHealth();
    }
  }

  // Discovery Management APIs
  static async triggerDiscovery(limitPerSource?: number): Promise<void> {
    if (DEMO_MODE) {
      return MockApiService.triggerDiscovery();
    }

    try {
      const params = limitPerSource ? `?limitPerSource=${limitPerSource}` : '';
      await api.post(`/api/v1/discovery/trigger${params}`);
    } catch (error) {
      console.log('🔄 API failed, using demo discovery trigger');
      return MockApiService.triggerDiscovery();
    }
  }

  static async triggerScoring(): Promise<void> {
    if (DEMO_MODE) {
      return MockApiService.triggerScoring();
    }

    try {
      await api.post('/api/v1/discovery/scoring/trigger');
    } catch (error) {
      console.log('🔄 API failed, using demo scoring trigger');
      return MockApiService.triggerScoring();
    }
  }

  static async getDataSources(): Promise<any> {
    if (DEMO_MODE) {
      return MockApiService.getDataSources();
    }

    try {
      const response = await api.get('/api/v1/discovery/sources');
      return response.data;
    } catch (error) {
      console.log('🔄 API failed, falling back to demo data sources');
      return MockApiService.getDataSources();
    }
  }
}

export default ApiService;