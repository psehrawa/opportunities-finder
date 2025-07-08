package com.psehrawa.oppfinder.discovery.service;

import com.psehrawa.oppfinder.common.dto.OpportunityDto;
import com.psehrawa.oppfinder.common.entity.OpportunityEntity;
import com.psehrawa.oppfinder.common.enums.*;
import com.psehrawa.oppfinder.discovery.mapper.OpportunityMapper;
import com.psehrawa.oppfinder.discovery.repository.OpportunityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OpportunityServiceTest {

    @Mock
    private OpportunityRepository opportunityRepository;

    @Mock
    private OpportunityMapper opportunityMapper;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private OpportunityService opportunityService;

    private OpportunityDto testOpportunityDto;
    private OpportunityEntity testOpportunityEntity;

    @BeforeEach
    void setUp() {
        testOpportunityDto = OpportunityDto.builder()
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

        testOpportunityEntity = OpportunityEntity.builder()
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
        
        // Set ID manually as it's not part of the builder (JPA managed)
        testOpportunityEntity.setId(1L);
        testOpportunityEntity.setVersion(1L);
    }

    @Test
    void saveOpportunity_NewOpportunity_ShouldCreateAndPublishEvent() {
        // Given
        when(opportunityRepository.findBySourceAndExternalId(DataSource.GITHUB, "test-123"))
            .thenReturn(Optional.empty());
        when(opportunityMapper.toEntity(testOpportunityDto)).thenReturn(testOpportunityEntity);
        when(opportunityRepository.save(testOpportunityEntity)).thenReturn(testOpportunityEntity);
        when(opportunityMapper.toDto(testOpportunityEntity)).thenReturn(testOpportunityDto);

        // When
        OpportunityDto result = opportunityService.saveOpportunity(testOpportunityDto);

        // Then
        assertThat(result).isEqualTo(testOpportunityDto);
        verify(opportunityRepository).save(testOpportunityEntity);
        verify(kafkaTemplate).send("opportunity.discovered", testOpportunityDto);
    }

    @Test
    void saveOpportunity_ExistingOpportunity_ShouldUpdateAndPublishEvent() {
        // Given
        when(opportunityRepository.findBySourceAndExternalId(DataSource.GITHUB, "test-123"))
            .thenReturn(Optional.of(testOpportunityEntity));
        when(opportunityRepository.save(testOpportunityEntity)).thenReturn(testOpportunityEntity);
        when(opportunityMapper.toDto(testOpportunityEntity)).thenReturn(testOpportunityDto);

        // When
        OpportunityDto result = opportunityService.saveOpportunity(testOpportunityDto);

        // Then
        assertThat(result).isEqualTo(testOpportunityDto);
        verify(opportunityMapper).updateEntityFromDto(testOpportunityDto, testOpportunityEntity);
        verify(opportunityRepository).save(testOpportunityEntity);
        verify(kafkaTemplate).send("opportunity.updated", testOpportunityDto);
    }

    @Test
    void findById_ExistingOpportunity_ShouldReturnDto() {
        // Given
        when(opportunityRepository.findById(1L)).thenReturn(Optional.of(testOpportunityEntity));
        when(opportunityMapper.toDto(testOpportunityEntity)).thenReturn(testOpportunityDto);

        // When
        Optional<OpportunityDto> result = opportunityService.findById(1L);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(testOpportunityDto);
    }

    @Test
    void findById_NonExistingOpportunity_ShouldReturnEmpty() {
        // Given
        when(opportunityRepository.findById(1L)).thenReturn(Optional.empty());

        // When
        Optional<OpportunityDto> result = opportunityService.findById(1L);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void updateOpportunityStatus_ExistingOpportunity_ShouldUpdateAndPublishEvent() {
        // Given
        when(opportunityRepository.findById(1L)).thenReturn(Optional.of(testOpportunityEntity));
        when(opportunityRepository.save(testOpportunityEntity)).thenReturn(testOpportunityEntity);
        when(opportunityMapper.toDto(testOpportunityEntity)).thenReturn(testOpportunityDto);

        // When
        OpportunityDto result = opportunityService.updateOpportunityStatus(1L, OpportunityStatus.ENGAGED);

        // Then
        assertThat(result).isEqualTo(testOpportunityDto);
        assertThat(testOpportunityEntity.getStatus()).isEqualTo(OpportunityStatus.ENGAGED);
        verify(opportunityRepository).save(testOpportunityEntity);
        verify(kafkaTemplate).send("opportunity.updated", testOpportunityDto);
    }

    @Test
    void updateOpportunityStatus_NonExistingOpportunity_ShouldThrowException() {
        // Given
        when(opportunityRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> opportunityService.updateOpportunityStatus(1L, OpportunityStatus.ENGAGED))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Opportunity not found: 1");
    }

    @Test
    void updateOpportunityScore_ExistingOpportunity_ShouldUpdateAndPublishEvent() {
        // Given
        BigDecimal newScore = BigDecimal.valueOf(85.0);
        when(opportunityRepository.findById(1L)).thenReturn(Optional.of(testOpportunityEntity));
        when(opportunityRepository.save(testOpportunityEntity)).thenReturn(testOpportunityEntity);
        when(opportunityMapper.toDto(testOpportunityEntity)).thenReturn(testOpportunityDto);

        // When
        OpportunityDto result = opportunityService.updateOpportunityScore(1L, newScore);

        // Then
        assertThat(result).isEqualTo(testOpportunityDto);
        assertThat(testOpportunityEntity.getScore()).isEqualTo(newScore);
        verify(opportunityRepository).save(testOpportunityEntity);
        verify(kafkaTemplate).send("opportunity.scored", testOpportunityDto);
    }

    @Test
    void existsBySourceAndExternalId_ExistingOpportunity_ShouldReturnTrue() {
        // Given
        when(opportunityRepository.existsBySourceAndExternalId(DataSource.GITHUB, "test-123"))
            .thenReturn(true);

        // When
        boolean result = opportunityService.existsBySourceAndExternalId(DataSource.GITHUB, "test-123");

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void deactivateOpportunity_ExistingOpportunity_ShouldSetInactive() {
        // Given
        when(opportunityRepository.findById(1L)).thenReturn(Optional.of(testOpportunityEntity));

        // When
        opportunityService.deactivateOpportunity(1L);

        // Then
        assertThat(testOpportunityEntity.getIsActive()).isFalse();
        verify(opportunityRepository).save(testOpportunityEntity);
    }
}