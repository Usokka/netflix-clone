package com.netflixclone.api.controllers;

import com.netflixclone.api.dtos.MovieCardResponse;
import com.netflixclone.api.services.WatchlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/watchlist")
@RequiredArgsConstructor
public class WatchlistController {

    private final WatchlistService watchlistService;

    @GetMapping
    public ResponseEntity<List<MovieCardResponse>> getWatchlist(
            @RequestHeader("X-Profile-Id") UUID profileId) {
        return ResponseEntity.ok(watchlistService.getWatchlist(profileId));
    }

    @PostMapping("/{movieId}")
    public ResponseEntity<Map<String, String>> addToWatchlist(
            @RequestHeader("X-Profile-Id") UUID profileId,
            @PathVariable String movieId) {
        watchlistService.addToWatchlist(profileId, movieId);
        return ResponseEntity.ok(Map.of("message", "Film ajouté à la liste avec succès"));
    }

    @DeleteMapping("/{movieId}")
    public ResponseEntity<Map<String, String>> removeFromWatchlist(
            @RequestHeader("X-Profile-Id") UUID profileId,
            @PathVariable String movieId) {
        watchlistService.removeFromWatchlist(profileId, movieId);
        return ResponseEntity.ok(Map.of("message", "Film retiré de la liste"));
    }
}