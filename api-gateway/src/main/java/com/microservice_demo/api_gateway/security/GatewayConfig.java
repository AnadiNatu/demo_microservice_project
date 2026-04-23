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

                // Core auth endpoints
                .route("auth-register-login", r -> r.path(
                        "/api/auth/login",
                                "/api/auth/register",
                                "/api/auth/refresh",
                                "/api/auth/validate",
                                "/api/auth/health",
                                "/api/auth/test")
                        .uri("lb://auth-service"))

                // OTP endpoints (send/verify email & sms)
                .route("auth-otp-all", r -> r.path(
                        "/api/otp/**")
                        .uri("lb://auth-service"))

                // Password forgot/reset/change
                .route("auth-password-all", r -> r.path(
                        "/api/password/**")
                        .uri("lb://auth-service"))

                // Phone OTP login
                .route("auth-phone-all", r -> r.path(
                        "/api/auth/phone/**")
                        .uri("lb://auth-service"))

        // Email
                .route("auth-email-all", r -> r.path(
                        "/api/email/**")
                        .uri("lb://auth-service"))

                        // Notification endpoints
                .route("auth-notifications-all", r -> r.path(
                        "/api/notifications/**")
                        .uri("lb://auth-service"))

        // OAuth2 flows
                .route("auth-oauth2-all", r -> r.path(
                        "/api/oauth2/**", "/oauth2/**", "/login/oauth2/**")
                        .uri("lb://auth-service"))

//                AUTH-SERVICE — PROTECTED
                .route("auth-profile-protected" , r -> r.path(
                        "/api/profile/**")
                        .filters(f -> f.filter(filter))
                        .uri("lb://auth-service"))

//                Wildcard catch-all for any remaining
                .route("auth-service-all" , r -> r.path(
                        "/api/auth/**")
                        .uri("lb://auth-service"))

//                DEMO-SERVICE-1 (Public sync & test)
                .route("ds1-users-sync" , r -> r.path(
                        "/api/users/sync")
                        .uri("lb://demo-service1"))
                .route("ds1-users-profile-sync" , r -> r.path(
                        "/api/users/sync/profile-picture")
                        .uri("lb://demo-service1"))
                .route("ds1-en1-test-public" , r -> r.path(
                        "/api/en1/test/public")
                        .uri("lb://demo-service1"))

//                DEMO-SERVICE-1 (Protected)
                .route("ds1-en1-protected", r -> r
                        .path("/api/en1/**")
                        .filters(f -> f.filter(filter))
                        .uri("lb://demo-service1"))

                .route("ds1-users-protected", r -> r
                        .path("/api/users/**")
                        .filters(f -> f.filter(filter))
                        .uri("lb://demo-service1"))

                .route("ds1-products-protected", r -> r
                        .path("/api/products/**")
                        .filters(f -> f.filter(filter))
                        .uri("lb://demo-service1"))

//                DEMO-SERVICE-2 (Public , sync , test , feign)
                .route("ds2-en2-sync", r -> r
                        .path("/api/en2/sync")
                        .uri("lb://demo-service2"))

                .route("ds2-en2-profile-sync", r -> r
                        .path("/api/en2/sync/profile-picture")
                        .uri("lb://demo-service2"))

                .route("ds2-en2-user-lookup", r -> r
                        .path("/api/en2/user/**")
                        .uri("lb://demo-service2"))

                .route("ds2-en2-test-public", r -> r
                        .path("/api/en2/test/public")
                        .uri("lb://demo-service2"))

                // Feign inter-service calls from demo-service1 — no JWT
                .route("ds2-orders-count", r -> r
                        .path("/api/orders/product/*/count")
                        .uri("lb://demo-service2"))

                .route("ds2-orders-user-exists", r -> r
                        .path("/api/orders/user/*/exists")
                        .uri("lb://demo-service2"))

//                DEMO-SERVICE-2
                .route("ds2-en2-protected", r -> r
                        .path("/api/en2/**")
                        .filters(f -> f.filter(filter))
                        .uri("lb://demo-service2"))

                .route("ds2-orders-protected", r -> r
                        .path("/api/orders/**")
                        .filters(f -> f.filter(filter))
                        .uri("lb://demo-service2"))

                .build();



//                .route("auth-register-login" ,r -> r.path("/api/auth/login" , "/api/auth/register" , "/api/password/**" , "/api/auth/phone/**"  ,"/api/auth/refresh" , "/api/auth/validate" , "/api/auth/health" , "/api/auth/test").uri("lb://auth-service"))
//                .route("auth-otp-all" , r -> r.path("/api/otp/**").uri("lb://auth-service"))
//                .route("auth-password-all" ,r -> r.path("/api/password/**").uri("lb://auth-service"))
//                .route("auth-phone-all" , r -> r.path("/api/auth/phone/**").uri("lb://auth-service"))
//                .route("auth-oauth2-all" , r -> r.path("/api/oauth2/**" , "/oauth2/**" , "/login/oauth2/**").uri("lb://auth-service"))
//
//                .route("ds1-users-profile-sync" , r -> r.path("/api/users/sync/profile-picture").uri("lb://demo-service1"))
//
//                .route("auth-profile-protected" , r -> r.path("/api/profile/**").filters(f -> f.filter(filter)).uri("lb://auth-service"))
//
//                .route("auth-service-all" , r -> r.path("/api/auth/**").uri("lb://auth-service"))
//
//                .route("ds1-en1-test-public" , r -> r.path("/api/en1/test/public").uri("lb://demo-service1"))
//                .route("ds1-users-sync" , r -> r.path("/api/users/sync").uri("lb://demo-service1"))
//                .route("ds1-en1-protected" , r -> r.path("/api/en1/**").filters(f -> f.filter(filter)).uri("lb://demo-service1"))
//                .route("ds1-users-protected" , r -> r.path("/api/users/**").filters(f -> f.filter(filter)).uri("lb://demo-service1"))
//                .route("ds1-products-protected" , r -> r.path("/api/products/**").filters(f -> f.filter(filter)).uri("lb://demo-service1"))
//
//
////                Sync routes also should be declared first
//                .route("ds1-en2-sync" , r -> r.path("/api/en2/sync").uri("lb://demo-service2"))
//                .route("ds2-en2-user-lookup" , r -> r.path("/api/en2/user/**").uri("lb://demo-service2"))
//                .route("ds2-en2-test-public" , r -> r.path("/api/en2/test/public").uri("lb://demo-service2"))
//                .route("ds2-orders-count" , r -> r.path("/api/orders/product/*/count").uri("lb://demo-service2"))
//                .route("ds2-orders-user-exists" , r -> r.path("/api/orders/user/*/exists").uri("lb://demo-service2"))
//
//                .route("ds2-en2-profile-sync" , r -> r.path("/api/en2/sync/profile-picture").uri("lb://demo-service2"))
//
//                .route("ds2-en2-protected" , r -> r.path("/api/en2/**").filters(f -> f.filter(filter)).uri("lb://demo-service2"))
//                .route("ds2-orders-protected" , r -> r.path("/api/orders/**").filters(f -> f.filter(filter)).uri("lb://demo-service2"))
//                .build();

    }
}
