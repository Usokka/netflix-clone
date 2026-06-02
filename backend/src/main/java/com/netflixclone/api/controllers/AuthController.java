package com.netflixclone.api.controllers;

import com.netflixclone.api.dtos.LoginRequest;
import com.netflixclone.api.repositories.UserRepository;
import com.netflixclone.api.security.JwtUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import com.netflixclone.api.dtos.RegisterRequest;
import com.netflixclone.api.models.User;
import com.netflixclone.api.repositories.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") 
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository; // 👈 Injecte le Repository
    private final PasswordEncoder passwordEncoder; // 👈 Injecte le BCrypt encoder

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request, HttpServletResponse response) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        UserDetails userDetails = userDetailsService.loadUserByUsername(request.getEmail());

        String jwt = jwtUtil.generateToken(userDetails);

        Cookie authCookie = new Cookie("AUTH_TOKEN", jwt);
        authCookie.setHttpOnly(true);
        authCookie.setSecure(false); 
        authCookie.setPath("/");
        authCookie.setMaxAge((int)jwtUtil.getJwtExpiration()/1000); 
        response.addCookie(authCookie);

        return ResponseEntity.ok(Map.of("message", "Connexion réussie"));
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Cet email est déjà utilisé"));
        }

        User newUser = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role("ROLE_USER") 
                .build();

        userRepository.save(newUser);

        return ResponseEntity.ok(Map.of("message", "Utilisateur enregistré avec succès"));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse response) {
        Cookie authCookie = new Cookie("AUTH_TOKEN", null);
        authCookie.setHttpOnly(true);
        authCookie.setPath("/");
        authCookie.setMaxAge(0); 
        response.addCookie(authCookie);

        return ResponseEntity.ok(Map.of("message", "Déconnexion réussie"));
    }
}