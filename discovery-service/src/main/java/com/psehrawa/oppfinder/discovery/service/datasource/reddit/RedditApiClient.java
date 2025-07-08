package com.psehrawa.oppfinder.discovery.service.datasource.reddit;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;

@Component
@Slf4j
public class RedditApiClient {

    private final WebClient webClient;
    private final String userAgent;

    public RedditApiClient(@Value("${oppfinder.discovery.data-sources.reddit.user-agent:OpportunityFinder/1.0}") String userAgent) {
        this.userAgent = userAgent;
        this.webClient = WebClient.builder()
                .baseUrl("https://www.reddit.com")
                .defaultHeader(HttpHeaders.USER_AGENT, userAgent)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public Mono<RedditResponse> getSubredditPosts(String subreddit, String sort, int limit) {
        return webClient
                .get()
                .uri("/r/{subreddit}/{sort}.json?limit={limit}&raw_json=1", subreddit, sort, limit)
                .retrieve()
                .bodyToMono(RedditResponse.class)
                .timeout(Duration.ofSeconds(10))
                .doOnError(error -> log.error("Failed to fetch posts from r/{}: {}", subreddit, error.getMessage()))
                .onErrorResume(error -> {
                    log.warn("Error fetching from r/{}, returning empty response", subreddit);
                    return Mono.just(new RedditResponse());
                });
    }

    public Mono<RedditResponse> searchSubreddit(String subreddit, String query, String sort, int limit) {
        return webClient
                .get()
                .uri("/r/{subreddit}/search.json?q={query}&restrict_sr=1&sort={sort}&limit={limit}&raw_json=1", 
                     subreddit, query, sort, limit)
                .retrieve()
                .bodyToMono(RedditResponse.class)
                .timeout(Duration.ofSeconds(10))
                .doOnError(error -> log.error("Failed to search r/{} for '{}': {}", subreddit, query, error.getMessage()))
                .onErrorResume(error -> {
                    log.warn("Error searching r/{} for '{}', returning empty response", subreddit, query);
                    return Mono.just(new RedditResponse());
                });
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RedditResponse {
        @JsonProperty("data")
        private RedditData data = new RedditData();
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RedditData {
        @JsonProperty("children")
        private List<RedditChild> children = List.of();
        
        @JsonProperty("after")
        private String after;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RedditChild {
        @JsonProperty("data")
        private RedditPost data = new RedditPost();
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RedditPost {
        @JsonProperty("id")
        private String id;
        
        @JsonProperty("title")
        private String title;
        
        @JsonProperty("selftext")
        private String selftext;
        
        @JsonProperty("author")
        private String author;
        
        @JsonProperty("subreddit")
        private String subreddit;
        
        @JsonProperty("url")
        private String url;
        
        @JsonProperty("permalink")
        private String permalink;
        
        @JsonProperty("score")
        private Integer score;
        
        @JsonProperty("ups")
        private Integer ups;
        
        @JsonProperty("downs")
        private Integer downs;
        
        @JsonProperty("num_comments")
        private Integer numComments;
        
        @JsonProperty("created_utc")
        private Long createdUtc;
        
        @JsonProperty("over_18")
        private Boolean over18;
        
        @JsonProperty("stickied")
        private Boolean stickied;
        
        @JsonProperty("is_self")
        private Boolean isSelf;
        
        @JsonProperty("link_flair_text")
        private String linkFlairText;
        
        @JsonProperty("post_hint")
        private String postHint;
        
        // Additional fields for opportunity detection
        @JsonProperty("gilded")
        private Integer gilded;
        
        @JsonProperty("total_awards_received")
        private Integer totalAwardsReceived;
        
        @JsonProperty("upvote_ratio")
        private Double upvoteRatio;
    }
}