package io.github.lstramke.coincollector.services;
import java.util.logging.Logger;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

/**
 * Provides a filter for HTTP handlers to validate user sessions.
 * <p>
 * This class wraps an {@link HttpHandler} and checks for a valid session before allowing access.
 * Unauthorized access attempts are logged and denied with a redirect response.
 */
public class SessionFilter {
    private static final Logger logger = Logger.getLogger(SessionFilter.class.getName());

    /**
     * Wraps an {@link HttpHandler} with session validation logic.
     * If the session is invalid, logs the attempt and sends a redirect response.
     *
     * @param handler The original HTTP handler to wrap
     * @param sessionService The session manager to use for validation
     * @return A new {@link HttpHandler} with session validation
     */
    public static HttpHandler withSessionValidation(HttpHandler handler, SessionManager sessionService) {
        return exchange -> {
            String sessionId = getSessionCookie(exchange);
            if (!sessionService.validateSession(sessionId)) {
                logger.warning("Unauthorized access attempt: SessionId=" + sessionId + ", RemoteAddress=" + exchange.getRemoteAddress());
                exchange.sendResponseHeaders(302, -1);
                return;
            }
            handler.handle(exchange);
        };
    }

    /**
     * Extracts the sessionId from the Cookie header of the HTTP request.
     *
     * @param exchange The HTTP exchange containing the request
     * @return The sessionId if present, otherwise null
     */
    private static String getSessionCookie(HttpExchange exchange) {
        String cookieHeader = exchange.getRequestHeaders().getFirst("Cookie");
        if (cookieHeader != null) {
            for (String cookie : cookieHeader.split(";")) {
                String[] parts = cookie.trim().split("=");
                if (parts.length == 2 && parts[0].equals("sessionId")) {
                    return parts[1];
                }
            }
        }
        return null;
    }
}
