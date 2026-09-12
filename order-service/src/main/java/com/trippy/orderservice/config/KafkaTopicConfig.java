package com.trippy.orderservice.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Value("${app.kafka.order-events-topic:order-events}")
    private String topicName;

    /**
     * order-service owns the topic it publishes to. 3 partitions is enough to demonstrate
     * parallel consumption while keeping per-product ordering, since OrderEventPublisher uses
     * productId as the Kafka message key (same key -> same partition -> in-order delivery).
     */
    @Bean
    public NewTopic orderEventsTopic() {
        return TopicBuilder.name(topicName)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
