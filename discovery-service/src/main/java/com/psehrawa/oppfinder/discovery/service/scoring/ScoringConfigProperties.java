package com.psehrawa.oppfinder.discovery.service.scoring;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for opportunity scoring
 */
@Configuration
@ConfigurationProperties(prefix = "oppfinder.discovery.scoring")
@Data
public class ScoringConfigProperties {

    private ScoringWeights weights = new ScoringWeights();
    private ScoringThresholds thresholds = new ScoringThresholds();
    private boolean enableMLScoring = false;
    private String mlModelPath;

    @Data
    public static class ScoringWeights {
        private double fundingStage = 0.25;
        private double companySize = 0.20;
        private double industryTrend = 0.20;
        private double socialSignals = 0.15;
        private double recency = 0.10;
        private double dataSourceReliability = 0.10;

        // Validate that weights sum to 1.0
        public void validate() {
            double sum = fundingStage + companySize + industryTrend + socialSignals + recency + dataSourceReliability;
            if (Math.abs(sum - 1.0) > 0.01) {
                throw new IllegalStateException("Scoring weights must sum to 1.0, current sum: " + sum);
            }
        }
    }

    @Data
    public static class ScoringThresholds {
        private double highQuality = 80.0;
        private double mediumQuality = 60.0;
        private double lowQuality = 40.0;
        private double minimumViable = 20.0;
    }
}