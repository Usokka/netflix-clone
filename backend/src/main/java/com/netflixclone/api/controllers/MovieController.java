package com.netflixclone.api.controllers;

import com.netflixclone.api.dtos.MovieCardResponse;
import com.netflixclone.api.services.MovieService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/movies")
@RequiredArgsConstructor
public class MovieController {

    private final MovieService movieService;

    @GetMapping
    public ResponseEntity<List<MovieCardResponse>> getAllMovies() {
        return ResponseEntity.ok(movieService.getAllMovies());
    }

    @GetMapping("/{id}")
    public ResponseEntity<MovieCardResponse> getMovieById(@PathVariable String id) {
        return ResponseEntity.ok(movieService.getMovieById(id));
    }

    @GetMapping("/genre/{genreId}")
    public ResponseEntity<List<MovieCardResponse>> getMoviesByGenre(@PathVariable Integer genreId) {
        return ResponseEntity.ok(movieService.getMoviesByGenre(genreId));
    }
}