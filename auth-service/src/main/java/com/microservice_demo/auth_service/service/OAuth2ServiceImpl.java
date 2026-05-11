package com.microservice_demo.auth_service.service;

import com.microservice_demo.auth_service.entity.Users;
import com.microservice_demo.auth_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
public class OAuth2ServiceImpl {

    private final UserRepository userRepository;
    private final UserSyncService userSyncService;

    public Users handleOAuthUser(String email, String name, String provider,
                                 String providerId, OAuth2User oAuth2User) {
        log.info("[OAUTH2] Handling OAuth user | email={} | provider={}", email, provider);

        return userRepository.findByEmail(email)
                .map(existing -> {
                    log.info("[OAUTH2] Existing user found | email={}", email);
                    existing.setProvider(provider);
                    existing.setProviderId(providerId);

                    if (existing.getProfilePicture() == null && oAuth2User != null) {
                        String picture = extractPictureUrl(oAuth2User, provider);
                        if (picture != null) {
                            existing.setProfilePicture(picture);
                        }
                    }
                    return userRepository.save(existing);
                })
                .orElseGet(() -> createOAuthUser(email, name, provider, providerId, oAuth2User));
    }

    public Users handleOAuthUser(String email, String name, String provider, String providerId) {
        return handleOAuthUser(email, name, provider, providerId, null);
    }

    private Users createOAuthUser(String email, String name, String provider,
                                  String providerId, OAuth2User oAuth2User) {
        log.info("[OAUTH2] Creating new OAuth user | email={} | provider={}", email, provider);

        Set<String> roles = new HashSet<>();
        roles.add("ROLE_USER");

        String username;
        if (name != null && !name.isBlank()) {
            username = name.trim().split("\\s+")[0];
        } else {
            username = email.split("@")[0];
        }

        String picture = null;
        if (oAuth2User != null) {
            picture = extractPictureUrl(oAuth2User, provider);
        }

        Users user = Users.builder()
                .username(username)
                .email(email)
                .password("")
                .phoneNumber(null)
                .roles(roles)
                .provider(provider)
                .providerId(providerId)
                .profilePicture(picture)
                .enabled(true)
                .accountNonExpired(true)
                .accountNonLocked(true)
                .credentialsNonExpired(true)
                .build();

        Users savedUser = userRepository.save(user);
        log.info("[OAUTH2] User created | email={} | provider={} | picture={}",
                email, provider, picture != null ? "yes" : "none");

        try {
            userSyncService.syncUserToMicroservices(savedUser);
        } catch (Exception ex) {
            log.warn("[OAUTH2] Failed to sync user to microservices | error={}", ex.getMessage());
        }

        return savedUser;
    }

    private String extractPictureUrl(OAuth2User oAuth2User, String provider) {
        if ("GOOGLE".equalsIgnoreCase(provider)) {
            return oAuth2User.getAttribute("picture");
        }
        if ("GITHUB".equalsIgnoreCase(provider)) {
            return oAuth2User.getAttribute("avatar_url");
        }
        String pic = oAuth2User.getAttribute("picture");
        if (pic == null) {
            pic = oAuth2User.getAttribute("avatar_url");
        }
        return pic;
    }

    public Map<String, Object> handleFailure(String error) {
        log.warn("[OAUTH2] Login failed | error={}", error);
        return Map.of(
                "error", "OAuth2 authentication failed",
                "message", error != null ? error : "Authentication was cancelled",
                "suggestion", "Please try again or use email/password login"
        );
    }

    public Map<String, Object> extractUserInfo(OAuth2User oAuth2User) {
        String provider = determineProvider(oAuth2User);
        return Map.of(
                "provider", provider,
                "email", oAuth2User.getAttribute("email"),
                "name", oAuth2User.getAttribute("name"),
                "attributes", oAuth2User.getAttributes()
        );
    }

    public String determineProvider(OAuth2User oAuth2User) {
        if (oAuth2User.getAttribute("sub") != null) return "GOOGLE";
        if (oAuth2User.getAttribute("login") != null) return "GITHUB";
        return "UNKNOWN";
    }
}