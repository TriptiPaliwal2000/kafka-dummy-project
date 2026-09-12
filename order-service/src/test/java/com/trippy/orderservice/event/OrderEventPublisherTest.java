package com.trippy.orderservice.event;

import com.trippy.orderevents.OrderPlacedEvent;
import com.trippy.orderevents.OrderStatus;
import com.trippy.orderservice.OrderServiceApplication;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies OrderEventPublisher end-to-end against a real (embedded, in-process) Kafka broker:
 * publish an event, then consume it back with a plain Kafka consumer and assert what actually
 * landed on the topic - partition key included, since OrderEventPublisher keys by productId
 * specifically so per-product ordering is preserved.
 */
@SpringBootTest(classes = OrderServiceApplication.class)
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1, topics = {"order-events"})
@TestPropertySource(properties = "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}")
class OrderEventPublisherTest {

    @Autowired
    private OrderEventPublisher publisher;

    private Consumer<String, OrderPlacedEvent> consumer;

    @AfterEach
    void closeConsumer() {
        if (consumer != null) {
            consumer.close();
        }
    }

    @Test
    void publish_sendsEventKeyedByProductId() {
        OrderPlacedEvent event = OrderPlacedEvent.of(42L, "PRODUCT-7", 3, BigDecimal.valueOf(5.50),
                "buyer@example.com", OrderStatus.CONFIRMED);

        publisher.publish(event);

        Map<String, Object> consumerProps = Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, System.getProperty("spring.embedded.kafka.brokers"),
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class,
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
                JsonDeserializer.TRUSTED_PACKAGES, "com.trippy.orderevents",
                JsonDeserializer.VALUE_DEFAULT_TYPE, OrderPlacedEvent.class.getName()
        );
        consumer = new DefaultKafkaConsumerFactory<String, OrderPlacedEvent>(consumerProps)
                .createConsumer("order-event-publisher-test", null);
        consumer.subscribe(Collections.singletonList("order-events"));

        ConsumerRecord<String, OrderPlacedEvent> record =
                KafkaTestUtils.getSingleRecord(consumer, "order-events", Duration.ofSeconds(10));

        assertThat(record.key()).isEqualTo("PRODUCT-7");
        assertThat(record.value().orderId()).isEqualTo(42L);
        assertThat(record.value().status()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(record.value().quantity()).isEqualTo(3);
    }
}
