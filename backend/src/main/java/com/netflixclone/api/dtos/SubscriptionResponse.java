package com.netflixclone.api.dtos;

import com.netflixclone.api.models.SubscriptionPlan;

import java.time.LocalDate;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SubscriptionResponse {
    private UUID id;
    private SubscriptionPlan plan;
    private LocalDate startedAt;
    private LocalDate expiresAt;
    private boolean active;
}
