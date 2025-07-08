package com.psehrawa.oppfinder.discovery.service.datasource.reddit;

import com.psehrawa.oppfinder.common.dto.OpportunityDto;
import com.psehrawa.oppfinder.common.enums.*;
import com.psehrawa.oppfinder.discovery.config.RateLimitConfig;
import com.psehrawa.oppfinder.discovery.service.datasource.AbstractDataSourceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Slf4j
public class RedditDataSourceService extends AbstractDataSourceService {

    private final RateLimitConfig rateLimitConfig;
    private final RedditApiClient redditApiClient;
    
    // High-value subreddits for startup opportunities
    private static final List<String> STARTUP_SUBREDDITS = List.of(
        "startups", "entrepreneur", "SaaS", "venturecapital", "investing",
        "business", "smallbusiness", "EntrepreneurRideAlong", "growmybusiness",
        "startupindia", "tech", "technology", "artificial", "MachineLearning",
        "fintech", "healthtech", "edtech", "cybersecurity", "blockchain",
        "webdev", "programming", "coding", "AppIdeas", "SideProject",
        "IndieBiz", "IMadeThis", "cofounder", "AngelInvestors",
        "startup_resources", "growthmarketing", "ProductManagement"
    );

    // Keywords that indicate opportunity potential
    private static final List<String> OPPORTUNITY_KEYWORDS = List.of(
        "funding", "investment", "investor", "venture capital", "seed round",
        "series a", "series b", "ipo", "acquisition", "exit", "valuation",
        "startup", "launch", "product launch", "beta launch", "new product",
        "co-founder", "cofounder", "partnership", "collaboration", "looking for",
        "hiring", "team", "CTO", "CEO", "VP", "director", "manager",
        "patent", "intellectual property", "innovation", "disruption",
        "market opportunity", "business model", "revenue model", "scale",
        "user growth", "customer acquisition", "traction", "metrics",
        "pivot", "expansion", "international", "global", "market entry"
    );

    private static final Pattern FUNDING_PATTERN = Pattern.compile(
        "(\\$[0-9]+(?:\\.[0-9]+)?[MmBbKk]?|[0-9]+(?:\\.[0-9]+)?\\s*(?:million|billion|thousand|M|B|K))\\s*" +
        "(?:funding|investment|raised|round|capital|valuation)", Pattern.CASE_INSENSITIVE
    );

    private static final Pattern COMPANY_PATTERN = Pattern.compile(
        "(?:^|\\s)([A-Z][a-zA-Z0-9]{2,}(?:\\s+[A-Z][a-zA-Z0-9]{2,}){0,2})\\s*" +
        "(?:raised|funding|launched|announces|acquires|partners)", Pattern.CASE_INSENSITIVE
    );

    public RedditDataSourceService(
            @Qualifier("redditWebClient") WebClient redditWebClient,
            @Qualifier("redisTemplate") RedisTemplate<String, Object> redisTemplate,
            RateLimitConfig rateLimitConfig,
            RedditApiClient redditApiClient) {
        super(redditWebClient, redisTemplate);
        this.rateLimitConfig = rateLimitConfig;
        this.redditApiClient = redditApiClient;
    }

    @Override
    public DataSource getDataSource() {
        return DataSource.REDDIT;
    }

    @Override
    public boolean isEnabled() {
        return rateLimitConfig.getRedditConfig().isEnabled();
    }

    @Override
    public boolean validateConfiguration() {
        return rateLimitConfig.getRedditConfig() != null && 
               rateLimitConfig.getRedditConfig().isEnabled();
    }

    @Override
    protected RateLimitConfig.DataSourceRateLimit getRateLimitConfig() {
        return rateLimitConfig.getRedditConfig().getRateLimit();
    }

