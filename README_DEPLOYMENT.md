# Opportunities Finder - Deployment Ready

## 🚀 Quick Deploy

Your application is now ready for deployment! Here's the quickest way to get it live:

### 1. Push to GitHub
```bash
git add .
git commit -m "Add deployment configuration"
git push origin main
```

### 2. Deploy Backend (Railway - Easiest)
1. Go to [railway.app](https://railway.app)
2. Sign up with GitHub
3. Click "Deploy from GitHub repo"
4. Select your repository
5. Railway will auto-detect the Java app and deploy
6. Add PostgreSQL service
7. Copy the generated URL (e.g., `https://oppfinder.up.railway.app`)

### 3. Deploy Frontend (Vercel - Free)
1. Go to [vercel.com](https://vercel.com)
2. Sign up with GitHub
3. Click "Add New Project"
4. Select your repository
5. Set build settings:
   - Framework: React
   - Root Directory: `frontend`
   - Build Command: `npm run build`
   - Output Directory: `build`
6. Add environment variable:
   - `REACT_APP_API_URL` = `https://oppfinder.up.railway.app`
7. Deploy!

## 🔧 What's Included

### Deployment Files Created:
- `.github/workflows/deploy.yml` - GitHub Actions CI/CD
- `railway.json` - Railway deployment config
- `render.yaml` - Alternative Render deployment
- `Dockerfile.backend` - Docker container for backend
- `frontend/vercel.json` - Vercel deployment config
- `frontend/.env.production` - Production environment
- `scripts/health-check.sh` - Health monitoring script

### Code Changes:
- ✅ CORS configuration for production
- ✅ Environment variable handling
- ✅ Production-ready API configuration
- ✅ Health check endpoints
- ✅ Database migration settings

## 💰 Free Tier Options

### Railway (Recommended)
- **Cost**: $5 free credit/month
- **Includes**: PostgreSQL, Redis, 500 hours
- **Pros**: Easiest setup, auto-scaling
- **Cons**: Limited free hours

### Render
- **Cost**: 750 hours free/month
- **Includes**: PostgreSQL (90 days), Redis extra
- **Pros**: More free hours
- **Cons**: Services sleep after 15 minutes

### Vercel (Frontend)
- **Cost**: Free forever
- **Includes**: Unlimited deployments, 100GB bandwidth
- **Perfect for**: React frontend hosting

## 🔄 CI/CD Pipeline

The GitHub Actions workflow will:
1. ✅ Run backend tests with PostgreSQL
2. ✅ Run frontend tests and linting
3. ✅ Build and deploy backend to Railway
4. ✅ Build and deploy frontend to Vercel
5. ✅ Only deploy on main branch pushes

## 📊 Monitoring

### Health Checks
- Backend: `https://your-app.railway.app/actuator/health`
- Discovery: `https://your-app.railway.app/api/v1/discovery/health`
- Stats: `https://your-app.railway.app/api/v1/discovery/stats`

### Run Health Check
```bash
./scripts/health-check.sh
```

## 🔐 Environment Variables

### Required for Backend (Railway):
```env
# Auto-configured by Railway
DATABASE_URL=postgresql://...
REDIS_URL=redis://...

# Optional (for better data)
GITHUB_API_TOKEN=ghp_...
REDDIT_API_KEY=...
```

### Required for Frontend (Vercel):
```env
REACT_APP_API_URL=https://oppfinder.up.railway.app
```

## 🚨 Troubleshooting

### Backend Issues:
1. **Database connection fails**: Check DATABASE_URL format
2. **CORS errors**: Verify CORS_ALLOWED_ORIGINS includes your Vercel URL
3. **App won't start**: Check logs in Railway dashboard

### Frontend Issues:
1. **API calls fail**: Verify REACT_APP_API_URL is correct
2. **Build fails**: Check Node.js version (use 18)
3. **Blank page**: Check browser console for errors

### GitHub Actions Issues:
1. **Tests fail**: Check if services start correctly
2. **Deploy fails**: Verify all secrets are set correctly
3. **Timeout**: Increase timeout in workflow file

## 🎯 Next Steps After Deployment

1. **Custom Domain** (optional)
   - Add domain in Vercel dashboard
   - Add domain in Railway dashboard

2. **Monitoring** (recommended)
   - Set up UptimeRobot for health checks
   - Add error tracking (Sentry has free tier)

3. **Database Backups**
   - Railway: Automatic backups in paid plans
   - Consider scheduled backup scripts

4. **Performance Optimization**
   - Enable Redis caching
   - Add CDN for static assets
   - Monitor performance metrics

## 📞 Support

If you run into issues:
1. Check the `DEPLOYMENT_GUIDE.md` for detailed instructions
2. Review platform documentation (Railway, Vercel, Render)
3. Check GitHub Actions logs for deployment issues
4. Use the health check script to diagnose problems

Your app is ready to go live! 🚀