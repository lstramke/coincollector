package io.github.lstramke.coincollector.handler;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import io.github.lstramke.coincollector.exceptions.userExceptions.UserNotFoundException;
import io.github.lstramke.coincollector.model.User;
import io.github.lstramke.coincollector.model.DTOs.Requests.LoginRequest;
import io.github.lstramke.coincollector.services.UserStorageService;
import io.github.lstramke.coincollector.services.SessionManager;

public class LoginHandler implements HttpHandler {

    private final UserStorageService userStorageService;
    private final SessionManager sessionManager;
    private final ObjectMapper mapper;
    private final static Logger logger = LoggerFactory.getLogger(LoginHandler.class);
    
    public LoginHandler(UserStorageService userStorageService, SessionManager sessionManager, ObjectMapper mapper) {
        this.userStorageService = userStorageService;
        this.sessionManager = sessionManager;
        this.mapper = mapper;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();
        
        logger.info("Route called: {} {}", method, path);
        switch (method) {
            case "POST" -> handleLogin(exchange);
            default -> {
                exchange.sendResponseHeaders(405, -1);
                exchange.close();
            }
        }
    }

    private void handleLogin(HttpExchange exchange) throws IOException{
        String body = new String(exchange.getRequestBody().readAllBytes());

        try {
            LoginRequest loginRequest = mapper.readValue(body, LoginRequest.class);
            
            User user = userStorageService.getByUsername(loginRequest.username());
            String sessionId = sessionManager.createSession(user.getId());

            exchange.getResponseHeaders().add(
                "Set-Cookie", 
                "sessionId=" + sessionId + "; Path=/; HttpOnly; SameSite=Strict"
            );

            exchange.sendResponseHeaders(200, 0);
            exchange.close();
        } catch (JacksonException e) {
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(400, 0);
            exchange.getResponseBody().write("{\"error\":\"Request is not valid\"}".getBytes());
            exchange.close();
        } catch (UserNotFoundException e) {
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(400, 0);
            exchange.getResponseBody().write("{\"error\":\"Request is not valid\"}".getBytes());
            exchange.close();
        }
    }
    
}