    @Override
    protected List<OpportunityDto> performDiscovery(
            List<Country> countries, LocalDateTime since, Integer limit) {
        
        log.info("Starting Reddit discovery for {} opportunities", limit);
        
        try {
            List<OpportunityDto> opportunities = new ArrayList<>();
            int perSubreddit = Math.max(1, limit / STARTUP_SUBREDDITS.size());
            
            // Process subreddits in parallel using reactive streams
            List<Mono<List<OpportunityDto>>> subredditTasks = STARTUP_SUBREDDITS.stream()
                .map(subreddit -> processSubreddit(subreddit, perSubreddit, since))
                .collect(Collectors.toList());
            
            // Wait for all subreddits to complete
            Flux.fromIterable(subredditTasks)
                .flatMap(mono -> mono)
                .collectList()
                .block()
                .forEach(opportunities::addAll);
            
            // Sort by score and limit results
            opportunities.sort((a, b) -> b.getScore().compareTo(a.getScore()));
            
            List<OpportunityDto> result = opportunities.stream()
                .limit(limit)
                .collect(Collectors.toList());
            
            log.info("Reddit discovery completed: {} opportunities found", result.size());
            return result;
            
        } catch (Exception e) {
            log.error("Error during Reddit discovery", e);
            return List.of();
        }
    }

    private Mono<List<OpportunityDto>> processSubreddit(String subreddit, int limit, LocalDateTime since) {
        return redditApiClient.getSubredditPosts(subreddit, "hot", limit)
            .flatMap(response -> {
                List<OpportunityDto> opportunities = new ArrayList<>();
                
                for (RedditApiClient.RedditChild child : response.getData().getChildren()) {
                    RedditApiClient.RedditPost post = child.getData();
                    
                    // Skip if post is too old
                    if (since != null && isPostTooOld(post, since)) {
                        continue;
                    }
                    
                    // Check if post contains opportunity indicators
                    if (isOpportunityPost(post)) {
                        OpportunityDto opportunity = createOpportunityFromPost(post);
                        if (opportunity != null) {
                            opportunities.add(opportunity);
                        }
                    }
                }
                
                return Mono.just(opportunities);
            })
            .onErrorResume(error -> {
                log.warn("Error processing subreddit r/{}: {}", subreddit, error.getMessage());
                return Mono.just(List.of());
            });
    }

    private boolean isPostTooOld(RedditApiClient.RedditPost post, LocalDateTime since) {
        if (post.getCreatedUtc() == null) return false;
        
        LocalDateTime postTime = LocalDateTime.ofInstant(
            Instant.ofEpochSecond(post.getCreatedUtc()), 
            ZoneId.systemDefault()
        );
        
        return postTime.isBefore(since);
    }

    private boolean isOpportunityPost(RedditApiClient.RedditPost post) {
        String content = ((post.getTitle() != null ? post.getTitle() : "") + " " + 
                         (post.getSelftext() != null ? post.getSelftext() : "")).toLowerCase();
        
        // Check for opportunity keywords
        boolean hasKeywords = OPPORTUNITY_KEYWORDS.stream()
            .anyMatch(keyword -> content.contains(keyword.toLowerCase()));
        
        // Check for funding patterns
        boolean hasFunding = FUNDING_PATTERN.matcher(content).find();
        
        // Check for company mentions
        boolean hasCompany = COMPANY_PATTERN.matcher(content).find();
        
        // Must have high engagement (score > 10 or comments > 5)
        boolean hasEngagement = (post.getScore() != null && post.getScore() > 10) || 
                               (post.getNumComments() != null && post.getNumComments() > 5);
        
        // Skip stickied posts, over 18 content, and deleted posts
        boolean isValidPost = (post.getStickied() == null || !post.getStickied()) &&
                             (post.getOver18() == null || !post.getOver18()) &&
                             !isDeletedPost(post);
        
        return isValidPost && hasEngagement && (hasKeywords || hasFunding || hasCompany);
    }

