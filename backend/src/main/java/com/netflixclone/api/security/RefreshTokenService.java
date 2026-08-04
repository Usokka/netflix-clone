package com.netflixclone.api.security;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final String SESSION_KEY_PREFIX = "refresh-session:";

    private final JwtUtil jwtUtil;
    private final StringRedisTemplate redisTemplate;

    public void register(String refreshToken) {
        JwtUtil.RefreshTokenDetails details = jwtUtil.parseRefreshToken(refreshToken);
        Duration ttl = Duration.between(Instant.now(), details.expiresAt());

        if (ttl.isNegative() || ttl.isZero()) {
            throw invalidRefreshToken();
        }

        try {
            redisTemplate.opsForValue().set(
                    sessionKey(details.tokenId()),
                    details.email(),
                    ttl
            );
        } catch (DataAccessException exception) {
            throw refreshStoreUnavailable(exception);
        }
    }

    public String consume(String refreshToken) {
        JwtUtil.RefreshTokenDetails details = jwtUtil.parseRefreshToken(refreshToken);

        try {
            String storedEmail = redisTemplate.opsForValue()
                    .getAndDelete(sessionKey(details.tokenId()));

            if (!details.email().equals(storedEmail)) {
                throw invalidRefreshToken();
            }

            return details.email();
        } catch (DataAccessException exception) {
            throw refreshStoreUnavailable(exception);
        }
    }

    public void revoke(String refreshToken) {
        JwtUtil.RefreshTokenDetails details = jwtUtil.parseRefreshToken(refreshToken);

        try {
            redisTemplate.delete(sessionKey(details.tokenId()));
        } catch (DataAccessException exception) {
            throw refreshStoreUnavailable(exception);
        }
    }

    private String sessionKey(String tokenId) {
        return SESSION_KEY_PREFIX + tokenId;
    }

    private ResponseStatusException invalidRefreshToken() {
        return new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Refresh token révoqué, expiré ou déjà utilisé"
        );
    }

    private ResponseStatusException refreshStoreUnavailable(DataAccessException cause) {
        return new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Le service de session est temporairement indisponible",
                cause
        );
    }
}
