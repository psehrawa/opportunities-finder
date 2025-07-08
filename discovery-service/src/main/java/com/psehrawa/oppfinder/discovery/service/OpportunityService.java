package com.psehrawa.oppfinder.discovery.service;

import com.psehrawa.oppfinder.common.dto.OpportunityDto;
import com.psehrawa.oppfinder.common.dto.OpportunitySearchCriteria;
import com.psehrawa.oppfinder.common.entity.OpportunityEntity;
import com.psehrawa.oppfinder.common.enums.DataSource;
import com.psehrawa.oppfinder.common.enums.OpportunityStatus;
import com.psehrawa.oppfinder.discovery.mapper.OpportunityMapper;
import com.psehrawa.oppfinder.discovery.repository.OpportunityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class OpportunityService {

    private final OpportunityRepository opportunityRepository;
    private final OpportunityMapper opportunityMapper;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public OpportunityDto saveOpportunity(OpportunityDto opportunityDto) {
        log.debug("Saving opportunity: {}", opportunityDto.getTitle());

        // Check for duplicates
        Optional<OpportunityEntity> existing = opportunityRepository
            .findBySourceAndExternalId(opportunityDto.getSource(), opportunityDto.getExternalId());

        OpportunityEntity entity;
        if (existing.isPresent()) {
            entity = existing.get();
            // Update existing opportunity
            opportunityMapper.updateEntityFromDto(opportunityDto, entity);
            log.debug("Updated existing opportunity: {}", entity.getId());
        } else {
            // Create new opportunity
            entity = opportunityMapper.toEntity(opportunityDto);
            log.debug("Created new opportunity: {}", entity.getTitle());
        }

        entity = opportunityRepository.save(entity);
        OpportunityDto result = opportunityMapper.toDto(entity);

        // Publish event
        String eventType = existing.isPresent() ? "opportunity.updated" : "opportunity.discovered";
        kafkaTemplate.send(eventType, result);

        return result;
    }

    @Transactional(readOnly = true)
    public Page<OpportunityDto> searchOpportunities(OpportunitySearchCriteria criteria) {
        log.debug("Searching opportunities with criteria: {}", criteria);

        Pageable pageable = createPageable(criteria);
        Page<OpportunityEntity> entities;

        if (criteria.getSearchTerm() != null && !criteria.getSearchTerm().trim().isEmpty()) {
            entities = opportunityRepository.searchOpportunities(criteria.getSearchTerm().trim(), pageable);
        } else {
            entities = opportunityRepository.findByCriteria(
                criteria.getTypes(),
                criteria.getCountries(),
                criteria.getIndustries(),
                criteria.getMinScore(),
                criteria.getMaxScore(),
                criteria.getStatuses() != null && !criteria.getStatuses().isEmpty() 
                    ? criteria.getStatuses().get(0) : null,
                pageable
            );
        }

        return entities.map(opportunityMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Optional<OpportunityDto> findById(Long id) {
        return opportunityRepository.findById(id)
            .map(opportunityMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Optional<OpportunityDto> findBySourceAndExternalId(DataSource source, String externalId) {
        return opportunityRepository.findBySourceAndExternalId(source, externalId)
            .map(opportunityMapper::toDto);
    }

    @Transactional(readOnly = true)
    public boolean existsBySourceAndExternalId(DataSource source, String externalId) {
        return opportunityRepository.existsBySourceAndExternalId(source, externalId);
    }

    public OpportunityDto updateOpportunityStatus(Long id, OpportunityStatus status) {
        log.debug("Updating opportunity {} status to {}", id, status);

        OpportunityEntity entity = opportunityRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Opportunity not found: " + id));

        entity.setStatus(status);
        entity = opportunityRepository.save(entity);

        OpportunityDto result = opportunityMapper.toDto(entity);
        kafkaTemplate.send("opportunity.updated", result);

        return result;
    }

    public OpportunityDto updateOpportunityScore(Long id, BigDecimal score) {
        log.debug("Updating opportunity {} score to {}", id, score);

        OpportunityEntity entity = opportunityRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Opportunity not found: " + id));

        entity.setScore(score);
        entity = opportunityRepository.save(entity);

        OpportunityDto result = opportunityMapper.toDto(entity);
        kafkaTemplate.send("opportunity.scored", result);

        return result;
    }

    @Transactional(readOnly = true)
    public List<OpportunityDto> findOpportunitiesForScoring(LocalDateTime since) {
        return opportunityRepository.findOpportunitiesForScoring(since)
            .stream()
            .map(opportunityMapper::toDto)
            .toList();
    }

    @Transactional(readOnly = true)
    public Page<OpportunityDto> findTrendingOpportunities(BigDecimal minScore, LocalDateTime since, Pageable pageable) {
        return opportunityRepository.findTrendingOpportunities(minScore, since, pageable)
            .map(opportunityMapper::toDto);
    }

    public void deactivateOpportunity(Long id) {
        log.debug("Deactivating opportunity: {}", id);

        OpportunityEntity entity = opportunityRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Opportunity not found: " + id));

        entity.setIsActive(false);
        opportunityRepository.save(entity);
    }

    public void cleanupStaleOpportunities(LocalDateTime cutoff) {
        log.info("Cleaning up stale opportunities older than {}", cutoff);

        List<OpportunityEntity> staleOpportunities = opportunityRepository.findStaleOpportunities(cutoff);
        for (OpportunityEntity opportunity : staleOpportunities) {
            opportunity.setIsActive(false);
        }

        if (!staleOpportunities.isEmpty()) {
            opportunityRepository.saveAll(staleOpportunities);
            log.info("Deactivated {} stale opportunities", staleOpportunities.size());
        }
    }

    private Pageable createPageable(OpportunitySearchCriteria criteria) {
        Sort sort = Sort.by(
            "DESC".equalsIgnoreCase(criteria.getSortDirection()) 
                ? Sort.Direction.DESC 
                : Sort.Direction.ASC,
            criteria.getSortBy()
        );

        return PageRequest.of(
            criteria.getPage(),
            Math.min(criteria.getSize(), 100), // Max 100 items per page
            sort
        );
    }
}