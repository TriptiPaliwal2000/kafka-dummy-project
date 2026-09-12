package com.trippy.orderservice.event;

import com.trippy.orderevents.OrderPlacedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Sole responsibility: publish {@link OrderPlacedEvent}s. Kept separate from OrderServiceImpl
 * so the order-placement workflow doesn't know anything about Kafka - it just calls
 * {@link #publish}, and this class could be swapped for a different transport without
 * touching the business logic.
 */
@Component
public class OrderEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(OrderEventPublisher.class);

    private final KafkaTemplate<String, OrderPlacedEvent> kafkaTemplate;
    private final String topic;

    public OrderEventPublisher(KafkaTemplate<String, OrderPlacedEvent> kafkaTemplate,
                                @Value("${app.kafka.order-events-topic:order-events}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    public void publish(OrderPlacedEvent event) {
        // Keying by productId keeps every event for a given product in the same partition,
        // so inventory-service always sees them in the order order-service sent them.
        kafkaTemplate.send(topic, event.productId(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish OrderPlacedEvent for order={}", event.orderId(), ex);
                    } else {
                        log.info("Published OrderPlacedEvent order={} status={} partition={} offset={}",
                                event.orderId(), event.status(),
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });
    }
}
