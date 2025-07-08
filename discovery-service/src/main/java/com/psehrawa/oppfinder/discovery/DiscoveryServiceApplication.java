package com.psehrawa.oppfinder.discovery;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {
    "com.psehrawa.oppfinder.discovery",
    "com.psehrawa.oppfinder.common"
})
@EntityScan(basePackages = "com.psehrawa.oppfinder.common.entity")
@EnableJpaRepositories(basePackages = "com.psehrawa.oppfinder.discovery.repository")
@EnableKafka
@EnableCaching
@EnableAsync
@EnableScheduling
public class DiscoveryServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(DiscoveryServiceApplication.class, args);
    }
}