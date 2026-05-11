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
//            Core auth - no token
            "/api/auth/register",
            "/api/auth/login",
            "/api/auth/refresh",
            "/api/auth/health",
            "/api/auth/validate",
            "/api/auth/test",

//            Phone OTP login
            "/api/auth/phone/**",

//            OTP send/verify
            "/api/otp/**",

//            Password forgot
            "/api/password/**",

//            Email utility endpoint
            "/api/email/**",

//            Notification
            "/api/notifications/**",

//            OAuth2 authorization & callback flows
            "/api/oauth2/**",
            "/oauth2/**",
            "/login/oauth2/**",

//            Demo-Service1 : internal sync
            "/api/users/sync",
            "/api/users/sync/profile-picture",

//            Demo-service1 : public smoke-test
            "/api/en1/test/public",

//            Demo-service2 : internal sync
            "/api/en2/sync",
            "/api/en2/sync/profile-picture",

//            Demo-service2 : public smoke-test
            "/api/en2/test/public",

//            Demo-service2 : Feign inter-service calls
            "/api/orders/product/*/count",
            "/api/orders/user/*/exists",

////            Demo-Service2 internal sync & public lookup
//            "/api/en2/sync",
//            "/api/en2/sync/profile-picture",
//            "/api/en2/user/**",
//
////            Smoke-test public endpoint
//            "/api/en1/test/public",
//            "/api/en2/test/public",
//            "/api/*/test/public",
//
////            Feign inter-service calls (demo-services 1&2)
//            "/api/orders/product/*/count",
//            "/api/orders/user/*/exist",

//            Infra + Health
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
