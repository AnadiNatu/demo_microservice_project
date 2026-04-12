package com.microservice_demo.demo_service_1.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenValidator jwtTokenValidator;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String uri = request.getRequestURI();

        // Skip authentication for public endpoints
        if (isPublicUri(uri)) {
            log.debug("[DS1 Auth] Public endpoint, skipping auth: {}", uri);
            filterChain.doFilter(request, response);
            return;
        }

        String gatewayUsername = request.getHeader("X-User-Username");
        String gatewayRoles = request.getHeader("X-User-Roles");
        String gatewayUserId = request.getHeader("X-User-Id");
        String authHeader = request.getHeader("Authorization");

        log.info("[DS1 Auth] 🔍 Request to '{}' - GW Username: {}, GW Roles: {}, Auth Header: {}",
                uri, gatewayUsername, gatewayRoles, authHeader != null ? "Present" : "Missing");

        boolean authenticated = false;

        // Path A: Trust headers set by the API Gateway (PREFERRED)
        if (gatewayUsername != null && gatewayRoles != null) {
            try {
                List<SimpleGrantedAuthority> authorities = splitRoles(gatewayRoles);

                // Use GatewayAuthentication to preserve userId if available
                if (gatewayUserId != null && !gatewayUserId.isEmpty()) {
                    try {
                        Long userId = Long.parseLong(gatewayUserId);
                        GatewayAuthentication authToken = new GatewayAuthentication(gatewayUsername, userId, authorities);
                        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authToken);
                        log.info("[DS1 Auth] Via Gateway headers - user='{}' userId={} roles={}",
                                gatewayUsername, userId, authorities);
                    } catch (NumberFormatException e) {
                        UsernamePasswordAuthenticationToken authToken =
                                new UsernamePasswordAuthenticationToken(gatewayUsername, null, authorities);
                        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authToken);
                        log.info("[DS1 Auth] Via Gateway headers (no userId) - user='{}' roles={}", gatewayUsername, authorities);
                    }
                } else {
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(gatewayUsername, null, authorities);
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    log.info("[DS1 Auth] Via Gateway headers - user='{}' roles={}", gatewayUsername, authorities);
                }
                authenticated = true;
            } catch (Exception e) {
                log.error("[DS1 Auth] Failed to parse gateway headers: {}", e.getMessage(), e);
            }
        }
        // Path B: Validate a raw Bearer token (FALLBACK for direct calls)
        else if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                if (jwtTokenValidator.validateToken(token)) {
                    String username = jwtTokenValidator.getUsername(token);
                    List<String> rawRoles = jwtTokenValidator.getRoles(token);

                    // Ensure roles have ROLE_ prefix for Spring Security
                    List<SimpleGrantedAuthority> authorities = rawRoles.stream()
                            .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
                            .map(SimpleGrantedAuthority::new)
                            .collect(Collectors.toList());

                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(username, null, authorities);
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    log.info("[DS1 Auth] Via Bearer token - user='{}' roles={}", username, authorities);
                    authenticated = true;
                } else {
                    log.warn("[DS1 Auth] Invalid/expired Bearer token on request to '{}'", uri);
                }
            } catch (Exception e) {
                log.error("[DS1 Auth] Token validation failed: {}", e.getMessage(), e);
            }
        }

        if (!authenticated) {
            log.error("[DS1 Auth] NO VALID AUTHENTICATION for protected endpoint: {}", uri);
            log.error("[DS1 Auth] ️ This will result in 403 Forbidden or 401 Unauthorized");
            log.error("[DS1 Auth]Headers - X-User-Username: '{}', X-User-Roles: '{}', Authorization: '{}'",
                    gatewayUsername, gatewayRoles, authHeader != null ? "Bearer [token]" : "null");
        }

        filterChain.doFilter(request, response);
    }

    private List<SimpleGrantedAuthority> splitRoles(String rolesHeader) {
        if (rolesHeader == null || rolesHeader.trim().isEmpty()) {
            log.warn("[DS1 Auth] Empty roles header received, using default ROLE_USER");
            return List.of(new SimpleGrantedAuthority("ROLE_USER"));
        }

        List<SimpleGrantedAuthority> authorities = Arrays.stream(
                        rolesHeader
                                .replace("[", "")
                                .replace("]", "")
                                .replace("\"", "")
                                .trim()
                                .split(","))
                .map(String::trim)
                .filter(r -> !r.isEmpty())
                .map(role -> {
                    // Ensure ROLE_ prefix exists
                    if (!role.startsWith("ROLE_")) {
                        return "ROLE_" + role;
                    }
                    return role;
                })
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());

        log.debug("[DS1 Auth] Parsed roles from '{}' to {}", rolesHeader, authorities);
        return authorities;
    }

    private boolean isPublicUri(String uri) {
        return uri.equals("/api/users/sync")
                || uri.equals("/api/en1/test/public")
                || uri.startsWith("/actuator/");
    }
}