package com.trippy.orderservice.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
 * The one synchronous, cross-service call in this system: before confirming an order we ask
 * inventory-service whether it can actually fulfil it.
 * <p>
 * This call is wrapped in a Resilience4j circuit breaker + time limiter rather than called
 * directly, on purpose. A checkout flow should not become unavailable just because a
 * downstream dependency is slow or down - instead we want to degrade gracefully: accept the
 * order as {@code PENDING_VERIFICATION} and let it be reconciled later, rather than failing
 * every checkout. {@link #checkStockFallback} is what implements that degradation.
 * <p>
 * The call runs on a bounded executor ({@code inventoryExecutor}) and returns a
 * {@link CompletableFuture} because Resilience4j's {@code @TimeLimiter} requires a
 * {@link java.util.concurrent.CompletionStage} to be able to time the call out and cancel it -
 * it can't impose a timeout on a plain blocking method call.
 */
@Component
public class InventoryClient {

    private static final Logger log = LoggerFactory.getLogger(InventoryClient.class);

    private final RestClient restClient;
    private final ExecutorService inventoryExecutor;

    public InventoryClient(RestClient.Builder loadBalancedRestClientBuilder,
                            @Value("${inventory-service.url:http://inventory-service}") String inventoryServiceUrl,
                            @Qualifier("inventoryExecutor") ExecutorService inventoryExecutor) {
        this.restClient = loadBalancedRestClientBuilder.baseUrl(inventoryServiceUrl).build();
        this.inventoryExecutor = inventoryExecutor;
    }

    @CircuitBreaker(name = "inventoryService", fallbackMethod = "checkStockFallback")
    @TimeLimiter(name = "inventoryService")
    public CompletableFuture<StockCheckResult> checkStock(String productId, int quantity) {
        return CompletableFuture.supplyAsync(() -> doCheckStock(productId, quantity), inventoryExecutor);
    }

    private StockCheckResult doCheckStock(String productId, int quantity) {
        return restClient.get()
                .uri("/api/inventory/{productId}/check?quantity={quantity}", productId, quantity)
                .retrieve()
                .body(StockCheckResult.class);
    }

    /**
     * Resilience4j fallback - same signature as {@link #checkStock}, plus the Throwable that
     * triggered it. Invoked when the circuit is open, the call throws, or the time limiter
     * times it out.
     */
    public CompletableFuture<StockCheckResult> checkStockFallback(String productId, int quantity, Throwable t) {
        log.warn("inventory-service unavailable while checking stock for product={} qty={} ({}: {}). " +
                        "Accepting the order as PENDING_VERIFICATION instead of failing checkout.",
                productId, quantity, t.getClass().getSimpleName(), t.getMessage());
        return CompletableFuture.completedFuture(StockCheckResult.unknown());
    }
}
