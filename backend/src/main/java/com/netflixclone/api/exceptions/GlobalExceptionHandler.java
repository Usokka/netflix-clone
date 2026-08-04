package com.netflixclone.api.exceptions;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
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

    @ExceptionHandler({
            org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class,
            org.springframework.web.bind.MissingRequestHeaderException.class,
            org.springframework.web.bind.MissingServletRequestParameterException.class,
            jakarta.validation.ConstraintViolationException.class
    })
    public ResponseEntity<Map<String, Object>> handleInvalidRequestValue(Exception ex) {
        return buildErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Un paramètre obligatoire est absent ou invalide."
        );
    }

    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    public ResponseEntity<Map<String, Object>> handleResourceNotFound(Exception ex) {
        return buildErrorResponse(HttpStatus.NOT_FOUND.value(), "Route introuvable.");
    }

    @ExceptionHandler(org.springframework.orm.ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<Map<String, Object>> handleOptimisticLockingFailure(Exception ex) {
        return buildErrorResponse(
                HttpStatus.CONFLICT.value(),
                "La ressource a été modifiée par une autre requête. Réessayez."
        );
    }

    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrityViolation(Exception ex) {
        return buildErrorResponse(
                HttpStatus.CONFLICT.value(),
                "Cette opération entre en conflit avec des données existantes."
        );
    }


    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleAllOtherExceptions(Exception ex) {
        log.error("Erreur interne non gérée", ex);
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
