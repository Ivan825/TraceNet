package com.tracenet.auth.service;

import com.tracenet.auth.entity.Permission;
import com.tracenet.auth.entity.UserCredential;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration-ms}")
    private long jwtExpirationMs;

    public String generateToken(UserCredential user) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtExpirationMs);

        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));

        List<String> permissions = getPermissions(user);

        return Jwts.builder()
                .subject(user.getEmail())
                .claim("userId", user.getId().toString())
                .claim("email", user.getEmail())
                .claim("orgId", user.getOrgId())
                .claim("role", user.getRole())
                .claim("permissions", permissions)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    public List<String> getPermissions(UserCredential user) {
        if (user.getRoleEntity() == null) {
            return List.of();
        }

        return user.getRoleEntity()
                .getPermissions()
                .stream()
                .map(Permission::getName)
                .sorted()
                .toList();
    }
}