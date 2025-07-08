package com.psehrawa.oppfinder.common.enums;

import lombok.Getter;

@Getter
public enum FundingStage {
    PRE_SEED("Pre-Seed", "Pre-seed funding stage", 0, 100000),
    SEED("Seed", "Seed funding stage", 100000, 2000000),
    SERIES_A("Series A", "Series A funding round", 2000000, 15000000),
    SERIES_B("Series B", "Series B funding round", 15000000, 50000000),
    SERIES_C("Series C", "Series C funding round", 50000000, 100000000),
    SERIES_D_PLUS("Series D+", "Series D and later rounds", 100000000, Long.MAX_VALUE),
    IPO("IPO", "Initial Public Offering", 0, Long.MAX_VALUE),
    ACQUISITION("Acquisition", "Acquired by another company", 0, Long.MAX_VALUE),
    PRIVATE_EQUITY("Private Equity", "Private equity funding", 10000000, Long.MAX_VALUE),
    DEBT_FINANCING("Debt Financing", "Debt or loan financing", 0, Long.MAX_VALUE),
    GRANT("Grant", "Government or institutional grant", 0, 10000000),
    CROWDFUNDING("Crowdfunding", "Crowdfunded project", 0, 5000000),
    REVENUE_BASED("Revenue Based", "Revenue-based financing", 100000, 10000000),
    UNKNOWN("Unknown", "Funding stage not specified", 0, Long.MAX_VALUE);

    private final String displayName;
    private final String description;
    private final long minAmount;
    private final long maxAmount;

    FundingStage(String displayName, String description, long minAmount, long maxAmount) {
        this.displayName = displayName;
        this.description = description;
        this.minAmount = minAmount;
        this.maxAmount = maxAmount;
    }

    public static FundingStage fromAmount(long amount) {
        for (FundingStage stage : values()) {
            if (amount >= stage.minAmount && amount <= stage.maxAmount && stage != UNKNOWN) {
                return stage;
            }
        }
        return UNKNOWN;
    }
}