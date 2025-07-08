package com.psehrawa.oppfinder.discovery.service.datasource.quora;

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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Slf4j
public class QuoraDataSourceService extends AbstractDataSourceService {

    private final RateLimitConfig rateLimitConfig;
    private final WebClient webClient;
    
    // Quora doesn't have a public API, so we'll simulate by using web scraping patterns
    // In production, you'd need to implement proper web scraping or use unofficial APIs
    
    private static final List<String> QUORA_STARTUP_TOPICS = List.of(
        "Startups", "Entrepreneurship", "Venture-Capital", "Technology-Startups",
        "Lean-Startups", "Startup-Funding", "Startup-Ideas", "Tech-Entrepreneurship",
        "Software-Development", "Artificial-Intelligence", "Machine-Learning",
        "Fintech", "SaaS", "Product-Development"
    );
    
    private static final List<String> SIGNAL_PATTERNS = List.of(
        "just raised", "announcing", "we built", "launching", "looking for",
        "hiring", "seed round", "series [A-Z]", "funding", "acquired",
        "beta testers", "early access", "feedback on", "validate", "MVP"
    );

    public QuoraDataSourceService(
            @Qualifier("webClient") WebClient webClient,
            RedisTemplate<String, Object> redisTemplate,
            RateLimitConfig rateLimitConfig) {
        super(webClient, redisTemplate);
        this.webClient = webClient;
        this.rateLimitConfig = rateLimitConfig;
    }

    @Override
    public DataSource getDataSource() {
        return DataSource.QUORA;
    }

    @Override
    public boolean isEnabled() {
        RateLimitConfig.DataSourceConfig config = rateLimitConfig.getQuora();
        return config != null && config.isEnabled();
    }

    @Override
    public boolean validateConfiguration() {
        RateLimitConfig.DataSourceConfig config = rateLimitConfig.getQuora();
        return config != null && config.isEnabled();
    }

    @Override
    protected RateLimitConfig.DataSourceRateLimit getRateLimitConfig() {
        RateLimitConfig.DataSourceConfig config = rateLimitConfig.getQuora();
        return config != null ? config.getRateLimit() : null;
    }

    @Override
    protected List<OpportunityDto> performDiscovery(List<Country> countries, LocalDateTime since, Integer limit) {
        List<OpportunityDto> opportunities = new ArrayList<>();
        
        // Note: This is a simulation. Real implementation would require web scraping
        // or using an unofficial API with proper authentication
        
        log.info("Simulating Quora discovery for demonstration purposes");
        
        // Generate sample opportunities based on Quora-style content
        opportunities.addAll(generateQuoraStyleOpportunities(limit != null ? limit : 20));
        
        return opportunities.stream()
            .filter(opp -> isRelevantOpportunity(opp))
            .limit(limit != null ? limit : 20)
            .collect(Collectors.toList());
    }

    private List<OpportunityDto> generateQuoraStyleOpportunities(int count) {
        List<OpportunityDto> opportunities = new ArrayList<>();
        
        // Generate more diverse sample data to simulate real Quora content
        List<QuoraSample> samples = generateDiverseSamples(count);
        
        for (int i = 0; i < Math.min(count, samples.size()); i++) {
            QuoraSample sample = samples.get(i);
            OpportunityDto opp = createQuoraOpportunity(sample, i);
            if (opp != null) {
                opportunities.add(opp);
            }
        }
        
        return opportunities;
    }

    private OpportunityDto createQuoraOpportunity(QuoraSample sample, int index) {
        try {
            // Use a stable ID based on the content to prevent duplicates
            String externalId = "quora-" + sample.question.hashCode() + "-" + sample.topic.hashCode();
            
            OpportunityDto opportunity = createBaseOpportunity(
                externalId,
                sample.question,
                sample.answer
            );

            opportunity.setType(determineOpportunityType(sample.answer));
            opportunity.setUrl("https://www.quora.com/simulated/" + externalId);
            opportunity.setCompanyName(extractCompanyName(sample.answer));
            opportunity.setIndustry(inferIndustry(sample.question + " " + sample.answer));
            opportunity.setCompanySize(CompanySize.STARTUP);
            opportunity.setStatus(OpportunityStatus.DISCOVERED);
            
            // Calculate score based on Quora metrics
            BigDecimal score = calculateQuoraScore(sample);
            opportunity.setScore(score);
            opportunity.setConfidenceScore(BigDecimal.valueOf(70.0));
            opportunity.setEngagementPotential(BigDecimal.valueOf(sample.answers * 2.5));

            // Add metadata
            Map<String, String> metadata = new HashMap<>();
            metadata.put("topic", sample.topic);
            metadata.put("views", String.valueOf(sample.views));
            metadata.put("answers", String.valueOf(sample.answers));
            metadata.put("question_type", "startup_opportunity");
            metadata.put("source_type", "qa_platform");
            opportunity.setMetadata(metadata);

            // Add tags
            List<String> tags = new ArrayList<>();
            tags.add("quora");
            tags.add(sample.topic.toLowerCase().replace(" ", "-"));
            tags.addAll(extractTags(sample.question + " " + sample.answer));
            opportunity.setTags(tags);

            return opportunity;

        } catch (Exception e) {
            log.error("Error creating Quora opportunity: {}", e.getMessage());
            return null;
        }
    }

