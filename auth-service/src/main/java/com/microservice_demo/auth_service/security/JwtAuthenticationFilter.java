package com.microservice_demo.auth_service.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsServiceImpl userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request , HttpServletResponse response , FilterChain filterChain) throws ServletException, IOException {

        String uri = request.getRequestURI();

        if (isPublicEndpoint(uri)){
            filterChain.doFilter(request , response);
            return;
        }

        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            try {
                if (jwtTokenProvider.validateToken(token)){
                    String username = jwtTokenProvider.getUsernameFromToken(token);
                    Set<String> roles = jwtTokenProvider.getRolesFromToken(token);

                    if(username != null && SecurityContextHolder.getContext().getAuthentication() == null){
                        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                        List<SimpleGrantedAuthority> authorities = roles.stream().map(SimpleGrantedAuthority::new).toList();
                        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(userDetails , null , authorities); // What all values can we put in this statement
                        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authToken);

                        log.debug("[AUTH] JWT authentication - user='{}' roles='{}'" , username , roles);
                    }
                }
            }catch (Exception ex){
                log.warn("[AUTH] JWT validation failed | uri={} | error={}", uri, ex.getMessage());
            }
        }
        filterChain.doFilter(request , response);
    }

    private boolean isPublicEndpoint(String uri){
        return uri.startsWith("/api/auth") ||
                uri.startsWith("/api/otp/") ||
                uri.startsWith("/api/password/") ||
                uri.startsWith("/api/oauth2/") ||
                uri.startsWith("/oauth2/") ||
                uri.startsWith("/login/oauth2/") ||
                uri.startsWith("/actuator/");
    }
}
