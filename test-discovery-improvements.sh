#!/bin/bash

echo "Testing Discovery Improvements..."
echo "================================"

# Function to count opportunities
count_opportunities() {
    curl -s "http://localhost:8090/api/v1/opportunities?page=0&size=1" | jq -r '.totalElements'
}

# Get initial count
INITIAL_COUNT=$(count_opportunities)
echo "Initial opportunity count: $INITIAL_COUNT"

# Trigger discovery
echo -e "\nTriggering discovery (20 per source)..."
DISCOVERY_RESPONSE=$(curl -s -X POST "http://localhost:8090/api/v1/discovery/trigger?limitPerSource=20")
echo "Discovery response: $DISCOVERY_RESPONSE"

# Wait for processing
sleep 5

# Get new count
NEW_COUNT=$(count_opportunities)
echo -e "\nNew opportunity count: $NEW_COUNT"
echo "Opportunities added: $((NEW_COUNT - INITIAL_COUNT))"

# Show breakdown by source
echo -e "\n\nOpportunities by source:"
curl -s "http://localhost:8090/api/v1/discovery/stats" | jq -r '.bySource'

# Trigger discovery again to test duplicates
echo -e "\n\nTesting duplicate prevention..."
echo "Triggering discovery again..."
DISCOVERY_RESPONSE2=$(curl -s -X POST "http://localhost:8090/api/v1/discovery/trigger?limitPerSource=20")
echo "Discovery response: $DISCOVERY_RESPONSE2"

sleep 5

# Check if count increased (it shouldn't if duplicates are prevented)
FINAL_COUNT=$(count_opportunities)
echo -e "\nFinal opportunity count: $FINAL_COUNT"
echo "Additional opportunities (should be minimal): $((FINAL_COUNT - NEW_COUNT))"

# Show some sample opportunities
echo -e "\n\nSample opportunities by source:"
echo -e "\nReddit samples:"
curl -s "http://localhost:8090/api/v1/opportunities?page=0&size=100" | jq -r '.content[] | select(.source == "REDDIT") | .title' | head -5

echo -e "\nQuora samples:"
curl -s "http://localhost:8090/api/v1/opportunities?page=0&size=100" | jq -r '.content[] | select(.source == "QUORA") | .title' | head -5

echo -e "\nBlind samples:"
curl -s "http://localhost:8090/api/v1/opportunities?page=0&size=100" | jq -r '.content[] | select(.source == "BLIND") | .title' | head -5

echo -e "\n\nTest complete!"