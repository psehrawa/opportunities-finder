package com.psehrawa.oppfinder.discovery.service.datasource;

import com.psehrawa.oppfinder.common.dto.OpportunityDto;
import com.psehrawa.oppfinder.common.enums.Country;
import com.psehrawa.oppfinder.common.enums.DataSource;
import com.psehrawa.oppfinder.discovery.config.RateLimitConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Abstract base class for data source services providing common functionality
 */
@Slf4j
public abstract class AbstractDataSourceService implements DataSourceService {

    protected final WebClient webClient;
    protected final RedisTemplate<String, Object> redisTemplate;
    
    protected AbstractDataSourceService(WebClient webClient, RedisTemplate<String, Object> redisTemplate) {
        this.webClient = webClient;
        this.redisTemplate = redisTemplate;
    }

    private volatile HealthStatus lastHealthStatus;
    private volatile LocalDateTime lastHealthCheck;

    @Override
    public CompletableFuture<List<OpportunityDto>> discoverOpportunities(
            List<Country> countries, 
            LocalDateTime since, 
            Integer limit) {
        
        log.debug("Starting discovery for {} with countries: {}, since: {}, limit: {}", 
            getDataSource(), countries, since, limit);

        return CompletableFuture.supplyAsync(() -> {
            try {
                // Check rate limit before making requests
                if (isRateLimited()) {
                    log.warn("Rate limited for {}, skipping discovery", getDataSource());
                    return List.of();
                }

                // Perform the actual discovery
                List<OpportunityDto> opportunities = performDiscovery(countries, since, limit);
                
                // Update rate limit tracking
                updateRateLimitTracking();
                
                log.info("Discovered {} opportunities from {}", opportunities.size(), getDataSource());
                return opportunities;
                
            } catch (Exception e) {
                log.error("Error discovering opportunities from {}: {}", getDataSource(), e.getMessage(), e);
                updateHealthStatus(false, "Error during discovery: " + e.getMessage());
                return List.of();
            }
        });
    }

    @Override
    public RateLimitStatus getRateLimitStatus() {
        try {
            String key = "rate_limit:" + getDataSource().name();
            String requestsKey = key + ":requests";
            String resetKey = key + ":reset";

            Object requestsObj = redisTemplate.opsForValue().get(requestsKey);
            Object resetObj = redisTemplate.opsForValue().get(resetKey);
            
            Integer requestsRemaining = requestsObj instanceof Number ? ((Number) requestsObj).intValue() : null;
            Long resetTimestamp = resetObj instanceof Number ? ((Number) resetObj).longValue() : null;

            int limit = getRequestsPerHour();
            int remaining = requestsRemaining != null ? requestsRemaining : limit;
            LocalDateTime resetTime = resetTimestamp != null ? 
                LocalDateTime.now().plusSeconds(resetTimestamp) : 
                LocalDateTime.now().plusHours(1);

            return new RateLimitStatus(
                remaining,
                limit,
                resetTime,
                remaining <= 0
            );
        } catch (Exception e) {
            log.debug("Rate limit check failed (Redis not available): {}", e.getMessage());
            // Return unlimited rate limit status when Redis is not available
            int limit = getRequestsPerHour();
            return new RateLimitStatus(
                limit,
                limit,
                LocalDateTime.now().plusHours(1),
                false
            );
        }
    }

    @Override
    public HealthStatus getHealthStatus() {
        // Return cached status if checked recently (within 5 minutes)
        if (lastHealthStatus != null && lastHealthCheck != null && 
            lastHealthCheck.isAfter(LocalDateTime.now().minusMinutes(5))) {
            return lastHealthStatus;
        }

        // Perform health check
        return performHealthCheck();
    }

    /**
     * Subclasses implement this method to perform the actual discovery
     */
    protected abstract List<OpportunityDto> performDiscovery(
        List<Country> countries, 
        LocalDateTime since, 
        Integer limit);

