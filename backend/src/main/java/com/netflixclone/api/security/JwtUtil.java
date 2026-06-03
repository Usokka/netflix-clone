package com.netflixclone.api.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
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
import java.util.Base64;
import java.util.Date;
import java.util.function.Function;

@Component
public class JwtUtil {

    @Value("${spring.jwt.private-key-path}")
    private String privateKeyPath;

    @Value("${spring.jwt.public-key-path}")
    private String publicKeyPath;

    @Getter
    @Value("${spring.jwt.access-expiration}")
    private long jwtExpiration;

    public String generateToken(UserDetails userDetails) {
        try {
            PrivateKey privateKey = loadPrivateKey(privateKeyPath);
            return Jwts.builder()
                    .subject(userDetails.getUsername())
                    .issuedAt(new Date(System.currentTimeMillis()))
                    .expiration(new Date(System.currentTimeMillis() + jwtExpiration))
                    .signWith(privateKey, Jwts.SIG.RS256)
                    .compact();
        } catch (Exception e) {
            throw new RuntimeException("Erreur de génération JWT", e);
        }
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        try {
            PublicKey publicKey = loadPublicKey(publicKeyPath);
            return Jwts.parser()
                    .verifyWith(publicKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            throw new RuntimeException("Token invalide ou erreur de lecture", e);
        }
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

    public String generateStreamingTicket(String movieId, String clientIp) {
    try {
        PrivateKey privateKey = loadPrivateKey(privateKeyPath);
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject("streaming-token")
                .issuer("netflix-backend")
                .issuedAt(new Date(now))
                .expiration(new Date(now + 900_000))
                .claim("movieId", movieId)
                .claim("ip", clientIp)
                .signWith(privateKey, Jwts.SIG.RS256)
                .compact();
    } catch (Exception e) {
        throw new RuntimeException("Erreur génération ticket streaming", e);
    }
}
}