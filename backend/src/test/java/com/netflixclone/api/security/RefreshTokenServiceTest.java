package com.netflixclone.api.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    private static final String TOKEN = "signed-refresh-token";
    private static final String EMAIL = "user@example.com";
    private static final String TOKEN_ID = "refresh-id";
    private static final String REDIS_KEY = "refresh-session:" + TOKEN_ID;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    @Test
    void registerStoresOnlyTheTokenIdentifierWithItsRemainingLifetime() {
        when(jwtUtil.parseRefreshToken(TOKEN)).thenReturn(details());
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        refreshTokenService.register(TOKEN);

        verify(valueOperations).set(eq(REDIS_KEY), eq(EMAIL), any(Duration.class));
    }

    @Test
    void consumeAtomicallyDeletesAValidSession() {
        when(jwtUtil.parseRefreshToken(TOKEN)).thenReturn(details());
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.getAndDelete(REDIS_KEY)).thenReturn(EMAIL);

        assertEquals(EMAIL, refreshTokenService.consume(TOKEN));

        verify(valueOperations).getAndDelete(REDIS_KEY);
    }

    @Test
    void consumeRejectsAReplayedOrRevokedSession() {
        when(jwtUtil.parseRefreshToken(TOKEN)).thenReturn(details());
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.getAndDelete(REDIS_KEY)).thenReturn(null);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> refreshTokenService.consume(TOKEN)
        );

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
    }

    @Test
    void revokeDeletesTheServerSideSession() {
        when(jwtUtil.parseRefreshToken(TOKEN)).thenReturn(details());

        refreshTokenService.revoke(TOKEN);

        verify(redisTemplate).delete(REDIS_KEY);
    }

    private JwtUtil.RefreshTokenDetails details() {
        return new JwtUtil.RefreshTokenDetails(
                EMAIL,
                TOKEN_ID,
                Instant.now().plusSeconds(300)
        );
    }
}
