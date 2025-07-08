package com.psehrawa.oppfinder.common.enums;

import lombok.Getter;

@Getter
public enum DataSource {
    GITHUB("GitHub API", "Repository trends and technology adoption", true, 5000),
    HACKER_NEWS("Hacker News API", "Community-validated trends and discussions", true, 0),
    REDDIT("Reddit API", "Startup and technology subreddits", false, 100),
    PRODUCT_HUNT("Product Hunt API", "New product launches and startup activity", false, 100),
    SEC_EDGAR("SEC EDGAR API", "Company filings and funding announcements", true, 1000),
    USPTO_PATENT("USPTO Patent API", "Technology innovation indicators", true, 1000),
    CRUNCHBASE_BASIC("Crunchbase Basic", "Limited startup data", false, 200),
    GOOGLE_TRENDS("Google Trends API", "Market interest and search volume", false, 1000),
    NEWS_API("News API", "Real-time news and announcements", false, 1000),
    TWITTER_API("Twitter/X API", "Social signals and announcements", false, 300),
    LINKEDIN_API("LinkedIn API", "Job postings and company growth signals", false, 500),
    YOUTUBE_API("YouTube API", "Technology content and conference talks", false, 10000),
    CRUNCHBASE_PRO("Crunchbase Pro", "Comprehensive startup database", false, 1000),
    PITCHBOOK("PitchBook API", "Venture capital and private market data", false, 500),
    OWLER("Owler API", "Company competitive intelligence", false, 1000),
    ANGELLIST("AngelList API", "Startup job postings and funding", false, 500),
    BLIND("Blind API", "Anonymous professional insights and company discussions", true, 1000),
    QUORA("Quora API", "Technology and startup Q&A discussions", true, 1000);

    private final String displayName;
    private final String description;
    private final boolean isFree;
    private final int dailyRequestLimit;

    DataSource(String displayName, String description, boolean isFree, int dailyRequestLimit) {
        this.displayName = displayName;
        this.description = description;
        this.isFree = isFree;
        this.dailyRequestLimit = dailyRequestLimit;
    }
}