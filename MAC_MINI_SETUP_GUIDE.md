# Mac Mini Home Server Setup Guide

This guide will help you set up your Mac mini as a home server for the OpportunityFinder application with a public URL and real data access.

## Prerequisites

- Mac mini with macOS 10.14 or later
- Admin access to your home router
- A stable internet connection
- Basic understanding of terminal commands

## Step 1: Initial Setup

1. **Clone the repository** (if not already done):
   ```bash
   git clone <your-repo-url> ~/Documents/projects/opportunities-finder
   cd ~/Documents/projects/opportunities-finder
   ```

2. **Make scripts executable**:
   ```bash
   chmod +x scripts/*.sh
   ```

## Step 2: Set Up Dynamic DNS

This will give you a public URL that always points to your home network.

1. **Run the DDNS setup script**:
   ```bash
   ./scripts/setup-ddns.sh
   ```

2. **Choose your provider**:
   - **DuckDNS (Recommended - Free)**:
     - Go to https://www.duckdns.org
     - Sign in with GitHub/Google/Reddit
     - Create a subdomain (e.g., `oppfinder`)
     - Copy your token
     - Enter these when prompted by the script

   - **Cloudflare** (if you own a domain):
     - You'll need your API key and Zone ID
     - Create an A record for your subdomain

3. **Save your domain** for later:
   ```bash
   echo "oppfinder.duckdns.org" > ~/.ddns-domain
   ```

## Step 3: Configure Your Router

1. **Find your Mac mini's IP address**:
   ```bash
   ipconfig getifaddr en0
   ```
   Example: `192.168.1.100`

2. **Set a static IP** (recommended):
   - Go to System Preferences → Network
   - Select your connection → Advanced → TCP/IP
   - Configure IPv4: Using DHCP with manual address
   - Enter the IP address you noted

3. **Configure port forwarding** on your router:
   - Access your router admin panel (usually `192.168.1.1`)
   - Find Port Forwarding/Virtual Server settings
   - Add these rules:
     ```
     External Port: 80  → Internal IP: [Mac IP] → Internal Port: 80
     External Port: 443 → Internal IP: [Mac IP] → Internal Port: 443
     ```

## Step 4: Set Up the Mac Server

1. **Run the server setup script**:
   ```bash
   ./scripts/setup-mac-server.sh
   ```
   
   This will:
   - Install PostgreSQL, Redis, Nginx
   - Create production database
   - Configure Redis with password
   - Create production environment file

2. **Update environment variables**:
   ```bash
   nano ~/oppfinder-server/.env.production
   ```
   
   Add your API keys:
   ```
   GITHUB_API_TOKEN=your_github_token_here
   REDDIT_CLIENT_ID=your_reddit_client_id
   REDDIT_CLIENT_SECRET=your_reddit_client_secret
   ```

## Step 5: Build the Application

```bash
./gradlew clean build
```

## Step 6: Set Up Nginx with SSL

1. **Run the Nginx setup script**:
   ```bash
   ./scripts/setup-nginx-ssl.sh
   ```

2. **Enter your domain** when prompted (e.g., `oppfinder.duckdns.org`)

3. **Choose SSL option**:
   - Option 1: Let's Encrypt (recommended for public domains)
   - Option 2: Self-signed (for testing)

## Step 7: Configure Auto-Startup

1. **Run the auto-startup script**:
   ```bash
   ./scripts/setup-autostart.sh
   ```

   This will:
   - Create startup scripts
   - Set up LaunchAgent for auto-start
   - Configure health monitoring
   - Add automatic restart on failure

## Step 8: Deploy the Application

1. **Run the complete deployment**:
   ```bash
   ./scripts/deploy-mac-server.sh
   ```

   This orchestrates all the setup scripts and starts the application.

## Step 9: Security Configuration

1. **Configure macOS Firewall**:
   - System Preferences → Security & Privacy → Firewall
   - Turn on firewall
   - Click "Firewall Options"
   - Add Nginx and allow incoming connections

