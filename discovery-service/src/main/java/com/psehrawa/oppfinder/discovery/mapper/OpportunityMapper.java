package com.psehrawa.oppfinder.discovery.mapper;

import com.psehrawa.oppfinder.common.dto.OpportunityDto;
import com.psehrawa.oppfinder.common.entity.OpportunityEntity;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class OpportunityMapper {

    public OpportunityDto toDto(OpportunityEntity entity) {
        if (entity == null) {
            return null;
        }

        return OpportunityDto.builder()
            .id(entity.getId())
            .externalId(entity.getExternalId())
            .title(entity.getTitle())
            .description(entity.getDescription())
            .source(entity.getSource())
            .type(entity.getType())
            .status(entity.getStatus())
            .country(entity.getCountry())
            .industry(entity.getIndustry())
            .fundingStage(entity.getFundingStage())
            .fundingAmount(entity.getFundingAmount())
            .companySize(entity.getCompanySize())
            .score(entity.getScore())
            .tags(entity.getTags())
            .metadata(entity.getMetadata())
            .discoveredAt(entity.getDiscoveredAt())
            .lastUpdated(entity.getLastUpdated())
            .isActive(entity.getIsActive())
            .url(entity.getUrl())
            .companyName(entity.getCompanyName())
            .location(entity.getLocation())
            .contactEmail(entity.getContactEmail())
            .confidenceScore(entity.getConfidenceScore())
            .engagementPotential(entity.getEngagementPotential())
            .version(entity.getVersion())
            .build();
    }

    public OpportunityEntity toEntity(OpportunityDto dto) {
        if (dto == null) {
            return null;
        }

        OpportunityEntity.OpportunityEntityBuilder builder = OpportunityEntity.builder();
        
        if (dto.getId() != null) {
            // Note: ID should typically not be set when creating new entities
            // But we'll include it for completeness in mapping
        }
        
        return builder
            .externalId(dto.getExternalId())
            .title(dto.getTitle())
            .description(dto.getDescription())
            .source(dto.getSource())
            .type(dto.getType())
            .status(dto.getStatus())
            .country(dto.getCountry())
            .industry(dto.getIndustry())
            .fundingStage(dto.getFundingStage())
            .fundingAmount(dto.getFundingAmount())
            .companySize(dto.getCompanySize())
            .score(dto.getScore())
            .tags(dto.getTags())
            .metadata(dto.getMetadata())
            .discoveredAt(dto.getDiscoveredAt())
            .lastUpdated(dto.getLastUpdated())
            .isActive(dto.getIsActive())
            .url(dto.getUrl())
            .companyName(dto.getCompanyName())
            .location(dto.getLocation())
            .contactEmail(dto.getContactEmail())
            .confidenceScore(dto.getConfidenceScore())
            .engagementPotential(dto.getEngagementPotential())
            .build();
    }

    public void updateEntityFromDto(OpportunityDto dto, OpportunityEntity entity) {
        if (dto == null || entity == null) {
            return;
        }

        // Update fields that can change
        entity.setTitle(dto.getTitle());
        entity.setDescription(dto.getDescription());
        entity.setType(dto.getType());
        entity.setCountry(dto.getCountry());
        entity.setIndustry(dto.getIndustry());
        entity.setFundingStage(dto.getFundingStage());
        entity.setFundingAmount(dto.getFundingAmount());
        entity.setCompanySize(dto.getCompanySize());
        entity.setTags(dto.getTags());
        entity.setMetadata(dto.getMetadata());
        entity.setUrl(dto.getUrl());
        entity.setCompanyName(dto.getCompanyName());
        entity.setLocation(dto.getLocation());
        entity.setContactEmail(dto.getContactEmail());
        entity.setConfidenceScore(dto.getConfidenceScore());
        entity.setEngagementPotential(dto.getEngagementPotential());
        
        // Update timestamp automatically handled by @PreUpdate
        entity.setLastUpdated(LocalDateTime.now());
    }
}