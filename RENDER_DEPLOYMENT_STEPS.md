# Render Deployment Steps

## 🚀 Deploy to Render with Free Domain

Your code is now pushed to GitHub: https://github.com/psehrawa/opportunities-finder

### Step 1: Deploy via Render Dashboard

1. **Go to Render Dashboard**
   - Visit: https://render.com
   - Sign in with GitHub (if not already)

2. **Create New Blueprint**
   - Click "New +"
   - Select "Blueprint"
   - Choose "Connect a repository"
   - Select `psehrawa/opportunities-finder`
   - Click "Connect"

3. **Review Services**
   Render will detect the `render.yaml` file and show:
   - ✅ **oppfinder-backend** (Web Service)
   - ✅ **oppfinder-frontend** (Static Site)  
   - ✅ **oppfinder-db** (PostgreSQL Database)

4. **Deploy All Services**
   - Click "Apply" to create all services
   - Wait for deployment (5-10 minutes)

### Step 2: Get Your Free Domains

After deployment, you'll get these free .onrender.com domains:

- **Frontend**: `https://oppfinder-frontend.onrender.com`
- **Backend**: `https://oppfinder-backend.onrender.com`
- **Database**: Internal connection (not public)

### Step 3: Verify Deployment

1. **Check Backend Health**
   ```bash
   curl https://oppfinder-backend.onrender.com/actuator/health
   ```

2. **Check Discovery Service**
   ```bash
   curl https://oppfinder-backend.onrender.com/api/v1/discovery/health
   ```

3. **Visit Frontend**
   - Open: https://oppfinder-frontend.onrender.com
   - You should see the Opportunities Finder app!

### Step 4: Test the Application

1. **Trigger Discovery**
   - Click "Trigger Discovery" in the frontend
   - Or use API: 
   ```bash
   curl -X POST https://oppfinder-backend.onrender.com/api/v1/discovery/trigger?limitPerSource=10
   ```

2. **View Opportunities**
   - The frontend should show discovered opportunities
   - Click on tiles to see detailed modal views

## 🔧 Configuration Details

### Services Created:
- **oppfinder-backend**: Spring Boot app with PostgreSQL
- **oppfinder-frontend**: React app with production build
- **oppfinder-db**: PostgreSQL database (free tier)

### Environment Variables Set:
- ✅ `SPRING_PROFILES_ACTIVE=prod`
- ✅ `DATABASE_URL` (auto-configured)
- ✅ `CORS_ALLOWED_ORIGINS` (set to frontend domain)
- ✅ `REACT_APP_API_URL` (set to backend domain)

### Free Tier Limits:
- **Web Services**: 750 hours/month (sleeps after 15 min idle)
- **Database**: 90 days free, then $7/month
- **Static Sites**: Unlimited deployments
- **Bandwidth**: 100GB/month

## 🔄 Auto-Deployment

Every time you push to the `main` branch, Render will automatically:
1. Pull the latest code
2. Build the backend Docker image
3. Build the frontend React app  
4. Deploy both services
5. Run health checks

## 📊 Monitor Your App

### Using Render Dashboard:
- View logs in real-time
- Monitor resource usage
- Check deployment history
- Manage environment variables

### Using CLI:
```bash
# View logs
render logs -o text

# List services
render services -o text

# Check specific service
render services --output json | jq '.[] | select(.name=="oppfinder-backend")'
```

### Health Check Script:
```bash
# Use our custom health check
BACKEND_URL=https://oppfinder-backend.onrender.com \
FRONTEND_URL=https://oppfinder-frontend.onrender.com \
./scripts/health-check.sh
```

## 🎯 Next Steps

1. **Custom Domain** (Optional)
   - Add custom domain in Render dashboard
   - Update CORS settings if needed

2. **Environment Variables** (Optional)
   - Add `GITHUB_API_TOKEN` for higher rate limits
   - Add `REDDIT_API_KEY` for more Reddit data

3. **Monitoring** (Recommended)
   - Set up UptimeRobot for health monitoring
   - Configure Render's notification settings

4. **Database Backup** (Before 90 days)
   - Export data before free database expires
   - Consider upgrading to paid plan ($7/month)

## 🚨 Troubleshooting

### Service Won't Start:
- Check logs in Render dashboard
- Verify environment variables
- Ensure Dockerfile builds correctly

### Frontend Can't Reach Backend:
- Check CORS settings
- Verify API URL in frontend
- Test backend health endpoint directly

### Database Connection Issues:
- Check DATABASE_URL format
- Ensure SSL connection (required by Render)
- Verify database is running

Your app should now be live at:
**https://oppfinder-frontend.onrender.com** 🎉