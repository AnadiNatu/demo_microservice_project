package com.microservice_demo.auth_service.security.oauth2;


import com.microservice_demo.auth_service.entity.Users;
import com.microservice_demo.auth_service.repository.UserRepository;
import com.microservice_demo.auth_service.security.JwtTokenProvider;
import com.microservice_demo.auth_service.security.UserDetailsServiceImpl;
import com.microservice_demo.auth_service.service.OAuth2ServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;


@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler
        implements AuthenticationSuccessHandler {

    public static final Logger log = LoggerFactory.getLogger(OAuth2SuccessHandler.class);

    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    private final JwtTokenProvider jwtTokenProvider;

    private final OAuth2ServiceImpl oAuth2Service;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication)
            throws IOException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        String provider = oAuth2Service.determineProvider(oAuth2User);

        // PROVIDER ID

        String providerId = oAuth2User.getAttribute("sub");

        // GitHub fallback
        if (providerId == null) {
            providerId = String.valueOf(oAuth2User.getAttribute("id"));
        }

        log.info("[OAUTH2] Success handler | provider={} | email={}", provider, email);

        // EMAIL VALIDATION
        if (email == null || email.isBlank()) {

            response.sendRedirect(
                    frontendUrl
                            + "/oauth2/callback?error="
                            + URLEncoder.encode(
                            "OAuth provider did not return email",
                            StandardCharsets.UTF_8
                    )
            );

            return;
        }

        try {

            // HANDLE USER
            Users user =
                    oAuth2Service.handleOAuthUser(
                            email,
                            name,
                            provider,
                            providerId,
                            oAuth2User
                    );

            // JWT TOKENs
            String token = jwtTokenProvider.generateTokenFromUser(user);
            String refreshToken = jwtTokenProvider.generateRefreshToken(user.getUsername());

            log.info("[OAUTH2] JWT generated | email={}", email);

            // REDIRECT URL
            String redirectUrl =
                    frontendUrl
                            + "/oauth2/callback"
                            + "?token=" + URLEncoder.encode(token, StandardCharsets.UTF_8)
                            + "&refreshToken=" + URLEncoder.encode(refreshToken, StandardCharsets.UTF_8)
                            + "&username=" + URLEncoder.encode(user.getUsername(), StandardCharsets.UTF_8)
                            + "&email=" + URLEncoder.encode(email, StandardCharsets.UTF_8)
                            + "&provider=" + URLEncoder.encode(provider, StandardCharsets.UTF_8)
                            + "&profilePicture=" + URLEncoder.encode(user.getProfilePicture() != null ? user.getProfilePicture() : "", StandardCharsets.UTF_8);

            response.sendRedirect(redirectUrl);

        } catch (Exception ex) {
            log.error("[OAUTH2] Success handler failed | error={}", ex.getMessage(), ex);
            response.sendRedirect(frontendUrl
                            + "/oauth2/callback?error="
                            + URLEncoder.encode(
                            ex.getMessage(),
                            StandardCharsets.UTF_8));
        }
    }
}