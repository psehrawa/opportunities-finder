package com.psehrawa.oppfinder.discovery.service.datasource.github;

import com.psehrawa.oppfinder.common.dto.OpportunityDto;
import com.psehrawa.oppfinder.common.enums.*;
import com.psehrawa.oppfinder.discovery.config.RateLimitConfig;
import com.psehrawa.oppfinder.discovery.service.datasource.AbstractDataSourceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class GitHubDataSourceService extends AbstractDataSourceService {

    private final RateLimitConfig rateLimitConfig;
    private final WebClient githubWebClient;

    public GitHubDataSourceService(
            @Qualifier("githubWebClient") WebClient githubWebClient,
            RedisTemplate<String, Object> redisTemplate,
            RateLimitConfig rateLimitConfig) {
        super(githubWebClient, redisTemplate);
        this.githubWebClient = githubWebClient;
        this.rateLimitConfig = rateLimitConfig;
    }

    @Override
    public DataSource getDataSource() {
        return DataSource.GITHUB;
    }

    @Override
    public boolean isEnabled() {
        RateLimitConfig.DataSourceConfig config = rateLimitConfig.getGithub();
        return config != null && config.isEnabled();
    }

    @Override
    public boolean validateConfiguration() {
        RateLimitConfig.DataSourceConfig config = rateLimitConfig.getGithub();
        return config != null && config.isEnabled();
    }

    @Override
    protected RateLimitConfig.DataSourceRateLimit getRateLimitConfig() {
        RateLimitConfig.DataSourceConfig config = rateLimitConfig.getGithub();
        return config != null ? config.getRateLimit() : null;
    }

    @Override
    protected List<OpportunityDto> performDiscovery(List<Country> countries, LocalDateTime since, Integer limit) {
        List<OpportunityDto> opportunities = new ArrayList<>();

        try {
            // Discover trending repositories
            opportunities.addAll(discoverTrendingRepositories(countries, since, limit));
            
            // Discover new releases
            opportunities.addAll(discoverNewReleases(countries, since, limit));
            
            // Discover repositories with funding information
            opportunities.addAll(discoverFundingRepositories(countries, since, limit));

        } catch (Exception e) {
            log.error("Error during GitHub discovery: {}", e.getMessage(), e);
        }

        return opportunities;
    }

    private List<OpportunityDto> discoverTrendingRepositories(List<Country> countries, LocalDateTime since, Integer limit) {
        List<OpportunityDto> opportunities = new ArrayList<>();

        try {
            String query = buildTrendingQuery(since);
            GitHubSearchResponse response = searchRepositories(query, Math.min(limit != null ? limit : 30, 100));

            for (GitHubRepository repo : response.items()) {
                if (isRelevantRepository(repo)) {
                    OpportunityDto opportunity = mapRepositoryToOpportunity(repo, OpportunityType.TECHNOLOGY_TREND);
                    if (opportunity != null) {
                        opportunities.add(opportunity);
                    }
                }
            }

        } catch (WebClientResponseException e) {
            handleWebClientException(e);
        } catch (Exception e) {
            log.error("Error discovering trending repositories: {}", e.getMessage());
        }

        return opportunities;
    }

    private List<OpportunityDto> discoverNewReleases(List<Country> countries, LocalDateTime since, Integer limit) {
        List<OpportunityDto> opportunities = new ArrayList<>();

        try {
            // Search for repositories with recent releases
            String query = "created:>" + since.format(DateTimeFormatter.ISO_LOCAL_DATE) + " stars:>100";
            GitHubSearchResponse response = searchRepositories(query, Math.min(limit != null ? limit : 20, 50));

            for (GitHubRepository repo : response.items()) {
                if (hasRecentReleases(repo)) {
                    OpportunityDto opportunity = mapRepositoryToOpportunity(repo, OpportunityType.PRODUCT_LAUNCH);
                    if (opportunity != null) {
                        opportunities.add(opportunity);
                    }
                }
            }

        } catch (Exception e) {
            log.error("Error discovering new releases: {}", e.getMessage());
        }

        return opportunities;
    }

    private List<OpportunityDto> discoverFundingRepositories(List<Country> countries, LocalDateTime since, Integer limit) {
        List<OpportunityDto> opportunities = new ArrayList<>();

        try {
            // Search for repositories with funding/sponsor information
            String query = "funding sponsor created:>" + since.minusDays(7).format(DateTimeFormatter.ISO_LOCAL_DATE);
            GitHubSearchResponse response = searchRepositories(query, Math.min(limit != null ? limit : 10, 30));

            for (GitHubRepository repo : response.items()) {
                OpportunityDto opportunity = mapRepositoryToOpportunity(repo, OpportunityType.STARTUP_FUNDING);
                if (opportunity != null) {
                    opportunities.add(opportunity);
                }
            }

        } catch (Exception e) {
            log.error("Error discovering funding repositories: {}", e.getMessage());
        }

        return opportunities;
    }

    private GitHubSearchResponse searchRepositories(String query, int perPage) {
        try {
            String url = "/search/repositories?q=" + query + "&sort=stars&order=desc&per_page=" + perPage;
            
            WebClient.RequestHeadersSpec<?> request = githubWebClient.get().uri(url);
            
            // Add authorization if API key is configured
            RateLimitConfig.DataSourceConfig config = rateLimitConfig.getGithub();
            if (config != null && config.getApiKey() != null && !config.getApiKey().isEmpty()) {
                request = request.header("Authorization", "token " + config.getApiKey());
            }

            return request.retrieve()
                .bodyToMono(GitHubSearchResponse.class)
                .block();

        } catch (WebClientResponseException e) {
            handleWebClientException(e);
            throw e;
        }
    }

    private String buildTrendingQuery(LocalDateTime since) {
        String date = since != null ? since.format(DateTimeFormatter.ISO_LOCAL_DATE) : 
                     LocalDateTime.now().minusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE);
        
        return "created:>" + date + " stars:>50 language:java OR language:python OR language:javascript OR language:typescript";
    }

    private boolean isRelevantRepository(GitHubRepository repo) {
        if (repo.stargazersCount() < 50) return false;
        if (repo.archived()) return false;
        
        // Check for relevant keywords in description or topics
        String description = repo.description() != null ? repo.description().toLowerCase() : "";
        List<String> topics = repo.topics() != null ? repo.topics() : List.of();
        
        List<String> relevantKeywords = List.of(
            "startup", "business", "enterprise", "saas", "fintech", "api", 
            "platform", "framework", "tool", "service", "app", "application"
        );
        
        return relevantKeywords.stream().anyMatch(keyword -> 
            description.contains(keyword) || 
            topics.stream().anyMatch(topic -> topic.toLowerCase().contains(keyword))
        );
    }

    private boolean hasRecentReleases(GitHubRepository repo) {
        // For now, assume repositories with recent activity have releases
        // In a full implementation, we'd check the releases API
        return repo.pushedAt() != null && 
               repo.pushedAt().isAfter(LocalDateTime.now().minusDays(30));
    }

    private OpportunityDto mapRepositoryToOpportunity(GitHubRepository repo, OpportunityType type) {
        try {
            OpportunityDto opportunity = createBaseOpportunity(
                "github-" + repo.id(),
                repo.fullName(),
                repo.description() != null ? repo.description() : "GitHub repository"
            );

            opportunity.setType(type);
            opportunity.setUrl(repo.htmlUrl());
            opportunity.setCompanyName(repo.owner().login());
            opportunity.setIndustry(inferIndustryFromRepository(repo));
            opportunity.setCompanySize(inferCompanySizeFromRepository(repo));
            
            // Set initial score based on repository metrics
            BigDecimal score = calculateRepositoryScore(repo);
            opportunity.setScore(score);
            opportunity.setConfidenceScore(BigDecimal.valueOf(75.0));

            // Add metadata
            Map<String, String> metadata = new HashMap<>();
            metadata.put("stars", String.valueOf(repo.stargazersCount()));
            metadata.put("forks", String.valueOf(repo.forksCount()));
            metadata.put("language", repo.language());
            metadata.put("created_at", repo.createdAt().toString());
            metadata.put("pushed_at", repo.pushedAt().toString());
            metadata.put("open_issues", String.valueOf(repo.openIssuesCount()));
            if (repo.topics() != null && !repo.topics().isEmpty()) {
                metadata.put("topics", String.join(",", repo.topics()));
            }
            opportunity.setMetadata(metadata);

            // Add tags
            List<String> tags = new ArrayList<>();
            tags.add("github");
            tags.add("repository");
            if (repo.language() != null) {
                tags.add(repo.language().toLowerCase());
            }
            if (repo.topics() != null) {
                tags.addAll(repo.topics());
            }
            opportunity.setTags(tags);

            return opportunity;

        } catch (Exception e) {
            log.error("Error mapping repository to opportunity: {}", e.getMessage());
            return null;
        }
    }

    private Industry inferIndustryFromRepository(GitHubRepository repo) {
        String description = repo.description() != null ? repo.description().toLowerCase() : "";
        List<String> topics = repo.topics() != null ? 
            repo.topics().stream().map(String::toLowerCase).toList() : List.of();
        
        // Check for fintech keywords
        if (description.contains("finance") || description.contains("payment") || description.contains("banking") ||
            topics.stream().anyMatch(t -> t.contains("fintech") || t.contains("finance") || t.contains("payment"))) {
            return Industry.FINTECH;
        }
        
        // Check for AI/ML keywords
        if (description.contains("ai") || description.contains("machine learning") || description.contains("artificial intelligence") ||
            topics.stream().anyMatch(t -> t.contains("ai") || t.contains("ml") || t.contains("machine-learning"))) {
            return Industry.ARTIFICIAL_INTELLIGENCE;
        }
        
        // Check for DevOps keywords
        if (description.contains("devops") || description.contains("deployment") || description.contains("ci/cd") ||
            topics.stream().anyMatch(t -> t.contains("devops") || t.contains("deployment") || t.contains("docker"))) {
            return Industry.DEVOPS;
        }
        
        // Default based on language
        if ("java".equals(repo.language()) || "kotlin".equals(repo.language())) {
            return Industry.ENTERPRISE_SOFTWARE;
        } else if ("javascript".equals(repo.language()) || "typescript".equals(repo.language())) {
            return Industry.WEB_DEVELOPMENT;
        }
        
        return Industry.ENTERPRISE_SOFTWARE; // Default
    }

    private CompanySize inferCompanySizeFromRepository(GitHubRepository repo) {
        // Infer company size based on repository metrics and owner type
        if (repo.owner().type().equals("Organization")) {
            if (repo.stargazersCount() > 10000) {
                return CompanySize.ENTERPRISE;
            } else if (repo.stargazersCount() > 1000) {
                return CompanySize.LARGE;
            } else {
                return CompanySize.MEDIUM;
            }
        } else {
            // Individual owner
            if (repo.stargazersCount() > 5000) {
                return CompanySize.MEDIUM;
            } else {
                return CompanySize.STARTUP;
            }
        }
    }

    private BigDecimal calculateRepositoryScore(GitHubRepository repo) {
        // Calculate score based on multiple factors
        double score = 0.0;
        
        // Stars factor (0-40 points)
        double starsScore = Math.min(40.0, (repo.stargazersCount() / 1000.0) * 10);
        score += starsScore;
        
        // Forks factor (0-20 points)
        double forksScore = Math.min(20.0, (repo.forksCount() / 100.0) * 5);
        score += forksScore;
        
        // Recent activity factor (0-20 points)
        if (repo.pushedAt() != null) {
            long daysSinceUpdate = java.time.Duration.between(repo.pushedAt(), LocalDateTime.now()).toDays();
            double activityScore = Math.max(0, 20.0 - (daysSinceUpdate / 30.0) * 20);
            score += activityScore;
        }
        
        // Issues factor (0-10 points) - fewer open issues is better
        if (repo.openIssuesCount() > 0) {
            double issuesScore = Math.max(0, 10.0 - (repo.openIssuesCount() / 100.0) * 10);
            score += issuesScore;
        } else {
            score += 10.0;
        }
        
        // Language popularity factor (0-10 points)
        if (repo.language() != null) {
            String lang = repo.language().toLowerCase();
            if (List.of("javascript", "python", "java", "typescript", "go").contains(lang)) {
                score += 10.0;
            } else if (List.of("rust", "kotlin", "swift", "scala").contains(lang)) {
                score += 8.0;
            } else {
                score += 5.0;
            }
        }
        
        return BigDecimal.valueOf(Math.min(100.0, score));
    }

    @Override
    protected boolean checkApiHealth() {
        try {
            githubWebClient.get()
                .uri("/rate_limit")
                .retrieve()
                .bodyToMono(String.class)
                .block();
            return true;
        } catch (Exception e) {
            log.error("GitHub API health check failed: {}", e.getMessage());
            return false;
        }
    }
}