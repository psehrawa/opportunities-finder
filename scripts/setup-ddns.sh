#!/bin/bash

# Dynamic DNS Setup Script for Mac mini Home Server
set -e

echo "🌐 Setting up Dynamic DNS for Home Server..."

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Dynamic DNS Options
echo -e "${BLUE}Choose your Dynamic DNS provider:${NC}"
echo "1) DuckDNS (Free, recommended)"
echo "2) No-IP (Free with limitations)"
echo "3) Cloudflare (Free with domain)"
echo "4) Google Domains (If you own a domain)"
read -p "Enter choice (1-4): " DDNS_CHOICE

case $DDNS_CHOICE in
    1)
        echo -e "${YELLOW}Setting up DuckDNS...${NC}"
        read -p "Enter your DuckDNS subdomain (e.g., oppfinder): " DUCKDNS_SUBDOMAIN
        read -p "Enter your DuckDNS token: " DUCKDNS_TOKEN
        
        # Create DuckDNS update script
        cat > ~/scripts/update-duckdns.sh << EOF
#!/bin/bash
echo url="https://www.duckdns.org/update?domains=${DUCKDNS_SUBDOMAIN}&token=${DUCKDNS_TOKEN}&ip=" | curl -k -o ~/duckdns.log -K -
EOF
        chmod +x ~/scripts/update-duckdns.sh
        
        # Add to crontab
        (crontab -l 2>/dev/null; echo "*/5 * * * * ~/scripts/update-duckdns.sh >/dev/null 2>&1") | crontab -
        
        echo -e "${GREEN}✅ DuckDNS configured! Your URL will be: https://${DUCKDNS_SUBDOMAIN}.duckdns.org${NC}"
        ;;
        
    2)
        echo -e "${YELLOW}Setting up No-IP...${NC}"
        read -p "Enter your No-IP hostname: " NOIP_HOSTNAME
        read -p "Enter your No-IP username: " NOIP_USERNAME
        read -s -p "Enter your No-IP password: " NOIP_PASSWORD
        echo
        
        # Download and install No-IP client
        cd /tmp
        curl -O https://www.noip.com/client/mac/noip-duc-mac.tar.gz
        tar xzf noip-duc-mac.tar.gz
        cd noip-*
        make
        sudo make install
        
        # Configure No-IP
        sudo /usr/local/bin/noip2 -C -U $NOIP_USERNAME -P $NOIP_PASSWORD
        
        echo -e "${GREEN}✅ No-IP configured! Your URL will be: https://${NOIP_HOSTNAME}${NC}"
        ;;
        
    3)
        echo -e "${YELLOW}Setting up Cloudflare DDNS...${NC}"
        read -p "Enter your domain (e.g., oppfinder.com): " CF_DOMAIN
        read -p "Enter your subdomain (e.g., home): " CF_SUBDOMAIN
        read -p "Enter your Cloudflare email: " CF_EMAIL
        read -p "Enter your Cloudflare API key: " CF_API_KEY
        read -p "Enter your Cloudflare Zone ID: " CF_ZONE_ID
        
        # Create Cloudflare update script
        cat > ~/scripts/update-cloudflare.sh << EOF
#!/bin/bash
IP=\$(curl -s https://api.ipify.org)
RECORD_ID=\$(curl -s -X GET "https://api.cloudflare.com/client/v4/zones/${CF_ZONE_ID}/dns_records?name=${CF_SUBDOMAIN}.${CF_DOMAIN}" \\
     -H "X-Auth-Email: ${CF_EMAIL}" \\
     -H "X-Auth-Key: ${CF_API_KEY}" \\
     -H "Content-Type: application/json" | jq -r '.result[0].id')

curl -s -X PUT "https://api.cloudflare.com/client/v4/zones/${CF_ZONE_ID}/dns_records/\${RECORD_ID}" \\
     -H "X-Auth-Email: ${CF_EMAIL}" \\
     -H "X-Auth-Key: ${CF_API_KEY}" \\
     -H "Content-Type: application/json" \\
     --data '{"type":"A","name":"${CF_SUBDOMAIN}","content":"'\${IP}'","ttl":1,"proxied":false}'
EOF
        chmod +x ~/scripts/update-cloudflare.sh
        
        # Add to crontab
        (crontab -l 2>/dev/null; echo "*/5 * * * * ~/scripts/update-cloudflare.sh >/dev/null 2>&1") | crontab -
        
        echo -e "${GREEN}✅ Cloudflare DDNS configured! Your URL will be: https://${CF_SUBDOMAIN}.${CF_DOMAIN}${NC}"
        ;;
        
    4)
        echo -e "${YELLOW}For Google Domains, use their built-in Dynamic DNS feature${NC}"
        echo "1. Go to domains.google.com"
        echo "2. Select your domain"
        echo "3. Go to DNS → Dynamic DNS"
        echo "4. Create a dynamic DNS record"
        echo "5. Use the provided username/password below"
        ;;
esac

echo -e "\n${BLUE}📋 Next Steps:${NC}"
echo "1. Configure port forwarding on your router:"
echo "   - Forward port 80 → Mac mini IP:80"
echo "   - Forward port 443 → Mac mini IP:443"
echo "2. Note your Mac mini's local IP: $(ipconfig getifaddr en0)"
echo "3. Set up SSL certificates with Let's Encrypt"
echo ""
echo -e "${GREEN}✅ Dynamic DNS setup complete!${NC}"