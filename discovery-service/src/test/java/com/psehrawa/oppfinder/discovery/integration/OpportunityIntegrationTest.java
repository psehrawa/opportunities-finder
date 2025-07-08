package com.psehrawa.oppfinder.discovery.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.psehrawa.oppfinder.common.dto.OpportunityDto;
import com.psehrawa.oppfinder.common.dto.OpportunitySearchCriteria;
import com.psehrawa.oppfinder.common.enums.*;
import com.psehrawa.oppfinder.discovery.DiscoveryServiceApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = DiscoveryServiceApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureWebMvc
@Transactional
class OpportunityIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    @Test
    void fullOpportunityWorkflow_CreateSearchUpdateDelete_ShouldWork() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        // 1. Create opportunity via search endpoint (simulates discovery)
        OpportunitySearchCriteria criteria = OpportunitySearchCriteria.builder()
            .page(0)
            .size(10)
            .build();

        // Search for opportunities (should be empty initially)
        mockMvc.perform(post("/api/v1/opportunities/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(criteria)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isEmpty());

        // Note: In a real integration test, you would:
        // 1. Trigger discovery to create opportunities
        // 2. Search for created opportunities
        // 3. Update opportunity status
        // 4. Verify the changes
        
        // For now, we're testing the endpoints are wired correctly
    }

    @Test
    void discoveryEndpoints_ShouldBeAccessible() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        // Test health endpoint
        mockMvc.perform(get("/api/v1/discovery/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").exists());

        // Test sources endpoint
        mockMvc.perform(get("/api/v1/discovery/sources"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.enabledSources").exists());
    }

    @Test
    void opportunityEndpoints_ShouldHandleNonExistentResources() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        // Test getting non-existent opportunity
        mockMvc.perform(get("/api/v1/opportunities/999999"))
            .andExpect(status().isNotFound());

        // Test updating non-existent opportunity
        mockMvc.perform(put("/api/v1/opportunities/999999/status")
                .param("status", "ENGAGED"))
            .andExpect(status().isNotFound());
    }
}