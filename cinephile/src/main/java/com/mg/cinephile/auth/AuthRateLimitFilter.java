package com.mg.cinephile.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Limits how often a single IP can hit login/register.
 * In-memory only — fine for a single app instance; use Redis later for multi-node.
 */
@Component
public class AuthRateLimitFilter extends OncePerRequestFilter {

    private final int maxRequests;
    private final long windowMs;
    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<String, Deque<Long>> requestLog = new ConcurrentHashMap<>();

    public AuthRateLimitFilter(
            @Value("${rate-limit.auth.max-requests:10}") int maxRequests,
            @Value("${rate-limit.auth.window-seconds:60}") int windowSeconds,
            ObjectMapper objectMapper) {
        this.maxRequests = maxRequests;
        this.windowMs = windowSeconds * 1000L;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String path = request.getRequestURI();
        return !"/api/auth/login".equals(path) && !"/api/auth/register".equals(path);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String ip = request.getRemoteAddr();
        long now = System.currentTimeMillis();

        Deque<Long> timestamps = requestLog.computeIfAbsent(ip, k -> new ConcurrentLinkedDeque<>());

        synchronized (timestamps) {
            while (!timestamps.isEmpty() && timestamps.peekFirst() < now - windowMs) {
                timestamps.pollFirst();
            }
            if (timestamps.size() >= maxRequests) {
                writeTooManyRequests(response);
                return;
            }
            timestamps.addLast(now);
        }

        filterChain.doFilter(request, response);
    }

    private void writeTooManyRequests(HttpServletResponse response) throws IOException {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.TOO_MANY_REQUESTS);
        problem.setTitle("Too many requests");
        problem.setDetail("Rate limit exceeded. Try again later.");

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), problem);
    }
}
