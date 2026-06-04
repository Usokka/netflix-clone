package com.netflixclone.api.controllers;

import com.netflixclone.api.dtos.SubscriptionResponse;
import com.netflixclone.api.services.SubscriptionService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @PostMapping
    public ResponseEntity<?> createSubscription(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody SubscriptionRequest request) {
        
        subscriptionService.createOrUpdateSubscription(userDetails.getUsername(), request.getPlan());
        return ResponseEntity.ok(Map.of("message", "Abonnement " + request.getPlan() + " activé avec succès"));
    }

    // NOUVEAU : Endpoint pour récupérer la liste
    @GetMapping
    public ResponseEntity<List<SubscriptionResponse>> getMySubscriptions(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(subscriptionService.getUserSubscriptions(userDetails.getUsername()));
    }

    @Data
    static class SubscriptionRequest {
        private String plan;
    }


}