package com.psehrawa.oppfinder.discovery.service.scoring;

import com.psehrawa.oppfinder.common.dto.OpportunityDto;
import com.psehrawa.oppfinder.common.enums.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;

/**
 * Service for scoring opportunities based on multiple factors
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class OpportunityScoringService {

    private final ScoringConfigProperties scoringConfig;

    /**
     * Calculate comprehensive score for an opportunity
     */
    public BigDecimal calculateOpportunityScore(OpportunityDto opportunity) {
        log.debug("Calculating score for opportunity: {}", opportunity.getTitle());

        try {
            double totalScore = 0.0;
            ScoringConfigProperties.ScoringWeights weights = scoringConfig.getWeights();

            // 1. Funding Stage Score (0-100)
            double fundingScore = calculateFundingStageScore(opportunity.getFundingStage());
            totalScore += fundingScore * weights.getFundingStage();

            // 2. Company Size Score (0-100)
            double companySizeScore = calculateCompanySizeScore(opportunity.getCompanySize());
            totalScore += companySizeScore * weights.getCompanySize();

            // 3. Industry Trend Score (0-100)
            double industryScore = calculateIndustryTrendScore(opportunity.getIndustry());
            totalScore += industryScore * weights.getIndustryTrend();

            // 4. Social Signals Score (0-100)
            double socialScore = calculateSocialSignalsScore(opportunity);
            totalScore += socialScore * weights.getSocialSignals();

            // 5. Recency Score (0-100)
            double recencyScore = calculateRecencyScore(opportunity.getDiscoveredAt());
            totalScore += recencyScore * weights.getRecency();

            // 6. Data Source Reliability Score (0-100)
            double sourceScore = calculateDataSourceScore(opportunity.getSource());
            totalScore += sourceScore * weights.getDataSourceReliability();

            // 7. Confidence Score factor
            double confidenceFactor = opportunity.getConfidenceScore() != null ? 
                opportunity.getConfidenceScore().doubleValue() / 100.0 : 0.5;
            totalScore *= confidenceFactor;

            // Apply type-specific multipliers
            totalScore *= getTypeMultiplier(opportunity.getType());

            // Apply country-specific adjustments
            totalScore *= getCountryMultiplier(opportunity.getCountry());

            // Ensure score is within bounds [0, 100]
            double finalScore = Math.max(0.0, Math.min(100.0, totalScore));

            log.debug("Calculated score {} for opportunity: {} (funding: {}, company: {}, industry: {}, social: {}, recency: {})", 
                finalScore, opportunity.getTitle(), fundingScore, companySizeScore, industryScore, socialScore, recencyScore);

            return BigDecimal.valueOf(finalScore).setScale(2, RoundingMode.HALF_UP);

        } catch (Exception e) {
            log.error("Error calculating score for opportunity {}: {}", opportunity.getTitle(), e.getMessage());
            return BigDecimal.valueOf(50.0); // Default middle score
        }
    }

    private double calculateFundingStageScore(FundingStage fundingStage) {
        if (fundingStage == null) return 50.0;

        return switch (fundingStage) {
            case PRE_SEED -> 60.0;
            case SEED -> 75.0;
            case SERIES_A -> 85.0;
            case SERIES_B -> 90.0;
            case SERIES_C -> 95.0;
            case SERIES_D_PLUS -> 90.0;
            case IPO -> 70.0; // Less opportunity for early engagement
            case ACQUISITION -> 60.0; // Limited opportunity
            case PRIVATE_EQUITY -> 80.0;
            case DEBT_FINANCING -> 65.0;
            case GRANT -> 70.0;
            case CROWDFUNDING -> 55.0;
            case REVENUE_BASED -> 75.0;
            case UNKNOWN -> 50.0;
        };
    }

    private double calculateCompanySizeScore(CompanySize companySize) {
        if (companySize == null) return 50.0;

        return switch (companySize) {
            case STARTUP -> 85.0; // High opportunity for early engagement
            case SMALL -> 80.0;
            case MEDIUM -> 75.0;
            case LARGE -> 60.0;
            case ENTERPRISE -> 40.0; // Harder to engage with large enterprises
            case UNKNOWN -> 50.0;
        };
    }

    private double calculateIndustryTrendScore(Industry industry) {
        if (industry == null) return 50.0;

        // High-trend industries get higher scores
        return switch (industry) {
            case ARTIFICIAL_INTELLIGENCE -> 95.0;
            case FINTECH -> 90.0;
            case HEALTHTECH -> 88.0;
            case CYBERSECURITY -> 85.0;
            case BLOCKCHAIN -> 82.0;
            case CLOUD_COMPUTING -> 85.0;
            case IOT -> 80.0;
            case EDTECH -> 78.0;
            case DEVOPS -> 75.0;
            case DATA_ANALYTICS -> 83.0;
            case MOBILE_TECHNOLOGY -> 70.0;
            case WEB_DEVELOPMENT -> 65.0;
            case ENTERPRISE_SOFTWARE -> 75.0;
            case CONSUMER_SOFTWARE -> 70.0;
            case GAMING -> 68.0;
            case ECOMMERCE -> 72.0;
            case AUTONOMOUS_VEHICLES -> 88.0;
            case VIRTUAL_REALITY -> 80.0;
            case RENEWABLE_ENERGY -> 85.0;
            case BIOTECHNOLOGY -> 87.0;
            case ROBOTICS -> 84.0;
            case AEROSPACE -> 78.0;
            default -> 60.0;
        };
    }

    private double calculateSocialSignalsScore(OpportunityDto opportunity) {
        double score = 50.0; // Base score

        Map<String, String> metadata = opportunity.getMetadata();
        if (metadata == null) return score;

        try {
            // GitHub specific signals
            if (opportunity.getSource() == DataSource.GITHUB) {
                score = calculateGitHubSocialScore(metadata);
            }
            // Add other data source social scoring here
            
        } catch (Exception e) {
            log.warn("Error calculating social signals score: {}", e.getMessage());
        }

        return Math.max(0.0, Math.min(100.0, score));
    }

    private double calculateGitHubSocialScore(Map<String, String> metadata) {
        double score = 0.0;

        // Stars factor (0-40 points)
        String starsStr = metadata.get("stars");
        if (starsStr != null) {
            try {
                int stars = Integer.parseInt(starsStr);
                score += Math.min(40.0, (stars / 1000.0) * 20);
            } catch (NumberFormatException ignored) {}
        }

        // Forks factor (0-25 points)
        String forksStr = metadata.get("forks");
        if (forksStr != null) {
            try {
                int forks = Integer.parseInt(forksStr);
                score += Math.min(25.0, (forks / 200.0) * 25);
            } catch (NumberFormatException ignored) {}
        }

        // Recent activity factor (0-20 points)
        String pushedAtStr = metadata.get("pushed_at");
        if (pushedAtStr != null) {
            try {
                LocalDateTime pushedAt = LocalDateTime.parse(pushedAtStr);
                long daysSince = ChronoUnit.DAYS.between(pushedAt, LocalDateTime.now());
                score += Math.max(0, 20.0 - (daysSince / 30.0) * 20);
            } catch (Exception ignored) {}
        }

        // Issues factor (0-15 points) - fewer issues is better
        String issuesStr = metadata.get("open_issues");
        if (issuesStr != null) {
            try {
                int issues = Integer.parseInt(issuesStr);
                if (issues == 0) {
                    score += 15.0;
                } else {
                    score += Math.max(0, 15.0 - (issues / 100.0) * 15);
                }
            } catch (NumberFormatException ignored) {}
        }

        return score;
    }

    private double calculateRecencyScore(LocalDateTime discoveredAt) {
        if (discoveredAt == null) return 50.0;

        long hoursAgo = ChronoUnit.HOURS.between(discoveredAt, LocalDateTime.now());
        
        // Recent discoveries get higher scores
        if (hoursAgo <= 1) return 100.0;
        if (hoursAgo <= 6) return 90.0;
        if (hoursAgo <= 24) return 80.0;
        if (hoursAgo <= 72) return 70.0;
        if (hoursAgo <= 168) return 60.0; // 1 week
        
        // Gradual decrease after 1 week
        double weeksAgo = hoursAgo / 168.0;
        return Math.max(20.0, 60.0 - (weeksAgo * 10.0));
    }

    private double calculateDataSourceScore(DataSource source) {
        if (source == null) return 50.0;

        // Score based on data source reliability and relevance
        return switch (source) {
            case GITHUB -> 85.0;
            case CRUNCHBASE_PRO -> 95.0;
            case CRUNCHBASE_BASIC -> 80.0;
            case SEC_EDGAR -> 90.0;
            case LINKEDIN_API -> 75.0;
            case HACKER_NEWS -> 70.0;
            case PRODUCT_HUNT -> 75.0;
            case REDDIT -> 60.0;
            case TWITTER_API -> 65.0;
            case NEWS_API -> 70.0;
            case GOOGLE_TRENDS -> 60.0;
            case USPTO_PATENT -> 85.0;
            case PITCHBOOK -> 95.0;
            case OWLER -> 80.0;
            case ANGELLIST -> 85.0;
            case YOUTUBE_API -> 55.0;
            case BLIND -> 80.0;  // High reliability for insider information
            case QUORA -> 65.0;  // Moderate reliability for Q&A content
        };
    }

    private double getTypeMultiplier(OpportunityType type) {
        if (type == null) return 1.0;

        return switch (type) {
            case STARTUP_FUNDING -> 1.2;
            case ACQUISITION_TARGET -> 1.15;
            case TECHNOLOGY_TREND -> 1.1;
            case MARKET_EXPANSION -> 1.1;
            case PRODUCT_LAUNCH -> 1.05;
            case PARTNERSHIP -> 1.0;
            case JOB_POSTING_SIGNAL -> 0.9;
            case PATENT_FILING -> 0.95;
            case CONFERENCE_ANNOUNCEMENT -> 0.8;
            case REGULATORY_CHANGE -> 0.9;
            case COMPETITOR_ANALYSIS -> 0.85;
            case TECHNOLOGY_ADOPTION -> 1.0;
        };
    }

    private double getCountryMultiplier(Country country) {
        if (country == null) return 1.0;

        // Adjust based on market size and business opportunity
        return switch (country) {
            case US -> 1.1;
            case GB -> 1.05;
            case CA -> 1.02;
            case AU -> 1.0;
            case DE -> 1.05;
            case FR -> 1.03;
            case IN -> 1.08;
            case SG -> 1.06;
            case JP -> 1.04;
            case CN -> 1.08;
            case IL -> 1.1; // High tech concentration
            case IE -> 1.05;
            case NL -> 1.03;
            case SE -> 1.04;
            case CH -> 1.06;
            default -> 1.0;
        };
    }

    /**
     * Recalculate score for existing opportunity (e.g., when metadata changes)
     */
    public BigDecimal recalculateScore(OpportunityDto opportunity) {
        log.debug("Recalculating score for opportunity: {}", opportunity.getId());
        return calculateOpportunityScore(opportunity);
    }

    /**
     * Calculate engagement potential score
     */
    public BigDecimal calculateEngagementPotential(OpportunityDto opportunity) {
        double potential = 50.0; // Base potential

        // Higher potential for smaller companies
        if (opportunity.getCompanySize() == CompanySize.STARTUP) potential += 20.0;
        else if (opportunity.getCompanySize() == CompanySize.SMALL) potential += 15.0;
        else if (opportunity.getCompanySize() == CompanySize.MEDIUM) potential += 10.0;

        // Higher potential for certain funding stages
        if (opportunity.getFundingStage() == FundingStage.SEED) potential += 15.0;
        else if (opportunity.getFundingStage() == FundingStage.SERIES_A) potential += 10.0;
        else if (opportunity.getFundingStage() == FundingStage.PRE_SEED) potential += 20.0;

        // Recent discoveries have higher engagement potential
        if (opportunity.getDiscoveredAt() != null) {
            long hoursAgo = ChronoUnit.HOURS.between(opportunity.getDiscoveredAt(), LocalDateTime.now());
            if (hoursAgo <= 24) potential += 15.0;
            else if (hoursAgo <= 72) potential += 10.0;
            else if (hoursAgo <= 168) potential += 5.0;
        }

        return BigDecimal.valueOf(Math.min(100.0, potential)).setScale(2, RoundingMode.HALF_UP);
    }
}