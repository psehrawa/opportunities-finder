# GitHub Secrets Configuration

This document lists all the GitHub secrets that need to be configured for CI/CD pipeline.

## Required Secrets

### 1. Docker Registry
- `DOCKER_USERNAME`: Your Docker Hub username
- `DOCKER_PASSWORD`: Your Docker Hub password or access token

### 2. Railway Deployment
- `RAILWAY_TOKEN_STAGING`: Railway API token for staging environment
- `RAILWAY_TOKEN_PRODUCTION`: Railway API token for production environment

### 3. Database
- `DATABASE_URL`: Production database connection string
- `DATABASE_USERNAME`: Production database username
- `DATABASE_PASSWORD`: Production database password

### 4. Redis
- `REDIS_HOST`: Redis server host
- `REDIS_PASSWORD`: Redis password
- `REDIS_SSL`: Whether to use SSL (true/false)

### 5. External APIs
- `GITHUB_API_TOKEN`: GitHub personal access token
- `REDDIT_CLIENT_ID`: Reddit application client ID
- `REDDIT_CLIENT_SECRET`: Reddit application client secret

### 6. Security
- `JWT_SECRET`: JWT signing secret (minimum 256 bits)

### 7. Monitoring
- `SENTRY_DSN`: Sentry DSN for error tracking

### 8. Notifications (Optional)
- `SLACK_WEBHOOK_URL`: Slack webhook for deployment notifications

## How to Add Secrets

1. Go to your GitHub repository
2. Click on Settings → Secrets and variables → Actions
3. Click "New repository secret"
4. Add each secret with the name and value

## Example Values for Testing

```bash
# Generate a secure JWT secret
openssl rand -base64 32

# Generate a secure password
openssl rand -base64 16
```

## Environment-Specific Secrets

For different environments, prefix the secret name:
- `STAGING_DATABASE_URL`
- `PROD_DATABASE_URL`

## Security Best Practices

1. Never commit secrets to the repository
2. Rotate secrets regularly
3. Use least privilege principle for API tokens
4. Enable secret scanning in GitHub
5. Audit secret usage regularly