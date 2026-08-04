package com.netflixclone.api.controllers;

import com.netflixclone.api.dtos.LoginRequest;
import com.netflixclone.api.dtos.RegisterRequest;
import com.netflixclone.api.models.User;
import com.netflixclone.api.repositories.UserRepository;
import com.netflixclone.api.security.AuthenticationCookieService;
import com.netflixclone.api.security.JwtUtil;
import com.netflixclone.api.security.RefreshTokenService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationCookieService authenticationCookieService;
    private final RefreshTokenService refreshTokenService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        String normalizedEmail = normalizeEmail(request.getEmail());
        validateBcryptPasswordLength(request.getPassword());
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(normalizedEmail, request.getPassword()));

        UserDetails userDetails = userDetailsService.loadUserByUsername(normalizedEmail);

        String jwt = jwtUtil.generateToken(userDetails);
        String refreshJwt = jwtUtil.generateRefreshToken(userDetails);
        refreshTokenService.register(refreshJwt);
        authenticationCookieService.setAuthenticationCookies(response, jwt, refreshJwt);

        return ResponseEntity.ok(Map.of("message", "Connexion réussie"));
    }

    @GetMapping("/csrf")
    public ResponseEntity<Map<String, String>> csrf(CsrfToken csrfToken) {
        return ResponseEntity.ok(Map.of(
                "headerName", csrfToken.getHeaderName(),
                "token", csrfToken.getToken()
        ));
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(HttpServletRequest request, HttpServletResponse response) {
        if (request.getCookies() == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Pas de refresh token"));

        String refreshToken = getCookieValue(request, AuthenticationCookieService.REFRESH_TOKEN_COOKIE);

        if (refreshToken == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Refresh token manquant"));

        try {
            String email = refreshTokenService.consume(refreshToken);
            UserDetails userDetails = userDetailsService.loadUserByUsername(email);

            String newJwt = jwtUtil.generateToken(userDetails);
            String newRefreshJwt = jwtUtil.generateRefreshToken(userDetails);
            refreshTokenService.register(newRefreshJwt);
            authenticationCookieService.setAuthenticationCookies(response, newJwt, newRefreshJwt);

            return ResponseEntity.ok(Map.of("message", "Jetons renouvelés"));
        } catch (org.springframework.web.server.ResponseStatusException exception) {
            throw exception;
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Refresh token invalide ou expiré"));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        String normalizedEmail = normalizeEmail(request.getEmail());
        validateBcryptPasswordLength(request.getPassword());
        if (userRepository.findByEmailIgnoreCase(normalizedEmail).isPresent()) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Cet email est déjà utilisé"
            );
        }

        User newUser = User.builder()
                .email(normalizedEmail)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role("ROLE_USER")
                .build();

        try {
            userRepository.saveAndFlush(newUser);
        } catch (DataIntegrityViolationException exception) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Cet email est déjà utilisé",
                    exception
            );
        }

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("message", "Utilisateur enregistré avec succès"));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = getCookieValue(request, AuthenticationCookieService.REFRESH_TOKEN_COOKIE);
        try {
            if (refreshToken != null) {
                refreshTokenService.revoke(refreshToken);
            }
        } catch (JwtException ignored) {
            // Le cookie local doit être supprimé même si son contenu est déjà invalide.
        } finally {
            authenticationCookieService.clearAuthenticationCookies(response);
        }
        return ResponseEntity.ok(Map.of("message", "Déconnexion réussie"));
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Non authentifié"));
        }

        String token = getCookieValue(request, AuthenticationCookieService.ACCESS_TOKEN_COOKIE);

        if (token == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Non authentifié"));
        }

        try {
            String email = jwtUtil.extractAccessTokenUsername(token);
            return ResponseEntity.ok(Map.of("email", email));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Token invalide ou expiré"));
        }
    }

    private String getCookieValue(HttpServletRequest request, String cookieName) {
        if (request.getCookies() == null) {
            return null;
        }

        return Arrays.stream(request.getCookies())
                .filter(cookie -> cookieName.equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private void validateBcryptPasswordLength(String password) {
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Le mot de passe ne peut pas dépasser 72 octets UTF-8"
            );
        }
    }
}
