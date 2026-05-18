package com.tracenet.auth.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_credentials")
public class UserCredential {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "org_id", nullable = false)
    private String orgId;

    @Column(nullable = false)
    private String role;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id")
    private Role roleEntity;

    @Column(nullable = false)
    private Instant createdAt;

    public UserCredential() {
    }

    public UserCredential(
            String email,
            String passwordHash,
            String orgId,
            String role,
            Role roleEntity,
            Instant createdAt
    ) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.orgId = orgId;
        this.role = role;
        this.roleEntity = roleEntity;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getOrgId() {
        return orgId;
    }

    public String getRole() {
        return role;
    }

    public Role getRoleEntity() {
        return roleEntity;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public void setOrgId(String orgId) {
        this.orgId = orgId;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public void setRoleEntity(Role roleEntity) {
        this.roleEntity = roleEntity;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}