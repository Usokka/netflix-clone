package com.netflixclone.api.services;

import com.netflixclone.api.dtos.MovieCardResponse;
import com.netflixclone.api.models.Movie;
import com.netflixclone.api.models.Profile;
import com.netflixclone.api.models.WatchList;
import com.netflixclone.api.models.WatchListId;
import com.netflixclone.api.repositories.MovieRepository;
import com.netflixclone.api.repositories.ProfileRepository;
import com.netflixclone.api.repositories.WatchListRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WatchlistService {

    private final WatchListRepository watchListRepository;
    private final ProfileRepository profileRepository;
    private final MovieRepository movieRepository;

    @Transactional(readOnly = true)
    public List<MovieCardResponse> getWatchlist(UUID profileId) {
        return watchListRepository.findAllByProfileId(profileId).stream()
                .map(watchList -> convertToCardResponse(watchList.getMovie()))
                .collect(Collectors.toList());
    }

    @Transactional
    public void addToWatchlist(UUID profileId, String movieIdStr) {
        UUID movieId = parseUUID(movieIdStr);
        WatchListId id = new WatchListId(profileId, movieId);

        if (watchListRepository.existsById(id)) {
            return; // Le film est déjà dans la liste, on ne fait rien
        }

        Profile profile = profileRepository.findById(profileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Profil introuvable"));
        
        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Film introuvable"));

        WatchList watchListEntry = WatchList.builder()
                .id(id)
                .profile(profile)
                .movie(movie)
                .addedAt(LocalDateTime.now())
                .build();

        watchListRepository.save(watchListEntry);
    }

    @Transactional
    public void removeFromWatchlist(UUID profileId, String movieIdStr) {
        UUID movieId = parseUUID(movieIdStr);
        WatchListId id = new WatchListId(profileId, movieId);
        
        if (watchListRepository.existsById(id)) {
            watchListRepository.deleteById(id);
        }
    }

    private UUID parseUUID(String id) {
        try {
            return UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ID de film invalide");
        }
    }

    private MovieCardResponse convertToCardResponse(Movie movie) {
        return MovieCardResponse.builder()
                .id(movie.getId().toString())
                .title(movie.getTitle())
                .description(movie.getDescription())
                .thumbnailUrl(movie.getThumbnailUrl())
                .durationSeconds(movie.getDurationSeconds())
                .releaseYear(movie.getReleaseYear())
                .maturityRating(movie.getMaturityRating())
                .build();
    }
}