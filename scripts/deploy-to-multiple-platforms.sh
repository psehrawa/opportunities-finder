#!/bin/bash

echo "🚀 Multi-Platform Deployment Guide"
echo "================================="
echo ""

# Build the frontend first
echo "📦 Building frontend..."
cd frontend
npm run build
cd ..

echo ""
echo "🌍 Available Deployment Platforms:"
echo ""

echo "1️⃣  VERCEL (Recommended)"
echo "   • Unlimited projects"
echo "   • Commands:"
echo "     vercel login"
echo "     cd frontend && vercel --prod"
echo "   • Result: https://oppfinder-frontend.vercel.app"
echo ""

echo "2️⃣  NETLIFY"
echo "   • Unlimited sites"
echo "   • Commands:"
echo "     npm install -g netlify-cli"
echo "     netlify login"
echo "     cd frontend && netlify deploy --prod --dir=build"
echo "   • Result: https://random-name.netlify.app"
echo ""

echo "3️⃣  GITHUB PAGES"
echo "   • Free with GitHub"
echo "   • Commands:"
echo "     npm install -g gh-pages"
echo "     cd frontend && npm run deploy"
echo "   • Result: https://psehrawa.github.io/opportunities-finder"
echo ""

echo "4️⃣  SURGE.SH"
echo "   • Instant static hosting"
echo "   • Commands:"
echo "     npm install -g surge"
echo "     cd frontend/build && surge"
echo "   • Result: https://random-name.surge.sh"
echo ""

echo "5️⃣  RAILWAY (Full-stack)"
echo "   • Backend + Database included"
echo "   • Commands:"
echo "     npm install -g @railway/cli"
echo "     railway login"
echo "     railway new"
echo "   • Result: https://oppfinder.up.railway.app"
echo ""

echo "🎯 RECOMMENDED: Start with Vercel for frontend"
echo "   Then add Railway for backend if needed"
echo ""

echo "⚡ Quick Deploy Options:"
echo "   A) Frontend-only: Vercel or Netlify"
echo "   B) Full-stack: Railway or Render"
echo "   C) Multiple apps: Vercel (unlimited) + Railway (3-5 apps)"
echo ""

echo "🔧 Choose your platform and run the commands above!"
echo "   Each platform supports multiple applications."
echo ""

# Ask user preference
echo "Which platform would you like to try first?"
echo "1) Vercel"
echo "2) Netlify" 
echo "3) GitHub Pages"
echo "4) Surge.sh"
echo "5) Railway"
echo ""
echo "Run this script with your choice, or follow the commands above manually."