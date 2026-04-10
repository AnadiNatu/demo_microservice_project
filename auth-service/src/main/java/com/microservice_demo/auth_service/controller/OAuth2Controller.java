package com.microservice_demo.auth_service.controller;

import com.microservice_demo.auth_service.entity.Users;
import com.microservice_demo.auth_service.security.JwtTokenProvider;
import com.microservice_demo.auth_service.service.OAuth2ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/oauth2/")
@CrossOrigin("*")
@RequiredArgsConstructor
@Slf4j
public class OAuth2Controller {

    private final JwtTokenProvider jwtUtil;
    private final OAuth2ServiceImpl oAuth2Service;

    @GetMapping("success")
    public ResponseEntity<Map<String , Object>> oauth2Success(@AuthenticationPrincipal OAuth2User oAuth2User){
        if (oAuth2User == null){
            log.error("[OAUTH2] No OAuth2User principal found");
            return ResponseEntity.badRequest().body(Map.of(
                    "error" , "Authentication failed",
                    "message" , "No user information received from OAuth2 previous"
            ));
        }
        try{
            String email = oAuth2User.getAttribute("email");
            String name = oAuth2User.getAttribute("name");
            String provider = oAuth2Service.determineProvider(oAuth2User);

//            Important
            String providerId = oAuth2User.getAttribute("sub");
            if (providerId == null){
                providerId = oAuth2User.getAttribute("id");
            }

            Users users = oAuth2Service.handleOAuthUser(email , name , provider, providerId);
            String token = jwtUtil.generateTokenFromUser(users);
        }
    }

}