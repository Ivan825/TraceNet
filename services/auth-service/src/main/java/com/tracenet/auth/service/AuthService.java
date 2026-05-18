package com.tracenet.auth.service;

import com.tracenet.auth.dto.AuthResponse;
import com.tracenet.auth.dto.LoginRequest;
import com.tracenet.auth.dto.RegisterRequest;
import com.tracenet.auth.entity.Role;
import com.tracenet.auth.entity.UserCredential;
import com.tracenet.auth.repository.RoleRepository;
import com.tracenet.auth.repository.UserCredentialRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Locale;

@Service
public class AuthService {

    private final UserCredentialRepository userCredentialRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            UserCredentialRepository userCredentialRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.userCredentialRepository = userCredentialRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public AuthResponse register(RegisterRequest request) {
        if (userCredentialRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already registered");
        }

        String normalizedRole = request.getRole().toUpperCase(Locale.ROOT);

        Role role = roleRepository.findByName(normalizedRole)
                .orElseThrow(() -> new IllegalArgumentException("Invalid role: " + request.getRole()));

        UserCredential user = new UserCredential(
                request.getEmail(),
                passwordEncoder.encode(request.getPassword()),
                request.getOrgId(),
                normalizedRole,
                role,
                Instant.now()
        );

        UserCredential savedUser = userCredentialRepository.save(user);
        String token = jwtService.generateToken(savedUser);
        List<String> permissions = jwtService.getPermissions(savedUser);

        return new AuthResponse(
                token,
                savedUser.getId().toString(),
                savedUser.getEmail(),
                savedUser.getOrgId(),
                savedUser.getRole(),
                permissions
        );
    }

    public AuthResponse login(LoginRequest request) {
        UserCredential user = userCredentialRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid email or password");
        }

        if (user.getRoleEntity() == null) {
            Role role = roleRepository.findByName(user.getRole())
                    .orElseThrow(() -> new IllegalArgumentException("Role not found for user"));

            user.setRoleEntity(role);
            userCredentialRepository.save(user);
        }

        String token = jwtService.generateToken(user);
        List<String> permissions = jwtService.getPermissions(user);

        return new AuthResponse(
                token,
                user.getId().toString(),
                user.getEmail(),
                user.getOrgId(),
                user.getRole(),
                permissions
        );
    }
}