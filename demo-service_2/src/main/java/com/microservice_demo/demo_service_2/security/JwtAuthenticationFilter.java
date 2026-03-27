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
        String authHeader = request.getHeader("Authorization");

//        Path A : trust Gateway-forwarded headers
        if (gatewayUser != null && gatewayRoles != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            List<SimpleGrantedAuthority> authorities = parseRoles(gatewayRoles);
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(gatewayUser, null, authorities);
            auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(auth);

            log.debug("[DS2 Auth] Via Gateway headers - user='{}' roles='{}'", gatewayUser, gatewayRoles);
        } // Path B : validate row Bearer token
        else if (authHeader != null && authHeader.startsWith("Bearer ") && SecurityContextHolder.getContext().getAuthentication() == null) {

            String token = authHeader.substring(7);

            if (jwtTokenValidator.validateToken(token)) {
                String username = jwtTokenValidator.getUsername(token);
                List<String> roles = jwtTokenValidator.getRoles(token);

                List<SimpleGrantedAuthority> authorities = roles
                        .stream()
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());

                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(username, null, authorities);
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);
                log.debug("[DS2] Auth Via Bearer token - user='{}'", username);
            } else {
                log.warn("[DS2 Auth] Invalid/expired Bearer token on '{}'", uri);
            }
        }
        filterChain.doFilter(request, response);
    }

    private boolean isPublicUri(String uri) {
        return uri.contains("/api/en2/sync") ||
                uri.contains("/api/en2/user/") ||
                uri.contains("/api/en2/test/public") ||
                (uri.startsWith("/api/orders/product/") && uri.endsWith("/count") )||
                (uri.startsWith("/api/orders/user/") && uri.endsWith("/exists"));
    }

    private List<SimpleGrantedAuthority> parseRoles(String rolesHeader) {
        return Arrays.stream(rolesHeader
                        .replace("[", "")
                        .replace("]", "")
                        .replace("\"", "")
                        .trim()
                        .split(","))
                .map(String::trim)
                .filter(r -> !r.isEmpty())
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }
}
//    @Override
//    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
//
//
//        if(request.getRequestURI().contains("/api/en2/sync") ||
//        request.getRequestURI().contains("/api/en2/user/")){
//            filterChain.doFilter(request , response);
//            return;
//        }
//
//        String username = request.getHeader("X-User-Username");
//        String rolesHeader = request.getHeader("X-User-Roles");
//
//        String authHeader = request.getHeader("Authorization");
//        String token = null;
//
//        if(authHeader != null && authHeader.startsWith("Bearer ")){
//            token = authHeader.substring(7);
//        }
//
//        if (username != null && rolesHeader != null && SecurityContextHolder.getContext().getAuthentication() == null){
//
//            String cleanedRoleString = rolesHeader
//                    .replace("[" , "")
//                    .replace("]" , "")
//                    .replace("\"" , "")
//                    .trim();
//
//            List<SimpleGrantedAuthority> authorities = Arrays.stream(cleanedRoleString.split(","))
//                    .map(String::trim)
//                    .filter(role -> !role.isEmpty())
//                    .map(SimpleGrantedAuthority::new)
//                    .collect(Collectors.toList());
//
//            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(username , null , authorities);
//
//            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
//            SecurityContextHolder.getContext().setAuthentication(authToken);
//        }
//        filterChain.doFilter(request , response);
//    }