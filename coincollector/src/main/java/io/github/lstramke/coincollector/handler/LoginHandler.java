package io.github.lstramke.coincollector.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import io.github.lstramke.coincollector.model.User;
import io.github.lstramke.coincollector.model.DTOs.Requests.LoginRequest;
import io.github.lstramke.coincollector.services.UserStorageService;
import io.github.lstramke.coincollector.services.SessionManager;
import jakarta.validation.Valid;

/**
 * REST controller for user login.
 *
 * <p>Handles login requests, creates a session via the {@code SessionManager}
 * and returns a sessionId cookie. Errors (e.g. invalid JSON or user not found)
 * are handled centrally by {@code GlobalExceptionHandler}.</p>
 */
@RestController
@RequestMapping("/api/v1")
public class LoginHandler {

    private final UserStorageService userStorageService;
    private final SessionManager sessionManager;
    private final PasswordEncoder passwordEncoder;
    private final static Logger logger = LoggerFactory.getLogger(LoginHandler.class);
    
    /**
     * Creates a new login controller with the required services.
    *
    * @param userStorageService Service for user access
    * @param sessionManager Service for session management
    */
    public LoginHandler(UserStorageService userStorageService, SessionManager sessionManager, PasswordEncoder passwordEncoder) {
        this.userStorageService = userStorageService;
        this.sessionManager = sessionManager;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Processes a login request.
     *
     * <p>Expects a JSON body of type {@link LoginRequest}. On successful
     * authentication a server-side session is created and an HttpOnly cookie is
     * returned.</p>
     *
     * @param request {@link LoginRequest} containing the username and password
     * @return {@link ResponseEntity} with a Set-Cookie header on success
     */
    @PostMapping("/login")
    public ResponseEntity<?> handleLogin(@Valid @RequestBody LoginRequest request) {
        logger.info("Login attempt for {}", request.username());

        User user = userStorageService.getByUsername(request.username());
        if(!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }
        
        String sessionId = sessionManager.createSession(user.getId());
        String cookie = ResponseCookie.from("sessionId", sessionId)
            .path("/")
            .httpOnly(true)
            .sameSite("Strict")
            .build().toString();

        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookie).build();
    }
}
