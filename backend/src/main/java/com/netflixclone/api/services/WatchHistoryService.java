package com.netflixclone.api.services;

import com.netflixclone.api.dtos.ContinueWatchingResponse;
import com.netflixclone.api.models.Movie;
import com.netflixclone.api.models.Profile;
import com.netflixclone.api.models.WatchHistory;
import com.netflixclone.api.models.WatchHistoryId;
import com.netflixclone.api.repositories.MovieRepository;
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
    private final MovieRepository movieRepository;
    private final ProfileService profileService;

    @Transactional
    public void updateProgress(String email, UUID profileId, String movieIdStr, int stoppedAtSeconds) {
        Profile profile = profileService.getProfileOwnedByUser(email, profileId);

        UUID movieId;
        try {
            movieId = UUID.fromString(movieIdStr);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ID de film invalide");
        }

        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Film introuvable"));

        if (stoppedAtSeconds < 0 || stoppedAtSeconds > movie.getDurationSeconds()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La progression doit être comprise entre 0 et la durée du film"
            );
        }

        WatchHistoryId historyId = new WatchHistoryId(profileId, movieId);

        WatchHistory history = watchHistoryRepository.findById(historyId).orElse(null);
        if (history != null && stoppedAtSeconds <= history.getStoppedAtSeconds()) {
            return;
        }

        if (history == null) {
            history = WatchHistory.builder()
                        .id(historyId)
                        .profile(profile)
                        .movie(movie)
                        .build();
        }

        history.setStoppedAtSeconds(stoppedAtSeconds);

        watchHistoryRepository.save(history);
    }

    @Transactional(readOnly = true)
    public List<ContinueWatchingResponse> getContinueWatching(String email, UUID profileId) {
        Profile profile = profileService.getProfileOwnedByUser(email, profileId);

        return watchHistoryRepository.findAllByProfileOrderByWatchedAtDesc(profile)
                .stream()
                .map(history -> {
                    Movie movie = history.getMovie(); 
                    if (movie == null) return null;

                    if (movie.getDurationSeconds() <= 0) return null;

                    int progressPercentage = (int) Math.round((double) history.getStoppedAtSeconds() / movie.getDurationSeconds() * 100);

                    if (progressPercentage > 95) return null;

                    return new ContinueWatchingResponse(
                            movie.getId().toString(),
                            movie.getTitle(),
                            movie.getThumbnailUrl(),
                            movie.getVideoFolderUrl(),
                            movie.getDurationSeconds(),
                            history.getStoppedAtSeconds(),
                            progressPercentage
                    );
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}
