package com.microservice_demo.demo_service_1.security;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

public class GatewayAuthentication extends UsernamePasswordAuthenticationToken {

    private final Long userId;

    public GatewayAuthentication(String username, Long userId , Collection<? extends GrantedAuthority> authorities){
        super(username , null , authorities);
        this.userId = userId;
    }

    public Long getUserId(){
        return userId;
    }
}
