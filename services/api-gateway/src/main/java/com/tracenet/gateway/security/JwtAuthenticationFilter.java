package com.tracenet.gateway.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.*;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();

        return path.startsWith("/api/auth/")
                || path.equals("/actuator/health")
                || path.equals("/actuator/info");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized", "Missing or invalid Authorization header");
            return;
        }

        String token = authHeader.substring(7);

        try {
            Claims claims = jwtService.validateAndExtractClaims(token);

            String userId = String.valueOf(claims.get("userId"));
            String email = String.valueOf(claims.get("email"));
            String orgId = String.valueOf(claims.get("orgId"));
            String role = String.valueOf(claims.get("role"));

            if (!isAllowed(request.getRequestURI(), role)) {
                writeError(response, HttpServletResponse.SC_FORBIDDEN, "Forbidden", "Role is not allowed to access this resource");
                return;
            }

            Map<String, String> headersToAdd = new LinkedHashMap<>();
            headersToAdd.put("X-User-Id", userId);
            headersToAdd.put("X-User-Email", email);
            headersToAdd.put("X-Org-Id", orgId);
            headersToAdd.put("X-Role", role);

            HeaderMapRequestWrapper wrappedRequest =
                    new HeaderMapRequestWrapper(request, headersToAdd);

            filterChain.doFilter(wrappedRequest, response);

        } catch (Exception e) {
            writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized", "Invalid or expired token");
        }
    }

    private boolean isAllowed(String path, String role) {
        if (role == null) {
            return false;
        }

        String normalizedRole = role.toUpperCase(Locale.ROOT);

        if (path.startsWith("/api/query/")) {
            return Set.of("ADMIN", "SRE", "DEVELOPER", "VIEWER").contains(normalizedRole);
        }

        if (path.startsWith("/api/traces/")) {
            return Set.of("ADMIN", "SRE").contains(normalizedRole);
        }

        return true;
    }

    private void writeError(
            HttpServletResponse response,
            int status,
            String error,
            String message
    ) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");

        response.getWriter().write("""
                {
                  "status": %d,
                  "error": "%s",
                  "message": "%s"
                }
                """.formatted(status, error, message));
    }

    private static class HeaderMapRequestWrapper extends HttpServletRequestWrapper {

        private final Map<String, String> customHeaders;

        HeaderMapRequestWrapper(HttpServletRequest request, Map<String, String> customHeaders) {
            super(request);
            this.customHeaders = new LinkedHashMap<>();

            for (Map.Entry<String, String> entry : customHeaders.entrySet()) {
                this.customHeaders.put(entry.getKey().toLowerCase(Locale.ROOT), entry.getValue());
            }
        }

        @Override
        public String getHeader(String name) {
            String customHeaderValue = customHeaders.get(name.toLowerCase(Locale.ROOT));

            if (customHeaderValue != null) {
                return customHeaderValue;
            }

            return super.getHeader(name);
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            String customHeaderValue = customHeaders.get(name.toLowerCase(Locale.ROOT));

            if (customHeaderValue != null) {
                return Collections.enumeration(List.of(customHeaderValue));
            }

            return super.getHeaders(name);
        }

        @Override
        public Enumeration<String> getHeaderNames() {
            Set<String> headerNames = new LinkedHashSet<>();

            Enumeration<String> existingHeaderNames = super.getHeaderNames();
            while (existingHeaderNames.hasMoreElements()) {
                headerNames.add(existingHeaderNames.nextElement());
            }

            headerNames.addAll(customHeaders.keySet());

            return Collections.enumeration(headerNames);
        }
    }
}