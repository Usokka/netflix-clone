package com.netflixclone.api.services;

import com.netflixclone.api.dtos.SubscriptionResponse;
import com.netflixclone.api.models.Subscription;
import com.netflixclone.api.models.User;
import com.netflixclone.api.repositories.SubscriptionRepository;
import com.netflixclone.api.repositories.UserRepository;
import org.springframework.data.redis.core.StringRedisTemplate; 
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
    private final StringRedisTemplate redisTemplate; 
    

    public void createOrUpdateSubscription(String email, String plan) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        Subscription subscription = subscriptionRepository.findByUser(user)
                .orElse(Subscription.builder().user(user).build());

        subscription.setPlan(plan);
        subscription.setStartedAt(LocalDate.now());
        subscription.setExpiresAt(LocalDate.now().plusMonths(1));
        subscription.setActive(true);

        subscriptionRepository.save(subscription);
        
        redisTemplate.opsForValue().set("subscription:active:" + user.getEmail(), "true", Duration.ofMinutes(15));
    }

    public List<SubscriptionResponse> getUserSubscriptions(String email) {
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

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
}