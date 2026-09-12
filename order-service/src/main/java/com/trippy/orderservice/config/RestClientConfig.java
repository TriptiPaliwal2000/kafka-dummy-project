package com.trippy.orderservice.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    /**
     * {@code @LoadBalanced} makes Spring Cloud LoadBalancer resolve "http://inventory-service"
     * to a real instance via Eureka instead of literal DNS, so order-service never hard-codes
     * inventory-service's host/port. Connect/read timeouts are set on the request factory
     * itself (rather than relying on Resilience4j's TimeLimiter alone) so a hung TCP connect
     * can't outlast the time budget we want the whole stock check to have.
     */
    @Bean
    @LoadBalanced
    public RestClient.Builder loadBalancedRestClientBuilder() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(1000);
        requestFactory.setReadTimeout(2000);
        return RestClient.builder().requestFactory(requestFactory);
    }
}
