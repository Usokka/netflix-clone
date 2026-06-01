package com.netflixclone.api.services;

import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.Date;

@Service
public class StreamingService {

    @Value("${jwt.private.key.path:/app/secrets/private.pem}")
    private String privateKeyPath;

    public String generateStreamingTicket(String movieId, String clientIp) {
        try {
            PrivateKey privateKey = loadPrivateKey(privateKeyPath);
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

        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la génération du ticket cryptographique", e);
        }
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