    private boolean isDeletedPost(RedditApiClient.RedditPost post) {
        return "[removed]".equals(post.getSelftext()) || 
               "[deleted]".equals(post.getSelftext()) ||
               "[deleted]".equals(post.getAuthor());
    }

    private OpportunityDto createOpportunityFromPost(RedditApiClient.RedditPost post) {
        try {
            OpportunityDto opportunity = createBaseOpportunity(
                "reddit-" + post.getId(),
                post.getTitle(),
                post.getSelftext()
            );

            // Set Reddit-specific fields
            opportunity.setUrl("https://www.reddit.com" + post.getPermalink());
            opportunity.setSource(DataSource.REDDIT);
            opportunity.setStatus(OpportunityStatus.DISCOVERED);
            
            // Determine opportunity type based on content
            opportunity.setType(determineOpportunityType(post));
            
            // Extract company information
            opportunity.setCompanyName(extractCompanyName(post));
            opportunity.setCompanySize(CompanySize.STARTUP); // Default for Reddit posts
            
            // Infer industry from content
            opportunity.setIndustry(inferIndustry(post));
            
            // Set timing information
            if (post.getCreatedUtc() != null) {
                LocalDateTime postTime = LocalDateTime.ofInstant(
                    Instant.ofEpochSecond(post.getCreatedUtc()),
                    ZoneId.systemDefault()
                );
                opportunity.setDiscoveredAt(postTime);
                opportunity.setLastUpdated(postTime);
            }

            // Calculate and set scores
            BigDecimal score = calculateRedditScore(post);
            opportunity.setScore(score);
            opportunity.setConfidenceScore(calculateConfidenceScore(post));
            opportunity.setEngagementPotential(calculateEngagementPotential(post));

            // Add metadata
            Map<String, String> metadata = new HashMap<>();
            metadata.put("subreddit", "r/" + post.getSubreddit());
            metadata.put("author", post.getAuthor());
            metadata.put("score", String.valueOf(post.getScore()));
            metadata.put("comments", String.valueOf(post.getNumComments()));
            metadata.put("upvote_ratio", String.valueOf(post.getUpvoteRatio()));
            metadata.put("awards", String.valueOf(post.getTotalAwardsReceived()));
            metadata.put("post_type", post.getIsSelf() ? "text" : "link");
            if (post.getLinkFlairText() != null) {
                metadata.put("flair", post.getLinkFlairText());
            }
            opportunity.setMetadata(metadata);

            // Add tags
            List<String> tags = new ArrayList<>();
            tags.add("reddit");
            tags.add(post.getSubreddit());
            tags.addAll(extractTags(post));
            opportunity.setTags(tags);

            return opportunity;
            
        } catch (Exception e) {
            log.error("Error creating opportunity from Reddit post {}: {}", post.getId(), e.getMessage());
            return null;
        }
    }

    private OpportunityType determineOpportunityType(RedditApiClient.RedditPost post) {
        String content = ((post.getTitle() != null ? post.getTitle() : "") + " " + 
                         (post.getSelftext() != null ? post.getSelftext() : "")).toLowerCase();
        
        if (content.contains("funding") || content.contains("investment") || 
            content.contains("raised") || content.contains("round")) {
            return OpportunityType.STARTUP_FUNDING;
        } else if (content.contains("launch") || content.contains("released") || 
                   content.contains("announcing")) {
            return OpportunityType.PRODUCT_LAUNCH;
        } else if (content.contains("hiring") || content.contains("looking for") || 
                   content.contains("cofounder")) {
            return OpportunityType.JOB_POSTING_SIGNAL;
        } else if (content.contains("partnership") || content.contains("collaboration")) {
            return OpportunityType.PARTNERSHIP;
        } else if (content.contains("acquisition") || content.contains("acquired")) {
            return OpportunityType.ACQUISITION_TARGET;
        } else if (content.contains("trend") || content.contains("future") || 
                   content.contains("innovation")) {
            return OpportunityType.TECHNOLOGY_TREND;
        } else {
            return OpportunityType.STARTUP_FUNDING; // Default
        }
    }

