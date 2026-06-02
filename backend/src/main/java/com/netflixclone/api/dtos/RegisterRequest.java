package com.netflixclone.api.dtos;

import lombok.Data;

@Data
public class RegisterRequest {
    private String email;
    private String password;
}