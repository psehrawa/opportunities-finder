package com.psehrawa.oppfinder.discovery.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.psehrawa.oppfinder.common.dto.OpportunityDto;
import com.psehrawa.oppfinder.common.dto.OpportunitySearchCriteria;
import com.psehrawa.oppfinder.common.enums.*;
import com.psehrawa.oppfinder.discovery.config.TestConfig;
import com.psehrawa.oppfinder.discovery.service.OpportunityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = OpportunityController.class, excludeAutoConfiguration = {
    org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration.class,
    org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration.class,
    org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration.class
})
@Import(TestConfig.class)
@TestPropertySource(properties = {
    "spring.profiles.active=test",
    "spring.jpa.hibernate.ddl-auto=none",
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration"
})
class OpportunityControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OpportunityService opportunityService;

    private OpportunityDto testOpportunity;

    @BeforeEach
    void setUp() {
        testOpportunity = OpportunityDto.builder()
            .id(1L)
            .externalId("test-123")
            .title("Test Opportunity")
            .description("Test Description")
            .source(DataSource.GITHUB)
            .type(OpportunityType.TECHNOLOGY_TREND)
            .status(OpportunityStatus.DISCOVERED)
            .country(Country.US)
            .industry(Industry.FINTECH)
            .score(BigDecimal.valueOf(75.0))
            .discoveredAt(LocalDateTime.now())
            .isActive(true)
            .build();
    }

    @Test
    void searchOpportunities_ValidCriteria_ShouldReturnPageOfOpportunities() throws Exception {
        // Given
        OpportunitySearchCriteria criteria = OpportunitySearchCriteria.builder()
            .page(0)
            .size(20)
            .sortBy("discoveredAt")
            .sortDirection("DESC")
            .build();

        Page<OpportunityDto> mockPage = new PageImpl<>(List.of(testOpportunity));
        when(opportunityService.searchOpportunities(any(OpportunitySearchCriteria.class)))
            .thenReturn(mockPage);

        // When & Then
        mockMvc.perform(post("/api/v1/opportunities/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(criteria)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.content[0].id").value(1))
            .andExpect(jsonPath("$.content[0].title").value("Test Opportunity"));
    }

    @Test
    void getOpportunityById_ExistingOpportunity_ShouldReturnOpportunity() throws Exception {
        // Given
        when(opportunityService.findById(1L)).thenReturn(Optional.of(testOpportunity));

        // When & Then
        mockMvc.perform(get("/api/v1/opportunities/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.title").value("Test Opportunity"))
            .andExpect(jsonPath("$.source").value("GITHUB"));
    }

    @Test
    void getOpportunityById_NonExistingOpportunity_ShouldReturnNotFound() throws Exception {
        // Given
        when(opportunityService.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(get("/api/v1/opportunities/999"))
            .andExpect(status().isNotFound());
    }

    @Test
    void getAllOpportunities_DefaultParameters_ShouldReturnPageOfOpportunities() throws Exception {
        // Given
        Page<OpportunityDto> mockPage = new PageImpl<>(List.of(testOpportunity));
        when(opportunityService.searchOpportunities(any(OpportunitySearchCriteria.class)))
            .thenReturn(mockPage);

        // When & Then
        mockMvc.perform(get("/api/v1/opportunities"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.content[0].id").value(1));
    }

    @Test
    void updateOpportunityStatus_ValidRequest_ShouldReturnUpdatedOpportunity() throws Exception {
        // Given
        OpportunityDto updatedOpportunity = OpportunityDto.builder()
            .id(1L)
            .title("Test Opportunity")
            .status(OpportunityStatus.ENGAGED)
            .build();

        when(opportunityService.updateOpportunityStatus(1L, OpportunityStatus.ENGAGED))
            .thenReturn(updatedOpportunity);

        // When & Then
        mockMvc.perform(put("/api/v1/opportunities/1/status")
                .param("status", "ENGAGED"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ENGAGED"));
    }

    @Test
    void updateOpportunityScore_ValidScore_ShouldReturnUpdatedOpportunity() throws Exception {
        // Given
        BigDecimal newScore = BigDecimal.valueOf(85.0);
        OpportunityDto updatedOpportunity = OpportunityDto.builder()
            .id(1L)
            .title("Test Opportunity")
            .score(newScore)
            .build();

        when(opportunityService.updateOpportunityScore(1L, newScore))
            .thenReturn(updatedOpportunity);

        // When & Then
        mockMvc.perform(put("/api/v1/opportunities/1/score")
                .param("score", "85.0"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.score").value(85.0));
    }

    @Test
    void updateOpportunityScore_InvalidScore_ShouldReturnBadRequest() throws Exception {
        // When & Then
        mockMvc.perform(put("/api/v1/opportunities/1/score")
                .param("score", "150.0"))
            .andExpect(status().isBadRequest());

        mockMvc.perform(put("/api/v1/opportunities/1/score")
                .param("score", "-10.0"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void engageWithOpportunity_ValidRequest_ShouldReturnEngagedOpportunity() throws Exception {
        // Given
        OpportunityDto engagedOpportunity = OpportunityDto.builder()
            .id(1L)
            .title("Test Opportunity")
            .status(OpportunityStatus.ENGAGED)
            .build();

        when(opportunityService.updateOpportunityStatus(1L, OpportunityStatus.ENGAGED))
            .thenReturn(engagedOpportunity);

        // When & Then
        mockMvc.perform(post("/api/v1/opportunities/1/engage"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ENGAGED"));
    }

    @Test
    void discardOpportunity_ValidRequest_ShouldReturnDiscardedOpportunity() throws Exception {
        // Given
        OpportunityDto discardedOpportunity = OpportunityDto.builder()
            .id(1L)
            .title("Test Opportunity")
            .status(OpportunityStatus.DISCARDED)
            .build();

        when(opportunityService.updateOpportunityStatus(1L, OpportunityStatus.DISCARDED))
            .thenReturn(discardedOpportunity);

        // When & Then
        mockMvc.perform(post("/api/v1/opportunities/1/discard"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("DISCARDED"));
    }

    @Test
    void getUnscoredOpportunities_ValidRequest_ShouldReturnList() throws Exception {
        // Given
        when(opportunityService.findOpportunitiesForScoring(any(LocalDateTime.class)))
            .thenReturn(List.of(testOpportunity));

        // When & Then
        mockMvc.perform(get("/api/v1/opportunities/unscored")
                .param("hoursBack", "24"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void getTrendingOpportunities_ValidRequest_ShouldReturnPageOfOpportunities() throws Exception {
        // Given
        Page<OpportunityDto> mockPage = new PageImpl<>(List.of(testOpportunity));
        when(opportunityService.findTrendingOpportunities(any(BigDecimal.class), any(LocalDateTime.class), any()))
            .thenReturn(mockPage);

        // When & Then
        mockMvc.perform(get("/api/v1/opportunities/trending")
                .param("minScore", "70.0")
                .param("hoursBack", "24"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.content[0].id").value(1));
    }
}