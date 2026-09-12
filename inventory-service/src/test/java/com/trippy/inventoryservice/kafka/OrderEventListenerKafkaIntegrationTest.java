package com.trippy.inventoryservice.kafka;

import com.trippy.inventoryservice.InventoryServiceApplication;
import com.trippy.inventoryservice.repository.InventoryRepository;
import com.trippy.orderevents.OrderPlacedEvent;
import com.trippy.orderevents.OrderStatus;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Proves the whole loop, not just each half in isolation: publish a real OrderPlacedEvent onto
 * an embedded Kafka broker and assert that inventory-service's @KafkaListener actually reduces
 * stock in the database - the same thing OrderEventPublisherTest proves from order-service's
 * side of the same topic.
 */
@SpringBootTest(classes = InventoryServiceApplication.class)
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1, topics = {"order-events"})
@TestPropertySource(properties = "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}")
class OrderEventListenerKafkaIntegrationTest {

    @Autowired
    private InventoryRepository inventoryRepository;

    @Test
    void publishingConfirmedOrder_eventuallyDecrementsStock() {
        int stockBefore = inventoryRepository.findById("PRODUCT-2").orElseThrow().getAvailableStock();

        KafkaTemplate<String, OrderPlacedEvent> producer = testProducer();
        OrderPlacedEvent event = new OrderPlacedEvent(java.util.UUID.randomUUID(), 555L, "PRODUCT-2", 4,
                BigDecimal.valueOf(29.99), "buyer@example.com", OrderStatus.CONFIRMED, Instant.now());
        producer.send("order-events", event.productId(), event);

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            int stockAfter = inventoryRepository.findById("PRODUCT-2").orElseThrow().getAvailableStock();
            assertThat(stockAfter).isEqualTo(stockBefore - 4);
        });
    }

    private KafkaTemplate<String, OrderPlacedEvent> testProducer() {
        Map<String, Object> producerProps = Map.of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, System.getProperty("spring.embedded.kafka.brokers"),
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class
        );
        return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(producerProps));
    }
}
