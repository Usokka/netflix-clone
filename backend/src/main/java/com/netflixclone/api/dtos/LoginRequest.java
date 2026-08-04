package com.netflixclone.api.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class LoginRequest {
    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "Le format de l'email est invalide")
    @Size(max = 254, message = "L'email est trop long")
    private String email;

    @NotBlank(message = "Le mot de passe est obligatoire")
    @Size(max = 72, message = "Le mot de passe est trop long")
    private String password;
}
