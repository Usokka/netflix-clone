package com.netflixclone.api.services;

import io.jsonwebtoken.Jwts;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class StreamingService {

    @Value("${jwt.private.key.path:/app/secrets/private.pem}")
    private String privateKeyPath;

    private final StringRedisTemplate redisTemplate;
    
    private PrivateKey privateKey;

    @PostConstruct
    public void initKey() {
        try {
            this.privateKey = loadPrivateKey(privateKeyPath);
        } catch (Exception e) {
            throw new RuntimeException("Erreur critique : Impossible de charger la clé RSA privée", e);
        }
    }

    public String generateStreamingTicket(String movieId, String clientIp, String userEmail) {
        
        String redisKey = "subscription:active:" + userEmail;
        String isActive = redisTemplate.opsForValue().get(redisKey);
        if (!"true".equalsIgnoreCase(isActive)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Abonnement inactif ou expiré.");
        }

        long nowMillis = System.currentTimeMillis();
        Date now = new Date(nowMillis);
        Date expiry = new Date(nowMillis + 900000); 

        return Jwts.builder()
                .subject("streaming-token")
                .issuer("netflix-backend")
                .issuedAt(now)
                .expiration(expiry)
                .claim("movieId", movieId)
                .claim("ip", clientIp)
                .signWith(privateKey, Jwts.SIG.RS256)
                .compact();
    }

    private PrivateKey loadPrivateKey(String path) throws Exception {
        String keyPEM = new String(Files.readAllBytes(Paths.get(path)))
                .replace("-----BEGIN RSA PRIVATE KEY-----", "")
                .replace("-----END RSA PRIVATE KEY-----", "")
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");

        byte[] encoded = Base64.getDecoder().decode(keyPEM);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encoded);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePrivate(keySpec);
    }
}