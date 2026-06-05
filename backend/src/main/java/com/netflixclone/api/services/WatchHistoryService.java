package com.netflixclone.api.services;

import com.netflixclone.api.dtos.ContinueWatchingResponse;
import com.netflixclone.api.models.Movie;
import com.netflixclone.api.models.Profile;
import com.netflixclone.api.models.WatchHistory;
import com.netflixclone.api.models.WatchHistoryId;
import com.netflixclone.api.repositories.MovieRepository;
import com.netflixclone.api.repositories.ProfileRepository;
import com.netflixclone.api.repositories.WatchHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WatchHistoryService {

    private final WatchHistoryRepository watchHistoryRepository;
    private final ProfileRepository profileRepository;
    private final MovieRepository movieRepository;

    @Transactional
    public void updateProgress(UUID profileId, String movieIdStr, int stoppedAtSeconds) {
        
        UUID movieId;
        try {
            movieId = UUID.fromString(movieIdStr);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ID de film invalide");
        }

        WatchHistoryId historyId = new WatchHistoryId(profileId, movieId);

        WatchHistory history = watchHistoryRepository.findById(historyId)
                .orElseGet(() -> {
                    Profile profile = profileRepository.findById(profileId)
                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Profil introuvable"));
                    
                    Movie movie = movieRepository.findById(movieId)
                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Film introuvable"));

                    return WatchHistory.builder()
                            .id(historyId)
                            .profile(profile)
                            .movie(movie)
                            .build();
                });

        // CORRIGÉ : On utilise le bon setter
        history.setStoppedAtSeconds(stoppedAtSeconds);
        
        // Plus besoin de history.setWatchedAt(LocalDateTime.now()) grâce à ton @PreUpdate !

        watchHistoryRepository.save(history);
    }

    @Transactional(readOnly = true)
    public List<ContinueWatchingResponse> getContinueWatching(UUID profileId) {
        Profile profile = profileRepository.findById(profileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Profil introuvable"));

        return watchHistoryRepository.findAllByProfileOrderByUpdatedAtDesc(profile)
                .stream()
                .map(history -> {
                    Movie movie = history.getMovie(); 
                    if (movie == null) return null;

                    // CORRIGÉ : On utilise getStoppedAtSeconds()
                    int progressPercentage = (int) Math.round((double) history.getStoppedAtSeconds() / movie.getDurationSeconds() * 100);

                    if (progressPercentage > 95) return null;

                    return new ContinueWatchingResponse(
                            movie.getId().toString(),
                            movie.getTitle(),
                            movie.getThumbnailUrl(),
                            movie.getVideoFolderUrl(),
                            movie.getDurationSeconds(),
                            history.getStoppedAtSeconds(), // On renvoie la valeur exacte
                            progressPercentage
                    );
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}