2. **Enable remote login** (optional, for SSH):
   - System Preferences → Sharing
   - Check "Remote Login"
   - Note the SSH command shown

## Step 10: Verify Everything Works

1. **Check local access**:
   ```bash
   curl http://localhost:8090/actuator/health
   ```

2. **Check DDNS update**:
   ```bash
   ~/scripts/update-duckdns.sh
   cat ~/duckdns.log  # Should show "OK"
   ```

3. **Test external access**:
   - From another network (mobile data)
   - Visit: https://oppfinder.duckdns.org/health

## Management Commands

### Service Control
```bash
# Start all services
launchctl start com.oppfinder.server

# Stop all services
launchctl stop com.oppfinder.server

# Check status
launchctl list | grep oppfinder

# View logs
tail -f ~/oppfinder-server/logs/oppfinder-app.log
```

### Database Management
```bash
# Connect to database
psql -U oppfinder -d oppfinder_production

# Backup database
pg_dump -U oppfinder oppfinder_production > backup.sql
```

### Monitoring
```bash
# Check all services
brew services list

# Monitor health
curl http://localhost:8090/actuator/health | jq
```

## Troubleshooting

### Port Forwarding Not Working
1. Check if ports are open:
   ```bash
   # From external network
   curl -I https://oppfinder.duckdns.org
   ```

2. Verify Mac firewall allows Nginx:
   ```bash
   sudo /usr/libexec/ApplicationFirewall/socketfilterfw --listapps
   ```

### SSL Certificate Issues
1. For Let's Encrypt renewal:
   ```bash
   sudo certbot renew --dry-run
   ```

2. Check certificate:
   ```bash
   openssl s_client -connect oppfinder.duckdns.org:443
   ```

### Application Won't Start
1. Check logs:
   ```bash
   tail -f ~/oppfinder-server/logs/oppfinder-app.log
   tail -f ~/oppfinder-server/logs/launchd-stderr.log
   ```

2. Verify database connection:
   ```bash
   psql -U oppfinder -d oppfinder_production -c "SELECT 1;"
   ```

### Performance Issues
1. Monitor resources:
   ```bash
   top -o cpu
   ```

2. Check disk space:
   ```bash
   df -h
   ```

## Backup Strategy

1. **Automated daily backups** are configured in the deployment
2. **Manual backup**:
   ```bash
   ~/oppfinder-server/scripts/backup-oppfinder.sh
   ```

3. **Restore from backup**:
   ```bash
   psql -U oppfinder oppfinder_production < backup.sql
   ```

## Security Best Practices

1. **Change default passwords** in `~/.env.production`
2. **Keep software updated**:
   ```bash
   brew update && brew upgrade
   ```
3. **Monitor access logs**:
   ```bash
   tail -f ~/oppfinder-server/logs/nginx-access.log
   ```
4. **Set up fail2ban** (optional) for brute force protection
5. **Consider VPN** for admin access

## Optional Enhancements

### 1. Set Up VPN Access
For secure remote administration:
```bash
brew install wireguard-tools
# Configure WireGuard for secure access
```

### 2. Add Monitoring Dashboard
```bash
# Install Grafana
brew install grafana
brew services start grafana
```

### 3. Configure Email Alerts
Edit `~/oppfinder-server/scripts/monitor-oppfinder.sh` to add email notifications.

## Regular Maintenance

### Weekly
- Check logs for errors
- Verify backups are running
- Review disk space

### Monthly
- Update software packages
- Review security logs
- Test restore procedure

### Quarterly
- Update SSL certificates (if not auto-renewing)
- Review and update API keys
- Performance tuning

## Support

If you encounter issues:
1. Check the logs first
2. Verify all services are running
3. Test each component individually
4. Check router/firewall settings

Your OpportunityFinder home server should now be accessible at your DDNS URL with automatic startup, monitoring, and SSL security!