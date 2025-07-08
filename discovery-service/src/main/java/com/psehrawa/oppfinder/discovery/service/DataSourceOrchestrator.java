package com.psehrawa.oppfinder.discovery.service;

import com.psehrawa.oppfinder.common.dto.OpportunityDto;
import com.psehrawa.oppfinder.common.enums.Country;
import com.psehrawa.oppfinder.discovery.service.datasource.DataSourceService;
import com.psehrawa.oppfinder.discovery.service.datasource.HealthStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Orchestrates discovery across multiple data sources
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DataSourceOrchestrator {

    private final List<DataSourceService> dataSourceServices;
    private final OpportunityService opportunityService;

    /**
     * Discover opportunities from all enabled data sources
     */
    public CompletableFuture<Integer> discoverOpportunitiesFromAllSources(
            List<Country> countries, 
            LocalDateTime since, 
            Integer limitPerSource) {
        
        log.info("Starting discovery from {} data sources", dataSourceServices.size());

        List<CompletableFuture<List<OpportunityDto>>> futures = dataSourceServices.stream()
            .filter(DataSourceService::isEnabled)
            .map(service -> {
                log.debug("Starting discovery from {}", service.getDataSource());
                return service.discoverOpportunities(countries, since, limitPerSource)
                    .whenComplete((opportunities, throwable) -> {
                        if (throwable != null) {
                            log.error("Error discovering from {}: {}", 
                                service.getDataSource(), throwable.getMessage());
                        } else {
                            log.info("Discovered {} opportunities from {}", 
                                opportunities.size(), service.getDataSource());
                        }
                    });
            })
            .toList();

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(v -> {
                int totalDiscovered = 0;
                for (CompletableFuture<List<OpportunityDto>> future : futures) {
                    try {
                        List<OpportunityDto> opportunities = future.get();
                        for (OpportunityDto opportunity : opportunities) {
                            // Save each opportunity (will handle duplicates)
                            opportunityService.saveOpportunity(opportunity);
                            totalDiscovered++;
                        }
                    } catch (Exception e) {
                        log.error("Error processing discovered opportunities: {}", e.getMessage());
                    }
                }
                
                log.info("Total opportunities discovered and saved: {}", totalDiscovered);
                return totalDiscovered;
            });
    }

    /**
     * Get health status of all data sources
     */
    public Map<String, HealthStatus> getDataSourcesHealth() {
        return dataSourceServices.stream()
            .collect(Collectors.toMap(
                service -> service.getDataSource().name(),
                DataSourceService::getHealthStatus
            ));
    }

    /**
     * Get enabled data sources
     */
    public List<String> getEnabledDataSources() {
        return dataSourceServices.stream()
            .filter(DataSourceService::isEnabled)
            .map(service -> service.getDataSource().name())
            .toList();
    }

    /**
     * Discover from a specific data source
     */
    public CompletableFuture<List<OpportunityDto>> discoverFromSource(
            String sourceName, 
            List<Country> countries, 
            LocalDateTime since, 
            Integer limit) {
        
        return dataSourceServices.stream()
            .filter(service -> service.getDataSource().name().equalsIgnoreCase(sourceName))
            .filter(DataSourceService::isEnabled)
            .findFirst()
            .map(service -> service.discoverOpportunities(countries, since, limit))
            .orElse(CompletableFuture.completedFuture(List.of()));
    }
}