package com.psehrawa.oppfinder.common.dto;

import com.psehrawa.oppfinder.common.enums.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OpportunitySearchCriteria {

    private List<OpportunityType> types;
    private List<OpportunityStatus> statuses;
    private List<DataSource> sources;
    private List<Country> countries;
    private List<Industry> industries;
    private List<FundingStage> fundingStages;
    private List<CompanySize> companySizes;

    private BigDecimal minScore;
    private BigDecimal maxScore;

    private BigDecimal minFundingAmount;
    private BigDecimal maxFundingAmount;

    private LocalDateTime discoveredAfter;
    private LocalDateTime discoveredBefore;

    private String searchTerm;
    private List<String> tags;

    private Boolean isActive;

    // Pagination
    @Builder.Default
    private Integer page = 0;

    @Builder.Default
    private Integer size = 20;

    // Sorting
    @Builder.Default
    private String sortBy = "discoveredAt";

    @Builder.Default
    private String sortDirection = "DESC";
}