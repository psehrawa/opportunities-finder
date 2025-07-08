package com.psehrawa.oppfinder.common.entity;

import com.psehrawa.oppfinder.common.enums.Country;
import com.psehrawa.oppfinder.common.enums.FundingStage;
import com.psehrawa.oppfinder.common.enums.Industry;
import com.psehrawa.oppfinder.common.enums.OpportunityStatus;
import com.psehrawa.oppfinder.common.enums.OpportunityType;
import com.psehrawa.oppfinder.common.enums.DataSource;
import com.psehrawa.oppfinder.common.enums.CompanySize;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "opportunities", indexes = {
    @Index(name = "idx_opportunity_source_external_id", columnList = "source, external_id", unique = true),
    @Index(name = "idx_opportunity_type", columnList = "type"),
    @Index(name = "idx_opportunity_country", columnList = "country"),
    @Index(name = "idx_opportunity_industry", columnList = "industry"),
    @Index(name = "idx_opportunity_score", columnList = "score"),
    @Index(name = "idx_opportunity_discovered_at", columnList = "discovered_at"),
    @Index(name = "idx_opportunity_status", columnList = "status"),
    @Index(name = "idx_opportunity_active", columnList = "is_active")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class OpportunityEntity extends BaseEntity {

    @Column(name = "external_id", nullable = false, length = 255)
    private String externalId;

    @Column(name = "title", nullable = false, length = 500)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false)
    private DataSource source;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private OpportunityType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private OpportunityStatus status = OpportunityStatus.DISCOVERED;

    @Enumerated(EnumType.STRING)
    @Column(name = "country")
    private Country country;

    @Enumerated(EnumType.STRING)
    @Column(name = "industry")
    private Industry industry;

    @Enumerated(EnumType.STRING)
    @Column(name = "funding_stage")
    private FundingStage fundingStage;

    @Column(name = "funding_amount", precision = 15, scale = 2)
    private BigDecimal fundingAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "company_size")
    private CompanySize companySize;

    @Column(name = "score", precision = 5, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal score = BigDecimal.ZERO;

    @ElementCollection
    @CollectionTable(name = "opportunity_tags", joinColumns = @JoinColumn(name = "opportunity_id"))
    @Column(name = "tag")
    private List<String> tags;

    @ElementCollection
    @CollectionTable(name = "opportunity_metadata", joinColumns = @JoinColumn(name = "opportunity_id"))
    @MapKeyColumn(name = "metadata_key")
    @Column(name = "metadata_value", columnDefinition = "TEXT")
    private Map<String, String> metadata;

    @Column(name = "discovered_at", nullable = false)
    @CreationTimestamp
    private LocalDateTime discoveredAt;

    @Column(name = "last_updated", nullable = false)
    @UpdateTimestamp
    private LocalDateTime lastUpdated;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "url", length = 2000)
    private String url;

    @Column(name = "company_name", length = 255)
    private String companyName;

    @Column(name = "location", length = 255)
    private String location;

    @Column(name = "contact_email", length = 255)
    private String contactEmail;

    @Column(name = "confidence_score", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal confidenceScore = BigDecimal.valueOf(50.0);

    @Column(name = "engagement_potential", precision = 5, scale = 2)
    private BigDecimal engagementPotential;

    @PrePersist
    protected void onCreate() {
        if (discoveredAt == null) {
            discoveredAt = LocalDateTime.now();
        }
        if (lastUpdated == null) {
            lastUpdated = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        lastUpdated = LocalDateTime.now();
    }
}