    private boolean isRelevantOpportunity(OpportunityDto opportunity) {
        String text = (opportunity.getTitle() + " " + opportunity.getDescription()).toLowerCase();
        
        // Check for startup signals
        return SIGNAL_PATTERNS.stream().anyMatch(pattern -> 
            Pattern.compile(pattern.toLowerCase()).matcher(text).find()
        );
    }

    private OpportunityType determineOpportunityType(String text) {
        String lowerText = text.toLowerCase();
        
        if (lowerText.contains("raised") || lowerText.contains("funding") || 
            lowerText.contains("series") || lowerText.contains("seed")) {
            return OpportunityType.STARTUP_FUNDING;
        } else if (lowerText.contains("launch") || lowerText.contains("beta") || 
                   lowerText.contains("early access")) {
            return OpportunityType.PRODUCT_LAUNCH;
        } else if (lowerText.contains("co-founder") || lowerText.contains("hiring") || 
                   lowerText.contains("looking for")) {
            return OpportunityType.JOB_POSTING_SIGNAL;
        } else if (lowerText.contains("partner") || lowerText.contains("collaboration")) {
            return OpportunityType.PARTNERSHIP;
        } else if (lowerText.contains("trend") || lowerText.contains("disrupting")) {
            return OpportunityType.TECHNOLOGY_TREND;
        }
        
        return OpportunityType.TECHNOLOGY_TREND;
    }

