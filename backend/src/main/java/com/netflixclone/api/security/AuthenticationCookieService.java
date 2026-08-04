package com.netflixclone.api.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class AuthenticationCookieService {

    public static final String ACCESS_TOKEN_COOKIE = "AUTH_TOKEN";
    public static final String REFRESH_TOKEN_COOKIE = "REFRESH_TOKEN";

    private static final String ACCESS_TOKEN_PATH = "/";
    private static final String REFRESH_TOKEN_PATH = "/";
    private static final String SAME_SITE_POLICY = "Lax";

    private final JwtUtil jwtUtil;
    private final boolean secureCookies;

    public AuthenticationCookieService(
            JwtUtil jwtUtil,
            @Value("${app.security.cookies.secure:true}") boolean secureCookies
    ) {
        this.jwtUtil = jwtUtil;
        this.secureCookies = secureCookies;
    }

    public void setAuthenticationCookies(
            HttpServletResponse response,
            String accessToken,
            String refreshToken
    ) {
        addCookie(
                response,
                ACCESS_TOKEN_COOKIE,
                accessToken,
                ACCESS_TOKEN_PATH,
                Duration.ofMillis(jwtUtil.getJwtExpiration())
        );
        addCookie(
                response,
                REFRESH_TOKEN_COOKIE,
                refreshToken,
                REFRESH_TOKEN_PATH,
                Duration.ofMillis(jwtUtil.getRefreshExpiration())
        );
    }

    public void clearAuthenticationCookies(HttpServletResponse response) {
        addCookie(response, ACCESS_TOKEN_COOKIE, "", ACCESS_TOKEN_PATH, Duration.ZERO);
        addCookie(response, REFRESH_TOKEN_COOKIE, "", REFRESH_TOKEN_PATH, Duration.ZERO);
    }

    private void addCookie(
            HttpServletResponse response,
            String name,
            String value,
            String path,
            Duration maxAge
    ) {
        ResponseCookie cookie = ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(secureCookies)
                .sameSite(SAME_SITE_POLICY)
                .path(path)
                .maxAge(maxAge)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
