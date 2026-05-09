package io.github.lstramke.security;

import java.io.IOException;
import java.util.Collections;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import io.github.lstramke.coincollector.services.SessionManager;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Validates the session cookie for incoming requests.
 * <p>
 * If a request contains a valid sessionId cookie, the filter resolves the
 * current user and stores it in the Spring Security context. Invalid requests
 * are rejected before the controller is reached.
 */
public class SessionFilter extends OncePerRequestFilter {

    private final static Logger logger = LoggerFactory.getLogger(SessionFilter.class);
    private final SessionManager sessionManager;

    public SessionFilter(SessionManager sessionManager){
        this.sessionManager = sessionManager;
    }

    /**
     * Checks the session cookie and sets the authenticated user.
     */
    @Override
    protected void doFilterInternal(
        HttpServletRequest request, 
        HttpServletResponse response, 
        FilterChain filterChain
    ) throws ServletException, IOException {
        logger.debug("Incoming request {} {} from {}", request.getMethod(), request.getRequestURI(), request.getRemoteAddr());

        var cookies = request.getCookies();
        String sessionId = null;
        if (cookies == null) {
            logger.debug("No cookies present on request");
        } else {
            for (var cookie : cookies) {
                if ("sessionId".equals(cookie.getName()) && cookie.getValue() != null && !cookie.getValue().isBlank()) {
                    sessionId = cookie.getValue();
                    logger.debug("Found sessionId cookie: {}", sessionId);
                    break;
                }
            }
        }

        if (sessionId != null && sessionManager.validateSession(sessionId)) {
            var userId = sessionManager.getUserId(sessionId);
            logger.info("Authenticated request for userId={}", userId);
            Authentication auth = new UsernamePasswordAuthenticationToken(userId, null, Collections.emptyList());
            SecurityContextHolder.getContext().setAuthentication(auth);
            try {
                filterChain.doFilter(request, response);
            } finally {
                SecurityContextHolder.clearContext();
                logger.debug("Cleared SecurityContext for request {}");
            }
        } else {
            // Leave unauthenticated requests to the security chain for a proper 401 response.
            logger.debug("No valid session for request {} {} from {}", request.getMethod(), request.getRequestURI(), request.getRemoteAddr());
            filterChain.doFilter(request, response);
        }
    }
}
