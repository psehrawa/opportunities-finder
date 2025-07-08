package com.psehrawa.oppfinder.discovery.service.datasource.blind;

import com.psehrawa.oppfinder.common.dto.OpportunityDto;
import com.psehrawa.oppfinder.common.enums.*;
import com.psehrawa.oppfinder.discovery.config.RateLimitConfig;
import com.psehrawa.oppfinder.discovery.service.datasource.AbstractDataSourceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.Collections;
import java.util.stream.Collectors;

@Service
@Slf4j
public class BlindDataSourceService extends AbstractDataSourceService {

    private final RateLimitConfig rateLimitConfig;
    private final WebClient webClient;
    
    // Blind is focused on anonymous workplace discussions
    // This implementation simulates data extraction from Blind-style content
    
    private static final List<String> BLIND_COMPANY_TOPICS = List.of(
        "Tech Companies", "Startups", "Unicorns", "FAANG", "Series A-C Companies",
        "Pre-IPO Companies", "Recently Funded", "High Growth", "Remote First"
    );
    
    private static final List<String> OPPORTUNITY_SIGNALS = List.of(
        "hiring like crazy", "massive expansion", "new funding", "IPO prep",
        "acquired", "new product launch", "scaling team", "opening new office",
        "stock options", "recruiting heavily", "headcount doubling", "new round",
        "unicorn status", "revenue milestone", "profitability", "new market"
    );
    
    private static final Map<String, Industry> COMPANY_INDUSTRY_MAP = Map.of(
        "stripe", Industry.FINTECH,
        "openai", Industry.ARTIFICIAL_INTELLIGENCE,
        "databricks", Industry.DATA_ANALYTICS,
        "snowflake", Industry.CLOUD_COMPUTING,
        "figma", Industry.ENTERPRISE_SOFTWARE,
        "canva", Industry.CONSUMER_SOFTWARE,
        "discord", Industry.CONSUMER_SOFTWARE,
        "notion", Industry.ENTERPRISE_SOFTWARE
    );

    public BlindDataSourceService(
            @Qualifier("webClient") WebClient webClient,
            RedisTemplate<String, Object> redisTemplate,
            RateLimitConfig rateLimitConfig) {
        super(webClient, redisTemplate);
        this.webClient = webClient;
        this.rateLimitConfig = rateLimitConfig;
    }

    @Override
    public DataSource getDataSource() {
        return DataSource.BLIND;
    }

    @Override
    public boolean isEnabled() {
        RateLimitConfig.DataSourceConfig config = rateLimitConfig.getBlind();
        return config != null && config.isEnabled();
    }

    @Override
    public boolean validateConfiguration() {
        RateLimitConfig.DataSourceConfig config = rateLimitConfig.getBlind();
        return config != null && config.isEnabled();
    }

    @Override
    protected RateLimitConfig.DataSourceRateLimit getRateLimitConfig() {
        RateLimitConfig.DataSourceConfig config = rateLimitConfig.getBlind();
        return config != null ? config.getRateLimit() : null;
    }

    @Override
    protected List<OpportunityDto> performDiscovery(List<Country> countries, LocalDateTime since, Integer limit) {
        List<OpportunityDto> opportunities = new ArrayList<>();
        
        // Note: This is a simulation. Real implementation would require proper API access
        // Blind doesn't have a public API, so this demonstrates the type of data we'd extract
        
        log.info("Simulating Blind discovery for demonstration purposes");
        
        // Generate sample opportunities based on Blind-style insider information
        opportunities.addAll(generateBlindStyleOpportunities(limit != null ? limit : 15));
        
        return opportunities.stream()
            .filter(this::isRelevantOpportunity)
            .limit(limit != null ? limit : 15)
            .collect(Collectors.toList());
    }

    private List<OpportunityDto> generateBlindStyleOpportunities(int count) {
        List<OpportunityDto> opportunities = new ArrayList<>();
        
        // Generate more diverse insider posts
        List<BlindPost> posts = generateDiverseBlindPosts(count);
        
        for (int i = 0; i < Math.min(count, posts.size()); i++) {
            BlindPost post = posts.get(i);
            OpportunityDto opp = createBlindOpportunity(post, i);
            if (opp != null) {
                opportunities.add(opp);
            }
        }
        
        return opportunities;
    }

