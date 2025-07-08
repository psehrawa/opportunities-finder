# Manual Render Deployment Guide

Since the Blueprint deployment isn't working, let's deploy manually:

## 🚀 Deploy Backend First

### Step 1: Create Backend Service
1. Go to [dashboard.render.com](https://dashboard.render.com)
2. Click "New +" → "Web Service"
3. Connect GitHub and select `psehrawa/opportunities-finder`
4. Configure:
   - **Name**: `oppfinder-backend`
   - **Runtime**: Docker
   - **Dockerfile Path**: `./Dockerfile.backend`
   - **Plan**: Free
   - **Branch**: main

### Step 2: Add Environment Variables
Add these environment variables:
```
SPRING_PROFILES_ACTIVE=prod
PORT=8090
JAVA_TOOL_OPTIONS=-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0
```

### Step 3: Create Database
1. Click "New +" → "PostgreSQL"
2. Configure:
   - **Name**: `oppfinder-db`
   - **Plan**: Free
   - **Database Name**: oppfinder
   - **User**: oppfinder

### Step 4: Connect Database to Backend
1. Go to backend service settings
2. Add environment variable:
   - **Key**: `DATABASE_URL`
   - **Value**: Select "From Database" → `oppfinder-db` → Connection String

### Step 5: Deploy Backend
- Click "Manual Deploy" or wait for auto-deploy
- Check logs for any errors
- Health check: `/actuator/health`

## 🌐 Deploy Frontend

### Step 1: Create Frontend Service
1. Click "New +" → "Static Site"
2. Connect to same repository: `psehrawa/opportunities-finder`
3. Configure:
   - **Name**: `oppfinder-frontend`
   - **Branch**: main
   - **Build Command**: `cd frontend && npm install && npm run build`
   - **Publish Directory**: `frontend/build`

### Step 2: Add Environment Variables
```
REACT_APP_API_URL=https://[YOUR-BACKEND-URL].onrender.com
```
(Replace with actual backend URL from step above)

### Step 3: Update CORS
1. Go to backend service
2. Add/update environment variable:
   - **Key**: `CORS_ALLOWED_ORIGINS`
   - **Value**: `https://[YOUR-FRONTEND-URL].onrender.com`

## 🔧 Test Deployment

Once both services are deployed:

1. **Backend Test**:
   ```bash
   curl https://[BACKEND-URL]/actuator/health
   ```

2. **Frontend Test**:
   Open `https://[FRONTEND-URL]` in browser

3. **Full Test**:
   ```bash
   BACKEND_URL=https://[BACKEND-URL] \
   FRONTEND_URL=https://[FRONTEND-URL] \
   ./scripts/health-check.sh
   ```

## 🚨 Common Issues

### Backend Won't Start:
- Check logs in Render dashboard
- Ensure Dockerfile.backend is correct
- Verify Java 17 compatibility

### Frontend Build Fails:
- Check Node.js version (should be 18+)
- Verify package.json in frontend directory
- Check build logs for npm errors

### Database Connection:
- Ensure DATABASE_URL is properly set
- Check database service is running
- Verify SSL connection (required by Render)

## 📱 Alternative: Quick Deploy

If manual deployment is complex, try this simple approach:

1. **Backend Only First**:
   - Deploy just the backend service
   - Test with API calls
   - Use Postman or curl to test endpoints

2. **Local Frontend + Remote Backend**:
   - Run frontend locally: `cd frontend && npm start`
   - Set `REACT_APP_API_URL=https://[BACKEND-URL]`
   - Test integration before deploying frontend

This step-by-step approach will help identify exactly where the issue is occurring.