package com.psehrawa.oppfinder.discovery.repository;

import com.psehrawa.oppfinder.common.entity.OpportunityEntity;
import com.psehrawa.oppfinder.common.enums.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OpportunityRepository extends JpaRepository<OpportunityEntity, Long>, 
                                             JpaSpecificationExecutor<OpportunityEntity> {

    // Find by external ID and source to prevent duplicates
    Optional<OpportunityEntity> findBySourceAndExternalId(DataSource source, String externalId);

    // Check if opportunity exists
    boolean existsBySourceAndExternalId(DataSource source, String externalId);

    // Find active opportunities
    Page<OpportunityEntity> findByIsActiveTrueOrderByDiscoveredAtDesc(Pageable pageable);

    // Find by status
    Page<OpportunityEntity> findByStatusOrderByDiscoveredAtDesc(OpportunityStatus status, Pageable pageable);

    // Find by type
    Page<OpportunityEntity> findByTypeAndIsActiveTrueOrderByScoreDesc(OpportunityType type, Pageable pageable);

    // Find by country
    Page<OpportunityEntity> findByCountryAndIsActiveTrueOrderByScoreDesc(Country country, Pageable pageable);

    // Find by industry
    Page<OpportunityEntity> findByIndustryAndIsActiveTrueOrderByScoreDesc(Industry industry, Pageable pageable);

    // Find by score range
    Page<OpportunityEntity> findByScoreBetweenAndIsActiveTrueOrderByScoreDesc(
        BigDecimal minScore, BigDecimal maxScore, Pageable pageable);

    // Find recent opportunities
    Page<OpportunityEntity> findByDiscoveredAtAfterAndIsActiveTrueOrderByDiscoveredAtDesc(
        LocalDateTime after, Pageable pageable);

    // Find high-scoring opportunities
    @Query("SELECT o FROM OpportunityEntity o WHERE o.score >= :minScore AND o.isActive = true ORDER BY o.score DESC, o.discoveredAt DESC")
    Page<OpportunityEntity> findHighScoringOpportunities(@Param("minScore") BigDecimal minScore, Pageable pageable);

    // Find opportunities by multiple criteria
    @Query("SELECT o FROM OpportunityEntity o WHERE " +
           "(:types IS NULL OR o.type IN :types) AND " +
           "(:countries IS NULL OR o.country IN :countries) AND " +
           "(:industries IS NULL OR o.industry IN :industries) AND " +
           "(:minScore IS NULL OR o.score >= :minScore) AND " +
           "(:maxScore IS NULL OR o.score <= :maxScore) AND " +
           "(:status IS NULL OR o.status = :status) AND " +
           "o.isActive = true " +
           "ORDER BY o.score DESC, o.discoveredAt DESC")
    Page<OpportunityEntity> findByCriteria(
        @Param("types") List<OpportunityType> types,
        @Param("countries") List<Country> countries,
        @Param("industries") List<Industry> industries,
        @Param("minScore") BigDecimal minScore,
        @Param("maxScore") BigDecimal maxScore,
        @Param("status") OpportunityStatus status,
        Pageable pageable);

    // Find opportunities for scoring (recently discovered, not yet scored)
    @Query("SELECT o FROM OpportunityEntity o WHERE o.score = 0 AND o.discoveredAt >= :since ORDER BY o.discoveredAt ASC")
    List<OpportunityEntity> findOpportunitiesForScoring(@Param("since") LocalDateTime since);

    // Find stale opportunities for cleanup
    @Query("SELECT o FROM OpportunityEntity o WHERE o.lastUpdated < :cutoff AND o.isActive = true")
    List<OpportunityEntity> findStaleOpportunities(@Param("cutoff") LocalDateTime cutoff);

    // Count opportunities by source
    @Query("SELECT o.source, COUNT(o) FROM OpportunityEntity o WHERE o.isActive = true GROUP BY o.source")
    List<Object[]> countOpportunitiesBySource();

    // Count opportunities by type
    @Query("SELECT o.type, COUNT(o) FROM OpportunityEntity o WHERE o.isActive = true GROUP BY o.type")
    List<Object[]> countOpportunitiesByType();

    // Find trending opportunities (high score, recent)
    @Query("SELECT o FROM OpportunityEntity o WHERE " +
           "o.score >= :minScore AND " +
           "o.discoveredAt >= :since AND " +
           "o.isActive = true " +
           "ORDER BY o.score DESC, o.discoveredAt DESC")
    Page<OpportunityEntity> findTrendingOpportunities(
        @Param("minScore") BigDecimal minScore,
        @Param("since") LocalDateTime since,
        Pageable pageable);

    // Full-text search in title and description
    @Query("SELECT o FROM OpportunityEntity o WHERE " +
           "(LOWER(o.title) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(o.description) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(o.companyName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) AND " +
           "o.isActive = true " +
           "ORDER BY o.score DESC, o.discoveredAt DESC")
    Page<OpportunityEntity> searchOpportunities(@Param("searchTerm") String searchTerm, Pageable pageable);
}