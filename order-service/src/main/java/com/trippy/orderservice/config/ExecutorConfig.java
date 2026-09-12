package com.trippy.orderservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class ExecutorConfig {

    /**
     * Dedicated, bounded pool for the inventory-service call. Keeping it separate from the
     * web server's request-handling threads means a slow/stuck downstream call can't starve
     * order-service's own ability to accept HTTP requests.
     */
    @Bean(destroyMethod = "shutdown")
    public ExecutorService inventoryExecutor() {
        return Executors.newFixedThreadPool(8);
    }
}
