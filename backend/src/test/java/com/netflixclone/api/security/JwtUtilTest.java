package com.netflixclone.api.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtUtilTest {

    private static final String EMAIL = "user@example.com";
    private static final long ACCESS_EXPIRATION = 60_000L;
    private static final long REFRESH_EXPIRATION = 120_000L;

    @TempDir
    Path tempDirectory;

    private JwtUtil jwtUtil;
    private UserDetails userDetails;
    private KeyPair keyPair;
    private Path privateKeyPath;
    private Path publicKeyPath;

    @BeforeEach
    void setUp() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        keyPair = generator.generateKeyPair();

        privateKeyPath = tempDirectory.resolve("private.pem");
        publicKeyPath = tempDirectory.resolve("public.pem");
        Files.writeString(privateKeyPath, toPem(
                "PRIVATE KEY",
                keyPair.getPrivate().getEncoded()
        ));
        Files.writeString(publicKeyPath, toPem(
                "PUBLIC KEY",
                keyPair.getPublic().getEncoded()
        ));

        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "privateKeyPath", privateKeyPath.toString());
        ReflectionTestUtils.setField(jwtUtil, "publicKeyPath", publicKeyPath.toString());
        ReflectionTestUtils.setField(jwtUtil, "jwtExpiration", ACCESS_EXPIRATION);
        ReflectionTestUtils.setField(jwtUtil, "refreshExpiration", REFRESH_EXPIRATION);
        jwtUtil.initKeys();

        userDetails = User.withUsername(EMAIL)
                .password("unused")
                .roles("USER")
                .build();
    }

    @Test
    void accessTokenCannotBeUsedAsRefreshToken() {
        String accessToken = jwtUtil.generateToken(userDetails);

        assertEquals(EMAIL, jwtUtil.extractAccessTokenUsername(accessToken));
        assertThrows(
                JwtException.class,
                () -> jwtUtil.extractRefreshTokenUsername(accessToken)
        );
    }

    @Test
    void refreshTokenCannotBeUsedAsAccessToken() {
        String refreshToken = jwtUtil.generateRefreshToken(userDetails);

        assertEquals(EMAIL, jwtUtil.extractRefreshTokenUsername(refreshToken));
        assertEquals(EMAIL, jwtUtil.parseRefreshToken(refreshToken).email());
        assertThrows(
                JwtException.class,
                () -> jwtUtil.extractAccessTokenUsername(refreshToken)
        );
    }

    @Test
    void tokenDurationsComeFromConfiguration() {
        Claims accessClaims = parse(jwtUtil.generateToken(userDetails));
        Claims refreshClaims = parse(jwtUtil.generateRefreshToken(userDetails));

        assertEquals(
                ACCESS_EXPIRATION,
                accessClaims.getExpiration().getTime() - accessClaims.getIssuedAt().getTime()
        );
        assertEquals(
                REFRESH_EXPIRATION,
                refreshClaims.getExpiration().getTime() - refreshClaims.getIssuedAt().getTime()
        );
    }

    @Test
    void loadedKeysAreReusedAfterInitialization() throws Exception {
        Files.delete(privateKeyPath);
        Files.delete(publicKeyPath);

        String accessToken = jwtUtil.generateToken(userDetails);

        assertEquals(EMAIL, jwtUtil.extractAccessTokenUsername(accessToken));
    }

    @Test
    void tokenFromUnexpectedIssuerIsRejected() {
        String token = Jwts.builder()
                .subject(EMAIL)
                .issuer("another-service")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + ACCESS_EXPIRATION))
                .claim("token_type", "access")
                .signWith(keyPair.getPrivate(), Jwts.SIG.RS256)
                .compact();

        assertThrows(JwtException.class, () -> jwtUtil.extractAccessTokenUsername(token));
    }

    private Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(keyPair.getPublic())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private String toPem(String type, byte[] encodedKey) {
        String encoded = Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.US_ASCII))
                .encodeToString(encodedKey);
        return "-----BEGIN " + type + "-----\n"
                + encoded
                + "\n-----END " + type + "-----\n";
    }
}
