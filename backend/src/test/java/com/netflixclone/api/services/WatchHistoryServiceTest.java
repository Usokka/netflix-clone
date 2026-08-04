package com.netflixclone.api.services;

import com.netflixclone.api.models.Movie;
import com.netflixclone.api.models.Profile;
import com.netflixclone.api.models.User;
import com.netflixclone.api.models.WatchHistory;
import com.netflixclone.api.models.WatchHistoryId;
import com.netflixclone.api.repositories.MovieRepository;
import com.netflixclone.api.repositories.WatchHistoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WatchHistoryServiceTest {

    private static final String EMAIL = "owner@example.com";
    private static final UUID PROFILE_ID = UUID.randomUUID();
    private static final String MOVIE_ID = UUID.randomUUID().toString();

    @Mock
    private WatchHistoryRepository watchHistoryRepository;

    @Mock
    private MovieRepository movieRepository;

    @Mock
    private ProfileService profileService;

    @InjectMocks
    private WatchHistoryService watchHistoryService;

    @Test
    void getContinueWatchingAllowsProfileOwner() {
        Profile profile = ownedProfile();
        when(profileService.getProfileOwnedByUser(EMAIL, PROFILE_ID)).thenReturn(profile);
        when(watchHistoryRepository.findAllByProfileOrderByWatchedAtDesc(profile)).thenReturn(List.of());

        assertEquals(List.of(), watchHistoryService.getContinueWatching(EMAIL, PROFILE_ID));

        verify(profileService).getProfileOwnedByUser(EMAIL, PROFILE_ID);
        verify(watchHistoryRepository).findAllByProfileOrderByWatchedAtDesc(profile);
    }

    @Test
    void getContinueWatchingRejectsProfileFromAnotherAccountBeforeReadingIt() {
        rejectProfileAccess();

        assertForbidden(() -> watchHistoryService.getContinueWatching(EMAIL, PROFILE_ID));

        verifyNoInteractions(watchHistoryRepository, movieRepository);
    }

    @Test
    void updateProgressRejectsProfileFromAnotherAccountBeforeWritingIt() {
        rejectProfileAccess();

        assertForbidden(() -> watchHistoryService.updateProgress(EMAIL, PROFILE_ID, MOVIE_ID, 120));

        verifyNoInteractions(watchHistoryRepository, movieRepository);
    }

    @Test
    void updateProgressRejectsTimestampPastMovieDuration() {
        Profile profile = ownedProfile();
        Movie movie = Movie.builder().id(UUID.fromString(MOVIE_ID)).durationSeconds(100).build();
        when(profileService.getProfileOwnedByUser(EMAIL, PROFILE_ID)).thenReturn(profile);
        when(movieRepository.findById(movie.getId())).thenReturn(Optional.of(movie));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> watchHistoryService.updateProgress(EMAIL, PROFILE_ID, MOVIE_ID, 101)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verifyNoInteractions(watchHistoryRepository);
    }

    @Test
    void updateProgressDoesNotOverwriteNewerProgressWithLateRequest() {
        Profile profile = ownedProfile();
        UUID movieId = UUID.fromString(MOVIE_ID);
        Movie movie = Movie.builder().id(movieId).durationSeconds(100).build();
        WatchHistory history = WatchHistory.builder()
                .id(new WatchHistoryId(PROFILE_ID, movieId))
                .profile(profile)
                .movie(movie)
                .stoppedAtSeconds(80)
                .build();
        when(profileService.getProfileOwnedByUser(EMAIL, PROFILE_ID)).thenReturn(profile);
        when(movieRepository.findById(movieId)).thenReturn(Optional.of(movie));
        when(watchHistoryRepository.findById(history.getId())).thenReturn(Optional.of(history));

        watchHistoryService.updateProgress(EMAIL, PROFILE_ID, MOVIE_ID, 60);

        assertEquals(80, history.getStoppedAtSeconds());
        verify(watchHistoryRepository, never()).save(history);
    }

    private void rejectProfileAccess() {
        when(profileService.getProfileOwnedByUser(EMAIL, PROFILE_ID))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN));
    }

    private void assertForbidden(Runnable action) {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, action::run);
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    }

    private Profile ownedProfile() {
        User owner = User.builder().id(UUID.randomUUID()).email(EMAIL).build();
        return Profile.builder().id(PROFILE_ID).user(owner).build();
    }
}
