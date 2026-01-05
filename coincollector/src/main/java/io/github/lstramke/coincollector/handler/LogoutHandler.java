package io.github.lstramke.coincollector.handler;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import io.github.lstramke.coincollector.services.SessionFilter;
import io.github.lstramke.coincollector.services.SessionManager;

/**
 * Handler for user logout HTTP requests.
 * Manages session invalidation and removes session cookies.
 * Ensures proper cleanup of user authentication state.
 */
public class LogoutHandler implements HttpHandler {
    
    private final SessionManager sessionManager;
    private final static Logger logger = LoggerFactory.getLogger(LoginHandler.class);

    /**
     * Constructs a new LogoutHandler with required dependencies.
     *
     * @param sessionManager the service for managing user sessions
     */
    public LogoutHandler(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();

        logger.info("Route called: {} {}", method, path);

        switch (method) {
            case "POST" -> handleLogout(exchange);
            default -> {
                exchange.sendResponseHeaders(405, -1);
                exchange.close();
            }
        }
    }

    /**
     * Handles POST requests for user logout.
     * Invalidates the user's session and removes the session cookie.
     * Returns 401 if no valid session cookie is present.
     *
     * @param exchange the HTTP exchange containing request and response information
     * @throws IOException if an I/O error occurs during request handling
     */
    private void handleLogout(HttpExchange exchange) throws IOException {
        String sessionId = SessionFilter.getSessionCookie(exchange);
        if(sessionId != null) {
            sessionManager.invalidateSession(sessionId);
            exchange.getResponseHeaders().add("Set-Cookie", "sessionId=; Path=/; Max-Age=0; HttpOnly; SameSite=Strict");
            exchange.sendResponseHeaders(204, -1);
        } else {
            exchange.sendResponseHeaders(401, -1);
        }
        exchange.close();
    }
}
