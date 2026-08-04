package com.netflixclone.api.models;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "subscriptions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private SubscriptionPlan plan;

    @Column(name = "started_at", nullable = false)
    private LocalDate startedAt;

    @Column(name = "expires_at")
    private LocalDate expiresAt;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;
}
