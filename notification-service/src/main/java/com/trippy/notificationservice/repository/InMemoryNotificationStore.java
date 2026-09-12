package com.trippy.notificationservice.repository;

import com.trippy.notificationservice.domain.Notification;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * A deliberately simple non-blocking store: this project's point is the reactive Kafka
 * pipeline, not a reactive database driver, so notifications live in memory (newest first)
 * for as long as the service runs. Swapping this for R2DBC later wouldn't change
 * NotificationService or the controller at all - they only depend on this Mono/Flux contract.
 */
@Repository
public class InMemoryNotificationStore {

    // Sorted by insertion order; ConcurrentSkipListMap keeps reads/writes lock-free-ish and ordered.
    private final Map<UUID, Notification> notifications = new ConcurrentHashMap<>();
    private final ConcurrentSkipListMap<Instant2, UUID> order = new ConcurrentSkipListMap<>();

    // Fan-out for the SSE /stream endpoint: every subscriber gets every notification saved
    // from the moment they subscribe onward.
    private final Sinks.Many<Notification> liveSink = Sinks.many().multicast().onBackpressureBuffer();

    public Mono<Notification> save(Notification notification) {
        return Mono.fromCallable(() -> {
            notifications.put(notification.id(), notification);
            order.put(new Instant2(notification.sentAt(), notification.id()), notification.id());
            liveSink.tryEmitNext(notification);
            return notification;
        });
    }

    public Flux<Notification> findAll() {
        return Flux.fromIterable(order.descendingMap().values())
                .map(notifications::get);
    }

    public Flux<Notification> stream() {
        return liveSink.asFlux();
    }

    /** Small helper so two notifications saved in the same millisecond still sort deterministically. */
    private record Instant2(java.time.Instant sentAt, UUID id) implements Comparable<Instant2> {
        @Override
        public int compareTo(Instant2 other) {
            int byTime = this.sentAt.compareTo(other.sentAt);
            return byTime != 0 ? byTime : this.id.compareTo(other.id);
        }
    }
}
