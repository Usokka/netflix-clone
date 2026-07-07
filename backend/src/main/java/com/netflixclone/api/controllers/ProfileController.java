package com.netflixclone.api.controllers;

import com.netflixclone.api.dtos.ProfileResponse;
import com.netflixclone.api.services.ProfileService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

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
            @RequestBody ProfileRequest request) {
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
            @RequestBody ProfileRequest request) {
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
        private String name;
        private String avatarUrl;
    }
}