package com.psehrawa.oppfinder.discovery.controller;

import com.psehrawa.oppfinder.common.dto.OpportunityDto;
import com.psehrawa.oppfinder.common.dto.OpportunitySearchCriteria;
import com.psehrawa.oppfinder.common.enums.OpportunityStatus;
import com.psehrawa.oppfinder.discovery.service.OpportunityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/opportunities")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*") // Configure properly for production
public class OpportunityController {

    private final OpportunityService opportunityService;

    @PostMapping("/search")
    public ResponseEntity<Page<OpportunityDto>> searchOpportunities(
            @Valid @RequestBody OpportunitySearchCriteria criteria) {
        log.debug("Searching opportunities with criteria: {}", criteria);
        
        Page<OpportunityDto> opportunities = opportunityService.searchOpportunities(criteria);
        return ResponseEntity.ok(opportunities);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OpportunityDto> getOpportunityById(@PathVariable Long id) {
        log.debug("Getting opportunity by id: {}", id);
        
        return opportunityService.findById(id)
            .map(opportunity -> ResponseEntity.ok(opportunity))
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<Page<OpportunityDto>> getAllOpportunities(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "discoveredAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {
        
        log.debug("Getting all opportunities - page: {}, size: {}", page, size);
        
        OpportunitySearchCriteria criteria = OpportunitySearchCriteria.builder()
            .page(page)
            .size(Math.min(size, 100)) // Max 100 items per page
            .sortBy(sortBy)
            .sortDirection(sortDirection)
            .isActive(true)
            .build();
            
        Page<OpportunityDto> opportunities = opportunityService.searchOpportunities(criteria);
        return ResponseEntity.ok(opportunities);
    }

    @GetMapping("/trending")
    public ResponseEntity<Page<OpportunityDto>> getTrendingOpportunities(
            @RequestParam(defaultValue = "70.0") BigDecimal minScore,
            @RequestParam(defaultValue = "24") int hoursBack,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        log.debug("Getting trending opportunities with minScore: {} from last {} hours", minScore, hoursBack);
        
        LocalDateTime since = LocalDateTime.now().minusHours(hoursBack);
        Pageable pageable = PageRequest.of(page, Math.min(size, 50));
        
        Page<OpportunityDto> opportunities = opportunityService.findTrendingOpportunities(minScore, since, pageable);
        return ResponseEntity.ok(opportunities);
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<OpportunityDto> updateOpportunityStatus(
            @PathVariable Long id,
            @RequestParam OpportunityStatus status) {
        
        log.debug("Updating opportunity {} status to {}", id, status);
        
        try {
            OpportunityDto updated = opportunityService.updateOpportunityStatus(id, status);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            log.error("Error updating opportunity status: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{id}/score")
    public ResponseEntity<OpportunityDto> updateOpportunityScore(
            @PathVariable Long id,
            @RequestParam BigDecimal score) {
        
        log.debug("Updating opportunity {} score to {}", id, score);
        
        if (score.compareTo(BigDecimal.ZERO) < 0 || score.compareTo(BigDecimal.valueOf(100)) > 0) {
            return ResponseEntity.badRequest().build();
        }
        
        try {
            OpportunityDto updated = opportunityService.updateOpportunityScore(id, score);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            log.error("Error updating opportunity score: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{id}/engage")
    public ResponseEntity<OpportunityDto> engageWithOpportunity(@PathVariable Long id) {
        log.debug("Engaging with opportunity: {}", id);
        
        try {
            OpportunityDto updated = opportunityService.updateOpportunityStatus(id, OpportunityStatus.ENGAGED);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            log.error("Error engaging with opportunity: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{id}/discard")
    public ResponseEntity<OpportunityDto> discardOpportunity(@PathVariable Long id) {
        log.debug("Discarding opportunity: {}", id);
        
        try {
            OpportunityDto updated = opportunityService.updateOpportunityStatus(id, OpportunityStatus.DISCARDED);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            log.error("Error discarding opportunity: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivateOpportunity(@PathVariable Long id) {
        log.debug("Deactivating opportunity: {}", id);
        
        try {
            opportunityService.deactivateOpportunity(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            log.error("Error deactivating opportunity: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/unscored")
    public ResponseEntity<List<OpportunityDto>> getUnscoredOpportunities(
            @RequestParam(defaultValue = "24") int hoursBack) {
        
        log.debug("Getting unscored opportunities from last {} hours", hoursBack);
        
        LocalDateTime since = LocalDateTime.now().minusHours(hoursBack);
        List<OpportunityDto> opportunities = opportunityService.findOpportunitiesForScoring(since);
        
        return ResponseEntity.ok(opportunities);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleException(Exception e) {
        log.error("Unexpected error in OpportunityController", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body("An unexpected error occurred: " + e.getMessage());
    }
}