    private String extractCompanyName(RedditApiClient.RedditPost post) {
        String content = (post.getTitle() != null ? post.getTitle() : "") + " " + 
                        (post.getSelftext() != null ? post.getSelftext() : "");
        
        Matcher matcher = COMPANY_PATTERN.matcher(content);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        
        // Fallback: look for capitalized words that might be company names
        Pattern fallbackPattern = Pattern.compile("\\b([A-Z][a-zA-Z]{2,}(?:\\s+[A-Z][a-zA-Z]{2,}){0,1})\\b");
        matcher = fallbackPattern.matcher(content);
        if (matcher.find()) {
            String candidate = matcher.group(1).trim();
            // Filter out common words
            if (!List.of("Reddit", "The", "This", "That", "We", "Our", "My").contains(candidate)) {
                return candidate;
            }
        }
        
        return "Unknown Company";
    }

    private Industry inferIndustry(RedditApiClient.RedditPost post) {
        String content = ((post.getTitle() != null ? post.getTitle() : "") + " " + 
                         (post.getSelftext() != null ? post.getSelftext() : "") + " " +
                         (post.getSubreddit() != null ? post.getSubreddit() : "")).toLowerCase();
        
        if (content.contains("ai") || content.contains("artificial") || 
            content.contains("machine learning") || content.contains("ml")) {
            return Industry.ARTIFICIAL_INTELLIGENCE;
        } else if (content.contains("fintech") || content.contains("finance") || 
                   content.contains("banking") || content.contains("payment")) {
            return Industry.FINTECH;
        } else if (content.contains("health") || content.contains("medical") || 
                   content.contains("healthcare")) {
            return Industry.HEALTHTECH;
        } else if (content.contains("education") || content.contains("edtech") || 
                   content.contains("learning")) {
            return Industry.EDTECH;
        } else if (content.contains("saas") || content.contains("software") || 
                   content.contains("app")) {
            return Industry.ENTERPRISE_SOFTWARE;
        } else if (content.contains("security") || content.contains("cyber")) {
            return Industry.CYBERSECURITY;
        } else if (content.contains("blockchain") || content.contains("crypto")) {
            return Industry.BLOCKCHAIN;
        } else if (content.contains("ecommerce") || content.contains("e-commerce") || 
                   content.contains("retail")) {
            return Industry.ECOMMERCE;
        } else {
            return Industry.ENTERPRISE_SOFTWARE; // Default
        }
    }

    private BigDecimal calculateRedditScore(RedditApiClient.RedditPost post) {
        double score = 0.0;
        
        // Base score from Reddit score (0-40 points)
        if (post.getScore() != null && post.getScore() > 0) {
            score += Math.min(40.0, Math.log(post.getScore()) * 5);
        }
        
        // Comments engagement (0-25 points)
        if (post.getNumComments() != null && post.getNumComments() > 0) {
            score += Math.min(25.0, Math.log(post.getNumComments()) * 3);
        }
        
        // Upvote ratio (0-15 points)
        if (post.getUpvoteRatio() != null) {
            score += post.getUpvoteRatio() * 15;
        }
        
        // Awards received (0-10 points)
        if (post.getTotalAwardsReceived() != null && post.getTotalAwardsReceived() > 0) {
            score += Math.min(10.0, post.getTotalAwardsReceived() * 2);
        }
        
        // Recency bonus (0-10 points)
        if (post.getCreatedUtc() != null) {
            long hoursOld = (System.currentTimeMillis() / 1000 - post.getCreatedUtc()) / 3600;
            if (hoursOld <= 24) {
                score += 10.0 * (24 - hoursOld) / 24;
            }
        }
        
        return BigDecimal.valueOf(Math.min(100.0, score));
    }

