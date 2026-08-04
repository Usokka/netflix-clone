package com.netflixclone.api.services;

import com.netflixclone.api.models.Subscription;
import com.netflixclone.api.models.SubscriptionPlan;
import com.netflixclone.api.models.User;
import com.netflixclone.api.repositories.SubscriptionRepository;
import com.netflixclone.api.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    private static final String EMAIL = "subscriber@example.com";
    private static final String CACHE_KEY = "subscription:active:" + EMAIL;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private SubscriptionService subscriptionService;

    @BeforeEach
    void setUpRedisOperations() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void hasActiveSubscriptionUsesPositiveCacheEntry() {
        when(valueOperations.get(CACHE_KEY)).thenReturn("true");

        assertTrue(subscriptionService.hasActiveSubscription(EMAIL));

        verifyNoInteractions(userRepository, subscriptionRepository);
    }

    @Test
    void hasActiveSubscriptionReadsDatabaseAndWarmsCacheOnMiss() {
        User user = user();
        when(valueOperations.get(CACHE_KEY)).thenReturn(null);
        when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(user));
        when(subscriptionRepository.existsByUserAndIsActiveTrueAndExpiresAtGreaterThanEqual(
                eq(user),
                any(LocalDate.class)
        )).thenReturn(true);

        assertTrue(subscriptionService.hasActiveSubscription(EMAIL));

        verify(valueOperations).set(CACHE_KEY, "true", Duration.ofMinutes(15));
    }

    @Test
    void hasActiveSubscriptionRejectsExpiredOrInactiveDatabaseEntry() {
        User user = user();
        when(valueOperations.get(CACHE_KEY)).thenReturn(null);
        when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(user));
        when(subscriptionRepository.existsByUserAndIsActiveTrueAndExpiresAtGreaterThanEqual(
                eq(user),
                any(LocalDate.class)
        )).thenReturn(false);

        assertFalse(subscriptionService.hasActiveSubscription(EMAIL));

        verify(valueOperations).set(CACHE_KEY, "false", Duration.ofMinutes(1));
    }

    @Test
    void hasActiveSubscriptionFallsBackToDatabaseWhenRedisIsUnavailable() {
        User user = user();
        when(valueOperations.get(CACHE_KEY))
                .thenThrow(new RedisConnectionFailureException("Redis unavailable"));
        when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(user));
        when(subscriptionRepository.existsByUserAndIsActiveTrueAndExpiresAtGreaterThanEqual(
                eq(user),
                any(LocalDate.class)
        )).thenReturn(true);

        assertTrue(subscriptionService.hasActiveSubscription(EMAIL));

        verify(subscriptionRepository).existsByUserAndIsActiveTrueAndExpiresAtGreaterThanEqual(
                eq(user),
                any(LocalDate.class)
        );
    }

    @Test
    void activateDemoSubscriptionIsDisabledByDefault() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> subscriptionService.activateDemoSubscription(EMAIL, SubscriptionPlan.PREMIUM)
        );

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.getStatusCode());
        verifyNoInteractions(userRepository, subscriptionRepository);
    }

    @Test
    void activateDemoSubscriptionPersistsAWhitelistedPlanWhenExplicitlyEnabled() {
        User user = user();
        ReflectionTestUtils.setField(subscriptionService, "demoActivationEnabled", true);
        when(userRepository.findByEmailForUpdate(EMAIL)).thenReturn(Optional.of(user));
        when(subscriptionRepository.findFirstByUserOrderByStartedAtDesc(user)).thenReturn(Optional.empty());
        when(subscriptionRepository.save(any(Subscription.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        subscriptionService.activateDemoSubscription(EMAIL, SubscriptionPlan.STANDARD);

        ArgumentCaptor<Subscription> subscriptionCaptor = ArgumentCaptor.forClass(Subscription.class);
        verify(subscriptionRepository).save(subscriptionCaptor.capture());
        assertEquals(SubscriptionPlan.STANDARD, subscriptionCaptor.getValue().getPlan());
        assertTrue(subscriptionCaptor.getValue().isActive());
        verify(redisTemplate).delete(CACHE_KEY);
    }

    private User user() {
        return User.builder().id(UUID.randomUUID()).email(EMAIL).build();
    }
}
