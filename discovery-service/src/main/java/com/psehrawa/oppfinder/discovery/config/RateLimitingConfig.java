package com.psehrawa.oppfinder.discovery.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
import io.github.bucket4j.Refill;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
@Component
public class RateLimitingConfig {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    /**
     * Get rate limit bucket for a specific IP address
     */
    public Bucket resolveBucket(String key) {
        return buckets.computeIfAbsent(key, k -> createNewBucket());
    }

    /**
     * Create a new bucket with rate limits
     * Default: 100 requests per minute
     */
    private Bucket createNewBucket() {
        Bandwidth limit = Bandwidth.classic(
            100, // capacity
            Refill.intervally(100, Duration.ofMinutes(1)) // refill 100 tokens every minute
        );
        return Bucket4j.builder()
            .addLimit(limit)
            .build();
    }

    /**
     * Create a bucket for premium users with higher limits
     * Premium: 1000 requests per minute
     */
    public Bucket createPremiumBucket() {
        Bandwidth limit = Bandwidth.classic(
            1000, // capacity
            Refill.intervally(1000, Duration.ofMinutes(1)) // refill 1000 tokens every minute
        );
        return Bucket4j.builder()
            .addLimit(limit)
            .build();
    }

    /**
     * Create a bucket for API endpoints with custom limits
     */
    public Bucket createApiEndpointBucket(int requestsPerMinute) {
        Bandwidth limit = Bandwidth.classic(
            requestsPerMinute,
            Refill.intervally(requestsPerMinute, Duration.ofMinutes(1))
        );
        return Bucket4j.builder()
            .addLimit(limit)
            .build();
    }
}