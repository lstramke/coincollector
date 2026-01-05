package io.github.lstramke.coincollector.services;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

/**
 * Provides a filter for HTTP handlers to validate user sessions.
 * <p>
 * This class wraps an {@link HttpHandler} and checks for a valid session before allowing access.
 * Unauthorized access attempts are logged and denied with a redirect response.
 */
public class SessionFilter {
    private final static Logger logger = LoggerFactory.getLogger(SessionFilter.class);

    /**
     * Wraps an {@link HttpHandler} with session validation logic.
     * If the session is invalid, logs the attempt and sends a redirect response.
     *
     * @param handler The original HTTP handler to wrap
     * @param sessionManager The session manager to use for validation
     * @return A new {@link HttpHandler} with session validation
     */
    public static HttpHandler withSessionValidation(HttpHandler handler, SessionManager sessionManager) {
        return exchange -> {
            String sessionId = getSessionCookie(exchange);
            if (!sessionManager.validateSession(sessionId)) {
                logger.warn("Unauthorized access attempt: SessionId=" + sessionId + ", RemoteAddress=" + exchange.getRemoteAddress());
                exchange.sendResponseHeaders(302, -1);
                exchange.close();
                return;
            }

            String userId = sessionManager.getUserId(sessionId);
            exchange.setAttribute("userId", userId);

            try {
                handler.handle(exchange);
            } catch (IOException | RuntimeException e) {
                logger.error("Exception in handler: {}", e.getMessage(), e); 
                String errorJson = "{\"error\":\"An unexpected error occurred\"}";
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(500, errorJson.length());
                exchange.getResponseBody().write(errorJson.getBytes());
                exchange.close();
            }
        };
    }

    /**
     * Extracts the sessionId from the Cookie header of the HTTP request.
     *
     * @param exchange The HTTP exchange containing the request
     * @return The sessionId if present, otherwise null
     */
    public static String getSessionCookie(HttpExchange exchange) {
        String cookieHeader = exchange.getRequestHeaders().getFirst("Cookie");
        if (cookieHeader != null) {
            for (String cookie : cookieHeader.split(";")) {
                String[] parts = cookie.trim().split("=");
                if (parts.length == 2 && parts[0].equals("sessionId") && !parts[1].isEmpty()) {
                    return parts[1];
                }
            }
        }
        return null;
    }
}
