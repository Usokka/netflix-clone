package com.netflixclone.api.dtos;

import java.time.LocalDate;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SubscriptionResponse {
    private UUID id;
    private String plan;
    private LocalDate startedAt;
    private LocalDate expiresAt;
    private boolean active;
}


   