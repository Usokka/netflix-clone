package com.netflixclone.api.services;

import com.netflixclone.api.dtos.MovieCardResponse;
import com.netflixclone.api.models.Movie;
import com.netflixclone.api.repositories.MovieRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Pageable;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MovieService {

    private final MovieRepository movieRepository;


    @Transactional(readOnly = true)
    public Page<MovieCardResponse> getAllMovies(Pageable pageable) {
        return movieRepository.findAll(pageable)
                .map(this::convertToCardResponse); 
    }

    @Transactional(readOnly = true)
    public MovieCardResponse getMovieById(String id) {
        UUID uuid;
        try {
            uuid = UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ID invalide : " + id);
        }

        Movie movie = movieRepository.findById(uuid)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Film introuvable avec l'ID : " + id
                ));

        return convertToCardResponse(movie);
    }

    @Transactional(readOnly = true)
    public List<MovieCardResponse> getMoviesByGenre(Integer genreId) {
        return movieRepository.findByGenreId(genreId).stream()
                .map(this::convertToCardResponse)
                .collect(Collectors.toList());
    }

    @Cacheable(value = "trendingMovies")
    @Transactional(readOnly = true)
    public List<MovieCardResponse> getTrendingMovies() {
        return movieRepository.findAll(PageRequest.of(0, 10)).stream()
                .map(this::convertToCardResponse)
                .collect(Collectors.toList());
    }


    private MovieCardResponse convertToCardResponse(Movie movie) {
        return MovieCardResponse.builder()
                .id(movie.getId().toString())
                .title(movie.getTitle())
                .description(movie.getDescription())
                .thumbnailUrl(movie.getThumbnailUrl())
                .videoFolderUrl(movie.getVideoFolderUrl())
                .durationSeconds(movie.getDurationSeconds())
                .releaseYear(movie.getReleaseYear())
                .maturityRating(movie.getMaturityRating())
                .language(movie.getLanguage())
                .genres(
                    movie.getGenres() != null
                        ? movie.getGenres().stream()
                            .map(g -> g.getName())
                            .collect(Collectors.toList())
                        : List.of()
                )
                .build();
    }

    @Transactional(readOnly = true)
    public List<MovieCardResponse> searchMovies(String query, Integer genreId) {
        List<Movie> movies;

        boolean hasQuery = query != null && !query.trim().isEmpty();

        if (hasQuery) {
            movies = movieRepository.findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(query, query);
        } else if (genreId != null) {
            movies = movieRepository.findByGenreId(genreId);
        } else {
            return List.of(); // Ni texte, ni genre : on renvoie vide
        }

        if (hasQuery && genreId != null) {
            movies = movies.stream()
                    .filter(m -> m.getGenres() != null && 
                                 m.getGenres().stream().anyMatch(g -> g.getId().equals(genreId)))
                    .collect(Collectors.toList());
        }

        return movies.stream().map(this::convertToCardResponse).collect(Collectors.toList());
    }
}