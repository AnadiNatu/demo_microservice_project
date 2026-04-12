package com.microservice_demo.auth_service.security.oauth2;


import com.microservice_demo.auth_service.entity.Users;
import com.microservice_demo.auth_service.repository.UserRepository;
import com.microservice_demo.auth_service.security.JwtTokenProvider;
import com.microservice_demo.auth_service.security.UserDetailsServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    public static final Logger log = LoggerFactory.getLogger(OAuth2SuccessHandler.class);

    private static final String FRONTEND_REDIRECT = "http://localhost:3000/oauth2/callback";

    private final UserRepository userType2Repository;
    private final JwtTokenProvider jwtUtil;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request , HttpServletResponse response, Authentication authentication) throws IOException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
//        String picture = oAuth2User.getAttribute("picture");

        log.info("[OAUTH2] Google login | email={} | name={}" , email , name);

//        Users userEntity = userType2Repository.findByEmail(email).orElseGet(() -> {

//        log.info("[OAUTH2 New Google Login | email={} | name={}]",email,name);

        response.sendRedirect("/api/oauth2/success");
    }
}