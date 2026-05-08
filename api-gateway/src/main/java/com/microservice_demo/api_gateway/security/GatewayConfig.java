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
                //  AUTH-SERVICE — PUBLIC
                // Core auth endpoints (login, register, refresh, validate, health, test)
                .route("auth-core-public", r -> r.path(
                                "/api/auth/login",
                                "/api/auth/register",
                                "/api/auth/refresh",
                                "/api/auth/validate",
                                "/api/auth/health",
                                "/api/auth/test")
                        .uri("lb://auth-service"))

                // Phone OTP login
                .route("auth-phone-public", r -> r.path("/api/auth/phone/**")
                        .uri("lb://auth-service"))

                // OTP send / verify
                .route("auth-otp-public", r -> r.path("/api/otp/**")
                        .uri("lb://auth-service"))

                // Password forgot / reset / change
                .route("auth-password-public", r -> r.path("/api/password/**")
                        .uri("lb://auth-service"))

                // Email utility
                .route("auth-email-public", r -> r.path("/api/email/**")
                        .uri("lb://auth-service"))

                // Notifications
                .route("auth-notifications-public", r -> r.path("/api/notifications/**")
                        .uri("lb://auth-service"))

                // OAuth2 flows
                .route("auth-oauth2-public", r -> r.path(
                                "/api/oauth2/**", "/oauth2/**", "/login/oauth2/**")
                        .uri("lb://auth-service"))

                // ── AUTH-SERVICE — PROTECTED ──────────────────────────────────
                .route("auth-profile-protected", r -> r.path("/api/profile/**")
                        .filters(f -> f.filter(filter))
                        .uri("lb://auth-service"))

                // Catch-all for any remaining /api/auth/** (e.g. future endpoints)
                .route("auth-catchall", r -> r.path("/api/auth/**")
                        .uri("lb://auth-service"))

                //  DEMO-SERVICE-1 — PUBLIC (sync + smoke-test)

                .route("ds1-users-sync-public", r -> r.path("/api/users/sync")
                        .uri("lb://demo-service1"))

                .route("ds1-users-profile-sync-public", r -> r.path("/api/users/sync/profile-picture")
                        .uri("lb://demo-service1"))

                .route("ds1-en1-test-public", r -> r.path("/api/en1/test/public")
                        .uri("lb://demo-service1"))

                // ── DEMO-SERVICE-1 — PROTECTED ────────────────────────────────
                .route("ds1-en1-protected", r -> r.path("/api/en1/**")
                        .filters(f -> f.filter(filter))
                        .uri("lb://demo-service1"))

                .route("ds1-users-protected", r -> r.path("/api/users/**")
                        .filters(f -> f.filter(filter))
                        .uri("lb://demo-service1"))

                .route("ds1-products-protected", r -> r.path("/api/products/**")
                        .filters(f -> f.filter(filter))
                        .uri("lb://demo-service1"))

                //  DEMO-SERVICE-2 — PUBLIC (sync + user-lookup + smoke-test + Feign)
                .route("ds2-en2-sync-public", r -> r.path("/api/en2/sync")
                        .uri("lb://demo-service2"))

                .route("ds2-en2-profile-sync-public", r -> r.path("/api/en2/sync/profile-picture")
                        .uri("lb://demo-service2"))

                // Public user-lookup (no JWT — used by DS1 or frontend anonymous calls)
                .route("ds2-en2-user-lookup-public", r -> r.path("/api/en2/user/**")
                        .uri("lb://demo-service2"))

                .route("ds2-en2-test-public", r -> r.path("/api/en2/test/public")
                        .uri("lb://demo-service2"))

                // Feign inter-service calls from demo-service1 → demo-service2 (no JWT).
                // Pattern: /api/orders/product/{productId}/count
                .route("ds2-orders-product-count-feign", r -> r.path("/api/orders/product/*/count")
                        .uri("lb://demo-service2"))

                // Pattern: /api/orders/user/{userId}/exists   ← FIXED: was "exist"
                .route("ds2-orders-user-exists-feign", r -> r.path("/api/orders/user/*/exists")
                        .uri("lb://demo-service2"))

                // ── DEMO-SERVICE-2 — PROTECTED ────────────────────────────────
                .route("ds2-en2-protected", r -> r.path("/api/en2/**")
                        .filters(f -> f.filter(filter))
                        .uri("lb://demo-service2"))

                .route("ds2-orders-protected", r -> r.path("/api/orders/**")
                        .filters(f -> f.filter(filter))
                        .uri("lb://demo-service2"))

                .build();
    }
}
