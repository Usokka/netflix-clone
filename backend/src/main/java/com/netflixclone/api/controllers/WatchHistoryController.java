package com.netflixclone.api.controllers;

import com.netflixclone.api.dtos.ContinueWatchingResponse;
import com.netflixclone.api.services.WatchHistoryService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
            @RequestHeader("X-Profile-Id") UUID profileId) {
        return ResponseEntity.ok(watchHistoryService.getContinueWatching(profileId));
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> saveProgress(
            @RequestHeader("X-Profile-Id") UUID profileId,
            @RequestBody WatchHistoryRequest request) {
        
        watchHistoryService.updateProgress(profileId, request.getMovieId(), request.getTimestamp());
        return ResponseEntity.ok(Map.of("message", "Progression sauvegardée"));
    }

    @Data
    static class WatchHistoryRequest {
        private String movieId; // On le reçoit en String
        private int timestamp;
    }
}