import React from 'react';
import { useApi } from '../hooks/useApi';
import { ApiService } from '../services/api';
import { HealthStatus, DiscoveryHealth } from '../types';

const Header: React.FC = () => {
  const { data: appHealth } = useApi<HealthStatus>(() => ApiService.getApplicationHealth());
  const { data: discoveryHealth } = useApi<DiscoveryHealth>(() => ApiService.getDiscoveryHealth());

  const getStatusColor = (status: string) => {
    switch (status.toLowerCase()) {
      case 'up':
      case 'healthy':
        return 'text-success-600 bg-success-50';
      case 'down':
        return 'text-danger-600 bg-danger-50';
      case 'degraded':
        return 'text-warning-600 bg-warning-50';
      default:
        return 'text-gray-600 bg-gray-50';
    }
  };

  return (
    <header className="bg-white shadow-sm border-b">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex justify-between items-center h-16">
          <div className="flex items-center">
            <h1 className="text-xl font-bold text-gray-900">
              TechOpportunity Intelligence Platform
            </h1>
          </div>
          
          <div className="flex items-center space-x-4">
            {/* Application Health */}
            <div className="flex items-center space-x-2">
              <span className="text-sm text-gray-500">App:</span>
              <span className={`px-2 py-1 rounded-full text-xs font-medium ${getStatusColor(appHealth?.status || 'unknown')}`}>
                {appHealth?.status || 'Loading...'}
              </span>
            </div>
            
            {/* Discovery Health */}
            <div className="flex items-center space-x-2">
              <span className="text-sm text-gray-500">Discovery:</span>
              <span className={`px-2 py-1 rounded-full text-xs font-medium ${getStatusColor(discoveryHealth?.status || 'unknown')}`}>
                {discoveryHealth?.status || 'Loading...'}
              </span>
            </div>
            
            {/* Data Sources Count */}
            {discoveryHealth && (
              <div className="flex items-center space-x-2">
                <span className="text-sm text-gray-500">Sources:</span>
                <span className="text-sm font-medium text-gray-900">
                  {discoveryHealth.enabledSources?.length || 0}
                </span>
              </div>
            )}
          </div>
        </div>
      </div>
    </header>
  );
};

export default Header;