    private OpportunityDto createBlindOpportunity(BlindPost post, int index) {
        try {
            // Use a stable ID based on the content to prevent duplicates
            String externalId = "blind-" + post.title.hashCode() + "-" + post.content.hashCode();
            
            OpportunityDto opportunity = createBaseOpportunity(
                externalId,
                post.title,
                post.content
            );

            opportunity.setType(determineOpportunityType(post));
            opportunity.setUrl("https://www.teamblind.com/post/" + externalId);
            opportunity.setCompanyName(extractCompanyName(post));
            opportunity.setIndustry(inferIndustry(post));
            opportunity.setCompanySize(inferCompanySize(post));
            opportunity.setStatus(OpportunityStatus.DISCOVERED);
            opportunity.setFundingStage(inferFundingStage(post));
            
            // Calculate score based on Blind metrics and content
            BigDecimal score = calculateBlindScore(post);
            opportunity.setScore(score);
            opportunity.setConfidenceScore(BigDecimal.valueOf(80.0)); // Higher confidence for insider info
            opportunity.setEngagementPotential(calculateEngagementPotential(post));

            // Extract funding amount if mentioned
            BigDecimal fundingAmount = extractFundingAmount(post.content);
            if (fundingAmount != null) {
                opportunity.setFundingAmount(fundingAmount);
            }

            // Add metadata
            Map<String, String> metadata = new HashMap<>();
            metadata.put("author_role", post.authorRole);
            metadata.put("likes", String.valueOf(post.likes));
            metadata.put("comments", String.valueOf(post.comments));
            metadata.put("post_type", "insider_info");
            metadata.put("verification_status", "verified_employee");
            metadata.put("tags", String.join(",", post.tags));
            opportunity.setMetadata(metadata);

            // Add tags
            List<String> tags = new ArrayList<>();
            tags.add("blind");
            tags.add("insider-info");
            tags.addAll(post.tags);
            tags.addAll(extractAdditionalTags(post.content));
            opportunity.setTags(tags);

            return opportunity;

        } catch (Exception e) {
            log.error("Error creating Blind opportunity: {}", e.getMessage());
            return null;
        }
    }

    private boolean isRelevantOpportunity(OpportunityDto opportunity) {
        String text = (opportunity.getTitle() + " " + opportunity.getDescription()).toLowerCase();
        
        // Check for opportunity signals
        return OPPORTUNITY_SIGNALS.stream().anyMatch(signal -> 
            text.contains(signal.toLowerCase())
        ) && opportunity.getScore().compareTo(BigDecimal.valueOf(40)) > 0;
    }

    private OpportunityType determineOpportunityType(BlindPost post) {
        String lowerContent = post.content.toLowerCase();
        
        if (lowerContent.contains("ipo")) {
            return OpportunityType.MARKET_EXPANSION;
        } else if (lowerContent.contains("acquired") || lowerContent.contains("acquisition")) {
            return OpportunityType.ACQUISITION_TARGET;
        } else if (lowerContent.contains("raised") || lowerContent.contains("funding") || 
                   lowerContent.contains("series")) {
            return OpportunityType.STARTUP_FUNDING;
        } else if (lowerContent.contains("hiring") || lowerContent.contains("headcount") || 
                   lowerContent.contains("recruiting")) {
            return OpportunityType.JOB_POSTING_SIGNAL;
        } else if (lowerContent.contains("launch") || lowerContent.contains("new product")) {
            return OpportunityType.PRODUCT_LAUNCH;
        } else if (lowerContent.contains("revenue") || lowerContent.contains("growth")) {
            return OpportunityType.MARKET_EXPANSION;
        }
        
        return OpportunityType.TECHNOLOGY_TREND;
    }

