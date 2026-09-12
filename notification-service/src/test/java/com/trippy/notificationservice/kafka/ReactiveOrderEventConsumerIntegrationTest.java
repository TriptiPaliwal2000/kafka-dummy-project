package com.trippy.notificationservice.kafka;

import com.trippy.notificationservice.NotificationServiceApplication;
import com.trippy.notificationservice.service.NotificationService;
import com.trippy.orderevents.OrderPlacedEvent;
import com.trippy.orderevents.OrderStatus;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Proves the reactive consumer pipeline end-to-end: publish a real event onto an embedded
 * Kafka broker and verify a Notification shows up via NotificationService - the same
 * produce-then-assert shape as order-service's and inventory-service's Kafka tests, just
 * exercised through the reactive stack instead of spring-kafka's @KafkaListener.
 */
@SpringBootTest(classes = NotificationServiceApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1, topics = {"order-events"})
@TestPropertySource(properties = "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}")
class ReactiveOrderEventConsumerIntegrationTest {

    @Autowired
    private NotificationService notificationService;

    @Test
    void consumingOrderPlacedEvent_producesNotification() {
        OrderPlacedEvent event = OrderPlacedEvent.of(777L, "PRODUCT-5", 1, BigDecimal.valueOf(299.00),
                "reactive-buyer@example.com", OrderStatus.CONFIRMED);

        try (KafkaProducer<String, OrderPlacedEvent> producer = new KafkaProducer<>(Map.of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, System.getProperty("spring.embedded.kafka.brokers"),
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class
        ))) {
            producer.send(new ProducerRecord<>("order-events", event.productId(), event));
        }

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            var notifications = notificationService.listAll().collectList().block();
            assertThat(notifications).isNotNull();
            assertThat(notifications)
                    .anySatisfy(n -> {
                        assertThat(n.orderId()).isEqualTo(777L);
                        assertThat(n.customerEmail()).isEqualTo("reactive-buyer@example.com");
                    });
        });
    }
}
