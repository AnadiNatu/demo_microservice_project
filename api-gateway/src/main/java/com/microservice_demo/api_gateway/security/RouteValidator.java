package com.microservice_demo.api_gateway.security;

import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Predicate;

@Component
public class RouteValidator {

    public static final List<String> openApiEndpoints = List.of(
            "/api/auth/register",
            "/api/auth/login",
            "/api/auth/refresh",
            "/api/auth/health",
//            "/api/users/sync",
//            "/api/en2/sync",
            "/api/*/sync",
            "/api/en2/user/{id}",
//            "/api/en1/test/public",
//            "/api/en2/test/public",
            "/api/*/test/public",
            "/eureka/**"
    );

    public Predicate<ServerHttpRequest> isSecured = request -> openApiEndpoints.stream().noneMatch(uri -> request.getURI().getPath().contains(uri));
}
