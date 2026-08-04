package com.netflixclone.api.controllers;

import com.netflixclone.api.dtos.SubscriptionResponse;
import com.netflixclone.api.models.SubscriptionPlan;
import com.netflixclone.api.services.SubscriptionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
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
    public ResponseEntity<Map<String, String>> activateDemoSubscription(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody SubscriptionRequest request) {
        subscriptionService.activateDemoSubscription(userDetails.getUsername(), request.getPlan());
        return ResponseEntity.ok(Map.of(
                "message", "Accès de démonstration " + request.getPlan() + " activé",
                "mode", "demo"
        ));
    }

    @GetMapping
    public ResponseEntity<List<SubscriptionResponse>> getMySubscriptions(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(subscriptionService.getUserSubscriptions(userDetails.getUsername()));
    }

    @Data
    static class SubscriptionRequest {
        @NotNull(message = "Le forfait est obligatoire")
        private SubscriptionPlan plan;
    }
}
