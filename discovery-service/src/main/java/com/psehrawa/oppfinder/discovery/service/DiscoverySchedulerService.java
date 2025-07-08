package com.psehrawa.oppfinder.discovery.service;

import com.psehrawa.oppfinder.common.dto.OpportunityDto;
import com.psehrawa.oppfinder.discovery.service.scoring.OpportunityScoringService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduled service for automated discovery and scoring
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DiscoverySchedulerService {

    private final DataSourceOrchestrator dataSourceOrchestrator;
    private final OpportunityService opportunityService;
    private final OpportunityScoringService scoringService;

    /**
     * Scheduled discovery from all data sources
     * Runs every 6 hours by default
     */
    @Scheduled(cron = "${oppfinder.discovery.scheduling.discovery-interval:0 0 */6 * * *}")
    public void scheduledDiscovery() {
        log.info("Starting scheduled opportunity discovery");

        try {
            LocalDateTime since = LocalDateTime.now().minusHours(6);
            
            dataSourceOrchestrator.discoverOpportunitiesFromAllSources(
                null, // All countries
                since,
                50 // Limit per source
            ).thenAccept(count -> {
                log.info("Scheduled discovery completed. Total opportunities discovered: {}", count);
            }).exceptionally(throwable -> {
                log.error("Error during scheduled discovery: {}", throwable.getMessage(), throwable);
                return null;
            });

        } catch (Exception e) {
            log.error("Error in scheduled discovery: {}", e.getMessage(), e);
        }
    }

    /**
     * Scheduled scoring of unscored opportunities
     * Runs every hour
     */
    @Scheduled(fixedRate = 3600000) // 1 hour
    public void scheduledScoring() {
        log.info("Starting scheduled opportunity scoring");

        try {
            LocalDateTime since = LocalDateTime.now().minusHours(24);
            List<OpportunityDto> unscoredOpportunities = opportunityService.findOpportunitiesForScoring(since);

            log.info("Found {} opportunities for scoring", unscoredOpportunities.size());

            for (OpportunityDto opportunity : unscoredOpportunities) {
                try {
                    // Calculate comprehensive score
                    var score = scoringService.calculateOpportunityScore(opportunity);
                    
                    // Calculate engagement potential
                    var engagementPotential = scoringService.calculateEngagementPotential(opportunity);
                    
                    // Update opportunity with scores
                    opportunity.setScore(score);
                    opportunity.setEngagementPotential(engagementPotential);
                    
                    opportunityService.updateOpportunityScore(opportunity.getId(), score);
                    
                    log.debug("Updated score for opportunity {}: {}", opportunity.getId(), score);

                } catch (Exception e) {
                    log.error("Error scoring opportunity {}: {}", opportunity.getId(), e.getMessage());
                }
            }

            log.info("Completed scoring {} opportunities", unscoredOpportunities.size());

        } catch (Exception e) {
            log.error("Error in scheduled scoring: {}", e.getMessage(), e);
        }
    }

    /**
     * Scheduled cleanup of stale opportunities
     * Runs daily at 2 AM
     */
    @Scheduled(cron = "${oppfinder.discovery.scheduling.cleanup-interval:0 0 2 * * *}")
    public void scheduledCleanup() {
        log.info("Starting scheduled opportunity cleanup");

        try {
            LocalDateTime cutoff = LocalDateTime.now().minusDays(30); // 30 days old
            opportunityService.cleanupStaleOpportunities(cutoff);
            
            log.info("Completed scheduled cleanup");

        } catch (Exception e) {
            log.error("Error in scheduled cleanup: {}", e.getMessage(), e);
        }
    }

    /**
     * Health check for data sources
     * Runs every 15 minutes
     */
    @Scheduled(fixedRate = 900000) // 15 minutes
    public void dataSourceHealthCheck() {
        log.debug("Performing data source health check");

        try {
            var healthStatuses = dataSourceOrchestrator.getDataSourcesHealth();
            
            healthStatuses.forEach((source, status) -> {
                if (!status.isHealthy()) {
                    log.warn("Data source {} is unhealthy: {}", source, status.message());
                } else {
                    log.debug("Data source {} is healthy", source);
                }
            });

        } catch (Exception e) {
            log.error("Error during health check: {}", e.getMessage());
        }
    }

    /**
     * Manual trigger for discovery (for testing/admin purposes)
     */
    public void triggerDiscovery() {
        log.info("Manually triggered discovery");
        scheduledDiscovery();
    }

    /**
     * Manual trigger for scoring (for testing/admin purposes)
     */
    public void triggerScoring() {
        log.info("Manually triggered scoring");
        scheduledScoring();
    }
}