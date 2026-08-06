package com.microservice_demo.demo_service_1.feign;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

//@Configuration
//public class FeignConfig {
//
//    @Bean
//    public FeignClientInterceptor feignClientInterceptor(){
//        return new FeignClientInterceptor();
//    }
//
//}


import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class FeignConfig {

    @Bean
    public RequestInterceptor feignRequestInterceptor() {
        return requestTemplate -> {
            // Try to get current HTTP request context
            try {
                ServletRequestAttributes attributes =
                        (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

                if (attributes != null) {
                    HttpServletRequest request = attributes.getRequest();

                    // Forward JWT token if present
                    String authHeader = request.getHeader("Authorization");
                    if (authHeader != null && !authHeader.isEmpty()) {
                        requestTemplate.header("Authorization", authHeader);
                        log.debug("[Feign] Forwarding Authorization header");
                    }

                    // Forward gateway headers
                    String userId = request.getHeader("X-User-Id");
                    String username = request.getHeader("X-User-Username");
                    String roles = request.getHeader("X-User-Roles");

                    if (userId != null) requestTemplate.header("X-User-Id", userId);
                    if (username != null) requestTemplate.header("X-User-Username", username);
                    if (roles != null) requestTemplate.header("X-User-Roles", roles);

                    log.debug("[Feign] Forwarding gateway headers | userId={}", userId);
                }
            } catch (IllegalStateException e) {
                log.debug("[Feign] No request context (async/scheduled call)");
                // This is OK — Feign calls outside of HTTP context won't have headers
            }
        };
    }
}
