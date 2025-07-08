package com.psehrawa.oppfinder.discovery.service.datasource;

import java.time.LocalDateTime;

/**
 * Rate limit information for a data source
 */
public record RateLimitStatus(
    int requestsRemaining,
    int requestsLimit,
    LocalDateTime resetTime,
    boolean isLimited
) {}