package com.psehrawa.oppfinder.discovery.service.scoring;

import com.psehrawa.oppfinder.common.dto.OpportunityDto;
import com.psehrawa.oppfinder.common.enums.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OpportunityScoringServiceTest {

    @Mock
    private ScoringConfigProperties scoringConfig;

    @InjectMocks
    private OpportunityScoringService scoringService;

    private ScoringConfigProperties.ScoringWeights weights;

    @BeforeEach
    void setUp() {
        weights = new ScoringConfigProperties.ScoringWeights();
        weights.setFundingStage(0.25);
        weights.setCompanySize(0.20);
        weights.setIndustryTrend(0.20);
        weights.setSocialSignals(0.15);
        weights.setRecency(0.10);
        weights.setDataSourceReliability(0.10);
    }
    
    private void setupScoringConfig() {
        when(scoringConfig.getWeights()).thenReturn(weights);
    }

    @Test
    void calculateOpportunityScore_HighQualityOpportunity_ShouldReturnHighScore() {
        // Given
        setupScoringConfig();
        Map<String, String> metadata = new HashMap<>();
        metadata.put("stars", "5000");
        metadata.put("forks", "1000");
        metadata.put("pushed_at", LocalDateTime.now().minusHours(1).toString());
        metadata.put("open_issues", "5");

        OpportunityDto opportunity = OpportunityDto.builder()
            .title("High Quality Fintech Startup")
            .source(DataSource.GITHUB)
            .type(OpportunityType.STARTUP_FUNDING)
            .fundingStage(FundingStage.SERIES_A)
            .companySize(CompanySize.STARTUP)
            .industry(Industry.FINTECH)
            .country(Country.US)
            .discoveredAt(LocalDateTime.now())
            .confidenceScore(BigDecimal.valueOf(90.0))
            .metadata(metadata)
            .build();

        // When
        BigDecimal score = scoringService.calculateOpportunityScore(opportunity);

        // Then
        assertThat(score).isGreaterThan(BigDecimal.valueOf(70.0));
        assertThat(score).isLessThanOrEqualTo(BigDecimal.valueOf(100.0));
    }

    @Test
    void calculateOpportunityScore_LowQualityOpportunity_ShouldReturnLowScore() {
        // Given
        setupScoringConfig();
        OpportunityDto opportunity = OpportunityDto.builder()
            .title("Low Quality Opportunity")
            .source(DataSource.REDDIT)
            .type(OpportunityType.CONFERENCE_ANNOUNCEMENT)
            .fundingStage(FundingStage.UNKNOWN)
            .companySize(CompanySize.ENTERPRISE)
            .industry(Industry.GAMING)
            .country(Country.US)
            .discoveredAt(LocalDateTime.now().minusDays(30))
            .confidenceScore(BigDecimal.valueOf(30.0))
            .build();

        // When
        BigDecimal score = scoringService.calculateOpportunityScore(opportunity);

        // Then
        assertThat(score).isLessThan(BigDecimal.valueOf(50.0));
        assertThat(score).isGreaterThanOrEqualTo(BigDecimal.valueOf(0.0));
    }

    @Test
    void calculateOpportunityScore_RecentDiscovery_ShouldGetRecencyBonus() {
        // Given
        setupScoringConfig();
        OpportunityDto recentOpportunity = OpportunityDto.builder()
            .title("Recent Discovery")
            .source(DataSource.GITHUB)
            .discoveredAt(LocalDateTime.now().minusMinutes(30))
            .confidenceScore(BigDecimal.valueOf(75.0))
            .build();

        OpportunityDto oldOpportunity = OpportunityDto.builder()
            .title("Old Discovery")
            .source(DataSource.GITHUB)
            .discoveredAt(LocalDateTime.now().minusDays(7))
            .confidenceScore(BigDecimal.valueOf(75.0))
            .build();

        // When
        BigDecimal recentScore = scoringService.calculateOpportunityScore(recentOpportunity);
        BigDecimal oldScore = scoringService.calculateOpportunityScore(oldOpportunity);

        // Then
        assertThat(recentScore).isGreaterThan(oldScore);
    }

    @Test
    void calculateOpportunityScore_AIIndustry_ShouldGetHighIndustryScore() {
        // Given
        setupScoringConfig();
        OpportunityDto aiOpportunity = OpportunityDto.builder()
            .title("AI Startup")
            .source(DataSource.GITHUB)
            .industry(Industry.ARTIFICIAL_INTELLIGENCE)
            .discoveredAt(LocalDateTime.now())
            .confidenceScore(BigDecimal.valueOf(75.0))
            .build();

        OpportunityDto genericOpportunity = OpportunityDto.builder()
            .title("Generic Startup")
            .source(DataSource.GITHUB)
            .industry(Industry.WEB_DEVELOPMENT)
            .discoveredAt(LocalDateTime.now())
            .confidenceScore(BigDecimal.valueOf(75.0))
            .build();

        // When
        BigDecimal aiScore = scoringService.calculateOpportunityScore(aiOpportunity);
        BigDecimal genericScore = scoringService.calculateOpportunityScore(genericOpportunity);

        // Then
        assertThat(aiScore).isGreaterThan(genericScore);
    }

    @Test
    void calculateEngagementPotential_StartupPreSeed_ShouldReturnHighPotential() {
        // Given
        OpportunityDto opportunity = OpportunityDto.builder()
            .companySize(CompanySize.STARTUP)
            .fundingStage(FundingStage.PRE_SEED)
            .discoveredAt(LocalDateTime.now().minusHours(12))
            .build();

        // When
        BigDecimal potential = scoringService.calculateEngagementPotential(opportunity);

        // Then
        assertThat(potential).isGreaterThan(BigDecimal.valueOf(70.0));
    }

    @Test
    void calculateEngagementPotential_EnterpriseCompany_ShouldReturnLowerPotential() {
        // Given
        OpportunityDto opportunity = OpportunityDto.builder()
            .companySize(CompanySize.ENTERPRISE)
            .fundingStage(FundingStage.IPO)
            .discoveredAt(LocalDateTime.now().minusWeeks(2))
            .build();

        // When
        BigDecimal potential = scoringService.calculateEngagementPotential(opportunity);

        // Then
        assertThat(potential).isLessThan(BigDecimal.valueOf(70.0));
    }

    @Test
    void calculateOpportunityScore_WithGitHubMetadata_ShouldConsiderSocialSignals() {
        // Given
        setupScoringConfig();
        Map<String, String> highSocialMetadata = new HashMap<>();
        highSocialMetadata.put("stars", "10000");
        highSocialMetadata.put("forks", "2000");
        highSocialMetadata.put("pushed_at", LocalDateTime.now().minusHours(2).toString());
        highSocialMetadata.put("open_issues", "0");

        Map<String, String> lowSocialMetadata = new HashMap<>();
        lowSocialMetadata.put("stars", "10");
        lowSocialMetadata.put("forks", "2");
        lowSocialMetadata.put("pushed_at", LocalDateTime.now().minusMonths(6).toString());
        lowSocialMetadata.put("open_issues", "500");

        OpportunityDto highSocialOpportunity = OpportunityDto.builder()
            .title("High Social Signals")
            .source(DataSource.GITHUB)
            .metadata(highSocialMetadata)
            .discoveredAt(LocalDateTime.now())
            .confidenceScore(BigDecimal.valueOf(75.0))
            .build();

        OpportunityDto lowSocialOpportunity = OpportunityDto.builder()
            .title("Low Social Signals")
            .source(DataSource.GITHUB)
            .metadata(lowSocialMetadata)
            .discoveredAt(LocalDateTime.now())
            .confidenceScore(BigDecimal.valueOf(75.0))
            .build();

        // When
        BigDecimal highSocialScore = scoringService.calculateOpportunityScore(highSocialOpportunity);
        BigDecimal lowSocialScore = scoringService.calculateOpportunityScore(lowSocialOpportunity);

        // Then
        assertThat(highSocialScore).isGreaterThan(lowSocialScore);
    }

    @Test
    void calculateOpportunityScore_NullValues_ShouldHandleGracefully() {
        // Given
        setupScoringConfig();
        OpportunityDto opportunity = OpportunityDto.builder()
            .title("Minimal Opportunity")
            .source(DataSource.GITHUB)
            .build();

        // When
        BigDecimal score = scoringService.calculateOpportunityScore(opportunity);

        // Then
        assertThat(score).isNotNull();
        assertThat(score).isGreaterThanOrEqualTo(BigDecimal.ZERO);
        assertThat(score).isLessThanOrEqualTo(BigDecimal.valueOf(100.0));
    }
}