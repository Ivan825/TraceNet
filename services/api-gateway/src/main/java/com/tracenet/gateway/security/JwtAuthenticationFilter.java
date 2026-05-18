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
            writeError(
                    response,
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "Unauthorized",
                    "Missing or invalid Authorization header"
            );
            return;
        }

        String token = authHeader.substring(7);

        try {
            Claims claims = jwtService.validateAndExtractClaims(token);

            String userId = String.valueOf(claims.get("userId"));
            String email = String.valueOf(claims.get("email"));
            String orgId = String.valueOf(claims.get("orgId"));
            String role = String.valueOf(claims.get("role"));

            List<String> permissions = extractPermissions(claims);

            if (!isAllowed(request.getRequestURI(), permissions)) {
                writeError(
                        response,
                        HttpServletResponse.SC_FORBIDDEN,
                        "Forbidden",
                        "User does not have required permission for this resource"
                );
                return;
            }

            Map<String, String> headersToAdd = new LinkedHashMap<>();
            headersToAdd.put("X-User-Id", userId);
            headersToAdd.put("X-User-Email", email);
            headersToAdd.put("X-Org-Id", orgId);
            headersToAdd.put("X-Role", role);
            headersToAdd.put("X-Permissions", String.join(",", permissions));

            HeaderMapRequestWrapper wrappedRequest =
                    new HeaderMapRequestWrapper(request, headersToAdd);

            filterChain.doFilter(wrappedRequest, response);

        } catch (Exception e) {
            writeError(
                    response,
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "Unauthorized",
                    "Invalid or expired token"
            );
        }
    }

    private List<String> extractPermissions(Claims claims) {
        Object rawPermissions = claims.get("permissions");

        if (rawPermissions == null) {
            return List.of();
        }

        if (rawPermissions instanceof List<?> permissionList) {
            return permissionList.stream()
                    .map(String::valueOf)
                    .map(permission -> permission.toUpperCase(Locale.ROOT))
                    .toList();
        }

        return List.of();
    }

    private boolean isAllowed(String path, List<String> permissions) {
        Set<String> permissionSet = new HashSet<>(permissions);

        if (path.startsWith("/api/query/")) {
            return permissionSet.contains("VIEW_TRACES");
        }

        if (path.startsWith("/api/analytics/")) {
            return permissionSet.contains("VIEW_TRACES");
        }

        if (path.startsWith("/api/alerts")) {
            return permissionSet.contains("MANAGE_ALERTS");
        }

        if (path.startsWith("/api/traces/")) {
            return permissionSet.contains("INGEST_TRACES");
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