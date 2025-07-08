import React, { useState } from 'react';
import Header from './components/Header';
import OpportunityList from './components/OpportunityList';
import HealthDashboard from './components/HealthDashboard';
import DemoBanner from './components/DemoBanner';

type TabType = 'opportunities' | 'health';

const App: React.FC = () => {
  const [activeTab, setActiveTab] = useState<TabType>('opportunities');
  
  // Check if we're in demo mode
  const isDemoMode = process.env.REACT_APP_DEMO_MODE === 'true' || 
                    window.location.hostname.includes('github.io');

  const tabs: { key: TabType; label: string; icon: string }[] = [
    {
      key: 'opportunities',
      label: 'Opportunities',
      icon: 'M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z'
    },
    {
      key: 'health',
      label: 'System Health',
      icon: 'M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z'
    }
  ];

  const renderContent = () => {
    switch (activeTab) {
      case 'opportunities':
        return <OpportunityList />;
      case 'health':
        return <HealthDashboard />;
      default:
        return <OpportunityList />;
    }
  };

  return (
    <div className="min-h-screen bg-gray-50">
      <DemoBanner isDemoMode={isDemoMode} />
      <Header />
      
      {/* Navigation Tabs */}
      <div className="bg-white shadow-sm">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex space-x-8">
            {tabs.map((tab) => (
              <button
                key={tab.key}
                onClick={() => setActiveTab(tab.key)}
                className={`flex items-center space-x-2 py-4 px-1 border-b-2 font-medium text-sm transition-colors duration-200 ${
                  activeTab === tab.key
                    ? 'border-primary-500 text-primary-600'
                    : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
                }`}
              >
                <svg
                  className="w-5 h-5"
                  fill="none"
                  stroke="currentColor"
                  viewBox="0 0 24 24"
                >
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    strokeWidth={2}
                    d={tab.icon}
                  />
                </svg>
                <span>{tab.label}</span>
              </button>
            ))}
          </div>
        </div>
      </div>

      {/* Main Content */}
      <main className="flex-1">
        {renderContent()}
      </main>

      {/* Footer */}
      <footer className="bg-white border-t">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-4">
          <div className="flex justify-between items-center">
            <p className="text-sm text-gray-500">
              © 2024 TechOpportunity Intelligence Platform
            </p>
            <div className="flex items-center space-x-4 text-sm text-gray-500">
              <span>
                {isDemoMode ? '🎭 Demo Mode - Sample Data' : 'API: http://localhost:8090'}
              </span>
              {!isDemoMode && (
                <a
                  href="http://localhost:8090/actuator/health"
                  target="_blank"
                  rel="noopener noreferrer"
                  className="hover:text-primary-600"
                >
                  Health Check
                </a>
              )}
              {isDemoMode && (
                <a
                  href="https://github.com/psehrawa/opportunities-finder"
                  target="_blank"
                  rel="noopener noreferrer"
                  className="hover:text-primary-600"
                >
                  View Source Code
                </a>
              )}
            </div>
          </div>
        </div>
      </footer>
    </div>
  );
};

export default App;