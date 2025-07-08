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
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class RedditDataSourceService extends AbstractDataSourceService {

    private final RateLimitConfig rateLimitConfig;
    private final WebClient redditWebClient;
    
    private static final List<String> STARTUP_SUBREDDITS = List.of(
        // Core startup/business subreddits
        "startups", "entrepreneur", "SaaS", "venturecapital", 
        "Startup_Ideas", "EntrepreneurRideAlong", "growmybusiness",
        "smallbusiness", "Entrepreneur", "startup", "business",
        
        // Technology and development
        "technology", "programming", "webdev", "machinelearning",
        "artificial", "datascience", "learnprogramming", "coding",
        "devops", "aws", "kubernetes", "golang", "rust", "node",
        
        // Specific industries
        "fintech", "healthtech", "edtech", "legaltech", "biotech",
        "cryptocurrency", "BlockChain", "defi", "ethereum",
        
        // Investment and funding
        "angelinvestors", "seedfunding", "startupfunding", "investing",
        "SecurityAnalysis", "stocks", "wallstreetbets", "StockMarket",
        
        // Product and growth
        "ProductManagement", "growth", "growthhacking", "marketing",
        "digitalmarketing", "SEO", "analytics", "customerservice",
        
        // Remote work and hiring
        "remotework", "digitalnomad", "freelance", "forhire",
        "jobbit", "techjobs", "cscareerquestions", "remotejobs",
        
        // Innovation and trends
        "Futurology", "singularity", "tech", "gadgets", "innovation"
    );
    
    private static final List<String> OPPORTUNITY_KEYWORDS = List.of(
        // Funding signals
        "funding", "raised", "series", "seed", "investment", "venture capital",
        "angel investor", "pre-seed", "series a", "series b", "series c",
        "valuation", "unicorn", "cap table", "term sheet", "due diligence",
        
        // Product/launch signals  
        "launch", "launching", "beta", "mvp", "pre-launch", "early access",
        "product hunt", "show hn", "feedback", "beta testers", "pilot",
        "soft launch", "going live", "release", "v1", "alpha",
        
        // Growth signals
        "hiring", "scaling", "growth", "expanding", "opening office",
        "doubling team", "recruitment", "talent", "headcount", "remote team",
        "10x", "hockey stick", "traction", "pmf", "product market fit",
        
        // Business signals
        "startup", "founded", "building", "bootstrapped", "profitable",
        "revenue", "arr", "mrr", "burn rate", "runway", "break even",
        "acquisition", "acquired", "ipo", "exit", "merger",
        
        // Collaboration signals
        "looking for", "co-founder", "partner", "advisor", "mentor",
        "accelerator", "incubator", "yc", "techstars", "500 startups",
        
        // Innovation signals
        "disrupting", "revolutionizing", "game changer", "breakthrough",
        "first of its kind", "novel approach", "patented", "proprietary"
    );

    public RedditDataSourceService(
            @Qualifier("redditWebClient") WebClient redditWebClient,
            RedisTemplate<String, Object> redisTemplate,
            RateLimitConfig rateLimitConfig) {
        super(redditWebClient, redisTemplate);
        this.redditWebClient = redditWebClient;
        this.rateLimitConfig = rateLimitConfig;
    }

    @Override
    public DataSource getDataSource() {
        return DataSource.REDDIT;
    }

    @Override
    public boolean isEnabled() {
        RateLimitConfig.DataSourceConfig config = rateLimitConfig.getReddit();
        return config != null && config.isEnabled();
    }

    @Override
    public boolean validateConfiguration() {
        RateLimitConfig.DataSourceConfig config = rateLimitConfig.getReddit();
        return config != null && config.isEnabled();
    }

    @Override
    protected RateLimitConfig.DataSourceRateLimit getRateLimitConfig() {
        RateLimitConfig.DataSourceConfig config = rateLimitConfig.getReddit();
        return config != null ? config.getRateLimit() : null;
    }

    @Override
    protected List<OpportunityDto> performDiscovery(List<Country> countries, LocalDateTime since, Integer limit) {
        List<OpportunityDto> opportunities = new ArrayList<>();
        // Fetch more posts per subreddit but filter better
        int perSubredditLimit = 10; // Fixed amount per subreddit to ensure diversity

        for (String subreddit : STARTUP_SUBREDDITS) {
            try {
                List<OpportunityDto> subredditOpps = discoverFromSubreddit(subreddit, since, perSubredditLimit);
                opportunities.addAll(subredditOpps);
                
                // Don't break early - collect from all subreddits then filter
                if (opportunities.size() >= (limit != null ? limit : 50) * 3) {
                    break; // Only break if we have way too many
                }
                
                // Rate limiting between subreddit calls
                Thread.sleep(100);
            } catch (Exception e) {
                log.error("Error discovering from subreddit {}: {}", subreddit, e.getMessage());
            }
        }

        // Sort by score and return top results
        return opportunities.stream()
            .sorted((a, b) -> b.getScore().compareTo(a.getScore()))
            .limit(limit != null ? limit : 50)
            .collect(Collectors.toList());
    }

    private List<OpportunityDto> discoverFromSubreddit(String subreddit, LocalDateTime since, Integer limit) {
        List<OpportunityDto> opportunities = new ArrayList<>();

        try {
            // Search for hot posts
            RedditListingResponse hotPosts = fetchSubredditPosts(subreddit, "hot", limit);
            opportunities.addAll(extractOpportunities(hotPosts, OpportunityType.TECHNOLOGY_TREND));

            // Search for new posts
            RedditListingResponse newPosts = fetchSubredditPosts(subreddit, "new", limit);
            opportunities.addAll(extractOpportunities(newPosts, OpportunityType.STARTUP_FUNDING));

        } catch (Exception e) {
            log.error("Error fetching from r/{}: {}", subreddit, e.getMessage());
        }

        return opportunities.stream()
            .filter(opp -> isRelevantOpportunity(opp))
            .limit(limit != null ? limit : 10)
            .collect(Collectors.toList());
    }

    private RedditListingResponse fetchSubredditPosts(String subreddit, String sort, Integer limit) {
        try {
            String url = String.format("/r/%s/%s.json?limit=%d", subreddit, sort, Math.min(limit != null ? limit : 25, 100));
            
            return redditWebClient.get()
                .uri(url)
                .header("User-Agent", "OpportunityFinder/1.0")
                .retrieve()
                .bodyToMono(RedditListingResponse.class)
                .block();

        } catch (WebClientResponseException e) {
            handleWebClientException(e);
            throw e;
        }
    }

    private List<OpportunityDto> extractOpportunities(RedditListingResponse listing, OpportunityType defaultType) {
        if (listing == null || listing.data() == null || listing.data().children() == null) {
            return List.of();
        }

        return listing.data().children().stream()
            .map(child -> child.data())
            .filter(post -> post != null)
            .map(post -> mapRedditPostToOpportunity(post, defaultType))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    private OpportunityDto mapRedditPostToOpportunity(RedditPost post, OpportunityType type) {
        try {
            String fullText = post.title() + " " + (post.selftext() != null ? post.selftext() : "");
            
            // Use stable external ID to prevent duplicates
            OpportunityDto opportunity = createBaseOpportunity(
                "reddit-" + post.id(), // Reddit IDs are stable
                post.title(),
                post.selftext() != null && !post.selftext().isEmpty() ? 
                    post.selftext().substring(0, Math.min(post.selftext().length(), 1000)) : 
                    post.title()
            );

            opportunity.setType(determineOpportunityType(fullText, type));
            opportunity.setUrl("https://reddit.com" + post.permalink());
            opportunity.setCompanyName(extractCompanyName(post));
            opportunity.setIndustry(inferIndustry(fullText));
            opportunity.setCompanySize(CompanySize.UNKNOWN);
            opportunity.setStatus(OpportunityStatus.DISCOVERED);
            opportunity.setDiscoveredAt(LocalDateTime.ofInstant(
                Instant.ofEpochSecond(post.created_utc().longValue()), 
                ZoneId.systemDefault()
            ));
            
            // Calculate score based on Reddit metrics
            BigDecimal score = calculateRedditScore(post);
            opportunity.setScore(score);
            opportunity.setConfidenceScore(BigDecimal.valueOf(65.0));
            opportunity.setEngagementPotential(calculateEngagementPotential(post));

            // Add metadata
            Map<String, String> metadata = new HashMap<>();
            metadata.put("subreddit", post.subreddit());
            metadata.put("author", post.author());
            metadata.put("score", String.valueOf(post.score()));
            metadata.put("num_comments", String.valueOf(post.num_comments()));
            metadata.put("upvote_ratio", String.valueOf(post.upvote_ratio()));
            metadata.put("created_utc", String.valueOf(post.created_utc()));
            metadata.put("permalink", post.permalink());
            if (post.link_flair_text() != null) {
                metadata.put("flair", post.link_flair_text());
            }
            opportunity.setMetadata(metadata);

            // Add tags
            List<String> tags = new ArrayList<>();
            tags.add("reddit");
            tags.add("r/" + post.subreddit());
            if (post.link_flair_text() != null) {
                tags.add(post.link_flair_text().toLowerCase());
            }
            tags.addAll(extractTags(fullText));
            opportunity.setTags(tags);

            return opportunity;

        } catch (Exception e) {
            log.error("Error mapping Reddit post to opportunity: {}", e.getMessage());
            return null;
        }
    }

    private boolean isRelevantOpportunity(OpportunityDto opportunity) {
        String text = (opportunity.getTitle() + " " + opportunity.getDescription()).toLowerCase();
        
        // Check for opportunity keywords
        boolean hasKeywords = OPPORTUNITY_KEYWORDS.stream()
            .anyMatch(keyword -> text.contains(keyword));
        
        // Filter out low-quality posts
        if (opportunity.getMetadata() != null) {
            int score = Integer.parseInt(opportunity.getMetadata().getOrDefault("score", "0"));
            int comments = Integer.parseInt(opportunity.getMetadata().getOrDefault("num_comments", "0"));
            
            // More lenient engagement requirements to get more results
            if (score < 2 && comments < 1) {
                return false;
            }
        }
        
        // More lenient filtering to include more opportunities
        return hasKeywords || opportunity.getScore().compareTo(BigDecimal.valueOf(20)) > 0;
    }

    private OpportunityType determineOpportunityType(String text, OpportunityType defaultType) {
        String lowerText = text.toLowerCase();
        
        if (lowerText.contains("raised") || lowerText.contains("funding") || 
            lowerText.contains("series") || lowerText.contains("seed")) {
            return OpportunityType.STARTUP_FUNDING;
        } else if (lowerText.contains("launch") || lowerText.contains("beta") || 
                   lowerText.contains("mvp") || lowerText.contains("released")) {
            return OpportunityType.PRODUCT_LAUNCH;
        } else if (lowerText.contains("hiring") || lowerText.contains("jobs") || 
                   lowerText.contains("positions")) {
            return OpportunityType.JOB_POSTING_SIGNAL;
        } else if (lowerText.contains("partner") || lowerText.contains("collaboration")) {
            return OpportunityType.PARTNERSHIP;
        } else if (lowerText.contains("acquire") || lowerText.contains("acquisition")) {
            return OpportunityType.ACQUISITION_TARGET;
        }
        
        return defaultType;
    }

    private String extractCompanyName(RedditPost post) {
        // Try to extract company name from title or flair
        String title = post.title();
        
        // Common patterns: "[Company Name]", "Company Name -", "Company Name:"
        if (title.contains("[") && title.contains("]")) {
            return title.substring(title.indexOf("[") + 1, title.indexOf("]")).trim();
        }
        
        if (title.contains(" - ")) {
            return title.substring(0, title.indexOf(" - ")).trim();
        }
        
        if (title.contains(":")) {
            String beforeColon = title.substring(0, title.indexOf(":")).trim();
            if (beforeColon.split(" ").length <= 3) {
                return beforeColon;
            }
        }
        
        return "r/" + post.subreddit();
    }

    private Industry inferIndustry(String text) {
        String lowerText = text.toLowerCase();
        
        if (lowerText.contains("fintech") || lowerText.contains("finance") || lowerText.contains("payment")) {
            return Industry.FINTECH;
        } else if (lowerText.contains("ai") || lowerText.contains("machine learning") || lowerText.contains("artificial intelligence")) {
            return Industry.ARTIFICIAL_INTELLIGENCE;
        } else if (lowerText.contains("saas") || lowerText.contains("software")) {
            return Industry.ENTERPRISE_SOFTWARE;
        } else if (lowerText.contains("health") || lowerText.contains("medical")) {
            return Industry.HEALTHTECH;
        } else if (lowerText.contains("education") || lowerText.contains("edtech")) {
            return Industry.EDTECH;
        } else if (lowerText.contains("blockchain") || lowerText.contains("crypto")) {
            return Industry.BLOCKCHAIN;
        } else if (lowerText.contains("cybersecurity") || lowerText.contains("security")) {
            return Industry.CYBERSECURITY;
        } else if (lowerText.contains("ecommerce") || lowerText.contains("marketplace")) {
            return Industry.ECOMMERCE;
        }
        
        return Industry.ENTERPRISE_SOFTWARE;
    }

    private BigDecimal calculateRedditScore(RedditPost post) {
        double score = 0.0;
        
        // Reddit score factor (0-40 points)
        double redditScore = Math.min(40.0, (post.score() / 100.0) * 20);
        score += redditScore;
        
        // Comments factor (0-20 points)
        double commentsScore = Math.min(20.0, (post.num_comments() / 50.0) * 20);
        score += commentsScore;
        
        // Upvote ratio factor (0-20 points)
        double ratioScore = post.upvote_ratio() * 20;
        score += ratioScore;
        
        // Recency factor (0-20 points)
        long hoursAgo = (System.currentTimeMillis() / 1000 - post.created_utc()) / 3600;
        double recencyScore = Math.max(0, 20.0 - (hoursAgo / 24.0) * 5);
        score += recencyScore;
        
        return BigDecimal.valueOf(Math.min(100.0, score));
    }

    private BigDecimal calculateEngagementPotential(RedditPost post) {
        // Calculate based on comment-to-score ratio and upvote ratio
        double commentRatio = post.score() > 0 ? (double) post.num_comments() / post.score() : 0;
        double engagement = (commentRatio * 50) + (post.upvote_ratio() * 50);
        return BigDecimal.valueOf(Math.min(100.0, engagement));
    }

    private List<String> extractTags(String text) {
        List<String> tags = new ArrayList<>();
        String lowerText = text.toLowerCase();
        
        // Technology tags
        if (lowerText.contains("javascript") || lowerText.contains("react") || lowerText.contains("node")) {
            tags.add("javascript");
        }
        if (lowerText.contains("python")) tags.add("python");
        if (lowerText.contains("java") && !lowerText.contains("javascript")) tags.add("java");
        if (lowerText.contains("golang") || lowerText.contains(" go ")) tags.add("golang");
        if (lowerText.contains("rust")) tags.add("rust");
        
        // Stage tags
        if (lowerText.contains("seed")) tags.add("seed-stage");
        if (lowerText.contains("series a")) tags.add("series-a");
        if (lowerText.contains("series b")) tags.add("series-b");
        if (lowerText.contains("ipo")) tags.add("ipo");
        
        return tags;
    }

    @Override
    protected boolean checkApiHealth() {
        try {
            redditWebClient.get()
                .uri("/r/startups/hot.json?limit=1")
                .header("User-Agent", "OpportunityFinder/1.0")
                .retrieve()
                .bodyToMono(String.class)
                .block();
            return true;
        } catch (Exception e) {
            log.error("Reddit API health check failed: {}", e.getMessage());
            return false;
        }
    }
}

// Reddit API response DTOs
record RedditListingResponse(RedditListingData data) {}

record RedditListingData(List<RedditChild> children, String after, String before) {}

record RedditChild(String kind, RedditPost data) {}

record RedditPost(
    String id,
    String title,
    String selftext,
    String author,
    String subreddit,
    String permalink,
    String url,
    Integer score,
    Integer num_comments,
    Double upvote_ratio,
    Long created_utc,
    String link_flair_text,
    Boolean is_self,
    Boolean over_18
) {}