import React from 'react';

interface DemoBannerProps {
  isDemoMode: boolean;
}

const DemoBanner: React.FC<DemoBannerProps> = ({ isDemoMode }) => {
  if (!isDemoMode) return null;

  return (
    <div className="bg-gradient-to-r from-blue-500 to-purple-600 text-white px-4 py-2 text-center">
      <div className="flex items-center justify-center space-x-2">
        <span className="text-sm font-medium">
          🎭 Demo Mode
        </span>
        <span className="text-xs opacity-90">
          • Using sample data • All features functional • 
        </span>
        <a 
          href="https://github.com/psehrawa/opportunities-finder" 
          target="_blank" 
          rel="noopener noreferrer"
          className="text-xs underline hover:no-underline"
        >
          View Code
        </a>
      </div>
    </div>
  );
};

export default DemoBanner;