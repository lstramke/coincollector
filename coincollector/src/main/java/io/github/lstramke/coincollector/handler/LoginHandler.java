package io.github.lstramke.coincollector.handler;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import com.sun.net.httpserver.HttpExchange;

import io.github.lstramke.coincollector.exceptions.userExceptions.UserNotFoundException;
import io.github.lstramke.coincollector.model.User;
import io.github.lstramke.coincollector.model.DTOs.LoginRequest;
import io.github.lstramke.coincollector.model.DTOs.LoginResponse;
import io.github.lstramke.coincollector.services.UserStorageService;

public class LoginHandler implements Handler {

    private final UserStorageService userStorageService;
    private final static Logger logger = LoggerFactory.getLogger(LoginHandler.class);
    
    public LoginHandler(UserStorageService userStorageService) {
        this.userStorageService = userStorageService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();
        
        logger.info("Route called: {} {}", method, path);
        switch (method) {
            case "POST" -> handleLogin(exchange);
            default -> exchange.sendResponseHeaders(405, -1);
        }
    }

    private void handleLogin(HttpExchange exchange) throws IOException{
        ObjectMapper mapper = new JsonMapper();
        String body = new String(exchange.getRequestBody().readAllBytes());

        LoginRequest loginRequest = mapper.readValue(body, LoginRequest.class);

        try {
            User user = userStorageService.getByUsername(loginRequest.username());
            String sessionId = java.util.UUID.randomUUID().toString();

            exchange.getResponseHeaders().add(
                "Set-Cookie", 
                "sessionId=" + sessionId + "; Path=/; HttpOnly"
            );

            LoginResponse response = new LoginResponse(user.getId());
            String responseJson = mapper.writeValueAsString(response);

            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, responseJson.getBytes().length);
            exchange.getResponseBody().write(responseJson.getBytes());
            
        } catch (UserNotFoundException e) {
            exchange.sendResponseHeaders(400, 0);
            exchange.getResponseBody().write("{\"error\":\"Request is not valid\"}".getBytes());
        } finally {
            exchange.getResponseBody().close();
        }
    }
    
}
