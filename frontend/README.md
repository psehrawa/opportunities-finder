# TechOpportunity Intelligence Platform - Frontend

React TypeScript frontend for the TechOpportunity Intelligence Platform.

## Features

- 🔍 **Opportunities Dashboard** - Browse and search discovered opportunities
- 📊 **Advanced Filtering** - Filter by type, status, source, score, and more
- 🏥 **Health Monitoring** - Real-time system and service health checks
- 🚀 **Discovery Management** - Trigger opportunity discovery and scoring
- 📱 **Responsive Design** - Works on desktop, tablet, and mobile
- ⚡ **Real-time Updates** - Live data from the backend API

## Technology Stack

- **React 18** with TypeScript
- **Tailwind CSS** for styling
- **Axios** for API communication
- **React Hooks** for state management
- **PostCSS** for CSS processing

## Getting Started

### Prerequisites

- Node.js 16+ 
- npm or yarn
- Backend API running on http://localhost:8090

### Installation

1. Install dependencies:
```bash
cd frontend
npm install
```

2. Start the development server:
```bash
npm start
```

3. Open [http://localhost:3000](http://localhost:3000) in your browser

### Available Scripts

- `npm start` - Start development server
- `npm build` - Build for production
- `npm test` - Run tests
- `npm run eject` - Eject from Create React App

## API Integration

The frontend communicates with the backend API at `http://localhost:8090`. Key endpoints:

- `GET /api/v1/opportunities` - Get opportunities with pagination and filtering
- `POST /api/v1/opportunities/search` - Advanced opportunity search
- `GET /actuator/health` - Application health status
- `GET /api/v1/discovery/health` - Discovery service health
- `POST /api/v1/discovery/trigger` - Trigger discovery process
- `POST /api/v1/discovery/scoring/trigger` - Trigger scoring process

## Component Architecture

```
src/
├── components/          # React components
│   ├── Header.tsx      # Application header with health status
│   ├── OpportunityList.tsx    # Main opportunities listing
│   ├── OpportunityCard.tsx    # Individual opportunity card
│   ├── SearchFilters.tsx      # Search and filter controls
│   └── HealthDashboard.tsx    # System health monitoring
├── services/           # API service layer
│   └── api.ts         # API client and methods
├── hooks/             # Custom React hooks
│   └── useApi.ts      # API data fetching hooks
├── types/             # TypeScript type definitions
│   └── index.ts       # All type definitions
└── App.tsx            # Main application component
```

## Features

### Opportunities Dashboard
- Browse all discovered opportunities
- Paginated results with customizable page sizes
- Sort by score, discovery date, or other fields
- Click on opportunities to view details

### Advanced Search & Filtering
- Full-text search across opportunity titles and descriptions
- Filter by opportunity type, status, source, industry
- Score range filtering (0-100)
- Company size and funding stage filters
- Date range filtering for discovery date

### Health Monitoring
- Real-time application health status
- Database, Redis, and disk space monitoring
- Discovery service health with data source status
- Individual data source health checks (GitHub, etc.)
- Last updated timestamps

### Discovery Management
- Trigger discovery from external data sources
- Trigger opportunity scoring algorithms
- View data source configuration and status
- Real-time feedback on operation status

## Styling

The application uses Tailwind CSS with a custom design system:

- **Primary Colors**: Blue theme for main actions
- **Status Colors**: Green (success), Yellow (warning), Red (danger)
- **Typography**: Clean, readable font hierarchy
- **Components**: Consistent card-based layout
- **Responsive**: Mobile-first responsive design

## Environment Variables

Create a `.env` file in the frontend directory:

```bash
REACT_APP_API_URL=http://localhost:8090
GENERATE_SOURCEMAP=false
FAST_REFRESH=true
```

## Development

### Adding New Components

1. Create component in `src/components/`
2. Add TypeScript interfaces in `src/types/`
3. Implement API methods in `src/services/api.ts`
4. Use custom hooks for data fetching

### Error Handling

All API calls include error handling with user-friendly messages. Errors are displayed in the UI with retry options.

### Performance

- Components use React.memo for optimization
- API calls are debounced for search inputs
- Pagination reduces data transfer
- Loading states improve perceived performance

## Deployment

1. Build the application:
```bash
npm run build
```

2. Serve the `build/` directory with any static file server
3. Ensure the API URL is correctly configured for production

## Troubleshooting

### Common Issues

1. **API Connection Failed**
   - Ensure backend is running on http://localhost:8090
   - Check CORS configuration in backend
   - Verify network connectivity

2. **Build Errors**
   - Run `npm install` to install dependencies
   - Check TypeScript configuration
   - Ensure all imports are correct

3. **Styling Issues**
   - Rebuild Tailwind CSS: `npm run build:css`
   - Check PostCSS configuration
   - Clear browser cache

## Contributing

1. Follow TypeScript strict mode
2. Use functional components with hooks
3. Implement proper error handling
4. Add loading states for async operations
5. Follow the existing component patterns