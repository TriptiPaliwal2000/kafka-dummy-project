package com.trippy.notificationservice.config;

import com.trippy.orderevents.OrderPlacedEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverOptions;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class ReactiveKafkaConfig {

    @Bean
    public ReceiverOptions<String, OrderPlacedEvent> receiverOptions(
            @Value("${spring.kafka.bootstrap-servers:localhost:9092}") String bootstrapServers,
            @Value("${app.kafka.consumer-group:notification-service}") String groupId,
            @Value("${app.kafka.order-events-topic:order-events}") String topic) {

        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        // Reactor Kafka commits offsets itself, driven by when our pipeline actually finishes
        // processing a record (see ReactiveOrderEventConsumer) - not on a fixed timer.
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

        JsonDeserializer<OrderPlacedEvent> valueDeserializer = new JsonDeserializer<>(OrderPlacedEvent.class);
        valueDeserializer.addTrustedPackages("com.trippy.orderevents");
        valueDeserializer.ignoreTypeHeaders();

        return ReceiverOptions.<String, OrderPlacedEvent>create(props)
                .withKeyDeserializer(new StringDeserializer())
                .withValueDeserializer(valueDeserializer)
                .subscription(java.util.Collections.singleton(topic));
    }

    @Bean
    public KafkaReceiver<String, OrderPlacedEvent> kafkaReceiver(ReceiverOptions<String, OrderPlacedEvent> receiverOptions) {
        return KafkaReceiver.create(receiverOptions);
    }
}
