package com.psehrawa.oppfinder.discovery.controller;

import com.psehrawa.oppfinder.common.dto.OpportunityDto;
import com.psehrawa.oppfinder.common.enums.Country;
import com.psehrawa.oppfinder.discovery.service.DataSourceOrchestrator;
import com.psehrawa.oppfinder.discovery.service.DiscoverySchedulerService;
import com.psehrawa.oppfinder.discovery.service.datasource.HealthStatus;
import com.psehrawa.oppfinder.discovery.repository.OpportunityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * REST controller for discovery operations
 */
@RestController
@RequestMapping("/api/v1/discovery")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class DiscoveryController {

    private final DataSourceOrchestrator dataSourceOrchestrator;
    private final DiscoverySchedulerService schedulerService;
    private final OpportunityRepository opportunityRepository;

    @PostMapping("/trigger")
    public ResponseEntity<Map<String, Object>> triggerDiscovery(
            @RequestParam(required = false) List<Country> countries,
            @RequestParam(required = false) Integer hoursBack,
            @RequestParam(required = false) Integer limitPerSource) {
        
        log.info("Manual discovery triggered - countries: {}, hoursBack: {}, limit: {}", 
            countries, hoursBack, limitPerSource);

        LocalDateTime since = hoursBack != null ? 
            LocalDateTime.now().minusHours(hoursBack) : 
            LocalDateTime.now().minusHours(24);
        
        Integer limit = limitPerSource != null ? limitPerSource : 20;

        CompletableFuture<Integer> future = dataSourceOrchestrator
            .discoverOpportunitiesFromAllSources(countries, since, limit);

        try {
            Integer count = future.get(); // Wait for completion
            
            Map<String, Object> response = Map.of(
                "status", "completed",
                "opportunitiesDiscovered", count,
                "timestamp", LocalDateTime.now(),
                "parameters", Map.of(
                    "countries", countries != null ? countries : "all",
                    "since", since,
                    "limitPerSource", limit
                )
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error during manual discovery: {}", e.getMessage());
            
            Map<String, Object> response = Map.of(
                "status", "error",
                "message", e.getMessage(),
                "timestamp", LocalDateTime.now()
            );
            
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @PostMapping("/trigger/{source}")
    public ResponseEntity<Map<String, Object>> triggerSourceDiscovery(
            @PathVariable String source,
            @RequestParam(required = false) List<Country> countries,
            @RequestParam(required = false) Integer hoursBack,
            @RequestParam(required = false) Integer limit) {
        
        log.info("Manual discovery triggered for source: {} - countries: {}, hoursBack: {}, limit: {}", 
            source, countries, hoursBack, limit);

        LocalDateTime since = hoursBack != null ? 
            LocalDateTime.now().minusHours(hoursBack) : 
            LocalDateTime.now().minusHours(24);
        
        Integer actualLimit = limit != null ? limit : 20;

        CompletableFuture<List<OpportunityDto>> future = dataSourceOrchestrator
            .discoverFromSource(source, countries, since, actualLimit);

        try {
            List<OpportunityDto> opportunities = future.get();
            
            Map<String, Object> response = Map.of(
                "status", "completed",
                "source", source,
                "opportunitiesDiscovered", opportunities.size(),
                "opportunities", opportunities,
                "timestamp", LocalDateTime.now()
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error during source discovery for {}: {}", source, e.getMessage());
            
            Map<String, Object> response = Map.of(
                "status", "error",
                "source", source,
                "message", e.getMessage(),
                "timestamp", LocalDateTime.now()
            );
            
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @PostMapping("/scoring/trigger")
    public ResponseEntity<Map<String, String>> triggerScoring() {
        log.info("Manual scoring triggered");
        
        try {
            schedulerService.triggerScoring();
            
            return ResponseEntity.ok(Map.of(
                "status", "triggered",
                "message", "Scoring process started",
                "timestamp", LocalDateTime.now().toString()
            ));
            
        } catch (Exception e) {
            log.error("Error triggering scoring: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of(
                "status", "error",
                "message", e.getMessage()
            ));
        }
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getDiscoveryHealth() {
        log.debug("Getting discovery health status");
        
        try {
            Map<String, HealthStatus> dataSourcesHealth = dataSourceOrchestrator.getDataSourcesHealth();
            List<String> enabledSources = dataSourceOrchestrator.getEnabledDataSources();
            
            boolean allHealthy = dataSourcesHealth.values().stream()
                .allMatch(HealthStatus::isHealthy);
            
            Map<String, Object> response = Map.of(
                "status", allHealthy ? "UP" : "DEGRADED",
                "enabledSources", enabledSources,
                "dataSourcesHealth", dataSourcesHealth,
                "timestamp", LocalDateTime.now()
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error getting discovery health: {}", e.getMessage());
            
            Map<String, Object> response = Map.of(
                "status", "DOWN",
                "message", e.getMessage(),
                "timestamp", LocalDateTime.now()
            );
            
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @GetMapping("/sources")
    public ResponseEntity<Map<String, Object>> getDataSources() {
        log.debug("Getting data sources information");
        
        try {
            List<String> enabledSources = dataSourceOrchestrator.getEnabledDataSources();
            Map<String, HealthStatus> sourcesHealth = dataSourceOrchestrator.getDataSourcesHealth();
            
            Map<String, Object> response = Map.of(
                "enabledSources", enabledSources,
                "totalSources", sourcesHealth.size(),
                "healthySources", sourcesHealth.values().stream().mapToLong(s -> s.isHealthy() ? 1 : 0).sum(),
                "sourcesDetail", sourcesHealth,
                "timestamp", LocalDateTime.now()
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error getting data sources: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of(
                "error", e.getMessage(),
                "timestamp", LocalDateTime.now()
            ));
        }
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getDiscoveryStats() {
        log.debug("Getting discovery statistics");
        
        try {
            // Get counts by source
            List<Object[]> sourceCounts = opportunityRepository.countOpportunitiesBySource();
            Map<String, Long> sourceCountsMap = new HashMap<>();
            long totalCount = 0;
            
            for (Object[] row : sourceCounts) {
                String source = row[0].toString();
                Long count = (Long) row[1];
                sourceCountsMap.put(source, count);
                totalCount += count;
            }
            
            // Get counts by type
            List<Object[]> typeCounts = opportunityRepository.countOpportunitiesByType();
            Map<String, Long> typeCountsMap = new HashMap<>();
            
            for (Object[] row : typeCounts) {
                String type = row[0].toString();
                Long count = (Long) row[1];
                typeCountsMap.put(type, count);
            }
            
            Map<String, Object> stats = Map.of(
                "totalOpportunities", totalCount,
                "opportunitiesBySource", sourceCountsMap,
                "opportunitiesByType", typeCountsMap,
                "enabledSources", dataSourceOrchestrator.getEnabledDataSources(),
                "timestamp", LocalDateTime.now()
            );
            
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            log.error("Error getting discovery statistics: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of(
                "error", e.getMessage(),
                "timestamp", LocalDateTime.now()
            ));
        }
    }
}