    private BigDecimal calculateConfidenceScore(RedditApiClient.RedditPost post) {
        double confidence = 50.0; // Base confidence
        
        // Higher confidence for posts with more engagement
        if (post.getScore() != null && post.getScore() > 50) {
            confidence += 20.0;
        }
        
        // Higher confidence for posts with many comments
        if (post.getNumComments() != null && post.getNumComments() > 20) {
            confidence += 15.0;
        }
        
        // Higher confidence for posts with awards
        if (post.getTotalAwardsReceived() != null && post.getTotalAwardsReceived() > 0) {
            confidence += 10.0;
        }
        
        // Higher confidence for certain subreddits
        if (post.getSubreddit() != null) {
            String subreddit = post.getSubreddit().toLowerCase();
            if (List.of("startups", "entrepreneur", "venturecapital").contains(subreddit)) {
                confidence += 15.0;
            }
        }
        
        return BigDecimal.valueOf(Math.min(100.0, confidence));
    }

    private BigDecimal calculateEngagementPotential(RedditApiClient.RedditPost post) {
        double potential = 50.0; // Base potential
        
        // Higher potential for recent posts
        if (post.getCreatedUtc() != null) {
            long hoursOld = (System.currentTimeMillis() / 1000 - post.getCreatedUtc()) / 3600;
            if (hoursOld <= 12) {
                potential += 20.0;
            }
        }
        
        // Higher potential for posts with good upvote ratio
        if (post.getUpvoteRatio() != null && post.getUpvoteRatio() > 0.8) {
            potential += 15.0;
        }
        
        // Higher potential for self posts (more likely to be genuine)
        if (post.getIsSelf() != null && post.getIsSelf()) {
            potential += 10.0;
        }
        
        // Higher potential for posts with author engagement
        if (post.getNumComments() != null && post.getScore() != null && 
            post.getNumComments() > 0 && post.getScore() > 0) {
            double ratio = (double) post.getNumComments() / post.getScore();
            if (ratio > 0.1) { // Good comment-to-score ratio
                potential += 15.0;
            }
        }
        
        return BigDecimal.valueOf(Math.min(100.0, potential));
    }

    private List<String> extractTags(RedditApiClient.RedditPost post) {
        List<String> tags = new ArrayList<>();
        
        String content = ((post.getTitle() != null ? post.getTitle() : "") + " " + 
                         (post.getSelftext() != null ? post.getSelftext() : "")).toLowerCase();
        
        // Add technology-related tags
        if (content.contains("ai") || content.contains("artificial intelligence")) {
            tags.add("ai");
        }
        if (content.contains("machine learning") || content.contains("ml")) {
            tags.add("machine-learning");
        }
        if (content.contains("blockchain") || content.contains("crypto")) {
            tags.add("blockchain");
        }
        if (content.contains("saas")) {
            tags.add("saas");
        }
        if (content.contains("mobile") || content.contains("app")) {
            tags.add("mobile");
        }
        if (content.contains("web") || content.contains("website")) {
            tags.add("web");
        }
        
        // Add funding-related tags
        if (content.contains("seed")) {
            tags.add("seed-funding");
        }
        if (content.contains("series a")) {
            tags.add("series-a");
        }
        if (content.contains("venture capital") || content.contains("vc")) {
            tags.add("venture-capital");
        }
        
        // Add flair as tag if available
        if (post.getLinkFlairText() != null) {
            tags.add(post.getLinkFlairText().toLowerCase().replace(" ", "-"));
        }
        
        return tags;
    }

    @Override
    protected boolean checkApiHealth() {
        try {
            // Test Reddit API health by fetching a single post from r/test
            return redditApiClient.getSubredditPosts("test", "hot", 1)
                .map(response -> response.getData() != null)
                .onErrorReturn(false)
                .block();
        } catch (Exception e) {
            log.error("Reddit API health check failed: {}", e.getMessage());
            return false;
        }
    }
}