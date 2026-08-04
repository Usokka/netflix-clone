package com.netflixclone.api.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import jakarta.annotation.PostConstruct;
import lombok.Getter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtUtil {

    private static final String TOKEN_TYPE_CLAIM = "token_type";
    private static final String TOKEN_ISSUER = "netflix-backend";

    @Value("${spring.jwt.private-key-path}")
    private String privateKeyPath;

    @Value("${spring.jwt.public-key-path}")
    private String publicKeyPath;

    @Getter
    @Value("${spring.jwt.access-expiration}")
    private long jwtExpiration;

    @Getter
    @Value("${spring.jwt.refresh-expiration}")
    private long refreshExpiration;

    private PrivateKey privateKey;
    private PublicKey publicKey;

    @PostConstruct
    public void initKeys() {
        try {
            this.privateKey = loadPrivateKey(privateKeyPath);
            this.publicKey = loadPublicKey(publicKeyPath);
        } catch (Exception exception) {
            throw new IllegalStateException("Impossible de charger les clés RSA JWT", exception);
        }
    }

    public String generateToken(UserDetails userDetails) {
        return generateToken(userDetails, jwtExpiration, TokenType.ACCESS);
    }

    public String generateRefreshToken(UserDetails userDetails) {
        return generateToken(userDetails, refreshExpiration, TokenType.REFRESH);
    }

    public String extractAccessTokenUsername(String token) {
        return extractUsername(token, TokenType.ACCESS);
    }

    public String extractRefreshTokenUsername(String token) {
        return extractUsername(token, TokenType.REFRESH);
    }

    public RefreshTokenDetails parseRefreshToken(String token) {
        Claims claims = extractAllClaims(token);
        validateTokenType(claims, TokenType.REFRESH);

        String tokenId = claims.getId();
        if (tokenId == null || tokenId.isBlank()) {
            throw new JwtException("Identifiant de refresh token manquant");
        }

        return new RefreshTokenDetails(
                claims.getSubject(),
                tokenId,
                claims.getExpiration().toInstant()
        );
    }

    private String generateToken(UserDetails userDetails, long expirationMillis, TokenType tokenType) {
        long nowMillis = System.currentTimeMillis();

        return Jwts.builder()
                .subject(userDetails.getUsername())
                .issuer(TOKEN_ISSUER)
                .id(UUID.randomUUID().toString())
                .issuedAt(new Date(nowMillis))
                .expiration(new Date(nowMillis + expirationMillis))
                .claim(TOKEN_TYPE_CLAIM, tokenType.claimValue)
                .signWith(privateKey, Jwts.SIG.RS256)
                .compact();
    }

    private String extractUsername(String token, TokenType expectedType) {
        Claims claims = extractAllClaims(token);
        validateTokenType(claims, expectedType);
        return claims.getSubject();
    }

    private void validateTokenType(Claims claims, TokenType expectedType) {
        String tokenType = claims.get(TOKEN_TYPE_CLAIM, String.class);
        if (!expectedType.claimValue.equals(tokenType)) {
            throw new JwtException("Type de jeton invalide");
        }
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .requireIssuer(TOKEN_ISSUER)
                .verifyWith(publicKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private PrivateKey loadPrivateKey(String path) throws Exception {
        String key = new String(Files.readAllBytes(Paths.get(path)))
                .replaceAll("-----[A-Z ]+-----", "")
                .replaceAll("\\s", "");
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(Base64.getDecoder().decode(key));
        return KeyFactory.getInstance("RSA").generatePrivate(keySpec);
    }

    private PublicKey loadPublicKey(String path) throws Exception {
        String key = new String(Files.readAllBytes(Paths.get(path)))
                .replaceAll("-----[A-Z ]+-----", "")
                .replaceAll("\\s", "");
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(Base64.getDecoder().decode(key));
        return KeyFactory.getInstance("RSA").generatePublic(keySpec);
    }

    private enum TokenType {
        ACCESS("access"),
        REFRESH("refresh");

        private final String claimValue;

        TokenType(String claimValue) {
            this.claimValue = claimValue;
        }
    }

    public record RefreshTokenDetails(String email, String tokenId, Instant expiresAt) {
    }
}
