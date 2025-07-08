import React from 'react';
import { useApi } from '../hooks/useApi';
import { ApiService } from '../services/api';
import { HealthStatus, DiscoveryHealth } from '../types';

const HealthDashboard: React.FC = () => {
  const { data: appHealth, loading: appLoading, error: appError, refetch: refetchApp } = useApi<HealthStatus>(
    () => ApiService.getApplicationHealth()
  );
  
  const { data: discoveryHealth, loading: discoveryLoading, error: discoveryError, refetch: refetchDiscovery } = useApi<DiscoveryHealth>(
    () => ApiService.getDiscoveryHealth()
  );

  const getStatusIcon = (status: string) => {
    switch (status.toLowerCase()) {
      case 'up':
      case 'healthy':
        return <div className="w-3 h-3 bg-success-500 rounded-full"></div>;
      case 'down':
        return <div className="w-3 h-3 bg-danger-500 rounded-full"></div>;
      case 'degraded':
        return <div className="w-3 h-3 bg-warning-500 rounded-full"></div>;
      default:
        return <div className="w-3 h-3 bg-gray-400 rounded-full"></div>;
    }
  };

  const getStatusColor = (status: string) => {
    switch (status.toLowerCase()) {
      case 'up':
      case 'healthy':
        return 'text-success-600';
      case 'down':
        return 'text-danger-600';
      case 'degraded':
        return 'text-warning-600';
      default:
        return 'text-gray-600';
    }
  };

  const formatDateTime = (dateString: string) => {
    return new Date(dateString).toLocaleString();
  };

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6">
      <div className="flex justify-between items-center mb-6">
        <h2 className="text-2xl font-bold text-gray-900">System Health</h2>
        <div className="flex space-x-2">
          <button
            onClick={refetchApp}
            disabled={appLoading}
            className="btn btn-secondary disabled:opacity-50"
          >
            {appLoading ? 'Refreshing...' : 'Refresh App'}
          </button>
          <button
            onClick={refetchDiscovery}
            disabled={discoveryLoading}
            className="btn btn-secondary disabled:opacity-50"
          >
            {discoveryLoading ? 'Refreshing...' : 'Refresh Discovery'}
          </button>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Application Health */}
        <div className="card">
          <div className="flex items-center justify-between mb-4">
            <h3 className="text-lg font-semibold text-gray-900">Application Health</h3>
            {appLoading ? (
              <div className="animate-spin rounded-full h-5 w-5 border-b-2 border-primary-600"></div>
            ) : (
              getStatusIcon(appHealth?.status || 'unknown')
            )}
          </div>

          {appError ? (
            <div className="text-danger-600 text-sm">
              Error: {appError}
            </div>
          ) : appHealth ? (
            <div className="space-y-3">
              <div className="flex justify-between items-center">
                <span className="text-sm text-gray-600">Overall Status:</span>
                <span className={`text-sm font-medium ${getStatusColor(appHealth.status)}`}>
                  {appHealth.status}
                </span>
              </div>

              {/* Component Health */}
              <div className="space-y-2">
                <h4 className="text-sm font-medium text-gray-700">Components:</h4>
                {Object.entries(appHealth.components).map(([component, health]) => (
                  <div key={component} className="flex justify-between items-center text-sm">
                    <div className="flex items-center space-x-2">
                      {getStatusIcon(health.status)}
                      <span className="text-gray-600 capitalize">{component}</span>
                    </div>
                    <span className={`font-medium ${getStatusColor(health.status)}`}>
                      {health.status}
                    </span>
                  </div>
                ))}
              </div>

              {/* Database Details */}
              {appHealth.components.db?.details && (
                <div className="mt-3 p-3 bg-gray-50 rounded-md">
                  <h5 className="text-xs font-medium text-gray-700 mb-1">Database Details:</h5>
                  <div className="text-xs text-gray-600">
                    Database: {appHealth.components.db.details.database}
                  </div>
                </div>
              )}

              {/* Disk Space Details */}
              {appHealth.components.diskSpace?.details && (
                <div className="mt-3 p-3 bg-gray-50 rounded-md">
                  <h5 className="text-xs font-medium text-gray-700 mb-1">Disk Space:</h5>
                  <div className="text-xs text-gray-600">
                    <div>Total: {Math.round(appHealth.components.diskSpace.details.total / 1024 / 1024 / 1024)} GB</div>
                    <div>Free: {Math.round(appHealth.components.diskSpace.details.free / 1024 / 1024 / 1024)} GB</div>
                  </div>
                </div>
              )}
            </div>
          ) : (
            <div className="text-gray-500 text-sm">Loading...</div>
          )}
        </div>

        {/* Discovery Health */}
        <div className="card">
          <div className="flex items-center justify-between mb-4">
            <h3 className="text-lg font-semibold text-gray-900">Discovery Service Health</h3>
            {discoveryLoading ? (
              <div className="animate-spin rounded-full h-5 w-5 border-b-2 border-primary-600"></div>
            ) : (
              getStatusIcon(discoveryHealth?.status || 'unknown')
            )}
          </div>

          {discoveryError ? (
            <div className="text-danger-600 text-sm">
              Error: {discoveryError}
            </div>
          ) : discoveryHealth ? (
            <div className="space-y-3">
              <div className="flex justify-between items-center">
                <span className="text-sm text-gray-600">Overall Status:</span>
                <span className={`text-sm font-medium ${getStatusColor(discoveryHealth.status)}`}>
                  {discoveryHealth.status}
                </span>
              </div>

              <div className="flex justify-between items-center">
                <span className="text-sm text-gray-600">Enabled Sources:</span>
                <span className="text-sm font-medium text-gray-900">
                  {discoveryHealth.enabledSources.length}
                </span>
              </div>

              <div className="flex justify-between items-center">
                <span className="text-sm text-gray-600">Last Updated:</span>
                <span className="text-xs text-gray-500">
                  {formatDateTime(discoveryHealth.timestamp)}
                </span>
              </div>

              {/* Data Sources Health */}
              <div className="space-y-2">
                <h4 className="text-sm font-medium text-gray-700">Data Sources:</h4>
                {Object.entries(discoveryHealth.dataSourcesHealth).map(([source, health]) => (
                  <div key={source} className="p-2 bg-gray-50 rounded-md">
                    <div className="flex justify-between items-center mb-1">
                      <div className="flex items-center space-x-2">
                        {getStatusIcon(health.status)}
                        <span className="text-sm font-medium text-gray-700">
                          {source.replace(/_/g, ' ')}
                        </span>
                      </div>
                      <span className={`text-xs font-medium ${getStatusColor(health.status)}`}>
                        {health.status}
                      </span>
                    </div>
                    <div className="text-xs text-gray-600">
                      {health.message}
                    </div>
                    <div className="text-xs text-gray-500 mt-1">
                      Last checked: {formatDateTime(health.lastChecked)}
                    </div>
                  </div>
                ))}
              </div>
            </div>
          ) : (
            <div className="text-gray-500 text-sm">Loading...</div>
          )}
        </div>
      </div>

      {/* System Overview Cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mt-6">
        <div className="card">
          <div className="flex items-center">
            <div className="flex-shrink-0">
              <div className="w-8 h-8 bg-primary-100 rounded-full flex items-center justify-center">
                <svg className="w-4 h-4 text-primary-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
              </div>
            </div>
            <div className="ml-3">
              <p className="text-sm font-medium text-gray-900">
                Application Status
              </p>
              <p className={`text-sm ${getStatusColor(appHealth?.status || 'unknown')}`}>
                {appHealth?.status || 'Loading...'}
              </p>
            </div>
          </div>
        </div>

        <div className="card">
          <div className="flex items-center">
            <div className="flex-shrink-0">
              <div className="w-8 h-8 bg-warning-100 rounded-full flex items-center justify-center">
                <svg className="w-4 h-4 text-warning-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
                </svg>
              </div>
            </div>
            <div className="ml-3">
              <p className="text-sm font-medium text-gray-900">
                Discovery Status
              </p>
              <p className={`text-sm ${getStatusColor(discoveryHealth?.status || 'unknown')}`}>
                {discoveryHealth?.status || 'Loading...'}
              </p>
            </div>
          </div>
        </div>

        <div className="card">
          <div className="flex items-center">
            <div className="flex-shrink-0">
              <div className="w-8 h-8 bg-success-100 rounded-full flex items-center justify-center">
                <svg className="w-4 h-4 text-success-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 7v10c0 2.21 3.582 4 8 4s8-1.79 8-4V7M4 7c0 2.21 3.582 4 8 4s8-1.79 8-4M4 7c0-2.21 3.582-4 8-4s8 1.79 8 4" />
                </svg>
              </div>
            </div>
            <div className="ml-3">
              <p className="text-sm font-medium text-gray-900">
                Active Sources
              </p>
              <p className="text-sm text-gray-600">
                {discoveryHealth?.enabledSources.length || 0} enabled
              </p>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default HealthDashboard;