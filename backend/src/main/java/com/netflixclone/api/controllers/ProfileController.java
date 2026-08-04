package com.netflixclone.api.controllers;

import com.netflixclone.api.dtos.ProfileResponse;
import com.netflixclone.api.services.ProfileService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.hibernate.validator.constraints.URL;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/profiles")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping
    public ResponseEntity<List<ProfileResponse>> getProfiles(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(profileService.getProfilesForUser(userDetails.getUsername()));
    }

    @PostMapping
    public ResponseEntity<ProfileResponse> createProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ProfileRequest request) {
        ProfileResponse created = profileService.createProfile(
                userDetails.getUsername(),
                request.getName(),
                request.getAvatarUrl()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{profileId}")
    public ResponseEntity<ProfileResponse> updateProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID profileId,
            @Valid @RequestBody ProfileRequest request) {
        return ResponseEntity.ok(profileService.updateProfile(
                userDetails.getUsername(),
                profileId,
                request.getName(),
                request.getAvatarUrl()
        ));
    }

    @DeleteMapping("/{profileId}")
    public ResponseEntity<Void> deleteProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID profileId) {
        profileService.deleteProfile(userDetails.getUsername(), profileId);
        return ResponseEntity.noContent().build();
    }

    @Data
    static class ProfileRequest {
        @Size(max = 50, message = "Le nom du profil est limité à 50 caractères")
        private String name;

        @Size(max = 2048, message = "L'URL de l'avatar est trop longue")
        @URL(protocol = "https", message = "L'avatar doit utiliser une URL HTTPS valide")
        private String avatarUrl;
    }
}
