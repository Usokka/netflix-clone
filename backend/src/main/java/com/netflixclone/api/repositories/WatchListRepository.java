package com.netflixclone.api.repositories;

import com.netflixclone.api.models.WatchList;
import com.netflixclone.api.models.WatchListId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface WatchListRepository extends JpaRepository<WatchList, WatchListId> {
    
    // Récupère toute la watchlist d'un profil spécifique
    @Query("SELECT w FROM WatchList w JOIN FETCH w.movie WHERE w.id.profileId = :profileId")
    List<WatchList> findAllByProfileId(@Param("profileId") UUID profileId);
}