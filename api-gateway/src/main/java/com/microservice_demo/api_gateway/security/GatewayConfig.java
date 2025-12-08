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
                .route("auth-service" , r -> r.path("/api/auth/**").uri("lb://auth-service"))
                .route("demo-service1" , r -> r.path("/api/users/**").filters(f -> f.filter(filter)).uri("lb://demo-service1"))
                .route("demo-service1" , r -> r.path("/api/en1/**").filters(f -> f.filter(filter)).uri("lb://demo-service1"))
                .route("demo-service1" , r -> r.path("/api/en1/test/**").filters(f -> f.filter(filter)).uri("lb://demo-service1"))
                .route("demo-service2" , r -> r.path("/api/en2/**").filters(f -> f.filter(filter)).uri("lb://demo-service2"))
                .route("demo-service2" , r -> r.path("/api/en2/test/**").filters(f -> f.filter(filter)).uri("lb://demo-service2"))
                .build();
    }

}
