package com.netflixclone.api.services;

import com.netflixclone.api.dtos.SubscriptionResponse;
import com.netflixclone.api.models.Subscription;
import com.netflixclone.api.models.SubscriptionPlan;
import com.netflixclone.api.models.User;
import com.netflixclone.api.repositories.SubscriptionRepository;
import com.netflixclone.api.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionService {

    private static final String CACHE_KEY_PREFIX = "subscription:active:";
    private static final Duration ACTIVE_CACHE_TTL = Duration.ofMinutes(15);
    private static final Duration INACTIVE_CACHE_TTL = Duration.ofMinutes(1);

    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
    private final StringRedisTemplate redisTemplate;

    @Value("${app.subscription.demo-activation-enabled:false}")
    private boolean demoActivationEnabled;

    @Transactional
    public void activateDemoSubscription(String email, SubscriptionPlan plan) {
        if (!demoActivationEnabled) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "L'activation de démonstration est désactivée. Configurez un prestataire de paiement pour la production."
            );
        }

        User user = userRepository.findByEmailForUpdate(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Utilisateur introuvable"
                ));

        Subscription subscription = subscriptionRepository.findFirstByUserOrderByStartedAtDesc(user)
                .orElse(Subscription.builder().user(user).build());

        subscription.setPlan(plan);
        subscription.setStartedAt(LocalDate.now());
        subscription.setExpiresAt(LocalDate.now().plusMonths(1));
        subscription.setActive(true);

        subscriptionRepository.save(subscription);
        evictSubscriptionStatus(user.getEmail());
    }

    @Transactional(readOnly = true)
    public List<SubscriptionResponse> getUserSubscriptions(String email) {
        User user = getUserByEmail(email);

        return subscriptionRepository.findAllByUserOrderByStartedAtDesc(user)
                .stream()
                .map(sub -> new SubscriptionResponse(
                        sub.getId(),
                        sub.getPlan(),
                        sub.getStartedAt(),
                        sub.getExpiresAt(),
                        sub.isActive()
                ))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public boolean hasActiveSubscription(String email) {
        String cacheKey = CACHE_KEY_PREFIX + email;

        try {
            String cachedStatus = redisTemplate.opsForValue().get(cacheKey);
            if (cachedStatus != null) {
                return Boolean.parseBoolean(cachedStatus);
            }
        } catch (DataAccessException exception) {
            log.warn("Redis indisponible pendant la lecture de l'abonnement de {}", email, exception);
        }

        User user = getUserByEmail(email);
        boolean isActive = subscriptionRepository
                .existsByUserAndIsActiveTrueAndExpiresAtGreaterThanEqual(user, LocalDate.now());

        cacheSubscriptionStatus(email, isActive);
        return isActive;
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Utilisateur introuvable"
                ));
    }

    private void cacheSubscriptionStatus(String email, boolean isActive) {
        Duration ttl = isActive ? ACTIVE_CACHE_TTL : INACTIVE_CACHE_TTL;

        try {
            redisTemplate.opsForValue().set(
                    CACHE_KEY_PREFIX + email,
                    Boolean.toString(isActive),
                    ttl
            );
        } catch (DataAccessException exception) {
            log.warn("Redis indisponible pendant la mise en cache de l'abonnement de {}", email, exception);
        }
    }

    private void evictSubscriptionStatus(String email) {
        try {
            redisTemplate.delete(CACHE_KEY_PREFIX + email);
        } catch (DataAccessException exception) {
            log.warn("Redis indisponible pendant l'invalidation de l'abonnement de {}", email, exception);
        }
    }
}