    private String extractCompanyName(String text) {
        // Look for company name patterns
        Pattern companyPattern = Pattern.compile("(?:We're building |I'm working on |launched |building )([A-Z][a-zA-Z]+(?:[A-Z][a-zA-Z]+)?)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = companyPattern.matcher(text);
        
        if (matcher.find()) {
            return matcher.group(1);
        }
        
        // Look for capitalized words that might be company names
        Pattern capsPattern = Pattern.compile("\\b([A-Z][a-zA-Z]{3,}(?:[A-Z][a-zA-Z]+)?)\\b");
        matcher = capsPattern.matcher(text);
        
        if (matcher.find()) {
            String potential = matcher.group(1);
            // Filter out common words
            if (!List.of("Series", "We're", "The", "This", "That").contains(potential)) {
                return potential;
            }
        }
        
        return "Unknown Startup";
    }

    private Industry inferIndustry(String text) {
        String lowerText = text.toLowerCase();
        
        if (lowerText.contains("ai") || lowerText.contains("artificial intelligence") || 
            lowerText.contains("machine learning") || lowerText.contains("neural")) {
            return Industry.ARTIFICIAL_INTELLIGENCE;
        } else if (lowerText.contains("fintech") || lowerText.contains("payment") || 
                   lowerText.contains("banking") || lowerText.contains("finance")) {
            return Industry.FINTECH;
        } else if (lowerText.contains("health") || lowerText.contains("medical") || 
                   lowerText.contains("patient")) {
            return Industry.HEALTHTECH;
        } else if (lowerText.contains("education") || lowerText.contains("learning") || 
                   lowerText.contains("edtech")) {
            return Industry.EDTECH;
        } else if (lowerText.contains("developer") || lowerText.contains("code") || 
                   lowerText.contains("devops")) {
            return Industry.DEVOPS;
        } else if (lowerText.contains("saas") || lowerText.contains("software")) {
            return Industry.ENTERPRISE_SOFTWARE;
        } else if (lowerText.contains("cyber") || lowerText.contains("security")) {
            return Industry.CYBERSECURITY;
        } else if (lowerText.contains("data") || lowerText.contains("analytics")) {
            return Industry.DATA_ANALYTICS;
        }
        
        return Industry.ENTERPRISE_SOFTWARE;
    }

    private BigDecimal calculateQuoraScore(QuoraSample sample) {
        double score = 0.0;
        
        // Views factor (0-40 points)
        double viewsScore = Math.min(40.0, (sample.views / 1000.0) * 20);
        score += viewsScore;
        
        // Answers factor (0-30 points)
        double answersScore = Math.min(30.0, (sample.answers / 20.0) * 30);
        score += answersScore;
        
        // Topic relevance (0-30 points)
        if (sample.topic.contains("Startup") || sample.topic.contains("AI") || 
            sample.topic.contains("Fintech")) {
            score += 30.0;
        } else {
            score += 15.0;
        }
        
        return BigDecimal.valueOf(Math.min(100.0, score));
    }

    private List<String> extractTags(String text) {
        List<String> tags = new ArrayList<>();
        String lowerText = text.toLowerCase();
        
        // Technology tags
        if (lowerText.contains("ai") || lowerText.contains("artificial intelligence")) {
            tags.add("ai");
        }
        if (lowerText.contains("machine learning")) tags.add("machine-learning");
        if (lowerText.contains("blockchain")) tags.add("blockchain");
        if (lowerText.contains("iot")) tags.add("iot");
        if (lowerText.contains("saas")) tags.add("saas");
        
        // Stage tags
        if (lowerText.contains("mvp")) tags.add("mvp");
        if (lowerText.contains("beta")) tags.add("beta");
        if (lowerText.contains("seed")) tags.add("seed-stage");
        if (lowerText.contains("series a")) tags.add("series-a");
        
        // Type tags
        if (lowerText.contains("b2b")) tags.add("b2b");
        if (lowerText.contains("b2c")) tags.add("b2c");
        if (lowerText.contains("marketplace")) tags.add("marketplace");
        
        return tags;
    }

    @Override
    protected boolean checkApiHealth() {
        // Since Quora doesn't have a public API, we'll simulate health check
        // In a real implementation, this would check if web scraping is working
        return true;
    }

    private List<QuoraSample> generateDiverseSamples(int requestedCount) {
        List<QuoraSample> allSamples = new ArrayList<>();
        
        // AI/ML opportunities
        allSamples.add(new QuoraSample(
            "What are some promising AI startups to watch in 2025?",
            "I've been following several exciting AI startups. NeuralFlow just raised $15M Series A for their conversational AI platform...",
            "AI Startups", 1250, 45
        ));
        allSamples.add(new QuoraSample(
            "Which machine learning companies are hiring aggressively?",
            "DeepSense is on a hiring spree after their $30M Series B. They're building ML infrastructure for enterprises...",
            "Machine Learning Jobs", 980, 38
        ));
        allSamples.add(new QuoraSample(
            "What AI companies recently got funded?",
            "VisionAI just announced $25M funding for computer vision platform. They're disrupting quality control in manufacturing...",
            "AI Funding", 1560, 62
        ));
        
        // SaaS opportunities
        allSamples.add(new QuoraSample(
            "How did you validate your SaaS idea before building?",
            "We launched DataSync last month after validating with 50 beta users. We're now looking for early adopters...",
            "SaaS Validation", 890, 32
        ));
        allSamples.add(new QuoraSample(
            "What B2B SaaS companies are growing fastest?",
            "CloudOps has grown 400% YoY. They just opened Series B at $40M valuation. Building DevOps automation tools...",
            "B2B SaaS Growth", 2100, 89
        ));
        allSamples.add(new QuoraSample(
            "Which SaaS startups are looking for co-founders?",
            "MetricsHub founder is looking for a technical co-founder. They have $500K pre-seed and early customers...",
            "SaaS Co-founders", 670, 25
        ));
        
        // Fintech opportunities
        allSamples.add(new QuoraSample(
            "What fintech companies are disrupting traditional banking?",
            "PaymentBridge is revolutionizing cross-border payments. They just announced their seed funding round...",
            "Fintech Innovation", 2100, 67
        ));
        allSamples.add(new QuoraSample(
            "Which crypto startups are actually solving real problems?",
            "ChainSecure raised $20M for blockchain security infrastructure. Major banks are already piloting their solution...",
            "Crypto Infrastructure", 1890, 73
        ));
        allSamples.add(new QuoraSample(
            "What payment startups are worth watching?",
            "FastPay just hit 1M users in 6 months. They're making instant payments possible for gig workers. Series A coming...",
            "Payment Innovation", 1340, 52
        ));
        
        // Developer tools
        allSamples.add(new QuoraSample(
            "Best practices for launching a developer tool startup?",
            "We're building CodeAssist, an AI-powered code review tool. Looking for beta testers from the community...",
            "Developer Tools", 560, 28
        ));
        allSamples.add(new QuoraSample(
            "What dev tools companies are getting traction?",
            "APIHub seeing explosive growth - 10K developers in 2 months. They simplify API integration. Just raised seed...",
            "Dev Tools Traction", 780, 31
        ));
        allSamples.add(new QuoraSample(
            "Which developer productivity startups to watch?",
            "DevFlow automating CI/CD pipelines with AI. GitHub integration launched last week. 500+ teams already using it...",
            "Developer Productivity", 920, 41
        ));
        
        // Healthtech opportunities
        allSamples.add(new QuoraSample(
            "How to find technical co-founders for a healthtech startup?",
            "I'm working on MedTrack, a patient monitoring platform. We have initial funding and looking for a CTO...",
            "Healthtech Startups", 430, 19
        ));
        allSamples.add(new QuoraSample(
            "What digital health startups got FDA approval recently?",
            "HealthAI's diagnostic tool just got FDA clearance. They're hiring regulatory and engineering talent...",
            "Digital Health", 1670, 68
        ));
        allSamples.add(new QuoraSample(
            "Which telemedicine platforms are expanding?",
            "TeleDoc raised $50M Series C. Opening new engineering hub in Austin. 100+ positions available...",
            "Telemedicine Growth", 1230, 49
        ));
        
        // E-commerce/Marketplace
        allSamples.add(new QuoraSample(
            "What marketplace startups are disrupting traditional retail?",
            "LocalMart connecting neighborhood stores online. $15M Series A, expanding to 50 cities this year...",
            "Marketplace Innovation", 980, 37
        ));
        allSamples.add(new QuoraSample(
            "Which D2C brands are scaling successfully?",
            "EcoWear sustainable fashion brand hit $10M ARR in year one. Opening Series A round next month...",
            "D2C Brands", 1120, 44
        ));
        
        // EdTech opportunities
        allSamples.add(new QuoraSample(
            "What edtech startups are transforming online learning?",
            "SkillPath's AI tutor showing 3x better outcomes. Just partnered with major universities. Hiring curriculum designers...",
            "EdTech Innovation", 890, 35
        ));
        allSamples.add(new QuoraSample(
            "Which coding bootcamps are expanding globally?",
            "CodeCamp raised $30M to expand to Asia. Looking for local partners and instructors in 10 countries...",
            "Coding Education", 760, 29
        ));
        
        // Climate Tech
        allSamples.add(new QuoraSample(
            "What climate tech startups are getting funded?",
            "CarbonZero raised $40M for carbon capture tech. Hiring chemical engineers and data scientists...",
            "Climate Tech", 1450, 58
        ));
        allSamples.add(new QuoraSample(
            "Which renewable energy startups to watch?",
            "SolarGrid making solar accessible for renters. $20M Series A, expanding to 20 states this year...",
            "Renewable Energy", 1280, 51
        ));
        
        // Security/Privacy
        allSamples.add(new QuoraSample(
            "What cybersecurity startups are solving real problems?",
            "ZeroTrust raised $35M for identity management platform. Fortune 500 clients already onboard...",
            "Cybersecurity", 1680, 67
        ));
        allSamples.add(new QuoraSample(
            "Which privacy-focused startups are gaining traction?",
            "PrivacyShield helps companies comply with data regulations. Growing 200% QoQ. Series A discussions ongoing...",
            "Privacy Tech", 920, 36
        ));
        
        // Remote Work
        allSamples.add(new QuoraSample(
            "What remote work tools are companies adopting?",
            "WorkSync revolutionizing async collaboration. 10K teams onboarded last quarter. Hiring across all roles...",
            "Remote Work Tools", 1340, 53
        ));
        allSamples.add(new QuoraSample(
            "Which virtual office startups are worth watching?",
            "VirtualHQ creating immersive remote workspaces. Just closed $25M Series B. Building metaverse for work...",
            "Virtual Office", 890, 34
        ));
        
        // Return requested number of samples, with some randomization
        Collections.shuffle(allSamples);
        return allSamples.subList(0, Math.min(requestedCount, allSamples.size()));
    }
    
    // Helper class for sample data
    private static class QuoraSample {
        final String question;
        final String answer;
        final String topic;
        final int views;
        final int answers;
        
        QuoraSample(String question, String answer, String topic, int views, int answers) {
            this.question = question;
            this.answer = answer;
            this.topic = topic;
            this.views = views;
            this.answers = answers;
        }
    }
}