package com.netflixclone.api.services;

import com.netflixclone.api.models.Profile;
import com.netflixclone.api.models.User;
import com.netflixclone.api.repositories.ProfileRepository;
import com.netflixclone.api.repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    private static final String EMAIL = "owner@example.com";
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID PROFILE_ID = UUID.randomUUID();

    @Mock
    private ProfileRepository profileRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ProfileService profileService;

    @Test
    void getProfileOwnedByUserReturnsProfileForItsOwner() {
        User owner = User.builder().id(USER_ID).email(EMAIL).build();
        Profile profile = Profile.builder().id(PROFILE_ID).user(owner).build();
        when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(owner));
        when(profileRepository.findById(PROFILE_ID)).thenReturn(Optional.of(profile));

        Profile result = profileService.getProfileOwnedByUser(EMAIL, PROFILE_ID);

        assertSame(profile, result);
    }

    @Test
    void getProfileOwnedByUserRejectsProfileFromAnotherAccount() {
        User authenticatedUser = User.builder().id(USER_ID).email(EMAIL).build();
        User profileOwner = User.builder().id(UUID.randomUUID()).email("other@example.com").build();
        Profile profile = Profile.builder().id(PROFILE_ID).user(profileOwner).build();
        when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(authenticatedUser));
        when(profileRepository.findById(PROFILE_ID)).thenReturn(Optional.of(profile));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> profileService.getProfileOwnedByUser(EMAIL, PROFILE_ID)
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    }
}
