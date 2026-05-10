package io.github.lstramke.coincollector.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import io.github.lstramke.coincollector.services.SessionManager;

/**
 * Handles user logout REST endpoints.
 */
@RestController
@RequestMapping("/api/v1")
public class LogoutHandler {
    
    private final static Logger logger = LoggerFactory.getLogger(LogoutHandler.class);
    private final SessionManager sessionManager;

    @Autowired
    public LogoutHandler(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    /**
     * Logs out the authenticated user and invalidates the session.
     *
     * @param authentication the authenticated user
     * @return no content response
     * @throws ResponseStatusException if user is not authenticated
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(Authentication authentication) {
        logger.debug("Logout requested");
        
        if (authentication == null || !authentication.isAuthenticated()) {
            logger.debug("Logout aborted: no authenticated user");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }

        String userId = authentication.getPrincipal().toString();
        String sessionId = sessionManager.getSessionId(userId);

        if (sessionId == null) {
            logger.debug("Logout aborted: no session found for userId={}", userId);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        
        try {
            sessionManager.invalidateSession(sessionId);
            logger.info("User logged out: userId={}, sessionId={}", userId, sessionId);
            String cookie = ResponseCookie.from("sessionId", "")
            .path("/")
            .httpOnly(true)
            .sameSite("Strict")
            .maxAge(0)
            .build()
            .toString();
            return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, cookie).build();
        } catch (Exception e) {
            logger.warn("Session invalidation failed: userId={}, sessionId={}", userId, sessionId);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error", e);
        }
    }
}
