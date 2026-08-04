package com.netflixclone.api.controllers;

import com.netflixclone.api.services.StreamingService;
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

    @GetMapping("/{videoFolder}/ticket")
    public ResponseEntity<Map<String, String>> getStreamingTicket(
            @PathVariable String videoFolder,
            @AuthenticationPrincipal UserDetails userDetails) {
        String ticket = streamingService.generateStreamingTicket(videoFolder, userDetails.getUsername());
        
        return ResponseEntity.ok(Map.of("ticket", ticket));
    }
}
