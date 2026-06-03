package com.netflixclone.api.controllers;

import com.netflixclone.api.dtos.MovieCardResponse;
import com.netflixclone.api.services.MovieService;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.cache.annotation.Cacheable; // À ajouter


import java.util.List;

@RestController
@RequestMapping("/api/v1/movies")
@RequiredArgsConstructor
public class MovieController {

    private final MovieService movieService;

    @GetMapping
    public ResponseEntity<Page<MovieCardResponse>> getAllMovies(Pageable pageable) {
        return ResponseEntity.ok(movieService.getAllMovies(pageable));
    }
    
    @GetMapping("/trending")
    public ResponseEntity<List<MovieCardResponse>> getTrendingMovies() {
        return ResponseEntity.ok(movieService.getTrendingMovies());
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