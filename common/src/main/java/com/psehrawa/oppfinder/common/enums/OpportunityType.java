package com.psehrawa.oppfinder.common.enums;

import lombok.Getter;

@Getter
public enum OpportunityType {
    STARTUP_FUNDING("Startup Funding", "Company seeking funding or announcing funding rounds"),
    PRODUCT_LAUNCH("Product Launch", "New product or service announcements"),
    TECHNOLOGY_TREND("Technology Trend", "Emerging technology or trend gaining traction"),
    MARKET_EXPANSION("Market Expansion", "Company expanding into new markets or regions"),
    PARTNERSHIP("Partnership", "Strategic partnership or collaboration opportunities"),
    ACQUISITION_TARGET("Acquisition Target", "Companies that might be acquisition candidates"),
    JOB_POSTING_SIGNAL("Job Posting Signal", "Hiring patterns indicating company growth"),
    PATENT_FILING("Patent Filing", "New patent filings indicating innovation"),
    CONFERENCE_ANNOUNCEMENT("Conference Announcement", "Industry events and announcements"),
    REGULATORY_CHANGE("Regulatory Change", "Policy or regulatory changes creating opportunities"),
    COMPETITOR_ANALYSIS("Competitor Analysis", "Competitive intelligence and market positioning"),
    TECHNOLOGY_ADOPTION("Technology Adoption", "Companies adopting new technologies");

    private final String displayName;
    private final String description;

    OpportunityType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
}