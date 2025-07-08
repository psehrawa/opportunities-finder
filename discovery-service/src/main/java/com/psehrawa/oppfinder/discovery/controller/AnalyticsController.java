package com.psehrawa.oppfinder.discovery.controller;

import com.psehrawa.oppfinder.discovery.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
@Slf4j
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
    public ResponseEntity<AnalyticsService.DashboardAnalytics> getDashboardAnalytics() {
        log.debug("Getting dashboard analytics");
        return ResponseEntity.ok(analyticsService.getDashboardAnalytics());
    }

    @GetMapping("/time-series")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
    public ResponseEntity<?> getTimeSeries(@RequestParam(defaultValue = "30") int days) {
        log.debug("Getting time series data for {} days", days);
        return ResponseEntity.ok(analyticsService.getOpportunityTimeSeries(days));
    }

    @GetMapping("/conversion-funnel")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
    public ResponseEntity<AnalyticsService.ConversionFunnel> getConversionFunnel() {
        log.debug("Getting conversion funnel analytics");
        return ResponseEntity.ok(analyticsService.getConversionFunnel());
    }

    @GetMapping("/source-performance")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
    public ResponseEntity<?> getSourcePerformance() {
        log.debug("Getting source performance metrics");
        return ResponseEntity.ok(analyticsService.getSourcePerformance());
    }
}