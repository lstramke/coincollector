package io.github.lstramke.coincollector.handler;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import io.github.lstramke.coincollector.exceptions.userExceptions.UserNotFoundException;
import io.github.lstramke.coincollector.exceptions.userExceptions.UserSaveException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler({ 
        HttpMessageNotReadableException.class, 
        UserNotFoundException.class 
    })
    public ResponseEntity<Map<String, String>> handleInvalidRequests(Exception e) {
        logger.warn("Invalid request: {}", e.getMessage());
        return ResponseEntity.badRequest()
            .body(Map.of("error", "Request is not valid"));
    }

    @ExceptionHandler(UserSaveException.class)
    public ResponseEntity<Map<String, String>> handleUserSave(UserSaveException e) {
        logger.error("User save failed: {}", e.getMessage(), e);
        return ResponseEntity.internalServerError()
            .body(Map.of("error", "An unexpected error occurred"));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String, String>> handleMethodNotAllowed(HttpRequestMethodNotSupportedException e) {
        logger.warn("Method not allowed: {}", e.getMessage());
        return ResponseEntity.status(405)
            .body(Map.of("error", "Method is not allowed"));
    }

    @ExceptionHandler({ 
        NoHandlerFoundException.class, 
        NoResourceFoundException.class 
    })
    public ResponseEntity<Map<String, String>> handleNotFound(Exception e) {
        logger.warn("Endpoint not found: {}", e.getMessage());
        return ResponseEntity.status(404)
            .body(Map.of("error", "Endpoint not found"));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> handleResponseStatus(ResponseStatusException e) {
        logger.warn("Response status exception: status={}, reason={}", e.getStatusCode(), e.getReason());
        return ResponseEntity.status(e.getStatusCode())
            .body(Map.of("error", e.getReason()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleAll(Exception e) {
        logger.error("Unhandled exception", e);
        return ResponseEntity.internalServerError()
            .body(Map.of("error", "Internal server error"));
    }
}
