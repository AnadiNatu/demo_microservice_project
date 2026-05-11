package com.microservice_demo.demo_service_2.security;

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
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        String uri = request.getRequestURI();

        if (isPublicUri(uri)) {
            filterChain.doFilter(request, response);
            return;
        }

        String gatewayUser = request.getHeader("X-User-Username");
        String gatewayRoles = request.getHeader("X-User-Roles");
        String gatewayUserId = request.getHeader("X-User-Id");
        String authHeader = request.getHeader("Authorization");

        log.info("[DS2 Auth] Request to '{}' - GW Username: {}, GW Roles: {}, Auth Header: {}",
                uri, gatewayUser, gatewayRoles, authHeader != null ? "Present" : "Missing");
        boolean authentication = false;
//        Path A : trust Gateway-forwarded headers
//        if (gatewayUser != null && gatewayRoles != null && SecurityContextHolder.getContext().getAuthentication() == null)

        if (gatewayUser != null && gatewayRoles != null) {
            try {
                List<SimpleGrantedAuthority> authorities = parseRoles(gatewayRoles);
                if (gatewayUserId != null && !gatewayUserId.isEmpty()) {
                    try {
                        Long userId = Long.parseLong(gatewayUserId);
                        GatewayAuthentication authToken = new GatewayAuthentication(gatewayUser, userId, authorities);
                        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authToken);
                        log.info("[DS2 Auth] Via Gateway headers - user='{}' userId={} roles={}",
                                gatewayUser, userId, authorities);
                    } catch (NumberFormatException ex) {
                        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(gatewayUser, null, authorities);
                        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authToken);
                        log.info("[DS2 Auth] Via Gateway headers (no userId) - user='{}' roles={}", gatewayUser, authorities);

                    }
                } else {
                    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(gatewayUser, null, authorities);
                    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                    log.debug("[DS2 Auth] Via Gateway headers - user='{}' roles='{}'", gatewayUser, gatewayRoles);
                }
                authentication = true;
            } catch (Exception ex) {
                log.error("[DS2 Auth] Failed to parse gateway headers: {}", ex.getMessage(), ex);
            }
        }

        // Path B : validate row Bearer token
        else if (authHeader != null && authHeader.startsWith("Bearer ") && SecurityContextHolder.getContext().getAuthentication() == null) {
            String token = authHeader.substring(7);
            try {
                if (jwtTokenValidator.validateToken(token)) {
                    String username = jwtTokenValidator.getUsername(token);
                    List<String> roles = jwtTokenValidator.getRoles(token);

                    List<SimpleGrantedAuthority> authorities = roles
                            .stream()
                            .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
                            .map(SimpleGrantedAuthority::new)
                            .collect(Collectors.toList());

                    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(username, null, authorities);
                    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                    log.info("[DS2 Auth] Via Bearer token - user='{}' roles={}", username, authorities);
//                    log.debug("[DS2] Auth Via Bearer token - user='{}'", username);
                    authentication = true;
                } else {
                    log.warn("[DS2 Auth] Invalid/expired Bearer token on '{}'", uri);
                }
            }catch (Exception ex){
                log.error("[DS2 Auth] Token validation failed: {}", ex.getMessage(), ex);
            }
        }

        if(!authentication){
            log.error("[DS2 Auth] NO VALID AUTHENTICATION for protected endpoint: {}", uri);
            log.error("[DS2 Auth] ️ This will result in 403 Forbidden or 401 Unauthorized");
            log.error("[DS2 Auth]Headers - X-User-Username: '{}', X-User-Roles: '{}', Authorization: '{}'",
                    gatewayUser, gatewayRoles, authHeader != null ? "Bearer [token]" : "null");
        }
        filterChain.doFilter(request, response);
    }

    private boolean isPublicUri(String uri) {
        return uri.equals("/api/en2/sync") ||
                uri.startsWith("/api/en2/user") ||
                uri.contains("/api/en2/test/public") ||

                uri.equals("/api/en2/sync/profile-picture")||
                uri.matches("/api/orders/product/\\d+/count") ||
                uri.matches("/api/orders/user/\\d+/exists") ||
                uri.startsWith("/actuator/") ||

                (uri.startsWith("/api/orders/product") && uri.endsWith("/count") )||
                (uri.startsWith("/api/orders/user") && uri.endsWith("/exists"));
    }

    private List<SimpleGrantedAuthority> parseRoles(String rolesHeader) {
        if (rolesHeader == null || rolesHeader.trim().isEmpty()) {
            log.warn("[DS2 Auth] Empty roles header, defaulting to ROLE_USER");
            return List.of(new SimpleGrantedAuthority("ROLE_USER"));
        }
        return Arrays.stream(rolesHeader
                        .replace("[", "")
                        .replace("]", "")
                        .replace("\"", "")
                        .trim()
                        .split(","))
                .map(String::trim)
                .filter(r -> !r.isEmpty())
                .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)  // FIX: was "Role"
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }
}
