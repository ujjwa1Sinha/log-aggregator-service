package com.dstrLogAggr.Log_Aggregator.config;

import com.dstrLogAggr.Log_Aggregator.client.LogAggregatorClient;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Auto-configuration for Log Aggregator library.
 * This enables automatic setup when the library is added as a dependency.
 * 
 * To use this library in your Spring Boot application:
 * 1. Add this JAR as a dependency
 * 2. Configure properties in application.properties:
 * log-aggregator.kafka.bootstrap-servers=localhost:9092
 * log-aggregator.service-name=my-service
 * 3. Inject LogAggregatorClient and start logging
 */
@AutoConfiguration
@ConditionalOnClass(KafkaTemplate.class)
@EnableConfigurationProperties(LogAggregatorProperties.class)
@ComponentScan(basePackages = "com.dstrLogAggr.Log_Aggregator")
@EnableAsync
public class LogAggregatorAutoConfiguration {

    /**
     * Create LogAggregatorClient bean if not already defined
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "log-aggregator", name = "enabled", havingValue = "true", matchIfMissing = true)
    public LogAggregatorClient logAggregatorClient(
            KafkaTemplate<String, String> kafkaTemplate,
            LogAggregatorProperties properties) {

        String serviceName = System.getProperty("spring.application.name", "default-service");

        return LogAggregatorClient.builder()
                .kafkaTemplate(kafkaTemplate)
                .serviceName(serviceName)
                .build();
    }
}
