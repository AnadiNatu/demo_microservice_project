package com.microservice_demo.auth_service.service;

import com.microservice_demo.auth_service.dto.*;
import com.microservice_demo.auth_service.entity.Users;
import com.microservice_demo.auth_service.repository.UserRepository;
import com.microservice_demo.auth_service.security.JwtTokenProvider;
import com.microservice_demo.auth_service.security.UserDetailsServiceImpl;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public AuthResponse register(RegisterRequest request) {

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Error : Username is already taken");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Error : Email is already in use");
        }

        Set<String> roles = new HashSet<>();
        if (request.getRoles() == null || request.getRoles().isEmpty()) {
            roles.add("ROLE_USER");
        } else {
            request.getRoles().forEach(role -> {
                if (!role.startsWith("ROLE_")) {
                    roles.add("ROLE_" + role.toUpperCase());
                } else {
                    roles.add(role.toUpperCase());
                }
            });
        }
        Users user = Users.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .roles(roles)
                .enabled(true)
                .accountNonExpired(true)
                .accountNonLocked(true)
                .credentialsNonExpired(true)
                .build();
        userRepository.save(user);

//        Auto-login after registration
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        String jwt = jwtTokenProvider.generateToken(authentication);
        String refreshToken = jwtTokenProvider.generateRefreshToken(request.getUsername());

        Users userDetails = (Users) authentication.getPrincipal();

        return AuthResponse.builder()
                .token(jwt)
                .refreshToken(refreshToken)
                .username(userDetails.getUsername())
                .email(user.getEmail())
                .roles(user.getRoles())
                .expiresIn(jwtTokenProvider.getExpirationMs())
                .build();
    }

    public AuthResponse login(LoginRequest request){
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername() , request.getPassword())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        String jwt = jwtTokenProvider.generateToken(authentication);
        String refreshToken = jwtTokenProvider.generateRefreshToken(request.getUsername());
        Users users = (Users) authentication.getPrincipal();

        return AuthResponse.builder()
                .token(jwt)
                .refreshToken(refreshToken)
                .username(users.getUsername())
                .email(users.getEmail())
                .roles(users.getRoles())
                .expiresIn(jwtTokenProvider.getExpirationMs())
                .build();
    }

    public AuthResponse refreshToken(RefreshTokenRequest request){
        String refreshToken = request.getRefreshToken();

        if (jwtTokenProvider.validateToken(refreshToken)){
            String username = jwtTokenProvider.getUsernameFromToken(refreshToken);
            Users users = userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found"));

            Users userDetails = Users.build(users);
            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    userDetails , null , userDetails.getAuthorities()
            );

            String newJwt = jwtTokenProvider.generateToken(authentication);
            String newRefreshToken = jwtTokenProvider.generateRefreshToken(username);

            return AuthResponse.builder()
                    .token(newJwt)
                    .refreshToken(newRefreshToken)
                    .username(userDetails.getUsername())
                    .email(userDetails.getEmail())
                    .roles(users.getRoles())
                    .expiresIn(jwtTokenProvider.getExpirationMs())
                    .build();
        }else {
            throw new RuntimeException("Invalid refresh token");
        }
    }

    public ValidateTokenResponse validateToken(ValidateTokenRequest request) {
        try {
            String token = request.getToken();
            if (jwtTokenProvider.validateToken(token)) {
                String username = jwtTokenProvider.getUsernameFromToken(token);
                Set<String> roles = jwtTokenProvider.getRolesFromToken(token);

                return ValidateTokenResponse.builder()
                        .valid(true)
                        .username(username)
                        .roles(roles)
                        .message("Token is valid")
                        .build();
            } else {
                return ValidateTokenResponse.builder()
                        .valid(false)
                        .message("Token is inalid")
                        .build();
            }

        } catch (Exception ex) {
            return ValidateTokenResponse.builder()
                    .valid(false)
                    .message("Token validation failed: " + ex.getMessage())
                    .build();
        }
    }


}
