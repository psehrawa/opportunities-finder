#!/bin/bash

# Health check script for monitoring deployment

BACKEND_URL="${BACKEND_URL:-http://localhost:8090}"
FRONTEND_URL="${FRONTEND_URL:-http://localhost:3000}"

echo "🏥 Health Check Report"
echo "====================="
echo "Timestamp: $(date)"
echo ""

# Check backend health
echo "Backend Health Check:"
echo "URL: $BACKEND_URL/actuator/health"
BACKEND_RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" "$BACKEND_URL/actuator/health")
if [ "$BACKEND_RESPONSE" = "200" ]; then
    echo "✅ Backend is healthy (HTTP $BACKEND_RESPONSE)"
    
    # Get detailed health info
    HEALTH_JSON=$(curl -s "$BACKEND_URL/actuator/health")
    echo "Details: $HEALTH_JSON" | jq -r '.status' 2>/dev/null || echo "$HEALTH_JSON"
else
    echo "❌ Backend is unhealthy (HTTP $BACKEND_RESPONSE)"
fi

echo ""

# Check discovery service health
echo "Discovery Service Health:"
DISCOVERY_RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" "$BACKEND_URL/api/v1/discovery/health")
if [ "$DISCOVERY_RESPONSE" = "200" ]; then
    echo "✅ Discovery service is healthy"
    
    # Get discovery stats
    STATS=$(curl -s "$BACKEND_URL/api/v1/discovery/stats" 2>/dev/null)
    if [ $? -eq 0 ]; then
        echo "Stats: $STATS" | jq -r '. | "Total Opportunities: \(.totalOpportunities), Enabled Sources: \(.enabledSources | length)"' 2>/dev/null || echo "$STATS"
    fi
else
    echo "❌ Discovery service is unhealthy (HTTP $DISCOVERY_RESPONSE)"
fi

echo ""

# Check frontend (if deployed)
if [ "$FRONTEND_URL" != "http://localhost:3000" ]; then
    echo "Frontend Health Check:"
    echo "URL: $FRONTEND_URL"
    FRONTEND_RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" "$FRONTEND_URL")
    if [ "$FRONTEND_RESPONSE" = "200" ]; then
        echo "✅ Frontend is accessible (HTTP $FRONTEND_RESPONSE)"
    else
        echo "❌ Frontend is not accessible (HTTP $FRONTEND_RESPONSE)"
    fi
fi

echo ""
echo "====================="

# Exit with error if any service is down
if [ "$BACKEND_RESPONSE" != "200" ] || [ "$DISCOVERY_RESPONSE" != "200" ]; then
    exit 1
fi

exit 0