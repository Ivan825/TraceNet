package com.tracenet.auth.service;

import com.tracenet.auth.dto.AuthResponse;
import com.tracenet.auth.dto.LoginRequest;
import com.tracenet.auth.dto.RegisterRequest;
import com.tracenet.auth.entity.UserCredential;
import com.tracenet.auth.repository.UserCredentialRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class AuthService {

    private final UserCredentialRepository userCredentialRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            UserCredentialRepository userCredentialRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.userCredentialRepository = userCredentialRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public AuthResponse register(RegisterRequest request) {
        if (userCredentialRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already registered");
        }

        UserCredential user = new UserCredential(
                request.getEmail(),
                passwordEncoder.encode(request.getPassword()),
                request.getOrgId(),
                request.getRole(),
                Instant.now()
        );

        UserCredential savedUser = userCredentialRepository.save(user);
        String token = jwtService.generateToken(savedUser);

        return new AuthResponse(
                token,
                savedUser.getId().toString(),
                savedUser.getEmail(),
                savedUser.getOrgId(),
                savedUser.getRole()
        );
    }

    public AuthResponse login(LoginRequest request) {
        UserCredential user = userCredentialRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid email or password");
        }

        String token = jwtService.generateToken(user);

        return new AuthResponse(
                token,
                user.getId().toString(),
                user.getEmail(),
                user.getOrgId(),
                user.getRole()
        );
    }
}