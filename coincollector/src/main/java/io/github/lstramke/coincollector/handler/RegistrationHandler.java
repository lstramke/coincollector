package io.github.lstramke.coincollector.handler;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.net.httpserver.HttpExchange;

import io.github.lstramke.coincollector.exceptions.userExceptions.UserSaveException;
import io.github.lstramke.coincollector.model.User;
import io.github.lstramke.coincollector.model.DTOs.LoginResponse;
import io.github.lstramke.coincollector.model.DTOs.RegistrationRequest;
import io.github.lstramke.coincollector.services.UserStorageService;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

public class RegistrationHandler implements Handler {

    private final UserStorageService userStorageService;
    private final static Logger logger = LoggerFactory.getLogger(LoginHandler.class);

    

    public RegistrationHandler(UserStorageService userStorageService) {
        this.userStorageService = userStorageService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();
        
        logger.info("Route called: {} {}", method, path);
        switch (method) {
            case "POST":
                handleRegistration(exchange);
                break;
            default:
                exchange.sendResponseHeaders(405, -1);
                break;
        }
    }

    private void handleRegistration(HttpExchange exchange) throws IOException {
        ObjectMapper mapper = new JsonMapper();
        String body = new String(exchange.getRequestBody().readAllBytes());

        RegistrationRequest registrationRequest = mapper.readValue(body, RegistrationRequest.class);
        
        try {
            User user = new User(registrationRequest.username());
            userStorageService.save(user);

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
            
        } catch (UserSaveException e) {
            exchange.sendResponseHeaders(400, 0);
            exchange.getResponseBody().write("{\"error\":\"Request is not valid\"}".getBytes());
        } finally {
            exchange.getResponseBody().close();
        }
    }
}
