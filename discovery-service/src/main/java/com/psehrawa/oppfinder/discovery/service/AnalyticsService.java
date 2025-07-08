package com.psehrawa.oppfinder.discovery.service;

import com.psehrawa.oppfinder.common.enums.DataSource;
import com.psehrawa.oppfinder.common.enums.Industry;
import com.psehrawa.oppfinder.common.enums.OpportunityStatus;
import com.psehrawa.oppfinder.common.enums.OpportunityType;
import com.psehrawa.oppfinder.discovery.repository.OpportunityRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class AnalyticsService {

    private final OpportunityRepository opportunityRepository;
    private final JdbcTemplate jdbcTemplate;

    /**
     * Get overall analytics dashboard
     */
    @Cacheable(value = "analytics", key = "'dashboard'")
    public DashboardAnalytics getDashboardAnalytics() {
        DashboardAnalytics analytics = new DashboardAnalytics();
        
        // Total opportunities
        analytics.setTotalOpportunities(opportunityRepository.count());
        
        // Active opportunities
        analytics.setActiveOpportunities(
            jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM opportunities WHERE is_active = true", 
                Long.class
            )
        );
        
        // Average score
        analytics.setAverageScore(
            jdbcTemplate.queryForObject(
                "SELECT AVG(score) FROM opportunities WHERE is_active = true", 
                BigDecimal.class
            )
        );
        
        // Opportunities by status
        Map<String, Long> statusCounts = jdbcTemplate.query(
            "SELECT status, COUNT(*) as count FROM opportunities GROUP BY status",
            (rs, rowNum) -> Map.entry(rs.getString("status"), rs.getLong("count"))
        ).stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        analytics.setOpportunitiesByStatus(statusCounts);
        
        // Opportunities by source
        Map<String, Long> sourceCounts = jdbcTemplate.query(
            "SELECT source, COUNT(*) as count FROM opportunities GROUP BY source",
            (rs, rowNum) -> Map.entry(rs.getString("source"), rs.getLong("count"))
        ).stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        analytics.setOpportunitiesBySource(sourceCounts);
        
        // Top industries
        analytics.setTopIndustries(getTopIndustries(5));
        
        // Recent high-score opportunities
        analytics.setRecentHighScoreOpportunities(getRecentHighScoreCount());
        
        // Growth metrics
        analytics.setGrowthMetrics(calculateGrowthMetrics());
        
        return analytics;
    }

    /**
     * Get time series data for opportunities
     */
    @Cacheable(value = "analytics", key = "'timeseries-' + #days")
    public List<TimeSeriesData> getOpportunityTimeSeries(int days) {
        LocalDateTime startDate = LocalDateTime.now().minusDays(days);
        
        String query = """
            SELECT DATE(discovered_at) as date, 
                   COUNT(*) as count,
                   AVG(score) as avg_score
            FROM opportunities 
            WHERE discovered_at >= ?
            GROUP BY DATE(discovered_at)
            ORDER BY date
        """;
        
        return jdbcTemplate.query(query, 
            ps -> ps.setObject(1, startDate),
            (rs, rowNum) -> {
                TimeSeriesData data = new TimeSeriesData();
                data.setDate(rs.getDate("date").toLocalDate());
                data.setCount(rs.getLong("count"));
                data.setAverageScore(rs.getBigDecimal("avg_score"));
                return data;
            }
        );
    }

    /**
     * Get conversion funnel analytics
     */
    public ConversionFunnel getConversionFunnel() {
        ConversionFunnel funnel = new ConversionFunnel();
        
        Map<String, Long> statusCounts = jdbcTemplate.query(
            "SELECT status, COUNT(*) as count FROM opportunities GROUP BY status",
            (rs, rowNum) -> Map.entry(
                OpportunityStatus.valueOf(rs.getString("status")), 
                rs.getLong("count")
            )
        ).stream().collect(Collectors.toMap(
            entry -> entry.getKey().toString(), 
            Map.Entry::getValue
        ));
        
        long total = statusCounts.values().stream().mapToLong(Long::longValue).sum();
        
        funnel.setDiscovered(statusCounts.getOrDefault("DISCOVERED", 0L));
        funnel.setAnalyzed(statusCounts.getOrDefault("ANALYZED", 0L));
        funnel.setEngaged(statusCounts.getOrDefault("ENGAGED", 0L));
        funnel.setConverted(statusCounts.getOrDefault("CONVERTED", 0L));
        
        if (total > 0) {
            funnel.setDiscoveredRate(100.0);
            funnel.setAnalyzedRate((funnel.getAnalyzed() * 100.0) / funnel.getDiscovered());
            funnel.setEngagedRate((funnel.getEngaged() * 100.0) / funnel.getAnalyzed());
            funnel.setConvertedRate((funnel.getConverted() * 100.0) / funnel.getEngaged());
        }
        
        return funnel;
    }

    /**
     * Get source performance metrics
     */
    public List<SourcePerformance> getSourcePerformance() {
        String query = """
            SELECT source,
                   COUNT(*) as total_opportunities,
                   AVG(score) as avg_score,
                   SUM(CASE WHEN status = 'CONVERTED' THEN 1 ELSE 0 END) as conversions,
                   AVG(confidence_score) as avg_confidence
            FROM opportunities
            GROUP BY source
            ORDER BY avg_score DESC
        """;
        
        return jdbcTemplate.query(query, (rs, rowNum) -> {
            SourcePerformance perf = new SourcePerformance();
            perf.setSource(DataSource.valueOf(rs.getString("source")));
            perf.setTotalOpportunities(rs.getLong("total_opportunities"));
            perf.setAverageScore(rs.getBigDecimal("avg_score"));
            perf.setConversions(rs.getLong("conversions"));
            perf.setAverageConfidence(rs.getBigDecimal("avg_confidence"));
            perf.setConversionRate(
                perf.getTotalOpportunities() > 0 
                    ? (perf.getConversions() * 100.0) / perf.getTotalOpportunities() 
                    : 0.0
            );
            return perf;
        });
    }

    private List<IndustryMetric> getTopIndustries(int limit) {
        String query = """
            SELECT industry, 
                   COUNT(*) as count,
                   AVG(score) as avg_score
            FROM opportunities 
            WHERE industry IS NOT NULL
            GROUP BY industry
            ORDER BY count DESC
            LIMIT ?
        """;
        
        return jdbcTemplate.query(query, 
            ps -> ps.setInt(1, limit),
            (rs, rowNum) -> {
                IndustryMetric metric = new IndustryMetric();
                metric.setIndustry(Industry.valueOf(rs.getString("industry")));
                metric.setCount(rs.getLong("count"));
                metric.setAverageScore(rs.getBigDecimal("avg_score"));
                return metric;
            }
        );
    }

    private long getRecentHighScoreCount() {
        return jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM opportunities WHERE score >= 80 AND discovered_at >= ?",
            Long.class,
            LocalDateTime.now().minusDays(7)
        );
    }

    private GrowthMetrics calculateGrowthMetrics() {
        GrowthMetrics metrics = new GrowthMetrics();
        
        // This week vs last week
        LocalDateTime thisWeekStart = LocalDateTime.now().minusDays(7);
        LocalDateTime lastWeekStart = LocalDateTime.now().minusDays(14);
        
        Long thisWeekCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM opportunities WHERE discovered_at >= ?",
            Long.class, thisWeekStart
        );
        
        Long lastWeekCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM opportunities WHERE discovered_at >= ? AND discovered_at < ?",
            Long.class, lastWeekStart, thisWeekStart
        );
        
        metrics.setWeeklyGrowthRate(
            lastWeekCount > 0 
                ? ((thisWeekCount - lastWeekCount) * 100.0) / lastWeekCount 
                : 100.0
        );
        
        // Monthly growth
        LocalDateTime thisMonthStart = LocalDateTime.now().minusDays(30);
        LocalDateTime lastMonthStart = LocalDateTime.now().minusDays(60);
        
        Long thisMonthCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM opportunities WHERE discovered_at >= ?",
            Long.class, thisMonthStart
        );
        
        Long lastMonthCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM opportunities WHERE discovered_at >= ? AND discovered_at < ?",
            Long.class, lastMonthStart, thisMonthStart
        );
        
        metrics.setMonthlyGrowthRate(
            lastMonthCount > 0 
                ? ((thisMonthCount - lastMonthCount) * 100.0) / lastMonthCount 
                : 100.0
        );
        
        return metrics;
    }

    // Analytics DTOs
    @Data
    public static class DashboardAnalytics {
        private Long totalOpportunities;
        private Long activeOpportunities;
        private BigDecimal averageScore;
        private Map<String, Long> opportunitiesByStatus;
        private Map<String, Long> opportunitiesBySource;
        private List<IndustryMetric> topIndustries;
        private Long recentHighScoreOpportunities;
        private GrowthMetrics growthMetrics;
    }

    @Data
    public static class TimeSeriesData {
        private java.time.LocalDate date;
        private Long count;
        private BigDecimal averageScore;
    }

    @Data
    public static class ConversionFunnel {
        private Long discovered;
        private Long analyzed;
        private Long engaged;
        private Long converted;
        private Double discoveredRate;
        private Double analyzedRate;
        private Double engagedRate;
        private Double convertedRate;
    }

    @Data
    public static class SourcePerformance {
        private DataSource source;
        private Long totalOpportunities;
        private BigDecimal averageScore;
        private Long conversions;
        private Double conversionRate;
        private BigDecimal averageConfidence;
    }

    @Data
    public static class IndustryMetric {
        private Industry industry;
        private Long count;
        private BigDecimal averageScore;
    }

    @Data
    public static class GrowthMetrics {
        private Double weeklyGrowthRate;
        private Double monthlyGrowthRate;
    }
}