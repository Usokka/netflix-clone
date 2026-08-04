package com.netflixclone.api.security;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthenticationCookieServiceTest {

    @Test
    void setAuthenticationCookiesUsesSecureHttpOnlySameSiteCookies() {
        JwtUtil jwtUtil = mock(JwtUtil.class);
        when(jwtUtil.getJwtExpiration()).thenReturn(900_000L);
        when(jwtUtil.getRefreshExpiration()).thenReturn(604_800_000L);
        AuthenticationCookieService cookieService = new AuthenticationCookieService(jwtUtil, true);
        MockHttpServletResponse response = new MockHttpServletResponse();

        cookieService.setAuthenticationCookies(response, "access-token", "refresh-token");

        Collection<String> cookies = response.getHeaders(HttpHeaders.SET_COOKIE);
        assertEquals(2, cookies.size());
        assertTrue(cookies.stream().allMatch(cookie -> cookie.contains("HttpOnly")));
        assertTrue(cookies.stream().allMatch(cookie -> cookie.contains("Secure")));
        assertTrue(cookies.stream().allMatch(cookie -> cookie.contains("SameSite=Lax")));
        assertTrue(cookies.stream().anyMatch(cookie -> cookie.contains("AUTH_TOKEN=access-token")));
        assertTrue(cookies.stream().anyMatch(cookie -> cookie.contains("REFRESH_TOKEN=refresh-token")));
    }

    @Test
    void clearAuthenticationCookiesExpiresAccessAndRefreshCookiesAtTheirOriginalPaths() {
        AuthenticationCookieService cookieService = new AuthenticationCookieService(mock(JwtUtil.class), false);
        MockHttpServletResponse response = new MockHttpServletResponse();

        cookieService.clearAuthenticationCookies(response);

        Collection<String> cookies = response.getHeaders(HttpHeaders.SET_COOKIE);
        assertEquals(2, cookies.size());
        assertTrue(cookies.stream().allMatch(cookie -> cookie.contains("Max-Age=0")));
        assertTrue(cookies.stream().anyMatch(cookie -> cookie.contains("AUTH_TOKEN=") && cookie.contains("Path=/")));
        assertTrue(cookies.stream().anyMatch(cookie -> cookie.contains("REFRESH_TOKEN=")
                && cookie.contains("Path=/")));
    }
}
