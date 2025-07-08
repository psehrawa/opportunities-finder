#!/bin/bash

echo "🚀 Quick Backend Deployment Guide"
echo "=================================="
echo ""
echo "Since Blueprint deployment didn't work, let's deploy step by step:"
echo ""

echo "📋 Manual Steps:"
echo "1. Go to: https://dashboard.render.com"
echo "2. Click 'New +' → 'Web Service'"
echo "3. Connect repository: psehrawa/opportunities-finder"
echo "4. Configure:"
echo "   - Name: oppfinder-backend"
echo "   - Runtime: Docker"
echo "   - Dockerfile: ./Dockerfile.backend"
echo "   - Plan: Free"
echo "   - Branch: main"
echo ""

echo "🔧 Environment Variables to Add:"
echo "SPRING_PROFILES_ACTIVE=prod"
echo "PORT=8090"
echo "JAVA_TOOL_OPTIONS=-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"
echo ""

echo "🗄️ Database Setup:"
echo "1. Create PostgreSQL service (Free plan)"
echo "2. Add DATABASE_URL to backend from database connection string"
echo ""

echo "🌐 URLs will be:"
echo "Backend: https://oppfinder-backend-[RANDOM].onrender.com"
echo "Health:  https://oppfinder-backend-[RANDOM].onrender.com/actuator/health"
echo ""

echo "⏱️ Deployment Time: 5-10 minutes"
echo ""

# Open the dashboard
echo "Opening Render dashboard..."
open "https://dashboard.render.com"

echo ""
echo "✅ Follow the manual steps above, then run:"
echo "   ./scripts/check-render-deployment.sh"
echo ""
echo "🆘 Need help? Check: MANUAL_DEPLOYMENT.md"