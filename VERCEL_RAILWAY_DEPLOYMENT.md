# Free Deployment: Vercel + Railway Combo

## 🎯 **Optimal Free Stack**

- **Frontend**: Vercel (unlimited projects)
- **Backend + Database**: Railway ($5/month credit = multiple apps)
- **Total Cost**: $0 (until Railway credit is exhausted)

## 🚀 **Quick Deploy Steps**

### Step 1: Deploy Frontend to Vercel

```bash
# Install Vercel CLI
npm install -g vercel

# Deploy frontend
cd frontend
vercel

# Follow prompts:
# ? Set up and deploy "frontend"? Y
# ? Which scope? (your account)
# ? Link to existing project? N
# ? Project name: oppfinder-frontend
# ? Directory: ./
# ? Override settings? N
```

### Step 2: Deploy Backend to Railway

```bash
# Install Railway CLI
npm install -g @railway/cli

# Login
railway login

# Deploy from root directory
cd ..
railway new

# Follow prompts:
# ? Project name: oppfinder-backend
# ? Deploy now? Y

# Add PostgreSQL
railway add postgresql

# Deploy
railway up
```

### Step 3: Connect Services

1. **Get Railway URLs**:
   ```bash
   railway status
   ```

2. **Update Vercel Environment**:
   ```bash
   cd frontend
   vercel env add REACT_APP_API_URL
   # Enter: https://oppfinder-backend.up.railway.app
   
   # Redeploy frontend
   vercel --prod
   ```

3. **Update Railway CORS**:
   ```bash
   railway variables set CORS_ALLOWED_ORIGINS=https://oppfinder-frontend.vercel.app
   ```

## 🌐 **Your Free Domains**

- **Frontend**: `https://oppfinder-frontend.vercel.app`
- **Backend**: `https://oppfinder-backend.up.railway.app`

## 💰 **Cost Breakdown**

| Service | Free Tier | Multiple Apps |
|---------|-----------|---------------|
| Vercel  | Unlimited | ✅ Unlimited |
| Railway | $5 credit | ✅ 3-5 apps  |

## 🔄 **Alternative: All Vercel**

Since Vercel supports serverless functions, you could deploy everything there:

```bash
# Create API routes in frontend/api/
# Deploy everything as one Vercel project
vercel
```

## 🏗️ **Other Platform Options**

### Netlify + Supabase
```bash
# Frontend on Netlify
netlify deploy --prod --dir=frontend/build

# Backend on Supabase (BaaS)
# Use Supabase for database, auth, edge functions
```

### Fly.io (All-in-One)
```bash
# Deploy full-stack app
fly deploy

# Get 3 free apps total
fly apps list
```

## ⚡ **Fastest Option: Vercel Only**

Let me set up a Vercel-only deployment right now:

```bash
cd frontend
vercel --prod
```

This gives you:
- ✅ Instant deployment
- ✅ Free domain
- ✅ Unlimited projects
- ✅ Global CDN
- ⚠️ Need to adapt backend for serverless

Choose your preferred option and I'll help deploy!