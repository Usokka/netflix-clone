package com.netflixclone.api.repositories;

import com.netflixclone.api.models.Profile;
import com.netflixclone.api.models.WatchHistory;
import com.netflixclone.api.models.WatchHistoryId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WatchHistoryRepository extends JpaRepository<WatchHistory, WatchHistoryId> {
    
    // Pour récupérer tous les films en cours d'un profil, triés du plus récent au plus ancien
    List<WatchHistory> findAllByProfileOrderByWatchedAtDesc(Profile profile);
}