package com.trippy.notificationservice.service;

import com.trippy.notificationservice.domain.Notification;
import com.trippy.notificationservice.repository.InMemoryNotificationStore;
import com.trippy.orderevents.OrderPlacedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final InMemoryNotificationStore store;

    public NotificationService(InMemoryNotificationStore store) {
        this.store = store;
    }

    /** Composes and "sends" (here: logs) a notification for an incoming order event, then stores it. */
    public Mono<Notification> handle(OrderPlacedEvent event) {
        Notification notification = Notification.from(event);
        return Mono.just(notification)
                .doOnNext(n -> log.info("Notifying {} about order #{}: {}", n.customerEmail(), n.orderId(), n.message()))
                .flatMap(store::save);
    }

    public Flux<Notification> listAll() {
        return store.findAll();
    }

    public Flux<Notification> stream() {
        return store.stream();
    }
}
