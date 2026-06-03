package com.netflixclone.api.exceptions;

import io.jsonwebtoken.ExpiredJwtException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {


    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatusException(ResponseStatusException ex) {
        return buildErrorResponse(ex.getStatusCode().value(), ex.getReason());
    }


    @ExceptionHandler(org.springframework.security.authentication.BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleBadCredentialsException(Exception ex) {
        return buildErrorResponse(HttpStatus.UNAUTHORIZED.value(), "Email ou mot de passe incorrect.");
    }


    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleHttpMessageNotReadable(Exception ex) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST.value(), "Requête malformée : le format des données est invalide.");
    }


    @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(org.springframework.web.bind.MethodArgumentNotValidException ex) {
    
        String errorMessage = ex.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        return buildErrorResponse(HttpStatus.BAD_REQUEST.value(), errorMessage);
    }


    @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> handleMethodNotSupported(Exception ex) {
        return buildErrorResponse(HttpStatus.METHOD_NOT_ALLOWED.value(), "Méthode HTTP non autorisée pour cette route.");
    }


    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleAllOtherExceptions(Exception ex) {
    
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Une erreur interne inattendue s'est produite.");
    }

    private ResponseEntity<Map<String, Object>> buildErrorResponse(int status, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", status);
        body.put("message", message);
        return ResponseEntity.status(status).body(body);
    }
}