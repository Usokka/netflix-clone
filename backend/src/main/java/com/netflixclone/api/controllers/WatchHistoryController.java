package com.netflixclone.api.controllers;

import com.netflixclone.api.dtos.ContinueWatchingResponse;
import com.netflixclone.api.services.WatchHistoryService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/watch-history")
@RequiredArgsConstructor
public class WatchHistoryController {

    private final WatchHistoryService watchHistoryService;

    @GetMapping
    public ResponseEntity<List<ContinueWatchingResponse>> getContinueWatching(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestHeader("X-Profile-Id") UUID profileId) {
        return ResponseEntity.ok(watchHistoryService.getContinueWatching(userDetails.getUsername(), profileId));
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> saveProgress(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestHeader("X-Profile-Id") UUID profileId,
            @Valid @RequestBody WatchHistoryRequest request) {
        watchHistoryService.updateProgress(
                userDetails.getUsername(),
                profileId,
                request.getMovieId(),
                request.getTimestamp()
        );
        return ResponseEntity.ok(Map.of("message", "Progression sauvegardée"));
    }

    @Data
    static class WatchHistoryRequest {
        @NotBlank(message = "L'identifiant du film est obligatoire")
        private String movieId;

        @Min(value = 0, message = "La progression ne peut pas être négative")
        private int timestamp;
    }
}
