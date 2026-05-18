package com.tracenet.auth.dto;

public class AuthResponse {

    private String token;
    private String tokenType;
    private String userId;
    private String email;
    private String orgId;
    private String role;

    public AuthResponse(String token, String userId, String email, String orgId, String role) {
        this.token = token;
        this.tokenType = "Bearer";
        this.userId = userId;
        this.email = email;
        this.orgId = orgId;
        this.role = role;
    }

    public String getToken() {
        return token;
    }

    public String getTokenType() {
        return tokenType;
    }

    public String getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    public String getOrgId() {
        return orgId;
    }

    public String getRole() {
        return role;
    }
}