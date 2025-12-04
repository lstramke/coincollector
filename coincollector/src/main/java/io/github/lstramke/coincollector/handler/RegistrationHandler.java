package io.github.lstramke.coincollector.handler;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import io.github.lstramke.coincollector.exceptions.userExceptions.UserSaveException;
import io.github.lstramke.coincollector.model.User;
import io.github.lstramke.coincollector.model.DTOs.Requests.RegistrationRequest;
import io.github.lstramke.coincollector.services.UserStorageService;
import io.github.lstramke.coincollector.services.SessionManager;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

public class RegistrationHandler implements HttpHandler {

    private final UserStorageService userStorageService;
    private final SessionManager sessionManager;
    private final static Logger logger = LoggerFactory.getLogger(RegistrationHandler.class);

    

    public RegistrationHandler(UserStorageService userStorageService, SessionManager sessionManager) {
        this.userStorageService = userStorageService;
        this.sessionManager = sessionManager;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();
        
        logger.info("Route called: {} {}", method, path);
        switch (method) {
            case "POST" -> handleRegistration(exchange);
            default -> exchange.sendResponseHeaders(405, -1);
        }
    }

    private void handleRegistration(HttpExchange exchange) throws IOException {
        ObjectMapper mapper = new JsonMapper();
        String body = new String(exchange.getRequestBody().readAllBytes());

        
        try {
            var registrationRequest = mapper.readValue(body, RegistrationRequest.class);
            User user = new User(registrationRequest.username());
            userStorageService.save(user);

            String sessionId = sessionManager.createSession(user.getId());
            exchange.getResponseHeaders().add(
                "Set-Cookie", 
                "sessionId=" + sessionId + "; Path=/; HttpOnly; SameSite=Strict"
            );

            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(201, 0);
            exchange.getResponseBody().close();
            
        } catch (JacksonException e) {
            exchange.sendResponseHeaders(400, 0);
            exchange.getResponseBody().write("{\"error\":\"Request is not valid\"}".getBytes());
        } catch (UserSaveException e) {
            exchange.sendResponseHeaders(500, 0);
            exchange.getResponseBody().write("{\"error\":\"An unexpected error occurred\"}".getBytes());
        } finally {
            exchange.getResponseBody().close();
        }
    }
}
