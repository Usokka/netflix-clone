package com.netflixclone.api.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final int WINDOW_SECONDS = 60;
    private static final int LOGIN_LIMIT = 5;
    private static final int STREAM_TICKET_LIMIT = 30;
    private static final DefaultRedisScript<Long> INCREMENT_SCRIPT = new DefaultRedisScript<>(
            "local count = redis.call('INCR', KEYS[1]); "
                    + "if count == 1 then redis.call('EXPIRE', KEYS[1], ARGV[1]); end; "
                    + "return count;",
            Long.class
    );

    private final StringRedisTemplate redisTemplate;
    private final ClientIpResolver clientIpResolver;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return limitFor(request) == null;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        Limit limit = limitFor(request);
        if (limit == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = clientIpResolver.resolve(request);
        String key = "rate-limit:" + limit.scope() + ':' + clientIp;

        try {
            Long requestCount = redisTemplate.execute(
                    INCREMENT_SCRIPT,
                    List.of(key),
                    Integer.toString(WINDOW_SECONDS)
            );
            if (requestCount != null && requestCount > limit.maximumRequests()) {
                writeRateLimitResponse(response);
                return;
            }
        } catch (DataAccessException exception) {
            log.warn("Rate limiting indisponible pour {}", key, exception);
        }

        filterChain.doFilter(request, response);
    }

    private Limit limitFor(HttpServletRequest request) {
        String method = request.getMethod();
        String path = request.getRequestURI();
        if ("POST".equals(method) && "/api/v1/auth/login".equals(path)) {
            return new Limit("login", LOGIN_LIMIT);
        }
        if ("GET".equals(method)
                && path.startsWith("/api/v1/stream/")
                && path.endsWith("/ticket")) {
            return new Limit("stream-ticket", STREAM_TICKET_LIMIT);
        }
        return null;
    }

    private void writeRateLimitResponse(HttpServletResponse response) throws IOException {
        response.setStatus(429);
        response.setContentType("application/json;charset=UTF-8");
        response.setHeader("Retry-After", Integer.toString(WINDOW_SECONDS));
        response.getWriter().write(String.format(
                "{\"timestamp\":\"%s\",\"status\":429,\"message\":\"Trop de requêtes. Réessayez dans une minute.\"}",
                LocalDateTime.now()
        ));
    }

    private record Limit(String scope, int maximumRequests) {
    }
}
