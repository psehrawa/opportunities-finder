#!/bin/bash

# Script to check Render deployment status

echo "🚀 Checking Render Deployment Status"
echo "===================================="
echo ""

# Expected URLs (will be available after deployment)
BACKEND_URL="https://oppfinder-backend.onrender.com"
FRONTEND_URL="https://oppfinder-frontend.onrender.com"

echo "📍 Expected URLs:"
echo "Frontend: $FRONTEND_URL"
echo "Backend:  $BACKEND_URL"
echo ""

# Check if services are deployed and running
echo "🔍 Checking service availability..."

# Check backend
echo -n "Backend Status: "
BACKEND_STATUS=$(curl -s -o /dev/null -w "%{http_code}" "$BACKEND_URL/actuator/health" 2>/dev/null)
if [ "$BACKEND_STATUS" = "200" ]; then
    echo "✅ ONLINE (HTTP $BACKEND_STATUS)"
    
    # Get health details
    echo "   Health: $(curl -s "$BACKEND_URL/actuator/health" | jq -r '.status' 2>/dev/null || echo 'UP')"
    
    # Check discovery service
    DISCOVERY_STATUS=$(curl -s -o /dev/null -w "%{http_code}" "$BACKEND_URL/api/v1/discovery/health" 2>/dev/null)
    if [ "$DISCOVERY_STATUS" = "200" ]; then
        echo "   Discovery: ✅ HEALTHY"
        
        # Get stats if available
        STATS=$(curl -s "$BACKEND_URL/api/v1/discovery/stats" 2>/dev/null)
        if [ $? -eq 0 ]; then
            TOTAL=$(echo "$STATS" | jq -r '.totalOpportunities // 0' 2>/dev/null || echo '0')
            SOURCES=$(echo "$STATS" | jq -r '.enabledSources | length // 0' 2>/dev/null || echo '0')
            echo "   Stats: $TOTAL opportunities, $SOURCES sources enabled"
        fi
    else
        echo "   Discovery: ❌ UNHEALTHY (HTTP $DISCOVERY_STATUS)"
    fi
else
    echo "❌ OFFLINE (HTTP $BACKEND_STATUS)"
    if [ "$BACKEND_STATUS" = "000" ]; then
        echo "   💡 Service might be spinning up (cold start)"
        echo "   💡 Try again in 30-60 seconds"
    fi
fi

echo ""

# Check frontend
echo -n "Frontend Status: "
FRONTEND_STATUS=$(curl -s -o /dev/null -w "%{http_code}" "$FRONTEND_URL" 2>/dev/null)
if [ "$FRONTEND_STATUS" = "200" ]; then
    echo "✅ ONLINE (HTTP $FRONTEND_STATUS)"
    
    # Check if it's actually the React app (look for React in response)
    CONTENT=$(curl -s "$FRONTEND_URL" 2>/dev/null | head -20)
    if echo "$CONTENT" | grep -q "Opportunities Finder\|React\|root"; then
        echo "   App: ✅ React app loaded successfully"
    else
        echo "   App: ⚠️  Unexpected content"
    fi
else
    echo "❌ OFFLINE (HTTP $FRONTEND_STATUS)"
fi

echo ""

# Show deployment info
echo "📋 Deployment Information:"
echo "Repository: https://github.com/psehrawa/opportunities-finder"
echo "Blueprint: render.yaml"
echo ""

# Check if services exist using Render CLI
echo "🔧 Render CLI Status:"
if command -v render &> /dev/null; then
    echo "✅ Render CLI installed"
    
    # Try to list services (non-interactive)
    SERVICES=$(render services -o text 2>/dev/null || echo "Unable to fetch services")
    if [ "$SERVICES" != "Unable to fetch services" ]; then
        echo "📊 Services found in Render account"
    else
        echo "📊 Services: Use 'render services' to view (requires interactive mode)"
    fi
else
    echo "❌ Render CLI not installed"
    echo "   Install: npm install -g @render-oss/cli"
fi

echo ""
echo "🎯 Next Steps:"
if [ "$BACKEND_STATUS" != "200" ] || [ "$FRONTEND_STATUS" != "200" ]; then
    echo "• Deploy to Render using: https://render.com/deploy"
    echo "• Connect repository: https://github.com/psehrawa/opportunities-finder"
    echo "• Use Blueprint deployment (render.yaml detected)"
else
    echo "• ✅ Deployment successful!"
    echo "• 🌐 Visit your app: $FRONTEND_URL"
    echo "• 📊 Monitor in Render dashboard: https://dashboard.render.com"
fi

echo ""
echo "===================================="