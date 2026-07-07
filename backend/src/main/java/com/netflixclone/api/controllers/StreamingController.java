package com.netflixclone.api.controllers;

import com.netflixclone.api.services.StreamingService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/stream")
@RequiredArgsConstructor
public class StreamingController {

    private final StreamingService streamingService;

    @GetMapping("/{movieId}/ticket")
    public ResponseEntity<Map<String, String>> getStreamingTicket(
            @PathVariable String movieId,
            HttpServletRequest request,
            @AuthenticationPrincipal UserDetails userDetails) { // Injection du contexte de sécurité

        String clientIp = request.getHeader("X-Forwarded-For");
        if (clientIp == null || clientIp.isBlank() || "unknown".equalsIgnoreCase(clientIp))
            clientIp = request.getRemoteAddr();
        if ("0:0:0:0:0:0:0:1".equals(clientIp))
            clientIp = "127.0.0.1";

        // On transmet l'email (username) au service
        String ticket = streamingService.generateStreamingTicket(movieId, clientIp, userDetails.getUsername());
        
        return ResponseEntity.ok(Map.of("ticket", ticket));
    }
}