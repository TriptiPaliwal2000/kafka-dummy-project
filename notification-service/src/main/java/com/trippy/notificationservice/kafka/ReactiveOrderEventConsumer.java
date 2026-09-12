package com.trippy.notificationservice.kafka;

import com.trippy.orderevents.OrderPlacedEvent;
import com.trippy.notificationservice.service.NotificationService;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.util.retry.Retry;

import java.time.Duration;

/**
 * The reactive counterpart of inventory-service's (blocking) {@code @KafkaListener}: same
 * topic, same event type, fully non-blocking pipeline instead.
 * <p>
 * {@code concatMap} (not {@code flatMap}) deliberately processes one record at a time, in
 * order, and only acknowledges each record's offset after {@link NotificationService#handle}
 * completes for it - so a crash mid-stream re-delivers that notification instead of silently
 * dropping it, at the cost of not parallelizing across records within a partition.
 */
@Component
public class ReactiveOrderEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(ReactiveOrderEventConsumer.class);

    private final KafkaReceiver<String, OrderPlacedEvent> kafkaReceiver;
    private final NotificationService notificationService;
    private Disposable subscription;

    public ReactiveOrderEventConsumer(KafkaReceiver<String, OrderPlacedEvent> kafkaReceiver,
                                       NotificationService notificationService) {
        this.kafkaReceiver = kafkaReceiver;
        this.notificationService = notificationService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void startConsuming() {
        subscription = kafkaReceiver.receive()
                .concatMap(record -> notificationService.handle(record.value())
                        .doOnSuccess(n -> record.receiverOffset().acknowledge())
                        .onErrorResume(ex -> {
                            log.error("Failed to process OrderPlacedEvent at offset {}: {}",
                                    record.receiverOffset(), ex.getMessage(), ex);
                            // Acknowledge anyway: a poison-pill message should not block the
                            // whole partition forever. A production system would instead route
                            // this to a dead-letter topic before acknowledging.
                            record.receiverOffset().acknowledge();
                            return Mono.empty();
                        }))
                .retryWhen(Retry.backoff(Long.MAX_VALUE, Duration.ofSeconds(2)).maxBackoff(Duration.ofSeconds(30)))
                .doOnSubscribe(s -> log.info("notification-service subscribed to order-events"))
                .subscribe();
    }

    @PreDestroy
    public void stopConsuming() {
        if (subscription != null && !subscription.isDisposed()) {
            subscription.dispose();
        }
    }
}
