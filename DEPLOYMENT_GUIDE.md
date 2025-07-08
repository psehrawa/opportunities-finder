# Deployment Guide

This guide explains how to deploy the Opportunities Finder application using free hosting services.

## Architecture Overview

- **Frontend**: React app deployed to Vercel (free tier)
- **Backend**: Spring Boot app deployed to Railway or Render (free tier)
- **Database**: PostgreSQL provided by the hosting platform
- **Redis**: Redis instance for caching (optional in free tier)

## Prerequisites

1. GitHub account with the code pushed to a repository
2. Accounts on deployment platforms (see below)

## Option 1: Railway (Recommended)

Railway offers a generous free tier with PostgreSQL and Redis included.

### Setup Steps:

1. **Create Railway Account**
   - Go to [railway.app](https://railway.app)
   - Sign up with GitHub

2. **Deploy Backend**
   ```bash
   # Install Railway CLI
   npm install -g @railway/cli
   
   # Login
   railway login
   
   # Create new project
   railway new
   
   # Add PostgreSQL
   railway add postgresql
   
   # Add Redis (optional)
   railway add redis
   
   # Deploy
   railway up
   ```

3. **Get Environment Variables**
   - Go to Railway dashboard
   - Copy the `RAILWAY_TOKEN` for GitHub Actions
   - Note the backend URL (e.g., `https://oppfinder.up.railway.app`)

## Option 2: Render

Render also offers free PostgreSQL and web services.

### Setup Steps:

1. **Create Render Account**
   - Go to [render.com](https://render.com)
   - Sign up with GitHub

2. **Deploy with Blueprint**
   - Connect your GitHub repository
   - Render will auto-detect the `render.yaml` file
   - Click "Apply" to create all services

## Frontend Deployment (Vercel)

1. **Create Vercel Account**
   - Go to [vercel.com](https://vercel.com)
   - Sign up with GitHub

2. **Import Project**
   ```bash
   # Install Vercel CLI
   npm install -g vercel
   
   # In the frontend directory
   cd frontend
   vercel
   
   # Follow prompts to link project
   ```

3. **Set Environment Variables**
   - In Vercel dashboard, go to Settings > Environment Variables
   - Add: `REACT_APP_API_URL` = `https://your-backend-url.railway.app`

## GitHub Actions Setup

1. **Add Secrets to GitHub Repository**
   - Go to Settings > Secrets and variables > Actions
   - Add the following secrets:

   For Railway:
   - `RAILWAY_TOKEN`: Your Railway API token

   For Vercel:
   - `VERCEL_TOKEN`: Your Vercel API token
   - `VERCEL_ORG_ID`: Your Vercel organization ID
   - `VERCEL_PROJECT_ID`: Your Vercel project ID

2. **Get Vercel Tokens**
   ```bash
   # In frontend directory
   cd frontend
   vercel link
   
   # This creates .vercel/project.json with IDs
   cat .vercel/project.json
   ```

3. **Push to GitHub**
   ```bash
   git add .
   git commit -m "Add deployment configuration"
   git push origin main
   ```

## Environment Variables

### Backend (Railway/Render)

```env
# Database (auto-configured by platform)
DATABASE_URL=postgresql://...
DATABASE_USERNAME=...
DATABASE_PASSWORD=...

# Redis (auto-configured if added)
REDIS_URL=redis://...

# Application
SPRING_PROFILES_ACTIVE=prod
PORT=8090

# Optional API Keys
GITHUB_API_TOKEN=your_github_token
REDDIT_API_KEY=your_reddit_key
```

### Frontend (Vercel)

```env
REACT_APP_API_URL=https://your-backend-url.railway.app
REACT_APP_ENVIRONMENT=production
```

## Free Tier Limitations

### Railway
- $5 free credit per month
- 500 hours of runtime
- 100GB bandwidth
- PostgreSQL: 1GB storage
- Redis: 1GB memory

### Render
- 750 hours free per month
- PostgreSQL: 90 days free, then requires paid plan
- Services spin down after 15 minutes of inactivity

### Vercel
- Unlimited deployments
- 100GB bandwidth per month
- Serverless functions included

## Monitoring

1. **Backend Health Check**
   - `https://your-backend-url/actuator/health`

2. **Logs**
   - Railway: `railway logs`
   - Render: Dashboard > Logs
   - Vercel: Dashboard > Functions > Logs

## Cost Optimization Tips

1. **Use GitHub Actions caching** to reduce build times
2. **Enable auto-sleep** on Render to save hours
3. **Use Vercel for frontend** (generous free tier)
4. **Monitor usage** in platform dashboards

## Troubleshooting

### Backend won't start
- Check logs for database connection errors
- Ensure all environment variables are set
- Verify Java version compatibility

### Frontend can't reach backend
- Check CORS settings in Spring Boot
- Verify API URL in frontend environment
- Check backend health endpoint

### Database connection issues
- Verify DATABASE_URL format
- Check SSL requirements (some platforms require SSL)
- Ensure database is in same region as app

## Next Steps

1. Set up custom domain (optional)
2. Configure monitoring (e.g., UptimeRobot)
3. Set up error tracking (e.g., Sentry free tier)
4. Configure backup strategy for database

## Useful Commands

```bash
# Railway
railway logs          # View logs
railway status       # Check deployment status
railway vars         # List environment variables

# Vercel
vercel --prod        # Deploy to production
vercel env pull      # Download environment variables
vercel logs          # View function logs

# GitHub Actions
gh workflow view     # View workflow status
gh run list         # List recent runs
```