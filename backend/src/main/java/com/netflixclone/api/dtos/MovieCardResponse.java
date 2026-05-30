package com.netflixclone.api.dtos;

import lombok.*;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MovieCardResponse {
    private UUID id;
    private String title;
    private String thumbnailUrl;
    private String videoFolderUrl;
    private int durationSeconds;
}