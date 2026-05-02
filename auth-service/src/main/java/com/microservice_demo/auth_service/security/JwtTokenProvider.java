package com.microservice_demo.auth_service.security;

import com.microservice_demo.auth_service.entity.Users;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;


@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private static final Logger logger = LoggerFactory.getLogger(JwtTokenProvider.class);

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    @Value("${jwt.refresh-expiration}")
    private long refreshExpirationMs;

    private SecretKey getSigningKey(){
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(Authentication authentication){

        Users user = (Users) authentication.getPrincipal();

        return Jwts.builder()
                .subject(user.getUsername())
                .claim("roles" , user.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toList()))
                .claim("email" , user.getEmail())
                .claim("userId" , user.getId())
                .id(UUID.randomUUID().toString())
                .issuer("auth-service")
                .audience().add("api-gateway").and()
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(getSigningKey())
                .compact();

    }

    public String generateRefreshToken(String username){
        return Jwts.builder()
                .subject(username)
                .claim("type" , "refresh")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + refreshExpirationMs))
                .signWith(getSigningKey())
                .compact();
    }

    public String getUsernameFromToken(String token){
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    @SuppressWarnings("unchecked")
    public Set<String> getRolesFromToken(String token){
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        List<String> rolesList = (List<String>) claims.get("roles");
        return new HashSet<>(rolesList);
    }

    public boolean validateToken(String authToken){
        try{
            Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(authToken);

            return true;
        }catch (MalformedJwtException ex){
            logger.error("Invalid JWT token : {}" , ex.getMessage());
        }catch (ExpiredJwtException ex){
            logger.error("JWT token is expired: {}" , ex.getMessage());
        }catch (UnsupportedJwtException ex){
            logger.error("JWT token is unsupported : {}" , ex.getMessage());
        }catch (IllegalArgumentException ex){
            logger.error("JWT claims string is empty : {}" , ex.getMessage());
        }

        return false;
    }

    public String generateTokenFromUser(Users users){
        Map<String , Object> claims = new HashMap<>();

        claims.put("role" , users.getRoles());
        claims.put("provider" , users.getProvider());
        claims.put("providerId" , users.getProviderId());

        return Jwts.builder()
                .claims()
                .add(claims)
                .subject(users.getUsername())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis()+jwtExpiration))
                .and()
                .signWith(getSigningKey()).compact();
    }

    public long getExpirationMs(){
        return jwtExpiration;
    }
}
