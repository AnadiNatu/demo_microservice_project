package com.microservice_demo.demo_service_1.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;

import java.util.List;

//@Configuration
//@EnableWebSecurity
//@EnableMethodSecurity(prePostEnabled = true)
//@RequiredArgsConstructor
//public class SecurityConfig {
//
//    private final JwtAuthenticationFilter jwtAuthFilter;
//
//    @Bean
//    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
//
//        http
//                // Disable CSRF (stateless microservice)
//                .csrf(AbstractHttpConfigurer::disable)
//
//                // Enable CORS (important when gateway passes requests)
//                .cors(cors -> cors.configurationSource(request -> {
//                    CorsConfiguration config = new CorsConfiguration();
//                    config.setAllowedOrigins(List.of("*"));
//                    config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
//                    config.setAllowedHeaders(List.of("*"));
//                    return config;
//                }))
//
//                // Stateless session because token is passed on each request
//                .sessionManagement(session ->
//                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
//
//                // Route permissions
//                .authorizeHttpRequests(auth -> auth
//                        .requestMatchers("/actuator/**").permitAll()     // health checks
//                        .requestMatchers("/api/users/sync").permitAll()  // 🔥 Allow sync endpoint without auth
//                        .requestMatchers("/api/en1/test/public").permitAll()
//                        .anyRequest().authenticated()                    // everything else secured
//                )
//
//                // Add JWT filter BEFORE UsernamePasswordAuthenticationFilter
//                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
//
//        return http.build();
//    }
//
//    // Required in Spring Security 6+ to avoid bean creation errors
//    @Bean
//    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
//        return config.getAuthenticationManager();
//    }
//}

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
@Slf4j
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        log.info("[DS1 Security] Configuring Security Filter Chain");

        http
                .csrf(AbstractHttpConfigurer::disable)

                .cors(cors -> cors.configurationSource(request -> {
                    CorsConfiguration config = new CorsConfiguration();
                    config.setAllowedOrigins(List.of("*"));
                    config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
                    config.setAllowedHeaders(List.of("*"));
                    config.setExposedHeaders(List.of("*"));  // New line added
                    return config;
                }))

                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> auth
                        // Public endpoints - NO authentication required
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers("/api/users/sync").permitAll()
                        .requestMatchers("/api/users/sync/profile-picture").permitAll()
                        .requestMatchers("/api/en1/test/public").permitAll()
                        // All other endpoints require authentication
                        .anyRequest().authenticated()
                )

                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        log.info("[DS1 Security] Security Filter Chain configured successfully");

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        log.info("[DS1 Security] 🔧 Creating AuthenticationManager bean");
        return config.getAuthenticationManager();
    }
}
