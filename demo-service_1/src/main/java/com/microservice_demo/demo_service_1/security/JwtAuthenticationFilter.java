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

        if (isPublicUri(uri)) {
            log.debug("[DS1 Auth] Public endpoint — skipping auth | uri='{}'", uri);
            filterChain.doFilter(request, response);
            return;
        }

        String gatewayUsername = request.getHeader("X-User-Username");
        String gatewayRoles    = request.getHeader("X-User-Roles");
        String gatewayUserId   = request.getHeader("X-User-Id");
        String authHeader      = request.getHeader("Authorization");

        log.debug("[DS1 Auth] uri='{}' gwUser='{}' gwRoles='{}' auth={}",
                uri, gatewayUsername, gatewayRoles, authHeader != null ? "present" : "absent");

        boolean authenticated = false;

        // ── Path A: trust gateway-forwarded headers
        if (gatewayUsername != null && gatewayRoles != null) {
            try {
                List<SimpleGrantedAuthority> authorities = splitRoles(gatewayRoles);

                if (gatewayUserId != null && !gatewayUserId.isBlank()) {
                    try {
                        Long userId = Long.parseLong(gatewayUserId);
                        GatewayAuthentication authToken =
                                new GatewayAuthentication(gatewayUsername, userId, authorities);
                        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authToken);
                        log.info("[DS1 Auth] Via gateway headers | user='{}' userId={} roles={}",
                                gatewayUsername, userId, authorities);
                    } catch (NumberFormatException e) {
                        setSimpleAuth(gatewayUsername, authorities, request);
                        log.info("[DS1 Auth] Via gateway headers (no userId) | user='{}' roles={}",
                                gatewayUsername, authorities);
                    }
                } else {
                    setSimpleAuth(gatewayUsername, authorities, request);
                    log.info("[DS1 Auth] Via gateway headers | user='{}' roles={}",
                            gatewayUsername, authorities);
                }
                authenticated = true;

            } catch (Exception e) {
                log.error("[DS1 Auth] Failed to parse gateway headers: {}", e.getMessage(), e);
            }
        }
        // ── Path B: validate raw Bearer token (dev/testing fallback)
        else if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                if (jwtTokenValidator.validateToken(token)) {
                    String username = jwtTokenValidator.getUsername(token);
                    List<String> roles = jwtTokenValidator.getRoles(token);

                    List<SimpleGrantedAuthority> authorities = roles.stream()
                            .map(r -> r.startsWith("ROLE_") ? r : "ROLE_" + r)
                            .map(SimpleGrantedAuthority::new)
                            .collect(Collectors.toList());

                    setSimpleAuth(username, authorities, request);
                    log.info("[DS1 Auth] Via Bearer token | user='{}' roles={}", username, authorities);
                    authenticated = true;
                } else {
                    log.warn("[DS1 Auth] Invalid/expired token | uri='{}'", uri);
                }
            } catch (Exception e) {
                log.error("[DS1 Auth] Token validation failed: {}", e.getMessage(), e);
            }
        }

        if (!authenticated) {
            log.warn("[DS1 Auth] No valid authentication for protected uri='{}'", uri);
        }

        filterChain.doFilter(request, response);
    }

    private void setSimpleAuth(String username,
                               List<SimpleGrantedAuthority> authorities,
                               HttpServletRequest request) {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(username, null, authorities);
        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private List<SimpleGrantedAuthority> splitRoles(String rolesHeader) {
        if (rolesHeader == null || rolesHeader.isBlank()) {
            log.warn("[DS1 Auth] Empty roles header — defaulting to ROLE_USER");
            return List.of(new SimpleGrantedAuthority("ROLE_USER"));
        }
        return Arrays.stream(rolesHeader
                        .replace("[", "").replace("]", "").replace("\"", "")
                        .trim().split(","))
                .map(String::trim)
                .filter(r -> !r.isEmpty())
                .map(r -> r.startsWith("ROLE_") ? r : "ROLE_" + r)
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }

    private boolean isPublicUri(String uri) {
        return uri.equals("/api/users/sync")
                || uri.equals("/api/users/sync/profile-picture")
                || uri.equals("/api/en1/test/public")
                || uri.startsWith("/actuator/");
    }
}