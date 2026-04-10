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
        String picture = oAuth2User.getAttribute("picture");

        log.info("[OAUTH2] Google login | email={} | name={}" , email , name);

        Users userEntity = userType2Repository.findByEmail(email).orElseGet(() -> {
            log.info("[OAUTH2 New Google Login | email={} | name={}]",email,name);

            String role = userEntity.getRoles().iterator().next().toLowerCase();
            String[] parts = (name != null ? name : "Google User").split(" ",2);
            String firstName = parts[0];
            String lastName = parts.length > 1 ? parts[1] : "";

            Users newUser = Users.builder()
                    .email(email)
                    .password("")
                    .roles()
                    .profilePicture(picture)
                    .build();

            return userType2Repository.save(newUser);
        });

        Users domainUser = new Users();
        domainUser.setId(userEntity.getId());
        domainUser.setEmail(userEntity.getEmail());
        domainUser.setFname(userEntity.getFname());
        domainUser.setLname(userEntity.getLname());
        domainUser.setPassword(userEntity.getPassword());
        domainUser.setRole(UserRoles2.valueOf(userEntity.getRole()));
        domainUser.setProfilePicture(userEntity.getProfilePicture());

        String token = jwtUtil.generateToken(userEntity.getEmail());

        log.info("[OAUTH2] JWT issued for Google user | email={}" , email);

        String redirectUrl = FRONTEND_REDIRECT
                + "?token=" + URLEncoder.encode(token , StandardCharsets.UTF_8)
                + "&email=" + URLEncoder.encode(email , StandardCharsets.UTF_8)
                + "&name=" + URLEncoder.encode(name != null ? name : "" , StandardCharsets.UTF_8);

        response.sendRedirect(redirectUrl);

    }

}