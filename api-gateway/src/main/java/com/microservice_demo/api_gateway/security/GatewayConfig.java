package com.microservice_demo.api_gateway.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayConfig {

    @Autowired
    private AuthenticationFilter filter;

    @Bean
    public RouteLocator routes(RouteLocatorBuilder builder){
        return builder.routes()
//                .route("auth-service" , r -> r.path("/api/auth/**").uri("lb://auth-service"))
//                .route("demo-service1" , r -> r.path("/api/users/**").filters(f -> f.filter(filter)).uri("lb://demo-service1"))
//                .route("demo-service1" , r -> r.path("/api/en1/**").filters(f -> f.filter(filter)).uri("lb://demo-service1"))
//                .route("demo-service1" , r -> r.path("/api/en1/test/**").filters(f -> f.filter(filter)).uri("lb://demo-service1"))
//                .route("demo-service2" , r -> r.path("/api/en2/**").filters(f -> f.filter(filter)).uri("lb://demo-service2"))
//                .route("demo-service1-products" , r -> r.path("/api/products/**").filters(f -> f.filter(filter)).uri("lb://demo-service1"))
//                .route("demo-service2-orders" , r -> r.path("/api/orders/**").filters(f -> f.filter(filter)).uri("lb://demo-service2"))
//                .route("demo-service2" , r -> r.path("/api/en2/test/**").filters(f -> f.filter(filter)).uri("lb://demo-service2"))
                .route("auth-service-all" , r -> r.path("/api/auth/**").uri("lb://auth-service"))

//                .route("ds-1-users-sync" , r -> r.path("/api/auth/**").uri("lb://auth-service"))
                .route("ds-en1-test-public" , r -> r.path("/api/en1/test/public").uri("lb://demo-service1"))
                .route("ds1-users-protected" , r -> r.path("/api/users/**").filters(f -> f.filter(filter)).uri("lb://demo-service1"))
                .route("ds1-products-protected" , r -> r.path("/api/products/**").filters( f -> f.filter(filter)).uri("lb://demo-service1"))

                .route("ds2-en2-sync" , r -> r.path("/api/en2/sync").uri("lb://demo-service2"))
                .route("ds2-en2-user-lookup" , r -> r.path("/api/en2/user/**").uri("lb://demo-service2"))
                .route("ds2-en2-test-public" , r -> r.path("/api/en2/test/public").uri("lb://demo-service2"))
                .route("ds2-orders-count" , r -> r.path("/api/orders/product/*/count").uri("lb://demo-service2"))
                .route("ds2-orders-user-exists" , r -> r.path("/api/orders/user/*/exists").uri("lb://demo-service2"))
                .route("ds2-en2-protected" , r -> r.path("/api/en2/**").filters(f -> f.filter(filter)).uri("lb://demo-service2"))
                .route("ds2-orders-protected" , r -> r.path("/api/orders/**").filters(f -> f.filter(filter)).uri("lb://demo-service2"))
                .build();
    }

}
