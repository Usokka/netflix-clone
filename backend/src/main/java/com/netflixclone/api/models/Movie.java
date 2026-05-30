package com.netflixclone.api.models;

import jakarta.persistence.*;
import lombok.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "movies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Movie {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String title;

    @Column(length = 1000)
    private String description;

    @Column(name = "thumbnail_url")
    private String thumbnailUrl;

    @Column(name = "video_folder_url", nullable = false)
    private String videoFolderUrl;

    @Column(name = "duration_seconds", nullable = false)
    private int durationSeconds;

    @Column(name = "release_year", nullable = false)
    private int releaseYear;

    @Column(name = "maturity_rating")
    private String maturityRating;

    @Column(name = "language")
    private String language;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "movie_genres",
        joinColumns = @JoinColumn(name = "movie_id"),
        inverseJoinColumns = @JoinColumn(name = "genre_id")
    )
    @Builder.Default
    private List<Genre> genres = new ArrayList<>();
}