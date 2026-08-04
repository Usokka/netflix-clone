package com.netflixclone.api.services;

import com.netflixclone.api.models.Profile;
import com.netflixclone.api.models.User;
import com.netflixclone.api.repositories.MovieRepository;
import com.netflixclone.api.repositories.WatchListRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WatchlistServiceTest {

    private static final String EMAIL = "owner@example.com";
    private static final UUID PROFILE_ID = UUID.randomUUID();
    private static final String MOVIE_ID = UUID.randomUUID().toString();

    @Mock
    private WatchListRepository watchListRepository;

    @Mock
    private MovieRepository movieRepository;

    @Mock
    private ProfileService profileService;

    @InjectMocks
    private WatchlistService watchlistService;

    @Test
    void getWatchlistAllowsProfileOwner() {
        Profile profile = ownedProfile();
        when(profileService.getProfileOwnedByUser(EMAIL, PROFILE_ID)).thenReturn(profile);
        when(watchListRepository.findAllByProfileId(PROFILE_ID)).thenReturn(List.of());

        assertEquals(List.of(), watchlistService.getWatchlist(EMAIL, PROFILE_ID));

        verify(profileService).getProfileOwnedByUser(EMAIL, PROFILE_ID);
        verify(watchListRepository).findAllByProfileId(PROFILE_ID);
    }

    @Test
    void getWatchlistRejectsProfileFromAnotherAccountBeforeReadingIt() {
        rejectProfileAccess();

        assertForbidden(() -> watchlistService.getWatchlist(EMAIL, PROFILE_ID));

        verifyNoInteractions(watchListRepository, movieRepository);
    }

    @Test
    void addToWatchlistRejectsProfileFromAnotherAccountBeforeWritingIt() {
        rejectProfileAccess();

        assertForbidden(() -> watchlistService.addToWatchlist(EMAIL, PROFILE_ID, MOVIE_ID));

        verifyNoInteractions(watchListRepository, movieRepository);
    }

    @Test
    void removeFromWatchlistRejectsProfileFromAnotherAccountBeforeDeletingIt() {
        rejectProfileAccess();

        assertForbidden(() -> watchlistService.removeFromWatchlist(EMAIL, PROFILE_ID, MOVIE_ID));

        verifyNoInteractions(watchListRepository, movieRepository);
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
