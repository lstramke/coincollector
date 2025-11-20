package io.github.lstramke.coincollector.handler;

import java.io.IOException;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.net.httpserver.HttpExchange;

import io.github.lstramke.coincollector.services.EuroCoinCollectionGroupStorageService;

public class GroupHandler implements Handler {

    private final EuroCoinCollectionGroupStorageService groupStorageService;
    private final static Logger logger = LoggerFactory.getLogger(GroupHandler.class);

    
    public GroupHandler(EuroCoinCollectionGroupStorageService groupStorageService) {
        this.groupStorageService = groupStorageService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();

        logger.info("Route called: {} {}", method, path);
        switch (method) {
            case "GET" -> {
                if (path.equals("/api/groups")) {
                    handleGetAll(exchange);
                } else if(isGroupIdPath(path)) {
                    handleGetWithId(exchange);
                } else {
                    exchange.sendResponseHeaders(405, -1);
                }
            }
            case "POST" -> handleCreate(exchange);
            case "PATCH" -> handleUpdate(exchange);
            case "DELETE" -> handleDelete(exchange);
            default -> {
               exchange.sendResponseHeaders(405, -1);
            }
        }
    }

    private void handleGetAll(HttpExchange exchange) throws IOException{

    }

    private void handleCreate(HttpExchange exchange) throws IOException{

    }

    private void handleGetWithId(HttpExchange exchange) throws IOException {

    }

    private void handleUpdate(HttpExchange exchange) throws IOException {

    }

    private void handleDelete(HttpExchange exchange) throws IOException{

    }

    private boolean isGroupIdPath(String path) {
        String prefix = "/api/groups/";
        if (path.startsWith(prefix)) {
            String id = path.substring(prefix.length());
            try {
                UUID.fromString(id);
                return true;
            } catch (IllegalArgumentException e) {
                return false;
            }
        }
        return false;
    }
    
}
