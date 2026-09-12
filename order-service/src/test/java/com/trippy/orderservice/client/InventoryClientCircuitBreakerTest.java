package com.trippy.orderservice.client;

import com.sun.net.httpserver.HttpServer;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestClient;

import com.trippy.orderservice.OrderServiceApplication;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end(ish) proof that the Resilience4j wiring in {@link InventoryClient} actually
 * behaves the way the Javadoc on that class claims: repeated failures open the circuit, and
 * callers always get a usable {@link StockCheckResult} (never an exception) either way.
 * <p>
 * Uses a bare {@code com.sun.net.httpserver.HttpServer} instead of a mocking library so the
 * "downstream" really is a separate HTTP server being called over the network, not a mock
 * object - that's what makes this test a meaningful check of the annotation wiring (AOP
 * proxying, timeout, fallback signature) rather than just of our own code.
 */
@SpringBootTest(classes = OrderServiceApplication.class)
@ActiveProfiles("test")
class InventoryClientCircuitBreakerTest {

    private static HttpServer failingServer;

    @TestConfiguration
    static class NonLoadBalancedRestClientConfig {
        /**
         * Overrides the app's {@code @LoadBalanced} RestClient.Builder with a plain one, since
         * this test points directly at a local HttpServer and has no Eureka/LoadBalancer
         * registry to resolve a service id against. Marked @Primary so it wins the by-type
         * injection in InventoryClient's constructor.
         */
        @Bean
        @Primary
        RestClient.Builder plainRestClientBuilder() {
            return RestClient.builder();
        }
    }

    @DynamicPropertySource
    static void pointAtFailingServer(DynamicPropertyRegistry registry) throws IOException {
        failingServer = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        failingServer.createContext("/api/inventory", exchange -> {
            exchange.sendResponseHeaders(500, -1);
            exchange.close();
        });
        failingServer.start();
        registry.add("inventory-service.url",
                () -> "http://localhost:" + failingServer.getAddress().getPort());
    }

    @AfterAll
    static void stopServer() {
        if (failingServer != null) {
            failingServer.stop(0);
        }
    }

    @Autowired
    private InventoryClient inventoryClient;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @Test
    void repeatedDownstreamFailures_openTheCircuit_andFallbackAlwaysReturnsUsableResult() throws Exception {
        // application-test.yml: sliding-window-size=4, minimum-number-of-calls=2,
        // failure-rate-threshold=50% - two failures in a row is already enough to open it.
        for (int i = 0; i < 4; i++) {
            StockCheckResult result = inventoryClient.checkStock("PRODUCT-1", 1).get(3, TimeUnit.SECONDS);
            // Whether this particular call failed outright or was rejected by an already-open
            // circuit, the caller (OrderServiceImpl) never sees an exception - just UNKNOWN.
            assertThat(result.availability()).isEqualTo(StockCheckResult.Availability.UNKNOWN);
        }

        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("inventoryService");
        assertThat(circuitBreaker.getState())
                .isIn(CircuitBreaker.State.OPEN, CircuitBreaker.State.HALF_OPEN);
    }
}
