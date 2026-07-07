package com.netflixclone.api.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ContinueWatchingResponse {
    private String id;
    private String title;
    private String thumbnailUrl;
    private String videoFolderUrl;
    private int durationSeconds;
    private int timestamp;
    private int progressPercentage;
}