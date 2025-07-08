package com.psehrawa.oppfinder.discovery.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.psehrawa.oppfinder.common.dto.OpportunityDto;
import com.psehrawa.oppfinder.common.enums.Country;
import com.psehrawa.oppfinder.discovery.config.TestConfig;
import com.psehrawa.oppfinder.discovery.service.DataSourceOrchestrator;
import com.psehrawa.oppfinder.discovery.service.DiscoverySchedulerService;
import com.psehrawa.oppfinder.discovery.service.datasource.HealthStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = DiscoveryController.class, excludeAutoConfiguration = {
    org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration.class,
    org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration.class,
    org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration.class
})
@Import(TestConfig.class)
@TestPropertySource(properties = {
    "spring.profiles.active=test"
})
class DiscoveryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DataSourceOrchestrator dataSourceOrchestrator;

    @MockBean
    private DiscoverySchedulerService schedulerService;

    @Test
    void triggerDiscovery_ValidRequest_ShouldReturnSuccess() throws Exception {
        // Given
        when(dataSourceOrchestrator.discoverOpportunitiesFromAllSources(
            any(), any(), any()
        )).thenReturn(CompletableFuture.completedFuture(5));

        // When & Then
        mockMvc.perform(post("/api/v1/discovery/trigger")
                .param("limitPerSource", "10")
                .param("hoursBack", "24"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("completed"))
            .andExpect(jsonPath("$.opportunitiesDiscovered").value(5));
    }

    @Test
    void triggerSourceDiscovery_ValidSource_ShouldReturnOpportunities() throws Exception {
        // Given
        OpportunityDto mockOpportunity = OpportunityDto.builder()
            .id(1L)
            .title("Test Opportunity")
            .build();

        when(dataSourceOrchestrator.discoverFromSource(
            eq("github"), any(), any(), any()
        )).thenReturn(CompletableFuture.completedFuture(List.of(mockOpportunity)));

        // When & Then
        mockMvc.perform(post("/api/v1/discovery/trigger/github")
                .param("limit", "5"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("completed"))
            .andExpect(jsonPath("$.source").value("github"))
            .andExpect(jsonPath("$.opportunitiesDiscovered").value(1))
            .andExpect(jsonPath("$.opportunities[0].id").value(1));
    }

    @Test
    void triggerScoring_ValidRequest_ShouldReturnSuccess() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/v1/discovery/scoring/trigger"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("triggered"))
            .andExpect(jsonPath("$.message").value("Scoring process started"));
    }

    @Test
    void getDiscoveryHealth_AllHealthy_ShouldReturnUp() throws Exception {
        // Given
        Map<String, HealthStatus> healthMap = Map.of(
            "GITHUB", new HealthStatus(true, "UP", "API responding", LocalDateTime.now()),
            "HACKER_NEWS", new HealthStatus(true, "UP", "API responding", LocalDateTime.now())
        );
        List<String> enabledSources = List.of("GITHUB", "HACKER_NEWS");

        when(dataSourceOrchestrator.getDataSourcesHealth()).thenReturn(healthMap);
        when(dataSourceOrchestrator.getEnabledDataSources()).thenReturn(enabledSources);

        // When & Then
        mockMvc.perform(get("/api/v1/discovery/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("UP"))
            .andExpect(jsonPath("$.enabledSources").isArray())
            .andExpect(jsonPath("$.enabledSources[0]").value("GITHUB"));
    }

    @Test
    void getDiscoveryHealth_SomeUnhealthy_ShouldReturnDegraded() throws Exception {
        // Given
        Map<String, HealthStatus> healthMap = Map.of(
            "GITHUB", new HealthStatus(true, "UP", "API responding", LocalDateTime.now()),
            "REDDIT", new HealthStatus(false, "DOWN", "Rate limited", LocalDateTime.now())
        );
        List<String> enabledSources = List.of("GITHUB", "REDDIT");

        when(dataSourceOrchestrator.getDataSourcesHealth()).thenReturn(healthMap);
        when(dataSourceOrchestrator.getEnabledDataSources()).thenReturn(enabledSources);

        // When & Then
        mockMvc.perform(get("/api/v1/discovery/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("DEGRADED"))
            .andExpect(jsonPath("$.dataSourcesHealth.GITHUB.isHealthy").value(true))
            .andExpect(jsonPath("$.dataSourcesHealth.REDDIT.isHealthy").value(false));
    }

    @Test
    void getDataSources_ValidRequest_ShouldReturnSourceInfo() throws Exception {
        // Given
        List<String> enabledSources = List.of("GITHUB", "HACKER_NEWS");
        Map<String, HealthStatus> healthMap = Map.of(
            "GITHUB", new HealthStatus(true, "UP", "OK", LocalDateTime.now()),
            "HACKER_NEWS", new HealthStatus(true, "UP", "OK", LocalDateTime.now())
        );

        when(dataSourceOrchestrator.getEnabledDataSources()).thenReturn(enabledSources);
        when(dataSourceOrchestrator.getDataSourcesHealth()).thenReturn(healthMap);

        // When & Then
        mockMvc.perform(get("/api/v1/discovery/sources"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.enabledSources").isArray())
            .andExpect(jsonPath("$.totalSources").value(2))
            .andExpect(jsonPath("$.healthySources").value(2));
    }

    @Test
    void getDiscoveryStats_ValidRequest_ShouldReturnStats() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/discovery/stats"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void triggerDiscovery_WithCountryFilter_ShouldPassCorrectParameters() throws Exception {
        // Given
        when(dataSourceOrchestrator.discoverOpportunitiesFromAllSources(
            argThat(countries -> countries != null && countries.contains(Country.US)),
            any(),
            eq(20)
        )).thenReturn(CompletableFuture.completedFuture(3));

        // When & Then
        mockMvc.perform(post("/api/v1/discovery/trigger")
                .param("countries", "US", "GB")
                .param("limitPerSource", "20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.opportunitiesDiscovered").value(3));
    }

    @Test
    void triggerDiscovery_DiscoveryFails_ShouldReturnError() throws Exception {
        // Given
        CompletableFuture<Integer> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("Discovery failed"));
        
        when(dataSourceOrchestrator.discoverOpportunitiesFromAllSources(
            any(), any(), any()
        )).thenReturn(failedFuture);

        // When & Then
        mockMvc.perform(post("/api/v1/discovery/trigger"))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.status").value("error"))
            .andExpect(jsonPath("$.message").exists());
    }
}