    /**
     * Get the rate limit configuration for this data source
     */
    protected abstract RateLimitConfig.DataSourceRateLimit getRateLimitConfig();
    
    /**
     * Get requests per hour from configuration
     */
    protected int getRequestsPerHour() {
        RateLimitConfig.DataSourceRateLimit config = getRateLimitConfig();
        return config != null ? config.getRequestsPerHour() : 100; // Default limit
    }

    /**
     * Perform a health check for this data source
     */
    protected HealthStatus performHealthCheck() {
        try {
            boolean isHealthy = checkApiHealth();
            String status = isHealthy ? "UP" : "DOWN";
            String message = isHealthy ? "API is responding" : "API is not responding";
            
            HealthStatus healthStatus = new HealthStatus(isHealthy, status, message, LocalDateTime.now());
            updateHealthStatus(isHealthy, message);
            
            return healthStatus;
            
        } catch (Exception e) {
            log.error("Health check failed for {}: {}", getDataSource(), e.getMessage());
            HealthStatus healthStatus = new HealthStatus(false, "DOWN", "Health check failed: " + e.getMessage(), LocalDateTime.now());
            updateHealthStatus(false, "Health check failed: " + e.getMessage());
            return healthStatus;
        }
    }

    /**
     * Check if the API is healthy (override in subclasses for specific checks)
     */
    protected boolean checkApiHealth() {
        return validateConfiguration() && isEnabled();
    }

    /**
     * Check if we're currently rate limited
     */
    protected boolean isRateLimited() {
        RateLimitStatus status = getRateLimitStatus();
        return status.isLimited();
    }

    /**
     * Update rate limit tracking after making a request
     */
    protected void updateRateLimitTracking() {
        try {
            String key = "rate_limit:" + getDataSource().name();
            String requestsKey = key + ":requests";
            String resetKey = key + ":reset";

            // Decrement requests remaining
            Long remaining = redisTemplate.opsForValue().decrement(requestsKey);
            
            // Set initial values if not exists
            if (remaining == null || remaining.longValue() == -1) {
                int hourlyLimit = getRequestsPerHour();
                redisTemplate.opsForValue().set(requestsKey, hourlyLimit - 1);
                redisTemplate.expire(requestsKey, Duration.ofHours(1));
                
                redisTemplate.opsForValue().set(resetKey, 3600); // 1 hour in seconds
                redisTemplate.expire(resetKey, Duration.ofHours(1));
            }
        } catch (Exception e) {
            log.debug("Rate limit tracking update failed (Redis not available): {}", e.getMessage());
            // Skip rate limit tracking when Redis is not available
        }
    }

    /**
     * Update health status
     */
    protected void updateHealthStatus(boolean isHealthy, String message) {
        lastHealthStatus = new HealthStatus(isHealthy, 
            isHealthy ? "UP" : "DOWN", 
            message, 
            LocalDateTime.now());
        lastHealthCheck = LocalDateTime.now();
    }

    /**
     * Helper method to handle web client exceptions
     */
    protected void handleWebClientException(WebClientResponseException e) {
        log.error("API call failed for {}: Status {}, Body: {}", 
            getDataSource(), e.getStatusCode(), e.getResponseBodyAsString());
        
        if (e.getStatusCode().is4xxClientError()) {
            if (e.getStatusCode().value() == 429) {
                log.warn("Rate limit exceeded for {}", getDataSource());
            } else if (e.getStatusCode().value() == 401 || e.getStatusCode().value() == 403) {
                log.error("Authentication/Authorization failed for {}", getDataSource());
            }
        }
    }

    /**
     * Helper method to create opportunity with common fields
     */
    protected OpportunityDto createBaseOpportunity(String externalId, String title, String description) {
        return OpportunityDto.builder()
            .externalId(externalId)
            .title(title)
            .description(description)
            .source(getDataSource())
            .discoveredAt(LocalDateTime.now())
            .isActive(true)
            .build();
    }
}