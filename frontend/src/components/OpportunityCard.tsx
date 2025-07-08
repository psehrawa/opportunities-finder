import React from 'react';
import { Opportunity } from '../types';

interface OpportunityCardProps {
  opportunity: Opportunity;
  onClick?: () => void;
}

const OpportunityCard: React.FC<OpportunityCardProps> = ({ opportunity, onClick }) => {
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
    <div 
      className="card hover:shadow-lg hover:scale-[1.02] transition-all duration-200 cursor-pointer"
      onClick={onClick}
    >
      {/* Header */}
      <div className="flex justify-between items-start mb-3">
        <div className="flex-1">
          <h3 className="text-lg font-semibold text-gray-900 mb-1 line-clamp-2">
            {opportunity.title}
          </h3>
          {opportunity.companyName && (
            <p className="text-sm text-gray-600 mb-1">
              {opportunity.companyName}
            </p>
          )}
        </div>
        <div className="flex flex-col items-end space-y-1 ml-4">
          <span className={`px-2 py-1 rounded-full text-xs font-medium ${getScoreColor(opportunity.score)}`}>
            Score: {opportunity.score}
          </span>
        </div>
      </div>

      {/* Description */}
      {opportunity.description && (
        <p className="text-sm text-gray-700 mb-3 line-clamp-3">
          {opportunity.description}
        </p>
      )}

      {/* Metadata */}
      <div className="grid grid-cols-2 gap-2 mb-3 text-xs text-gray-600">
        <div>
          <span className="font-medium">Type:</span> {formatType(opportunity.type)}
        </div>
        <div>
          <span className="font-medium">Source:</span> {formatSource(opportunity.source)}
        </div>
        {opportunity.industry && (
          <div>
            <span className="font-medium">Industry:</span> {opportunity.industry}
          </div>
        )}
        {opportunity.location && (
          <div>
            <span className="font-medium">Location:</span> {opportunity.location}
          </div>
        )}
        {opportunity.fundingStage && (
          <div>
            <span className="font-medium">Stage:</span> {formatType(opportunity.fundingStage)}
          </div>
        )}
        {opportunity.fundingAmount && (
          <div>
            <span className="font-medium">Funding:</span> ${opportunity.fundingAmount.toLocaleString()}
          </div>
        )}
      </div>

      {/* Footer */}
      <div className="flex justify-between items-center pt-3 border-t border-gray-200">
        <div className="flex items-center space-x-2">
          <span className={`px-2 py-1 rounded-full text-xs font-medium ${getStatusColor(opportunity.status)}`}>
            {opportunity.status.replace(/_/g, ' ')}
          </span>
          {opportunity.confidenceScore && (
            <span className="text-xs text-gray-500">
              Confidence: {opportunity.confidenceScore}%
            </span>
          )}
        </div>
        <div className="text-xs text-gray-500">
          {formatDate(opportunity.discoveredAt)}
        </div>
      </div>

      {/* External Link */}
      {opportunity.url && (
        <div className="mt-2 pt-2 border-t border-gray-100">
          <a
            href={opportunity.url}
            target="_blank"
            rel="noopener noreferrer"
            className="text-xs text-primary-600 hover:text-primary-700 hover:underline"
            onClick={(e) => e.stopPropagation()}
          >
            View Original →
          </a>
        </div>
      )}
    </div>
  );
};

export default OpportunityCard;