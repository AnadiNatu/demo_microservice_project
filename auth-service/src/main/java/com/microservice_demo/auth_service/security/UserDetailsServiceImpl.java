package com.microservice_demo.auth_service.security;

import com.microservice_demo.auth_service.entity.Users;
import com.microservice_demo.auth_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

//   unified login requires that Spring Security's
//   can resolve a user regardless of whether the caller supplied a username, email,
//   or phone number as the "username" field.
    @Override
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
         Users user = userRepository.findByUsername(identifier).orElse(null);

//         Email Fallback
        if (user == null){
            user = userRepository.findByEmail(identifier).orElse(null);
        }

//        Phone number Fallback
        if (user == null){
            user = userRepository.findByPhoneNumber(identifier).orElse(null);
        }

        if (user == null){
            log.warn("[Auth] User not found for identifier: {}", identifier);
            throw new UsernameNotFoundException("User not found: " + identifier);
        }

        return user;
    }
}
