package com.microservice_demo.api_gateway.security;

import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import java.util.List;
import java.util.function.Predicate;

@Component
public class RouteValidator {

    private static final AntPathMatcher pathMatcher = new AntPathMatcher();

    public static final List<String> openApiEndpoints = List.of(
            "/api/auth/register",
            "/api/auth/login",
            "/api/auth/refresh",
            "/api/auth/health",
            "/api/auth/validate",
            "/api/users/sync",
            "/api/en2/sync",
            "/api/*/sync" , // auth-service Feign :- POST /api/users/sync , /api/en2/sync
            "/api/en2/user/**" , // public user lookup in demo-service2

            "/api/auth/phone/**" , // Phone authentication
            "/api/otp/**" , // OTP send/verify
            "/api/password/**" , // Password reset/change
            "/api/oauth2/**"  , // OAuth2 callbacks
            "/oauth2/**" , // OAuth2 authorization
            "/login/oauth2/**" , // OAuth2 login

            "/api/en1/test/public",
            "/api/en2/test/public",

//            Profile endpoints
            "/api/users/sync/profile-picture",  // Demo-Service1
            "/api/en2/sync/profile-picture",    // Demo-Service2

            "/api/*/test/public", // smoke-test : /api/en1/test/public , /api/en2/test/public
            "/api/orders/product/*/count" , // demo-service1 Feign : GET order count for a product
            "/api/orders/user/*/exists",
            "/eureka/**",
            "/actuator/**"
    );

//    public Predicate<ServerHttpRequest> isSecured = request -> openApiEndpoints.stream().noneMatch(uri -> request.getURI().getPath().contains(uri));

    public Predicate<ServerHttpRequest> isSecured = request -> {
        String path = request.getURI().getPath();
        boolean isPublic = openApiEndpoints.stream()
                .anyMatch(pattern -> pathMatcher.match(pattern , path));

        return !isPublic;
    };
}
