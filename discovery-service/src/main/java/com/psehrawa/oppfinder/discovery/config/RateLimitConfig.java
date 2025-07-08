package com.psehrawa.oppfinder.discovery.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "oppfinder.discovery.data-sources")
@Data
public class RateLimitConfig {

    private DataSourceConfig github = new DataSourceConfig();
    private DataSourceConfig hackerNews = new DataSourceConfig();
    private DataSourceConfig reddit = new DataSourceConfig();
    private DataSourceConfig productHunt = new DataSourceConfig();
    private DataSourceConfig blind = new DataSourceConfig();
    private DataSourceConfig quora = new DataSourceConfig();

    @Data
    public static class DataSourceConfig {
        private boolean enabled = false;
        private String apiKey;
        private DataSourceRateLimit rateLimit = new DataSourceRateLimit();
    }

    @Data
    public static class DataSourceRateLimit {
        private int requestsPerHour = 100;
        private int burstCapacity = 10;
        private int retryDelaySeconds = 60;
        private int maxRetries = 3;
    }

    // Helper methods - now direct access since we have direct properties
    public DataSourceConfig getGithubConfig() {
        return github;
    }

    public DataSourceConfig getHackerNewsConfig() {
        return hackerNews;
    }

    public DataSourceConfig getRedditConfig() {
        return reddit;
    }

    public DataSourceConfig getProductHuntConfig() {
        return productHunt;
    }
    
    public DataSourceConfig getBlind() {
        return blind;
    }
    
    public DataSourceConfig getQuora() {
        return quora;
    }
}