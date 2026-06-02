package com.netflixclone.api.dtos;

import lombok.*;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MovieCardResponse {
    private String id; 
    private String thumbnailUrl;
    private String videoFolderUrl;
    private int durationSeconds;
}