    private String extractCompanyName(BlindPost post) {
        // Try to extract from title first
        String[] titleWords = post.title.split(" ");
        for (String word : titleWords) {
            if (word.length() > 3 && Character.isUpperCase(word.charAt(0))) {
                // Check if it's a known company
                if (COMPANY_INDUSTRY_MAP.containsKey(word.toLowerCase())) {
                    return word;
                }
            }
        }
        
        // Look for company names in content
        String[] companyPatterns = {
            "([A-Z][a-zA-Z]+(?:[A-Z][a-zA-Z]+)?)",
            "(?:joined |at |from )([A-Z][a-zA-Z]+)",
            "([A-Z][a-zA-Z]+) (?:just|revenue|raised|hit)"
        };
        
        for (String pattern : companyPatterns) {
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
            java.util.regex.Matcher m = p.matcher(post.content);
            if (m.find()) {
                String potential = m.group(1);
                if (!List.of("Series", "IPO", "FDA", "ARR", "YoY").contains(potential)) {
                    return potential;
                }
            }
        }
        
        return "Stealth Startup";
    }

    private Industry inferIndustry(BlindPost post) {
        String lowerContent = (post.title + " " + post.content).toLowerCase();
        
        // Check known companies first
        for (Map.Entry<String, Industry> entry : COMPANY_INDUSTRY_MAP.entrySet()) {
            if (lowerContent.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        
        // Infer from content
        if (lowerContent.contains("ai") || lowerContent.contains("ml") || 
            lowerContent.contains("machine learning")) {
            return Industry.ARTIFICIAL_INTELLIGENCE;
        } else if (lowerContent.contains("fintech") || lowerContent.contains("payment") || 
                   lowerContent.contains("banking")) {
            return Industry.FINTECH;
        } else if (lowerContent.contains("health") || lowerContent.contains("medical") || 
                   lowerContent.contains("diagnostic")) {
            return Industry.HEALTHTECH;
        } else if (lowerContent.contains("cloud") || lowerContent.contains("infrastructure")) {
            return Industry.CLOUD_COMPUTING;
        } else if (lowerContent.contains("crypto") || lowerContent.contains("blockchain")) {
            return Industry.BLOCKCHAIN;
        } else if (lowerContent.contains("data") || lowerContent.contains("analytics")) {
            return Industry.DATA_ANALYTICS;
        } else if (lowerContent.contains("devtool") || lowerContent.contains("developer")) {
            return Industry.DEVOPS;
        }
        
        return Industry.ENTERPRISE_SOFTWARE;
    }

    private CompanySize inferCompanySize(BlindPost post) {
        String content = post.content.toLowerCase();
        
        if (content.contains("unicorn") || content.contains("ipo")) {
            return CompanySize.LARGE;
        } else if (content.contains("#15") || content.contains("founding") || 
                   content.contains("stealth")) {
            return CompanySize.STARTUP;
        } else if (content.contains("series c") || content.contains("series d")) {
            return CompanySize.MEDIUM;
        } else if (content.contains("series a") || content.contains("series b")) {
            return CompanySize.SMALL;
        }
        
        return CompanySize.SMALL;
    }

    private FundingStage inferFundingStage(BlindPost post) {
        String content = post.content.toLowerCase();
        
        if (content.contains("seed")) return FundingStage.SEED;
        if (content.contains("series a")) return FundingStage.SERIES_A;
        if (content.contains("series b")) return FundingStage.SERIES_B;
        if (content.contains("series c")) return FundingStage.SERIES_C;
        if (content.contains("series d") || content.contains("series e")) return FundingStage.SERIES_D_PLUS;
        if (content.contains("ipo")) return FundingStage.IPO;
        if (content.contains("acquired") || content.contains("acquisition")) return FundingStage.ACQUISITION;
        
        return FundingStage.UNKNOWN;
    }

    private BigDecimal extractFundingAmount(String content) {
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
            "\\$([0-9]+(?:\\.[0-9]+)?)[MBK](?:illion)?", 
            java.util.regex.Pattern.CASE_INSENSITIVE
        );
        java.util.regex.Matcher matcher = pattern.matcher(content);
        
        if (matcher.find()) {
            String amountStr = matcher.group(1);
            String unit = matcher.group(0).toUpperCase();
            
            try {
                double amount = Double.parseDouble(amountStr);
                if (unit.contains("B")) {
                    amount *= 1_000_000_000;
                } else if (unit.contains("M")) {
                    amount *= 1_000_000;
                } else if (unit.contains("K")) {
                    amount *= 1_000;
                }
                return BigDecimal.valueOf(amount);
            } catch (NumberFormatException e) {
                log.error("Failed to parse funding amount: {}", amountStr);
            }
        }
        
        return null;
    }

    private BigDecimal calculateBlindScore(BlindPost post) {
        double score = 0.0;
        
        // Engagement factor (0-40 points)
        double engagementScore = Math.min(40.0, (post.likes / 100.0) * 20 + (post.comments / 50.0) * 20);
        score += engagementScore;
        
        // Content quality factor (0-30 points)
        long signalCount = OPPORTUNITY_SIGNALS.stream()
            .filter(signal -> post.content.toLowerCase().contains(signal))
            .count();
        score += Math.min(30.0, signalCount * 10);
        
        // Author credibility (0-20 points)
        if (post.authorRole.contains("VP") || post.authorRole.contains("Director")) {
            score += 20.0;
        } else if (post.authorRole.contains("Senior") || post.authorRole.contains("Manager")) {
            score += 15.0;
        } else {
            score += 10.0;
        }
        
        // Tag relevance (0-10 points)
        if (post.tags.contains("unicorn") || post.tags.contains("ipo")) {
            score += 10.0;
        } else if (post.tags.contains("series-b") || post.tags.contains("series-c")) {
            score += 8.0;
        } else {
            score += 5.0;
        }
        
        return BigDecimal.valueOf(Math.min(100.0, score));
    }

    private BigDecimal calculateEngagementPotential(BlindPost post) {
        // Based on likes/comments ratio and content quality
        double ratio = post.comments > 0 ? (double) post.likes / post.comments : 0;
        double potential = (ratio * 30) + (post.comments / 10.0) + (post.likes / 100.0) * 20;
        return BigDecimal.valueOf(Math.min(100.0, potential));
    }

    private List<String> extractAdditionalTags(String content) {
        List<String> tags = new ArrayList<>();
        String lowerContent = content.toLowerCase();
        
        // Growth indicators
        if (lowerContent.contains("hypergrowth") || lowerContent.contains("300%")) {
            tags.add("hypergrowth");
        }
        if (lowerContent.contains("unicorn")) tags.add("unicorn");
        if (lowerContent.contains("retention")) tags.add("retention-bonus");
        
        // Technology tags
        if (lowerContent.contains("infrastructure")) tags.add("infrastructure");
        if (lowerContent.contains("ml ") || lowerContent.contains("machine learning")) {
            tags.add("machine-learning");
        }
        if (lowerContent.contains("security")) tags.add("security");
        
        // Market tags
        if (lowerContent.contains("b2b")) tags.add("b2b");
        if (lowerContent.contains("enterprise")) tags.add("enterprise");
        if (lowerContent.contains("institutional")) tags.add("institutional");
        
        return tags;
    }

    @Override
    protected boolean checkApiHealth() {
        // Since Blind doesn't have a public API, we'll simulate health check
        // In a real implementation, this would check if web scraping is working
        return true;
    }

    private List<BlindPost> generateDiverseBlindPosts(int requestedCount) {
        List<BlindPost> allPosts = new ArrayList<>();
        
        // Unicorn and high-growth companies
        allPosts.add(new BlindPost(
            "Stealth AI Startup",
            "Just joined a stealth AI startup as #15. We just closed $40M Series B led by Sequoia. " +
            "Building next-gen AI infrastructure. Hiring across all engineering roles. TC packages are insane!",
            "Verified Employee", 892, 156, List.of("series-b", "ai", "hiring")
        ));
        allPosts.add(new BlindPost(
            "Fintech Unicorn Alert",
            "PayFlow just hit $2B valuation after latest funding. Internal sources say IPO planned for Q2 2025. " +
            "They're doubling headcount - especially in product and engineering. Great equity packages.",
            "Senior Engineer", 2340, 289, List.of("unicorn", "fintech", "ipo")
        ));
        allPosts.add(new BlindPost(
            "DataMesh Acquisition",
            "CONFIRMED: DataMesh getting acquired by Google for $800M. Announcement next week. " +
            "All employees getting retention bonuses. This validates their approach to distributed data.",
            "Product Manager", 5670, 423, List.of("acquisition", "google", "data")
        ));
        allPosts.add(new BlindPost(
            "CloudNative's Explosive Growth",
            "CloudNative revenue just crossed $100M ARR. Growing 300% YoY. " +
            "Opening offices in NYC and London. Hiring 200+ engineers this quarter. Stock options still attractive.",
            "VP Engineering", 1560, 198, List.of("hypergrowth", "cloud", "expansion")
        ));
        
        // Series A/B companies
        allPosts.add(new BlindPost(
            "HealthTech Revolution",
            "MedConnect raised $60M Series C. Building AI-powered diagnostic tools. " +
            "FDA approval expected Q1. This could be the next big healthtech IPO. Hiring ML engineers aggressively.",
            "Data Scientist", 980, 134, List.of("healthtech", "series-c", "ai")
        ));
        allPosts.add(new BlindPost(
            "DevTools Startup Momentum",
            "CodeStream seeing crazy adoption - 50k developers in 3 months. " +
            "Just raised $25M Series A. Building the future of collaborative coding. Great time to join!",
            "Founding Engineer", 1230, 167, List.of("devtools", "series-a", "saas")
        ));
        allPosts.add(new BlindPost(
            "Crypto Infrastructure Play",
            "ChainBridge building institutional crypto infrastructure. $45M in funding, blue-chip VCs. " +
            "Solving real problems, not another DeFi clone. Hiring security and backend engineers.",
            "Security Engineer", 780, 98, List.of("crypto", "infrastructure", "security")
        ));
        allPosts.add(new BlindPost(
            "B2B SaaS Rocket Ship",
            "SalesForce competitor hitting $50M ARR in 18 months. Series B oversubscribed at $500M valuation. " +
            "Opening EU office, need sales engineers and customer success leads.",
            "Director of Sales", 3200, 267, List.of("b2b-saas", "series-b", "hypergrowth")
        ));
        
        // Early stage with strong signals
        allPosts.add(new BlindPost(
            "Ex-Stripe Team's New Venture",
            "5 ex-Stripe engineers building payment infra for Web3. Already have $10M seed from a16z. " +
            "If you want to get in early with proven team, DM me. Looking for backend and smart contract devs.",
            "Founding Engineer", 4500, 389, List.of("fintech", "web3", "seed")
        ));
        allPosts.add(new BlindPost(
            "ML Infra Startup Exploding",
            "ModelServe making ML deployment 10x easier. From 0 to $5M ARR in 6 months. " +
            "Tiger Global leading Series A. Hiring ML engineers and DevOps. Base + equity is competitive.",
            "ML Engineer", 2100, 178, List.of("ml-infra", "series-a", "tiger-global")
        ));
        allPosts.add(new BlindPost(
            "Security Startup Gold Rush",
            "ZeroDay just landed Fortune 100 contracts. Revenue growing 50% MoM. " +
            "Series A coming soon. Need security engineers and enterprise sales. RSUs will be worth a lot.",
            "Security Architect", 1890, 145, List.of("cybersecurity", "enterprise", "pre-series-a")
        ));
        
        // Industry disruption signals
        allPosts.add(new BlindPost(
            "Real Estate Tech Revolution",
            "PropTech startup eliminating brokers. $30M Series B, Bessemer leading. " +
            "Launching in 10 new markets. Hiring ops managers and engineers. This will disrupt $100B industry.",
            "Operations Lead", 2670, 213, List.of("proptech", "series-b", "disruption")
        ));
        allPosts.add(new BlindPost(
            "Climate Tech Breakthrough",
            "GreenEnergy's battery tech is game-changing. Bill Gates fund just invested $50M. " +
            "Manufacturing scaling up. Need hardware engineers and supply chain experts. Pre-IPO opportunity.",
            "Hardware Engineer", 3100, 248, List.of("climate-tech", "hardware", "breakthrough")
        ));
        allPosts.add(new BlindPost(
            "Legal Tech Unicorn in Making",
            "LawBot automating contract review. $2B worth of contracts processed last month. " +
            "Series C at $800M valuation closing. Hiring AI researchers and enterprise sales.",
            "Head of Product", 2900, 234, List.of("legaltech", "ai", "unicorn-potential")
        ));
        
        // Geographic expansion
        allPosts.add(new BlindPost(
            "APAC Expansion Alert",
            "TechCo opening Singapore hub as APAC HQ. $100M allocated for expansion. " +
            "Looking for country managers and local engineering teams. Relocation packages available.",
            "Regional Director", 1560, 126, List.of("expansion", "apac", "hiring")
        ));
        allPosts.add(new BlindPost(
            "EU Market Entry",
            "US unicorn entering EU market after GDPR compliance. Opening offices in Berlin and Paris. " +
            "Need compliance officers and local sales teams. Stock options for early hires.",
            "Compliance Manager", 1340, 108, List.of("expansion", "europe", "gdpr")
        ));
        
        // Insider trading/acquisition signals
        allPosts.add(new BlindPost(
            "Microsoft Shopping Again",
            "Heard from reliable source - MSFT in talks to acquire DevTools startup for $2B+. " +
            "Employees will get huge retention bonuses. Great time to join if you can.",
            "Principal Engineer", 6700, 512, List.of("acquisition", "microsoft", "insider-info")
        ));
        allPosts.add(new BlindPost(
            "IPO Filing Imminent",
            "FinanceApp filing S-1 next month. $500M revenue last year, profitable. " +
            "Blackout period starting soon. If you want pre-IPO equity, apply now.",
            "Finance Director", 5200, 423, List.of("ipo", "fintech", "profitable")
        ));
        
        // Competitive intelligence
        allPosts.add(new BlindPost(
            "Competitor Struggling",
            "Major competitor laying off 30% next week. Their best engineers looking for jobs. " +
            "Our company ready to hire their A-players. Referral bonus increased to $20K.",
            "Engineering Manager", 3400, 278, List.of("competitive-intel", "hiring", "layoffs")
        ));
        allPosts.add(new BlindPost(
            "Market Leader Vulnerable",
            "Industry leader's product is dated. Lost 3 major customers last quarter. " +
            "Perfect time for disruption. Multiple startups raising to compete. Watch this space.",
            "Product Director", 2890, 234, List.of("market-opportunity", "disruption", "competition")
        ));
        
        // Technical breakthroughs
        allPosts.add(new BlindPost(
            "AI Breakthrough at Startup",
            "Small team achieved SOTA results on major benchmark. Google and Meta recruiters circling. " +
            "Raising $100M at $1B valuation. Join before it becomes impossible to get in.",
            "AI Researcher", 4300, 356, List.of("ai", "breakthrough", "hiring")
        ));
        allPosts.add(new BlindPost(
            "Quantum Computing Milestone",
            "QubitLabs achieved quantum advantage in optimization. IBM wants to partner/acquire. " +
            "Only 30 employees now. Hiring quantum engineers and researchers. Once-in-lifetime opportunity.",
            "Quantum Engineer", 3900, 312, List.of("quantum", "breakthrough", "acquisition-target")
        ));
        
        // Compensation insights
        allPosts.add(new BlindPost(
            "Insane Comp Packages",
            "NewCo offering $400K base + $600K equity for senior engineers. " +
            "Competing with FAANG for talent. AI/ML focus. Multiple offers already accepted.",
            "Senior SWE", 5100, 423, List.of("compensation", "hiring", "ai")
        ));
        allPosts.add(new BlindPost(
            "Pre-IPO Equity Goldmine",
            "DataCo refreshing equity grants before IPO. New hires getting 2x normal grants. " +
            "Expected 10x return based on private market valuations. Window closing soon.",
            "Staff Engineer", 4200, 345, List.of("pre-ipo", "equity", "compensation")
        ));
        
        // Return requested number of posts with some randomization
        Collections.shuffle(allPosts);
        return allPosts.subList(0, Math.min(requestedCount, allPosts.size()));
    }
    
    // Helper class for sample data
    private static class BlindPost {
        final String title;
        final String content;
        final String authorRole;
        final int likes;
        final int comments;
        final List<String> tags;
        
        BlindPost(String title, String content, String authorRole, int likes, int comments, List<String> tags) {
            this.title = title;
            this.content = content;
            this.authorRole = authorRole;
            this.likes = likes;
            this.comments = comments;
            this.tags = tags;
        }
    }
}