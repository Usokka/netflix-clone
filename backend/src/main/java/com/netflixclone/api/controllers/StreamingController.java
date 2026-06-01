package com.netflixclone.api.controllers;

import com.netflixclone.api.services.StreamingService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/stream")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class StreamingController {

    private final StreamingService streamingService;

    @GetMapping("/{movieId}/ticket")
    public ResponseEntity<Map<String, String>> getStreamingTicket(
            @PathVariable String movieId,
            HttpServletRequest request) {
        
        String clientIp = request.getHeader("X-Forwarded-For");
        if (clientIp == null || clientIp.isEmpty() || "unknown".equalsIgnoreCase(clientIp)) {
            clientIp = request.getRemoteAddr();
        }

        if ("0:0:0:0:0:0:0:1".equals(clientIp) || "127.0.0.1".equals(clientIp)) {
            clientIp = "127.0.0.1";
        }

        String ticket = streamingService.generateStreamingTicket(movieId, clientIp);
        
        return ResponseEntity.ok(Map.of("ticket", ticket));
    }
}