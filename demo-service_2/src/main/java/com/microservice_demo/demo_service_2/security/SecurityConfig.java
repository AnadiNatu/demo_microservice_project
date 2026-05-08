package com.microservice_demo.demo_service_2.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
@Slf4j
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        log.info("[DS2 Security] Configuring Security Filter Chain");

        http
                .csrf(AbstractHttpConfigurer::disable)

                .cors(cors -> cors.configurationSource(request -> {
                    CorsConfiguration config = new CorsConfiguration();
                    config.setAllowedOrigins(List.of("*"));
                    config.setAllowedMethods(List.of("GET" , "POST" , "PUT" , "DELETE" , "PATCH" ,"OPTIONS"));
                    config.setAllowedHeaders(List.of("*"));
                    config.setExposedHeaders(List.of("*"));
                    return config;
                }))

                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers("/api/en2/sync").permitAll()
                        .requestMatchers("/api/en2/sync/profile-picture").permitAll()
                        .requestMatchers("/api/en2/user/**").permitAll()
                        .requestMatchers("/api/en2/test/public").permitAll()
                        .requestMatchers("/api/orders/product/*/count").permitAll()
                        .requestMatchers("/api/orders/user/*/exists").permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        log.info("[DS2 Security] Security Filter Chain configured successfully");

        return http.build();
    }
}
