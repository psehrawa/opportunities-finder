package com.psehrawa.oppfinder.common.enums;

import lombok.Getter;

@Getter
public enum OpportunityStatus {
    DISCOVERED("Discovered", "Newly discovered opportunity"),
    ANALYZED("Analyzed", "Opportunity has been analyzed and scored"),
    ENGAGED("Engaged", "User has engaged with this opportunity"),
    DISCARDED("Discarded", "User has discarded this opportunity"),
    MONITORING("Monitoring", "Opportunity is being actively monitored"),
    CONVERTED("Converted", "Opportunity has resulted in business value"),
    EXPIRED("Expired", "Opportunity is no longer relevant"),
    DUPLICATE("Duplicate", "Duplicate of another opportunity");

    private final String displayName;
    private final String description;

    OpportunityStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
}