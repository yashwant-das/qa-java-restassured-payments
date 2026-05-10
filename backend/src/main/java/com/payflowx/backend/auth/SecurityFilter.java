package com.payflowx.backend.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payflowx.backend.config.SecurityProperties;
import com.payflowx.backend.dto.ErrorResponse;
import com.payflowx.backend.util.CryptoUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SecurityFilter extends OncePerRequestFilter {
    private static final Set<String> replayCache = ConcurrentHashMap.newKeySet();

    private final SecurityProperties properties;
    private final ObjectMapper mapper;

    public SecurityFilter(SecurityProperties properties, ObjectMapper mapper) {
        this.properties = properties;
        this.mapper = mapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (request.getRequestURI().startsWith("/actuator")) {
            chain.doFilter(request, response);
            return;
        }

        String token = request.getHeader("x-access-token");
        String signature = request.getHeader("x-dp-signature");
        String timestamp = request.getHeader("x-dp-timestamp");
        if (!properties.accessToken().equals(token)) {
            reject(response, HttpStatus.UNAUTHORIZED, "INVALID_TOKEN", "Access token is invalid");
            return;
        }
        if (signature == null || timestamp == null) {
            reject(response, HttpStatus.UNAUTHORIZED, "MISSING_SECURITY_HEADERS", "Signature and timestamp are required");
            return;
        }
        long requestEpoch;
        try {
            requestEpoch = Long.parseLong(timestamp);
        } catch (NumberFormatException ex) {
            reject(response, HttpStatus.UNAUTHORIZED, "INVALID_TIMESTAMP", "Timestamp must be epoch milliseconds");
            return;
        }
        long now = Instant.now().toEpochMilli();
        if (Math.abs(now - requestEpoch) > properties.timestampSkewSeconds() * 1_000) {
            reject(response, HttpStatus.UNAUTHORIZED, "EXPIRED_TIMESTAMP", "Timestamp is outside allowed skew");
            return;
        }

        String canonical = request.getMethod() + "\n" + request.getRequestURI() + "\n" + queryString(request) + "\n" + timestamp;
        String expected = CryptoUtil.hmacSha256(properties.signingSecret(), canonical);
        if (!expected.equals(signature)) {
            reject(response, HttpStatus.UNAUTHORIZED, "INVALID_SIGNATURE", "Request signature is invalid");
            return;
        }
        if (!replayCache.add(timestamp + ":" + signature)) {
            reject(response, HttpStatus.CONFLICT, "REPLAY_ATTACK_DETECTED", "Duplicate signed request rejected");
            return;
        }
        if (replayCache.size() > 10_000) {
            replayCache.clear();
        }
        chain.doFilter(request, response);
    }

    private static String queryString(HttpServletRequest request) {
        return request.getQueryString() == null ? "" : request.getQueryString();
    }

    private void reject(HttpServletResponse response, HttpStatus status, String code, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType("application/json");
        mapper.writeValue(response.getWriter(), new ErrorResponse(code, message, Instant.now()));
    }
}
