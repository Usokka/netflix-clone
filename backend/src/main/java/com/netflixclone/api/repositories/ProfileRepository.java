package com.netflixclone.api.repositories;

import com.netflixclone.api.models.Profile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProfileRepository extends JpaRepository<Profile, UUID> {
    List<Profile> findByUserId(UUID userId);
    long countByUserId(UUID userId);
}