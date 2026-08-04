package com.netflixclone.api.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class HealthController {

    private final JdbcTemplate jdbcTemplate;
    private final StringRedisTemplate redisTemplate;

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getHealthStatus() {
        Map<String, String> components = new LinkedHashMap<>();
        components.put("database", databaseIsHealthy() ? "UP" : "DOWN");
        components.put("redis", redisIsHealthy() ? "UP" : "DOWN");

        boolean healthy = components.values().stream().allMatch("UP"::equals);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", healthy ? "UP" : "DOWN");
        body.put("components", components);

        return ResponseEntity.status(healthy ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE).body(body);
    }

    private boolean databaseIsHealthy() {
        try {
            return Integer.valueOf(1).equals(jdbcTemplate.queryForObject("SELECT 1", Integer.class));
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private boolean redisIsHealthy() {
        try {
            Boolean healthy = redisTemplate.execute((RedisCallback<Boolean>) connection ->
                    "PONG".equalsIgnoreCase(connection.ping())
            );
            return Boolean.TRUE.equals(healthy);
        } catch (RuntimeException exception) {
            return false;
        }
    }
}
