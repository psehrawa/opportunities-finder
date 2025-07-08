package com.psehrawa.oppfinder.discovery.service.datasource;

import com.psehrawa.oppfinder.common.dto.OpportunityDto;
import com.psehrawa.oppfinder.common.enums.Country;
import com.psehrawa.oppfinder.common.enums.DataSource;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Interface for all data source services.
 * Each data source (GitHub, Reddit, etc.) implements this interface.
 */
public interface DataSourceService {

    /**
     * Get the data source type this service handles
     */
    DataSource getDataSource();

    /**
     * Check if this data source is enabled and configured
     */
    boolean isEnabled();

    /**
     * Discover opportunities from this data source
     * 
     * @param countries List of countries to filter by (null for all)
     * @param since Only discover opportunities since this time (null for all time)
     * @param limit Maximum number of opportunities to discover (null for no limit)
     * @return CompletableFuture with list of discovered opportunities
     */
    CompletableFuture<List<OpportunityDto>> discoverOpportunities(
        List<Country> countries, 
        LocalDateTime since, 
        Integer limit);

    /**
     * Get the current rate limit status
     */
    RateLimitStatus getRateLimitStatus();

    /**
     * Validate the configuration for this data source
     */
    boolean validateConfiguration();

    /**
     * Get health status of this data source
     */
    HealthStatus getHealthStatus();
}