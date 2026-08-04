package com.netflixclone.api.repositories;

import com.netflixclone.api.models.Subscription;
import com.netflixclone.api.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.time.LocalDate;


@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {

    Optional<Subscription> findFirstByUserOrderByStartedAtDesc(User user);
    List<Subscription> findAllByUserOrderByStartedAtDesc(User user);
    boolean existsByUserAndIsActiveTrueAndExpiresAtGreaterThanEqual(User user, LocalDate date);
}
