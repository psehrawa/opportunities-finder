import axios, { AxiosResponse } from 'axios';
import {
  Opportunity,
  OpportunitySearchCriteria,
  ApiResponse,
  HealthStatus,
  DiscoveryHealth
} from '../types';

const API_BASE_URL = process.env.REACT_APP_API_URL || 'http://localhost:8090';

// Log the API URL in development
if (process.env.NODE_ENV === 'development') {
  console.log('API Base URL:', API_BASE_URL);
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
    return Promise.reject(error);
  }
);

// API Service Class
export class ApiService {
  // Opportunities API
  static async getOpportunities(criteria: OpportunitySearchCriteria = {}): Promise<ApiResponse<Opportunity>> {
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
  }

  static async getOpportunityById(id: number): Promise<Opportunity> {
    const response: AxiosResponse<Opportunity> = await api.get(`/api/v1/opportunities/${id}`);
    return response.data;
  }

  static async searchOpportunities(criteria: OpportunitySearchCriteria): Promise<ApiResponse<Opportunity>> {
    const response: AxiosResponse<ApiResponse<Opportunity>> = await api.post(
      '/api/v1/opportunities/search',
      criteria
    );
    return response.data;
  }

  // Health Check APIs
  static async getApplicationHealth(): Promise<HealthStatus> {
    const response: AxiosResponse<HealthStatus> = await api.get('/actuator/health');
    return response.data;
  }

  static async getDiscoveryHealth(): Promise<DiscoveryHealth> {
    const response: AxiosResponse<DiscoveryHealth> = await api.get('/api/v1/discovery/health');
    return response.data;
  }

  // Discovery Management APIs
  static async triggerDiscovery(limitPerSource?: number): Promise<void> {
    const params = limitPerSource ? `?limitPerSource=${limitPerSource}` : '';
    await api.post(`/api/v1/discovery/trigger${params}`);
  }

  static async triggerScoring(): Promise<void> {
    await api.post('/api/v1/discovery/scoring/trigger');
  }

  static async getDataSources(): Promise<any> {
    const response = await api.get('/api/v1/discovery/sources');
    return response.data;
  }
}

export default ApiService;