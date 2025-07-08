package com.psehrawa.oppfinder.discovery.service.datasource;

import java.time.LocalDateTime;

/**
 * Health status of a data source
 */
public record HealthStatus(
    boolean isHealthy,
    String status,
    String message,
    LocalDateTime lastChecked
) {}