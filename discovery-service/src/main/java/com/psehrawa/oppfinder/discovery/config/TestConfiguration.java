package com.psehrawa.oppfinder.discovery.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.mockito.Mockito;

/**
 * Test configuration that provides mock beans for external dependencies
 * when running with 'test' profile
 */
@Configuration
@Profile("test")
public class TestConfiguration {

    @Bean
    @Primary
    public RedisConnectionFactory redisConnectionFactory() {
        return Mockito.mock(RedisConnectionFactory.class);
    }

    @Bean
    @Primary
    public RedisTemplate<String, Object> redisTemplate() {
        return Mockito.mock(RedisTemplate.class);
    }

    @Bean
    @Primary
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return Mockito.mock(KafkaTemplate.class);
    }
}