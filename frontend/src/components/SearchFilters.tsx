import React, { useState } from 'react';
import { OpportunitySearchCriteria, OpportunityType, OpportunityStatus, DataSource } from '../types';

interface SearchFiltersProps {
  onSearch: (criteria: OpportunitySearchCriteria) => void;
  loading?: boolean;
}

const SearchFilters: React.FC<SearchFiltersProps> = ({ onSearch, loading = false }) => {
  const [searchTerm, setSearchTerm] = useState('');
  const [selectedType, setSelectedType] = useState<OpportunityType | ''>('');
  const [selectedStatus, setSelectedStatus] = useState<OpportunityStatus | ''>('');
  const [selectedSource, setSelectedSource] = useState<DataSource | ''>('');
  const [minScore, setMinScore] = useState<number | ''>('');
  const [maxScore, setMaxScore] = useState<number | ''>('');

  const opportunityTypes: OpportunityType[] = [
    'STARTUP_FUNDING', 'PRODUCT_LAUNCH', 'TECHNOLOGY_TREND', 'MARKET_EXPANSION',
    'PARTNERSHIP', 'ACQUISITION_TARGET', 'JOB_POSTING_SIGNAL', 'PATENT_FILING',
    'CONFERENCE_ANNOUNCEMENT', 'REGULATORY_CHANGE', 'COMPETITOR_ANALYSIS', 'TECHNOLOGY_ADOPTION'
  ];

  const opportunityStatuses: OpportunityStatus[] = [
    'DISCOVERED', 'ANALYZED', 'ENGAGED', 'DISCARDED', 'MONITORING', 'CONVERTED', 'EXPIRED', 'DUPLICATE'
  ];

  const dataSources: DataSource[] = [
    'GITHUB', 'HACKER_NEWS', 'REDDIT', 'PRODUCT_HUNT', 'CRUNCHBASE_BASIC'
  ];

  const handleSearch = () => {
    const criteria: OpportunitySearchCriteria = {
      searchTerm: searchTerm || undefined,
      types: selectedType ? [selectedType] : undefined,
      statuses: selectedStatus ? [selectedStatus] : undefined,
      sources: selectedSource ? [selectedSource] : undefined,
      minScore: minScore ? Number(minScore) : undefined,
      maxScore: maxScore ? Number(maxScore) : undefined,
      isActive: true,
      page: 0,
      size: 20,
      sortBy: 'score',
      sortDirection: 'DESC'
    };
    onSearch(criteria);
  };

  const handleReset = () => {
    setSearchTerm('');
    setSelectedType('');
    setSelectedStatus('');
    setSelectedSource('');
    setMinScore('');
    setMaxScore('');
    onSearch({ isActive: true, page: 0, size: 20, sortBy: 'discoveredAt', sortDirection: 'DESC' });
  };

  return (
    <div className="card mb-6">
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 mb-4">
        {/* Search Term */}
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">
            Search
          </label>
          <input
            type="text"
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            placeholder="Search opportunities..."
            className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-primary-500"
          />
        </div>

        {/* Type Filter */}
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">
            Type
          </label>
          <select
            value={selectedType}
            onChange={(e) => setSelectedType(e.target.value as OpportunityType)}
            className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-primary-500"
          >
            <option value="">All Types</option>
            {opportunityTypes.map(type => (
              <option key={type} value={type}>
                {type.replace(/_/g, ' ')}
              </option>
            ))}
          </select>
        </div>

        {/* Status Filter */}
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">
            Status
          </label>
          <select
            value={selectedStatus}
            onChange={(e) => setSelectedStatus(e.target.value as OpportunityStatus)}
            className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-primary-500"
          >
            <option value="">All Statuses</option>
            {opportunityStatuses.map(status => (
              <option key={status} value={status}>
                {status.replace(/_/g, ' ')}
              </option>
            ))}
          </select>
        </div>

        {/* Source Filter */}
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">
            Source
          </label>
          <select
            value={selectedSource}
            onChange={(e) => setSelectedSource(e.target.value as DataSource)}
            className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-primary-500"
          >
            <option value="">All Sources</option>
            {dataSources.map(source => (
              <option key={source} value={source}>
                {source.replace(/_/g, ' ')}
              </option>
            ))}
          </select>
        </div>
      </div>

      {/* Score Range */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mb-4">
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">
            Min Score
          </label>
          <input
            type="number"
            min="0"
            max="100"
            value={minScore}
            onChange={(e) => setMinScore(e.target.value ? Number(e.target.value) : '')}
            placeholder="0"
            className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-primary-500"
          />
        </div>
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">
            Max Score
          </label>
          <input
            type="number"
            min="0"
            max="100"
            value={maxScore}
            onChange={(e) => setMaxScore(e.target.value ? Number(e.target.value) : '')}
            placeholder="100"
            className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-primary-500"
          />
        </div>
      </div>

      {/* Action Buttons */}
      <div className="flex space-x-2">
        <button
          onClick={handleSearch}
          disabled={loading}
          className="btn btn-primary disabled:opacity-50"
        >
          {loading ? 'Searching...' : 'Search'}
        </button>
        <button
          onClick={handleReset}
          disabled={loading}
          className="btn btn-secondary disabled:opacity-50"
        >
          Reset
        </button>
      </div>
    </div>
  );
};

export default SearchFilters;