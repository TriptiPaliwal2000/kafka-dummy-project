package com.trippy.notificationservice.controller;

import com.trippy.notificationservice.domain.Notification;
import com.trippy.notificationservice.service.NotificationService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public Flux<Notification> listAll() {
        return notificationService.listAll();
    }

    /** Server-Sent Events stream: `curl -N localhost:8083/api/notifications/stream` to watch them arrive live. */
    @GetMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<Notification> stream() {
        return notificationService.stream();
    }
}
