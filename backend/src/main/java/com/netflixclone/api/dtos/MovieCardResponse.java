package com.netflixclone.api.dtos;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class MovieCardResponse {
    private String id;
    private String title;
    private String description;
    private String thumbnailUrl;
    private String videoFolderUrl;
    private Integer durationSeconds;
    private Integer releaseYear;
    private String maturityRating;
    private String language;
    private List<String> genres;
}