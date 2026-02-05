package com.microservice_demo.demo_service_1.feign;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class FeignClientInterceptor implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate requestTemplate) {

        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        if (attributes != null){
            HttpServletRequest request = attributes.getRequest();

            String authHeader = request.getHeader("Authorization");
            if (authHeader != null){
                requestTemplate.header("Authorization" , authHeader);
            }

            String username = request.getHeader("X-User-Username");
            if (username != null){
                requestTemplate.header("X-User-Username" , username);
            }

            String roles = request.getHeader("X-User-Roles");
            if (roles != null){
                requestTemplate.header("X-User-Roles" , roles);
            }
        }
    }
}
