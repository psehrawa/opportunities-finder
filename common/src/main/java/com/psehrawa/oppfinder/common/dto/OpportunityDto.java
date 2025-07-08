package com.psehrawa.oppfinder.common.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.psehrawa.oppfinder.common.enums.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OpportunityDto {

    private Long id;

    @NotBlank(message = "External ID is required")
    @Size(max = 255, message = "External ID must not exceed 255 characters")
    private String externalId;

    @NotBlank(message = "Title is required")
    @Size(max = 500, message = "Title must not exceed 500 characters")
    private String title;

    @Size(max = 5000, message = "Description must not exceed 5000 characters")
    private String description;

    @NotNull(message = "Source is required")
    private DataSource source;

    @NotNull(message = "Type is required")
    private OpportunityType type;

    @NotNull(message = "Status is required")
    @Builder.Default
    private OpportunityStatus status = OpportunityStatus.DISCOVERED;

    private Country country;

    private Industry industry;

    private FundingStage fundingStage;

    @DecimalMin(value = "0.0", message = "Funding amount must be non-negative")
    @DecimalMax(value = "999999999999.99", message = "Funding amount is too large")
    private BigDecimal fundingAmount;

    private CompanySize companySize;

    @DecimalMin(value = "0.0", message = "Score must be non-negative")
    @DecimalMax(value = "100.0", message = "Score must not exceed 100")
    @Builder.Default
    private BigDecimal score = BigDecimal.ZERO;

    private List<String> tags;

    private Map<String, String> metadata;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime discoveredAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime lastUpdated;

    @Builder.Default
    private Boolean isActive = true;

    @Size(max = 2000, message = "URL must not exceed 2000 characters")
    private String url;

    @Size(max = 255, message = "Company name must not exceed 255 characters")
    private String companyName;

    @Size(max = 255, message = "Location must not exceed 255 characters")
    private String location;

    @Email(message = "Contact email must be valid")
    @Size(max = 255, message = "Contact email must not exceed 255 characters")
    private String contactEmail;

    @DecimalMin(value = "0.0", message = "Confidence score must be non-negative")
    @DecimalMax(value = "100.0", message = "Confidence score must not exceed 100")
    @Builder.Default
    private BigDecimal confidenceScore = BigDecimal.valueOf(50.0);

    @DecimalMin(value = "0.0", message = "Engagement potential must be non-negative")
    @DecimalMax(value = "100.0", message = "Engagement potential must not exceed 100")
    private BigDecimal engagementPotential;

    private Long version;
}