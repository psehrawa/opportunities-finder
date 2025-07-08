#!/bin/bash

# Nginx and SSL Setup for Mac mini
set -e

echo "🔒 Setting up Nginx with SSL..."

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Get domain information
read -p "Enter your domain/DDNS hostname (e.g., oppfinder.duckdns.org): " DOMAIN_NAME

# Stop nginx if running
brew services stop nginx 2>/dev/null || true

# Create Nginx configuration
echo -e "${YELLOW}Creating Nginx configuration...${NC}"
cat > /usr/local/etc/nginx/servers/oppfinder.conf << EOF
# HTTP server - redirect to HTTPS
server {
    listen 80;
    server_name $DOMAIN_NAME;
    
    # Allow Let's Encrypt verification
    location /.well-known/acme-challenge/ {
        root /usr/local/var/www;
    }
    
    # Redirect all other traffic to HTTPS
    location / {
        return 301 https://\$server_name\$request_uri;
    }
}

# HTTPS server
server {
    listen 443 ssl http2;
    server_name $DOMAIN_NAME;
    
    # SSL certificates (will be created by certbot)
    ssl_certificate /Users/$(whoami)/oppfinder-server/ssl/fullchain.pem;
    ssl_certificate_key /Users/$(whoami)/oppfinder-server/ssl/privkey.pem;
    
    # SSL configuration
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;
    ssl_prefer_server_ciphers on;
    ssl_session_cache shared:SSL:10m;
    ssl_session_timeout 10m;
    
    # Security headers
    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;
    add_header X-Frame-Options "SAMEORIGIN" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-XSS-Protection "1; mode=block" always;
    
    # Logging
    access_log /Users/$(whoami)/oppfinder-server/logs/nginx-access.log;
    error_log /Users/$(whoami)/oppfinder-server/logs/nginx-error.log;
    
    # API proxy
    location /api/ {
        proxy_pass http://localhost:8090;
        proxy_http_version 1.1;
        proxy_set_header Upgrade \$http_upgrade;
        proxy_set_header Connection 'upgrade';
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
        proxy_cache_bypass \$http_upgrade;
        
        # Timeouts
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
    }
    
    # Health check
    location /health {
        proxy_pass http://localhost:8090/actuator/health;
        access_log off;
    }
    
    # Frontend (if serving static files)
    location / {
        root /Users/$(whoami)/oppfinder-server/frontend;
        try_files \$uri \$uri/ /index.html;
    }
}
EOF

# Start nginx
echo -e "${YELLOW}Starting Nginx...${NC}"
brew services start nginx

# Get SSL certificate
echo -e "${YELLOW}Obtaining SSL certificate...${NC}"
echo -e "${BLUE}Choose SSL certificate method:${NC}"
echo "1) Let's Encrypt (recommended for public domains)"
echo "2) Self-signed certificate (for testing)"
read -p "Enter choice (1-2): " SSL_CHOICE

case $SSL_CHOICE in
    1)
        # Let's Encrypt
        echo -e "${YELLOW}Getting Let's Encrypt certificate...${NC}"
        sudo certbot certonly --webroot -w /usr/local/var/www -d $DOMAIN_NAME --email your-email@example.com --agree-tos --non-interactive
        
        # Copy certificates to our SSL directory
        sudo cp /etc/letsencrypt/live/$DOMAIN_NAME/fullchain.pem ~/oppfinder-server/ssl/
        sudo cp /etc/letsencrypt/live/$DOMAIN_NAME/privkey.pem ~/oppfinder-server/ssl/
        sudo chown $(whoami):staff ~/oppfinder-server/ssl/*
        
        # Set up auto-renewal
        (crontab -l 2>/dev/null; echo "0 2 * * * /usr/local/bin/certbot renew --quiet && cp /etc/letsencrypt/live/$DOMAIN_NAME/*.pem ~/oppfinder-server/ssl/") | crontab -
        ;;
        
    2)
        # Self-signed certificate
        echo -e "${YELLOW}Creating self-signed certificate...${NC}"
        openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
            -keyout ~/oppfinder-server/ssl/privkey.pem \
            -out ~/oppfinder-server/ssl/fullchain.pem \
            -subj "/C=US/ST=State/L=City/O=Organization/CN=$DOMAIN_NAME"
        ;;
esac

# Reload nginx with new configuration
brew services reload nginx

echo -e "${GREEN}✅ Nginx and SSL setup complete!${NC}"
echo ""
echo -e "${BLUE}📋 Your server URLs:${NC}"
echo "Public URL: https://$DOMAIN_NAME"
echo "API Endpoint: https://$DOMAIN_NAME/api/"
echo "Health Check: https://$DOMAIN_NAME/health"
echo ""
echo -e "${YELLOW}⚠️ Don't forget to:${NC}"
echo "1. Configure port forwarding on your router (80 and 443)"
echo "2. Allow nginx through macOS firewall"
echo "3. Test from outside your network"