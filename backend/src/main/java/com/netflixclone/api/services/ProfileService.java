package com.netflixclone.api.services;

import com.netflixclone.api.dtos.ProfileResponse;
import com.netflixclone.api.models.Profile;
import com.netflixclone.api.models.User;
import com.netflixclone.api.repositories.ProfileRepository;
import com.netflixclone.api.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private static final int MAX_PROFILES_PER_USER = 4;

    private final ProfileRepository profileRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<ProfileResponse> getProfilesForUser(String email) {
        User user = getUserByEmail(email);
        return profileRepository.findByUserId(user.getId()).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ProfileResponse createProfile(String email, String name, String avatarUrl) {
        if (name == null || name.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le nom du profil est obligatoire");
        }

        User user = userRepository.findByEmailForUpdate(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Utilisateur introuvable"));

        if (profileRepository.countByUserId(user.getId()) >= MAX_PROFILES_PER_USER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Maximum " + MAX_PROFILES_PER_USER + " profils par compte");
        }

        Profile profile = Profile.builder()
                .user(user)
                .name(name.trim())
                .avatarUrl(avatarUrl)
                .build();

        return toResponse(profileRepository.save(profile));
    }

    @Transactional
    public ProfileResponse updateProfile(String email, UUID profileId, String name, String avatarUrl) {
        Profile profile = getProfileOwnedByUser(email, profileId);

        if (name != null && !name.isBlank()) profile.setName(name.trim());
        if (avatarUrl != null && !avatarUrl.isBlank()) profile.setAvatarUrl(avatarUrl);

        return toResponse(profileRepository.save(profile));
    }

    @Transactional
    public void deleteProfile(String email, UUID profileId) {
        Profile profile = getProfileOwnedByUser(email, profileId);
        profileRepository.delete(profile);
    }

    // --- Helpers ---

    private User getUserByEmail(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Utilisateur introuvable"));
    }

    @Transactional(readOnly = true)
    public Profile getProfileOwnedByUser(String email, UUID profileId) {
        User user = getUserByEmail(email);
        Profile profile = profileRepository.findById(profileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Profil introuvable"));

        if (!profile.getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Ce profil ne vous appartient pas");
        }
        return profile;
    }

    private ProfileResponse toResponse(Profile profile) {
        return ProfileResponse.builder()
                .id(profile.getId().toString())
                .name(profile.getName())
                .avatarUrl(profile.getAvatarUrl())
                .build();
    }
}
