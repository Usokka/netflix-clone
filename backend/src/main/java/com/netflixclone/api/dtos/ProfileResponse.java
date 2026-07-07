package com.netflixclone.api.dtos;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProfileResponse {
    private String id;
    private String name;
    private String avatarUrl;
}