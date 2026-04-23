package com.microservice_demo.auth_service.service;

import com.microservice_demo.auth_service.entity.Users;
import com.microservice_demo.auth_service.feign.DemoService1FeignClient;
import com.microservice_demo.auth_service.feign.DemoService2FeignClient;
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
    private final DemoService1FeignClient demoService1Client;
    private final DemoService2FeignClient demoService2Client;
    private final AuthService authService;

    public Users handleOAuthUser(String email , String name , String provider , String providerId){
        log.info("[OAUTH2] Handling OAuth user | email={} | provider={}", email, provider);

        return userRepository.findByEmail(email)
                .map(existing -> {
                    log.info("[OAUTH2] Existing user found : {}" , email);

                    existing.setProvider(provider);
                    existing.setProviderId(providerId);
                    return userRepository.save(existing);
                })
                .orElseGet(() -> createOAuthUser(email , name , provider , providerId));
    }

//       * Overload used by the OAuth2 success handler where the full OAuth2User
//     * object is available, allowing profile picture extraction.
    public Users handleOAuthUser(String email, String name,
                                 String provider, String providerId,
                                 OAuth2User oAuth2User) {
        log.info("[OAUTH2] Handling OAuth user (full) | email={} | provider={}", email, provider);

        return userRepository.findByEmail(email)
                .map(existing -> {
                    log.info("[OAUTH2] Existing user found | email={}", email);
                    existing.setProvider(provider);
                    existing.setProviderId(providerId);

                    // Update profile picture if the provider supplies one and we don't have one
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

    private Users createOAuthUser(String email, String name,
                                  String provider, String providerId,
                                  OAuth2User oAuth2User) {
        log.info("[OAUTH2] Creating new OAuth user | email={} | provider={}", email, provider);

        Set<String> roles = new HashSet<>();
        roles.add("ROLE_USER");

        // Derive a username from the display name or the email local-part
        String username;
        if (name != null && !name.isBlank()) {
            username = name.trim().split("\\s+")[0];
        } else {
            username = email.split("@")[0];
        }

        // Extract profile picture URL provided by the OAuth2 provider
        String picture = null;
        if (oAuth2User != null) {
            picture = extractPictureUrl(oAuth2User, provider);
        }

        Users user = Users.builder()
                .username(username)
                .email(email)
                .password("")           // no password for OAuth2 users
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
            authService.syncUserToMicroservices(savedUser);
        } catch (Exception ex) {
            log.warn("[OAUTH2] Failed to sync user to microservices | error={}", ex.getMessage());
        }

        return savedUser;
    }

    private Users createOAuthUser(String email, String name, String provider, String providerId){
        log.info("[OAUTH2] Creating new OAuth user | email = {} | provider = {}" , email , provider);

        Set<String> roles = new HashSet<>();
        roles.add("ROLE_USER");

        String username;
        if (name != null && !name.isBlank()){
            String[] parts = name.trim().split("\\s+" , 2);
            username = parts[0];
        }else {
            username = email.split("@")[0];
        }

        Users user = Users.builder()
                .username(username)
                .email(email)
                .password("")
                .phoneNumber(null)
                .roles(roles)
                .provider(provider)
                .providerId(providerId)
                .profilePicture(null)
                .enabled(true)
                .accountNonExpired(true)
                .accountNonLocked(true)
                .credentialsNonExpired(true)
                .build();

        Users savedUser = userRepository.save(user);
        log.info("✅ [OAUTH2] User created successfully: {} with provider: {}", savedUser.getUsername(), provider);

        try {
            authService.syncUserToMicroservices(savedUser);
        } catch (Exception ex) {
            log.warn("⚠️ [OAUTH2] Failed to sync user to microservices: {}", ex.getMessage());
        }
        return savedUser;
    }

    private String extractPictureUrl(OAuth2User oAuth2User , String provider){
        if ("GOOGLE".equalsIgnoreCase(provider)){
            return oAuth2User.getAttribute("picture");
        }

        if ("GITHUB".equalsIgnoreCase(provider)){
            return oAuth2User.getAttribute("avatar_url");
        }
//        Generic fallback
        String pic = oAuth2User.getAttribute("picture");
        if (pic == null){
            pic = oAuth2User.getAttribute("avatar_url");
        }
        return pic;
    }

    public Map<String , Object> handleFailure(String error){
        log.warn("[OAUTH2] Login failed | error = {} " , error);

        return Map.of(
                "error" , "OAuth2 authentication failed",
                "message" , error != null ? error : "Authentication was cancelled",
                "suggestion" , "Please try again or use email/password login"
        );
    }

    public  Map<String , Object> extractUserInfo(OAuth2User oAuth2User){
        String provider = determineProvider(oAuth2User);

        return Map.of(
                "provider", provider,
                "email", oAuth2User.getAttribute("email"),
                "name", oAuth2User.getAttribute("name"),
                "attributes", oAuth2User.getAttributes()
        );
    }

//    public Users createOAuthUser(String email , String name , String provider , String providerId){
//        log.info("[OAUTH2] Creating user | email={} | provider={}", email, provider);
//
//        Users users = new Users();
//
//        users.setEmail(email);
//
//        if (name != null && !name.isBlank()){
//            String[] parts = name.trim().split("\\s+" , 2);
//            users.setUsername(parts[0]);
//        }else {
//            users.setUsername(email.split("@")[0]);
//        }
//
//        users.setRoles();
//        users.setProvider(provider);
//        users.setProviderId(providerId);
//
//        return userRepository.save(users);
//    }

    public String determineProvider(OAuth2User oAuth2User){
        if (oAuth2User.getAttribute("sub") != null) return "GOOGLE";
        if (oAuth2User.getAttribute("login") != null) return "GITHUB";

        return "UNKNOWN";
    }
}