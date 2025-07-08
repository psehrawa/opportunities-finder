package com.psehrawa.oppfinder.discovery.service.datasource.github;

import com.psehrawa.oppfinder.common.dto.OpportunityDto;
import com.psehrawa.oppfinder.common.enums.Country;
import com.psehrawa.oppfinder.common.enums.DataSource;
import com.psehrawa.oppfinder.common.enums.Industry;
import com.psehrawa.oppfinder.common.enums.OpportunityType;
import com.psehrawa.oppfinder.discovery.config.RateLimitConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GitHubDataSourceServiceTest {

    @Mock
    private WebClient githubWebClient;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private RateLimitConfig rateLimitConfig;

    @Mock
    private RateLimitConfig.DataSourceConfig dataSourceConfig;

    @Mock
    private RateLimitConfig.DataSourceRateLimit rateLimitConfig2;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    private GitHubDataSourceService gitHubService;

    @BeforeEach
    void setUp() {
        when(rateLimitConfig.getGithub()).thenReturn(dataSourceConfig);
        when(dataSourceConfig.isEnabled()).thenReturn(true);
        when(dataSourceConfig.getRateLimit()).thenReturn(rateLimitConfig2);
        when(rateLimitConfig2.getRequestsPerHour()).thenReturn(5000);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        gitHubService = new GitHubDataSourceService(githubWebClient, redisTemplate, rateLimitConfig);
    }

    @Test
    void getDataSource_ShouldReturnGitHub() {
        // When
        DataSource result = gitHubService.getDataSource();

        // Then
        assertThat(result).isEqualTo(DataSource.GITHUB);
    }

    @Test
    void isEnabled_WhenConfigured_ShouldReturnTrue() {
        // When
        boolean result = gitHubService.isEnabled();

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void isEnabled_WhenNotConfigured_ShouldReturnFalse() {
        // Given
        when(rateLimitConfig.getGithub()).thenReturn(null);

        // When
        boolean result = gitHubService.isEnabled();

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void validateConfiguration_WhenValidConfig_ShouldReturnTrue() {
        // When
        boolean result = gitHubService.validateConfiguration();

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void discoverOpportunities_WhenRateLimited_ShouldReturnEmptyList() {
        // Given
        when(valueOperations.get(anyString())).thenReturn(0); // No requests remaining

        // When
        CompletableFuture<List<OpportunityDto>> result = gitHubService.discoverOpportunities(
            List.of(Country.US), LocalDateTime.now().minusHours(24), 10);

        // Then
        assertThat(result.join()).isEmpty();
    }

    @Test
    void discoverOpportunities_WhenApiAvailable_ShouldReturnOpportunities() {
        // Given
        setupMockWebClient();
        when(valueOperations.get(anyString())).thenReturn(100); // Requests available

        GitHubSearchResponse mockResponse = new GitHubSearchResponse(
            1, false,
            List.of(createMockRepository())
        );

        when(responseSpec.bodyToMono(GitHubSearchResponse.class))
            .thenReturn(Mono.just(mockResponse));

        // When
        CompletableFuture<List<OpportunityDto>> result = gitHubService.discoverOpportunities(
            List.of(Country.US), LocalDateTime.now().minusHours(24), 10);

        // Then
        List<OpportunityDto> opportunities = result.join();
        assertThat(opportunities).hasSize(1);
        
        OpportunityDto opportunity = opportunities.get(0);
        assertThat(opportunity.getTitle()).isEqualTo("test-user/test-repo");
        assertThat(opportunity.getSource()).isEqualTo(DataSource.GITHUB);
        assertThat(opportunity.getType()).isEqualTo(OpportunityType.TECHNOLOGY_TREND);
        assertThat(opportunity.getIndustry()).isEqualTo(Industry.ARTIFICIAL_INTELLIGENCE);
    }

    @Test
    void getRateLimitStatus_ShouldReturnCurrentStatus() {
        // Given
        when(valueOperations.get("rate_limit:GITHUB:requests")).thenReturn(4500);
        when(valueOperations.get("rate_limit:GITHUB:reset")).thenReturn(3600L);

        // When
        var status = gitHubService.getRateLimitStatus();

        // Then
        assertThat(status.requestsRemaining()).isEqualTo(4500);
        assertThat(status.requestsLimit()).isEqualTo(5000);
        assertThat(status.isLimited()).isFalse();
    }

    @Test
    void getHealthStatus_WhenApiHealthy_ShouldReturnHealthy() {
        // Given
        setupMockWebClient();
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just("{}"));

        // When
        var healthStatus = gitHubService.getHealthStatus();

        // Then
        assertThat(healthStatus.isHealthy()).isTrue();
        assertThat(healthStatus.status()).isEqualTo("UP");
    }

    private void setupMockWebClient() {
        when(githubWebClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
    }

    private GitHubRepository createMockRepository() {
        GitHubOwner owner = new GitHubOwner("test-user", 123L, "avatar.jpg", "https://github.com/test-user", "User");
        
        return new GitHubRepository(
            12345L,
            "test-repo",
            "test-user/test-repo",
            owner,
            false,
            "https://github.com/test-user/test-repo",
            "AI-powered test repository",
            false,
            LocalDateTime.now().minusDays(30),
            LocalDateTime.now().minusHours(1),
            LocalDateTime.now().minusHours(1),
            "git://github.com/test-user/test-repo.git",
            "git@github.com:test-user/test-repo.git",
            "https://github.com/test-user/test-repo.git",
            "https://github.com/test-user/test-repo",
            null,
            1024,
            150,
            150,
            "Python",
            true,
            true,
            true,
            false,
            false,
            false,
            false,
            5,
            20,
            "main",
            95.5,
            List.of("ai", "machine-learning", "python")
        );
    }
}