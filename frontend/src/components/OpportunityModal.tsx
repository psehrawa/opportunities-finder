import React, { useEffect } from 'react';
import { Opportunity } from '../types';

interface OpportunityModalProps {
  opportunity: Opportunity | null;
  isOpen: boolean;
  onClose: () => void;
}

const OpportunityModal: React.FC<OpportunityModalProps> = ({ opportunity, isOpen, onClose }) => {
  // Close modal on Escape key press
  useEffect(() => {
    const handleEscape = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        onClose();
      }
    };

    if (isOpen) {
      document.addEventListener('keydown', handleEscape);
      // Prevent body scroll when modal is open
      document.body.style.overflow = 'hidden';
    }

    return () => {
      document.removeEventListener('keydown', handleEscape);
      document.body.style.overflow = 'unset';
    };
  }, [isOpen, onClose]);

  if (!isOpen || !opportunity) return null;

  const getScoreColor = (score: number) => {
    if (score >= 80) return 'text-success-600 bg-success-50';
    if (score >= 60) return 'text-warning-600 bg-warning-50';
    return 'text-danger-600 bg-danger-50';
  };

  const getStatusColor = (status: string) => {
    switch (status.toLowerCase()) {
      case 'discovered':
        return 'text-blue-600 bg-blue-50';
      case 'analyzed':
        return 'text-purple-600 bg-purple-50';
      case 'engaged':
        return 'text-success-600 bg-success-50';
      case 'monitoring':
        return 'text-warning-600 bg-warning-50';
      case 'converted':
        return 'text-green-600 bg-green-50';
      case 'discarded':
      case 'expired':
        return 'text-gray-600 bg-gray-50';
      default:
        return 'text-gray-600 bg-gray-50';
    }
  };

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  const formatType = (type: string) => {
    return type.replace(/_/g, ' ').toLowerCase().replace(/\b\w/g, l => l.toUpperCase());
  };

  const formatSource = (source: string) => {
    return source.replace(/_/g, ' ').toLowerCase().replace(/\b\w/g, l => l.toUpperCase());
  };

  return (
    <div className="fixed inset-0 z-50 overflow-y-auto">
      {/* Backdrop */}
      <div 
        className="fixed inset-0 bg-gray-900 bg-opacity-75 transition-opacity animate-fadeIn"
        onClick={onClose}
      />

      {/* Modal Content */}
      <div className="flex min-h-full items-center justify-center p-4 text-center sm:p-0">
        <div className="relative transform overflow-hidden rounded-lg bg-white text-left shadow-xl transition-all sm:my-8 sm:w-full sm:max-w-3xl animate-slideIn">
          {/* Header */}
          <div className="bg-white px-6 py-5 border-b border-gray-200">
            <div className="flex items-start justify-between">
              <div className="flex-1 pr-4">
                <h2 className="text-2xl font-bold text-gray-900 mb-2">
                  {opportunity.title}
                </h2>
                {opportunity.companyName && (
                  <p className="text-lg text-gray-600">
                    {opportunity.companyName}
                  </p>
                )}
              </div>
              <button
                onClick={onClose}
                className="rounded-md bg-white text-gray-400 hover:text-gray-500 focus:outline-none focus:ring-2 focus:ring-primary-500 focus:ring-offset-2"
              >
                <span className="sr-only">Close</span>
                <svg className="h-6 w-6" fill="none" viewBox="0 0 24 24" strokeWidth="1.5" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12" />
                </svg>
              </button>
            </div>
          </div>

          {/* Body */}
          <div className="bg-white px-6 py-5 max-h-[calc(100vh-200px)] overflow-y-auto">
            {/* Scores and Status */}
            <div className="flex flex-wrap gap-3 mb-6">
              <span className={`px-3 py-1.5 rounded-full text-sm font-medium ${getScoreColor(opportunity.score)}`}>
                Score: {opportunity.score}
              </span>
              <span className={`px-3 py-1.5 rounded-full text-sm font-medium ${getStatusColor(opportunity.status)}`}>
                {opportunity.status.replace(/_/g, ' ')}
              </span>
              {opportunity.confidenceScore && (
                <span className="px-3 py-1.5 rounded-full text-sm font-medium bg-gray-100 text-gray-700">
                  Confidence: {opportunity.confidenceScore}%
                </span>
              )}
              {opportunity.engagementPotential && (
                <span className="px-3 py-1.5 rounded-full text-sm font-medium bg-indigo-50 text-indigo-700">
                  Engagement Potential: {opportunity.engagementPotential}%
                </span>
              )}
            </div>

            {/* Description */}
            {opportunity.description && (
              <div className="mb-6">
                <h3 className="text-lg font-semibold text-gray-900 mb-2">Description</h3>
                <p className="text-gray-700 whitespace-pre-wrap">{opportunity.description}</p>
              </div>
            )}

            {/* Key Information Grid */}
            <div className="mb-6">
              <h3 className="text-lg font-semibold text-gray-900 mb-3">Key Information</h3>
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div>
                  <p className="text-sm font-medium text-gray-500 mb-1">Type</p>
                  <p className="text-gray-900">{formatType(opportunity.type)}</p>
                </div>
                <div>
                  <p className="text-sm font-medium text-gray-500 mb-1">Source</p>
                  <p className="text-gray-900">{formatSource(opportunity.source)}</p>
                </div>
                {opportunity.industry && (
                  <div>
                    <p className="text-sm font-medium text-gray-500 mb-1">Industry</p>
                    <p className="text-gray-900">{opportunity.industry}</p>
                  </div>
                )}
                {opportunity.location && (
                  <div>
                    <p className="text-sm font-medium text-gray-500 mb-1">Location</p>
                    <p className="text-gray-900">{opportunity.location}</p>
                  </div>
                )}
                {opportunity.country && (
                  <div>
                    <p className="text-sm font-medium text-gray-500 mb-1">Country</p>
                    <p className="text-gray-900">{opportunity.country}</p>
                  </div>
                )}
                {opportunity.companySize && (
                  <div>
                    <p className="text-sm font-medium text-gray-500 mb-1">Company Size</p>
                    <p className="text-gray-900">{formatType(opportunity.companySize)}</p>
                  </div>
                )}
              </div>
            </div>

            {/* Funding Information */}
            {(opportunity.fundingStage || opportunity.fundingAmount) && (
              <div className="mb-6">
                <h3 className="text-lg font-semibold text-gray-900 mb-3">Funding Information</h3>
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                  {opportunity.fundingStage && (
                    <div>
                      <p className="text-sm font-medium text-gray-500 mb-1">Funding Stage</p>
                      <p className="text-gray-900">{formatType(opportunity.fundingStage)}</p>
                    </div>
                  )}
                  {opportunity.fundingAmount && (
                    <div>
                      <p className="text-sm font-medium text-gray-500 mb-1">Funding Amount</p>
                      <p className="text-gray-900">${opportunity.fundingAmount.toLocaleString()}</p>
                    </div>
                  )}
                </div>
              </div>
            )}

            {/* Contact Information */}
            {opportunity.contactEmail && (
              <div className="mb-6">
                <h3 className="text-lg font-semibold text-gray-900 mb-3">Contact Information</h3>
                <div>
                  <p className="text-sm font-medium text-gray-500 mb-1">Email</p>
                  <a 
                    href={`mailto:${opportunity.contactEmail}`}
                    className="text-primary-600 hover:text-primary-700 hover:underline"
                  >
                    {opportunity.contactEmail}
                  </a>
                </div>
              </div>
            )}

            {/* Metadata */}
            <div className="mb-6">
              <h3 className="text-lg font-semibold text-gray-900 mb-3">Metadata</h3>
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div>
                  <p className="text-sm font-medium text-gray-500 mb-1">External ID</p>
                  <p className="text-gray-900 font-mono text-sm">{opportunity.externalId}</p>
                </div>
                <div>
                  <p className="text-sm font-medium text-gray-500 mb-1">Discovered At</p>
                  <p className="text-gray-900">{formatDate(opportunity.discoveredAt)}</p>
                </div>
                <div>
                  <p className="text-sm font-medium text-gray-500 mb-1">Last Updated</p>
                  <p className="text-gray-900">{formatDate(opportunity.lastUpdated)}</p>
                </div>
                <div>
                  <p className="text-sm font-medium text-gray-500 mb-1">Version</p>
                  <p className="text-gray-900">{opportunity.version}</p>
                </div>
                <div>
                  <p className="text-sm font-medium text-gray-500 mb-1">Status</p>
                  <p className="text-gray-900">{opportunity.isActive ? 'Active' : 'Inactive'}</p>
                </div>
              </div>
            </div>

            {/* External Link */}
            {opportunity.url && (
              <div className="mb-6">
                <h3 className="text-lg font-semibold text-gray-900 mb-3">External Link</h3>
                <a
                  href={opportunity.url}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="inline-flex items-center text-primary-600 hover:text-primary-700 hover:underline"
                >
                  View Original Source
                  <svg className="ml-1 h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10 6H6a2 2 0 00-2 2v10a2 2 0 002 2h10a2 2 0 002-2v-4M14 4h6m0 0v6m0-6L10 14" />
                  </svg>
                </a>
              </div>
            )}
          </div>

          {/* Footer */}
          <div className="bg-gray-50 px-6 py-4 flex justify-end">
            <button
              onClick={onClose}
              className="btn btn-secondary"
            >
              Close
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

export default OpportunityModal;