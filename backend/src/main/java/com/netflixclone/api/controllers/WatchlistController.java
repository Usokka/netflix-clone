package com.netflixclone.api.controllers;

import com.netflixclone.api.dtos.MovieCardResponse;
import com.netflixclone.api.services.WatchlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
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
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestHeader("X-Profile-Id") UUID profileId) {
        return ResponseEntity.ok(watchlistService.getWatchlist(userDetails.getUsername(), profileId));
    }

    @PostMapping("/{movieId}")
    public ResponseEntity<Map<String, String>> addToWatchlist(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestHeader("X-Profile-Id") UUID profileId,
            @PathVariable String movieId) {
        watchlistService.addToWatchlist(userDetails.getUsername(), profileId, movieId);
        return ResponseEntity.ok(Map.of("message", "Film ajouté à la liste avec succès"));
    }

    @DeleteMapping("/{movieId}")
    public ResponseEntity<Map<String, String>> removeFromWatchlist(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestHeader("X-Profile-Id") UUID profileId,
            @PathVariable String movieId) {
        watchlistService.removeFromWatchlist(userDetails.getUsername(), profileId, movieId);
        return ResponseEntity.ok(Map.of("message", "Film retiré de la liste"));
    }
}
