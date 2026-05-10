package io.github.lstramke.coincollector.security;

import java.io.IOException;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Handles authentication failures for REST endpoints.
 * <p>
 * The class translates Spring Security authentication errors into a small
 * JSON response with HTTP status 401 so the frontend can react in a simple
 * and consistent way.
 */
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint{

    private static final Logger logger = LoggerFactory.getLogger(RestAuthenticationEntryPoint.class);
    
    @Override
    public void commence(
        HttpServletRequest request, 
        HttpServletResponse response,
        AuthenticationException authException
    ) throws IOException, ServletException {
        logger.debug("Unauthorized request: {} {}", request.getMethod(), request.getRequestURI());
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"Unauthorized\"}");
    }
    
}
