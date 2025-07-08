import React, { useState, useCallback } from 'react';
import { useApi } from '../hooks/useApi';
import { useAsyncOperation } from '../hooks/useApi';
import { ApiService } from '../services/api';
import { Opportunity, OpportunitySearchCriteria, ApiResponse } from '../types';
import OpportunityCard from './OpportunityCard';
import SearchFilters from './SearchFilters';
import OpportunityModal from './OpportunityModal';

const OpportunityList: React.FC = () => {
  const [searchCriteria, setSearchCriteria] = useState<OpportunitySearchCriteria>({
    isActive: true,
    page: 0,
    size: 20,
    sortBy: 'discoveredAt',
    sortDirection: 'DESC'
  });
  const [selectedOpportunity, setSelectedOpportunity] = useState<Opportunity | null>(null);
  const [isModalOpen, setIsModalOpen] = useState(false);

  const { data: opportunitiesData, loading, error, refetch } = useApi<ApiResponse<Opportunity>>(
    () => ApiService.getOpportunities(searchCriteria),
    [searchCriteria]
  );

  const { execute: triggerDiscovery, loading: discoveryLoading } = useAsyncOperation();
  const { execute: triggerScoring, loading: scoringLoading } = useAsyncOperation();

  const handleSearch = useCallback((criteria: OpportunitySearchCriteria) => {
    setSearchCriteria(criteria);
  }, []);

  const handlePageChange = (newPage: number) => {
    setSearchCriteria(prev => ({ ...prev, page: newPage }));
  };

  const handleTriggerDiscovery = async () => {
    try {
      await triggerDiscovery(() => ApiService.triggerDiscovery(10));
      // Refresh the opportunities list after discovery
      setTimeout(() => refetch(), 2000);
    } catch (error) {
      console.error('Failed to trigger discovery:', error);
    }
  };

  const handleTriggerScoring = async () => {
    try {
      await triggerScoring(() => ApiService.triggerScoring());
      // Refresh the opportunities list after scoring
      setTimeout(() => refetch(), 1000);
    } catch (error) {
      console.error('Failed to trigger scoring:', error);
    }
  };

  const handleOpportunityClick = (opportunity: Opportunity) => {
    setSelectedOpportunity(opportunity);
    setIsModalOpen(true);
  };

  const handleCloseModal = () => {
    setIsModalOpen(false);
    setSelectedOpportunity(null);
  };

  const renderPagination = () => {
    if (!opportunitiesData || opportunitiesData.totalPages <= 1) return null;

    const currentPage = opportunitiesData.number;
    const totalPages = opportunitiesData.totalPages;
    const pages = [];

    // Calculate page range to show
    const startPage = Math.max(0, currentPage - 2);
    const endPage = Math.min(totalPages - 1, currentPage + 2);

    for (let i = startPage; i <= endPage; i++) {
      pages.push(i);
    }

    return (
      <div className="flex justify-center items-center space-x-2 mt-6">
        <button
          onClick={() => handlePageChange(currentPage - 1)}
          disabled={currentPage === 0}
          className="btn btn-secondary disabled:opacity-50"
        >
          Previous
        </button>
        
        {pages.map(page => (
          <button
            key={page}
            onClick={() => handlePageChange(page)}
            className={`px-3 py-1 rounded-md ${
              page === currentPage
                ? 'bg-primary-600 text-white'
                : 'bg-gray-200 text-gray-700 hover:bg-gray-300'
            }`}
          >
            {page + 1}
          </button>
        ))}
        
        <button
          onClick={() => handlePageChange(currentPage + 1)}
          disabled={currentPage >= totalPages - 1}
          className="btn btn-secondary disabled:opacity-50"
        >
          Next
        </button>
      </div>
    );
  };

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6">
      {/* Action Bar */}
      <div className="flex justify-between items-center mb-6">
        <h2 className="text-2xl font-bold text-gray-900">Opportunities</h2>
        <div className="flex space-x-2">
          <button
            onClick={handleTriggerDiscovery}
            disabled={discoveryLoading}
            className="btn btn-primary disabled:opacity-50"
          >
            {discoveryLoading ? 'Discovering...' : 'Trigger Discovery'}
          </button>
          <button
            onClick={handleTriggerScoring}
            disabled={scoringLoading}
            className="btn btn-secondary disabled:opacity-50"
          >
            {scoringLoading ? 'Scoring...' : 'Trigger Scoring'}
          </button>
          <button
            onClick={refetch}
            disabled={loading}
            className="btn btn-secondary disabled:opacity-50"
          >
            {loading ? 'Refreshing...' : 'Refresh'}
          </button>
        </div>
      </div>

      {/* Search Filters */}
      <SearchFilters onSearch={handleSearch} loading={loading} />

      {/* Results Summary */}
      {opportunitiesData && (
        <div className="mb-4 text-sm text-gray-600">
          Showing {opportunitiesData.numberOfElements} of {opportunitiesData.totalElements} opportunities
          {searchCriteria.searchTerm && (
            <span> for "{searchCriteria.searchTerm}"</span>
          )}
        </div>
      )}

      {/* Loading State */}
      {loading && (
        <div className="flex justify-center items-center py-12">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600"></div>
          <span className="ml-2 text-gray-600">Loading opportunities...</span>
        </div>
      )}

      {/* Error State */}
      {error && (
        <div className="card bg-danger-50 border border-danger-200">
          <div className="flex items-center">
            <div className="text-danger-600">
              <svg className="w-5 h-5" fill="currentColor" viewBox="0 0 20 20">
                <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z" clipRule="evenodd" />
              </svg>
            </div>
            <div className="ml-2">
              <h3 className="text-sm font-medium text-danger-800">Error loading opportunities</h3>
              <p className="text-sm text-danger-700">{error}</p>
            </div>
          </div>
        </div>
      )}

      {/* Empty State */}
      {!loading && !error && opportunitiesData?.content.length === 0 && (
        <div className="text-center py-12">
          <div className="text-gray-400 mb-4">
            <svg className="w-16 h-16 mx-auto" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
            </svg>
          </div>
          <h3 className="text-lg font-medium text-gray-900 mb-2">No opportunities found</h3>
          <p className="text-gray-600 mb-4">
            {searchCriteria.searchTerm 
              ? 'Try adjusting your search criteria or discover new opportunities.'
              : 'Start by triggering discovery to find new opportunities.'
            }
          </p>
          <button
            onClick={handleTriggerDiscovery}
            disabled={discoveryLoading}
            className="btn btn-primary"
          >
            {discoveryLoading ? 'Discovering...' : 'Discover Opportunities'}
          </button>
        </div>
      )}

      {/* Opportunities Grid */}
      {!loading && !error && opportunitiesData && opportunitiesData.content.length > 0 && (
        <>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {opportunitiesData.content.map((opportunity) => (
              <OpportunityCard
                key={opportunity.id}
                opportunity={opportunity}
                onClick={() => handleOpportunityClick(opportunity)}
              />
            ))}
          </div>

          {/* Pagination */}
          {renderPagination()}
        </>
      )}

      {/* Opportunity Detail Modal */}
      <OpportunityModal
        opportunity={selectedOpportunity}
        isOpen={isModalOpen}
        onClose={handleCloseModal}
      />
    </div>
  );
};

export default OpportunityList;