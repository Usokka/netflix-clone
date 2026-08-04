package com.netflixclone.api.services;

import com.netflixclone.api.models.Movie;
import com.netflixclone.api.repositories.MovieRepository;
import io.jsonwebtoken.Jwts;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class StreamingService {

    private static final Pattern SAFE_VIDEO_FOLDER = Pattern.compile("^[A-Za-z0-9][A-Za-z0-9_-]{0,127}$");

    @Value("${streaming.ticket.private-key-path:/run/secrets/streaming_private_key}")
    private String privateKeyPath;

    private final SubscriptionService subscriptionService;
    private final MovieRepository movieRepository;
    
    private PrivateKey privateKey;

    @PostConstruct
    public void initKey() {
        try {
            this.privateKey = loadPrivateKey(privateKeyPath);
        } catch (Exception e) {
            throw new RuntimeException("Erreur critique : Impossible de charger la clé RSA privée", e);
        }
    }

    public String generateStreamingTicket(String videoFolder, String userEmail) {
        if (!subscriptionService.hasActiveSubscription(userEmail)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Abonnement inactif ou expiré.");
        }

        if (!SAFE_VIDEO_FOLDER.matcher(videoFolder).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dossier vidéo invalide");
        }

        Movie movie = movieRepository.findByVideoFolderUrl(videoFolder)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Film introuvable"));

        long nowMillis = System.currentTimeMillis();
        Date now = new Date(nowMillis);
        long ticketLifetimeSeconds = Math.clamp(
                (long) movie.getDurationSeconds() + TimeUnit.MINUTES.toSeconds(5),
                TimeUnit.MINUTES.toSeconds(15),
                TimeUnit.HOURS.toSeconds(4)
        );
        Date expiry = new Date(nowMillis + TimeUnit.SECONDS.toMillis(ticketLifetimeSeconds));

        return Jwts.builder()
                .subject("streaming-token")
                .issuer("netflix-backend")
                .issuedAt(now)
                .expiration(expiry)
                .claim("movieId", movie.getVideoFolderUrl())
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
