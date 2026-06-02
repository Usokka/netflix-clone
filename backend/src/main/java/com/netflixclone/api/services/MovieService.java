package com.netflixclone.api.services;

import com.netflixclone.api.dtos.MovieCardResponse;
import com.netflixclone.api.models.Movie;
import com.netflixclone.api.repositories.MovieRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MovieService {

    private final MovieRepository movieRepository;

    @Transactional(readOnly = true)
    public List<MovieCardResponse> getAllMovies() {
        return movieRepository.findAll().stream()
                .map(this::convertToCardResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MovieCardResponse> getMoviesByGenre(Integer genreId) {
        return movieRepository.findByGenreId(genreId).stream()
                .map(this::convertToCardResponse)
                .collect(Collectors.toList());
    }

    private MovieCardResponse convertToCardResponse(Movie movie) {
        return MovieCardResponse.builder()
                .id(movie.getId().toString()) 
                .thumbnailUrl(movie.getThumbnailUrl())
                .videoFolderUrl(movie.getVideoFolderUrl())
                .durationSeconds(movie.getDurationSeconds())
                .build();
    }
}