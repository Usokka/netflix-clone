package com.netflixclone.api.models;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "watch_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WatchHistory {

    @EmbeddedId
    private WatchHistoryId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("profileId")
    @JoinColumn(name = "profile_id")
    private Profile profile;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("movieId")
    @JoinColumn(name = "movie_id")
    private Movie movie;

    @Column(name = "stopped_at_seconds", nullable = false)
    private int stoppedAtSeconds; // C'est cette valeur que le lecteur HLS enverra régulièrement

    @Column(name = "watched_at", nullable = false)
    private LocalDateTime watchedAt;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        this.watchedAt = LocalDateTime.now();
    }
}