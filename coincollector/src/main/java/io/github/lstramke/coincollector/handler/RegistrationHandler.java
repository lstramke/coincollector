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

import io.github.lstramke.coincollector.model.User;
import io.github.lstramke.coincollector.model.DTOs.Requests.RegistrationRequest;
import io.github.lstramke.coincollector.services.UserStorageService;
import jakarta.validation.Valid;
import io.github.lstramke.coincollector.services.SessionManager;

/**
 * REST controller for user registration.
 *
 * <p>Handles registration requests, creates a new user account and a server-side
 * session, and returns a sessionId cookie. Errors are handled centrally by
 * {@code GlobalExceptionHandler}.</p>
 */
@RestController
@RequestMapping("/api/v1")
public class RegistrationHandler {

    private final UserStorageService userStorageService;
    private final SessionManager sessionManager;
    private final PasswordEncoder passwordEncoder;
    private final static Logger logger = LoggerFactory.getLogger(RegistrationHandler.class);

    /**
     * Creates a new registration controller with the required services.
    *
    * @param userStorageService Service for user persistence
    * @param sessionManager Service for session management
    */
    public RegistrationHandler(UserStorageService userStorageService, SessionManager sessionManager, PasswordEncoder passwordEncoder) {
        this.userStorageService = userStorageService;
        this.sessionManager = sessionManager;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Processes a registration request.
     *
     * <p>Expects a JSON body of type {@link RegistrationRequest}. Creates the
     * user, opens a session and returns a Set-Cookie header with the session id.</p>
     *
     * @param request {@link RegistrationRequest} containing the username
     * @return {@link ResponseEntity} with Set-Cookie header on success
     */
    @PostMapping("/registration")
    public ResponseEntity<?> handleRegistration(@Valid @RequestBody RegistrationRequest request) {
        logger.info("Registration attempt with username: {}", request.username());
        String passwordHash = passwordEncoder.encode(request.password());
        User user = new User(request.username(), passwordHash);
        userStorageService.save(user);
        String sessionId = sessionManager.createSession(user.getId());
        String cookie = ResponseCookie.from("sessionId", sessionId)
            .path("/")
            .httpOnly(true)
            .sameSite("Strict")
            .build().toString();

        return ResponseEntity.status(HttpStatus.CREATED).header(HttpHeaders.SET_COOKIE, cookie).build();
    }
}
