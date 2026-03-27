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
    protected void doFilterInternal(HttpServletRequest request,  HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        String uri = request.getRequestURI();

        // Skip authentication for sync endpoint
//        if (request.getRequestURI().contains("/api/users/sync") || uri.endsWith("/api/en1/test/public")) {
//            filterChain.doFilter(request, response);
//            return;
//        }

        // Skip authentication for all public / internal-Feign endpoints
        if (isPublicUri(uri)) {
            filterChain.doFilter(request, response);
            return;
        }


        String gatewayUsername = request.getHeader("X-User-Username");
        String gatewayRoles = request.getHeader("X-User-Roles");
        String authHeader = request.getHeader("Authorization");

//        Path A : trust header set by the API Gateway
        if (gatewayUsername != null && gatewayRoles != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            List<SimpleGrantedAuthority> authorities = splitRoles(gatewayRoles);
            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(gatewayUsername, null, authorities);
            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authToken);
            log.debug("[DS1 Auth] Via Gateway header - user='{}' roles='{}' ", gatewayUsername, gatewayRoles);
        }// Path B : validate a raw Bearer token
        else if (authHeader != null && authHeader.startsWith("Bearer ") && SecurityContextHolder.getContext().getAuthentication() == null) {
            String token = authHeader.substring(7);
            if (jwtTokenValidator.validateToken(token)) {
                String username = jwtTokenValidator.getUsername(token);
                List<String> rawRoles = jwtTokenValidator.getRoles(token);
                List<SimpleGrantedAuthority> authorities = rawRoles.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList());
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(username, null, authorities);
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
                log.debug("[DS1 Auth] Via Bearer token — user='{}' roles='{}'",
                        username, rawRoles);
            }
        } else {
            log.warn("[DS1 Auth] Invalid/expired Bearer token on request to '{}'", uri);
        }
        filterChain.doFilter(request , response);
    }

    private List<SimpleGrantedAuthority> splitRoles(String rolesHeader){
        return Arrays.stream(
                rolesHeader
                        .replace("[" , "").replace("]" , "").replace("\"" , "").trim().split(","))
                        .map(String :: trim)
                .filter(r -> !r.isEmpty())
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());

    }
//        // 1️⃣ Gateway forwarded headers
//        String usernameHeader = request.getHeader("X-User-Username");
//        String rolesHeader = request.getHeader("X-User-Roles");
//
//        // 2️⃣ Normal Authorization: Bearer token
//        String authHeader = request.getHeader("Authorization");
//        String token = null;
//
//        if (authHeader != null && authHeader.startsWith("Bearer ")) {
//            token = authHeader.substring(7);
//        }
//
//        // 🔹 First preference: Use Gateway headers
//        if (usernameHeader != null &&
//                rolesHeader != null &&
//                SecurityContextHolder.getContext().getAuthentication() == null) {
//
//            // Clean role header → remove brackets, quotes, whitespace
//            String cleanedRoleString = rolesHeader
//                    .replace("[", "")
//                    .replace("]", "")
//                    .replace("\"", "")
//                    .trim();
//
//            List<SimpleGrantedAuthority> authorities = Arrays.stream(cleanedRoleString.split(","))
//                    .map(String::trim)
//                    .filter(role -> !role.isEmpty())
//                    .map(SimpleGrantedAuthority::new)
//                    .collect(Collectors.toList());
//
//            UsernamePasswordAuthenticationToken authentication =
//                    new UsernamePasswordAuthenticationToken(
//                            usernameHeader,
//                            null,
//                            authorities
//                    );
//
//            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
//            SecurityContextHolder.getContext().setAuthentication(authentication);
//        }
//
//        // 🔹 Second preference: Validate JWT if gateway headers missing
//        else if (token != null &&
//                jwtTokenValidator.validateToken(token) &&
//                SecurityContextHolder.getContext().getAuthentication() == null) {
//
//            String username = jwtTokenValidator.getUsername(token);
//            List<String> roles = jwtTokenValidator.getRoles(token);
//
//            List<SimpleGrantedAuthority> authorities = roles.stream()
//                    .map(SimpleGrantedAuthority::new)
//                    .collect(Collectors.toList());
//
//            UsernamePasswordAuthenticationToken authentication =
//                    new UsernamePasswordAuthenticationToken(
//                            username,
//                            null,
//                            authorities
//                    );
//
//            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
//            SecurityContextHolder.getContext().setAuthentication(authentication);
//        }
//        filterChain.doFilter(request, response);
//    }

    private boolean isPublicUri(String uri) {
        return uri.equals("/api/users/sync")
                || uri.equals("/api/en1/test/public");
    }

}